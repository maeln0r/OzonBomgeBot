package ru.maelnor.ozonbomgebot.bot.service.integration.ozon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@ConditionalOnExpression("'${logging.level.root:INFO}'.equalsIgnoreCase('DEBUG')")
public class OzonPriceServiceWithDebug implements DisposableBean, OzonPriceService {

    /* ===================== Settings & constants ===================== */
    private static final boolean DEBUG_CHALLENGE = true;
    private static final java.nio.file.Path TRACE_PATH =
            java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "ozon-challenge-trace.zip");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration COMPOSER_TIMEOUT = Duration.ofSeconds(15);

    /* ===================== Deps ===================== */
    private final OzonProductParser parser;

    public OzonPriceServiceWithDebug(OzonProductParser parser) {
        this.parser = parser;
    }

    /* ===================== HTTP clients & cookies ===================== */
    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    private final HttpClient httpNoRedirect = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .cookieHandler(cookieManager)
            .build();

    private final ObjectMapper om = new ObjectMapper();

    /* ===================== Playwright ===================== */
    private Playwright pw;
    private Browser browser;
    private BrowserContext ctx;

    private void ensureBrowser() {
        // Лениво инициализируем Playwright/Chromium/Context один раз на процесс
        if (pw != null) return;
        pw = Playwright.create();
        browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage"))
        );
        ctx = browser.newContext(new Browser.NewContextOptions()
                .setRecordHarPath(java.nio.file.Paths.get("./data/tmp/ozon.har"))
                .setRecordHarOmitContent(false)
                .setLocale("ru-RU")
                .setUserAgent(USER_AGENT)
                .setViewportSize(1280, 900)
        );
        ctx.setExtraHTTPHeaders(Map.of("Origin", "https://api.ozon.ru", "Referer", "https://www.ozon.ru/"));
        ctx.addInitScript("""
                  Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                  Object.defineProperty(navigator, 'languages', {get: () => ['ru-RU','ru','en-US','en']});
                  Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3]});
                  const getParameter = WebGLRenderingContext.prototype.getParameter;
                  WebGLRenderingContext.prototype.getParameter = function(p) {
                    if (p === 37445) return 'Intel Inc.';
                    if (p === 37446) return 'Intel Iris OpenGL';
                    return getParameter.call(this, p);
                  };
                """);

        ctx.addInitScript("""
                  (() => {
                    const proto = Object.getPrototypeOf(navigator);
                
                    // navigator.platform -> "Win32"
                    try {
                      Object.defineProperty(proto, 'platform', {
                        get() { return 'Win32'; },
                        configurable: true
                      });
                    } catch (e) {}
                
                    // navigator.webdriver -> undefined
                    try {
                      Object.defineProperty(proto, 'webdriver', {
                        get() { return undefined; },
                        configurable: true
                      });
                    } catch (e) {}
                
                    // Приводим userAgentData к Windows, если он есть
                    try {
                      if ('userAgentData' in navigator) {
                        const brands = [
                          { brand: 'Chromium', version: '126' },
                          { brand: 'Not(A:Brand', version: '24' },
                          { brand: 'Google Chrome', version: '126' }
                        ];
                        const uach = {
                          brands,
                          mobile: false,
                          platform: 'Windows'
                        };
                        const hi = {
                          architecture: 'x86',
                          bitness: '64',
                          model: '',
                          platform: 'Windows',
                          platformVersion: '15.0.0',
                          uaFullVersion: '126.0.0.0',
                          fullVersionList: brands.map(b => ({ brand: b.brand, version: b.version })),
                          wow64: false
                        };
                
                        // подменяем объект с минимальной совместимостью
                        Object.defineProperty(navigator, 'userAgentData', {
                          get() {
                            return {
                              get brands() { return uach.brands; },
                              get mobile() { return uach.mobile; },
                              get platform() { return uach.platform; },
                              getHighEntropyValues: async (hints) => {
                                const out = {};
                                for (const k of hints || []) if (k in hi) out[k] = hi[k];
                                return out;
                              },
                              toJSON: () => uach
                            };
                          },
                          configurable: true
                        });
                      }
                    } catch (e) {}
                  })();
                """);

        ctx.addInitScript("""
                (() => {
                  const send = (what, detail) => {
                    try { window.jsProbe(what, String(detail ?? '')); } catch (_) {}
                  };
                
                  // helper'ы
                  const wrapGetter = (obj, prop, name=prop) => {
                    const d = Object.getOwnPropertyDescriptor(obj, prop);
                    if (!d || !d.get) return;
                    Object.defineProperty(obj, prop, {
                      configurable: true, enumerable: d.enumerable,
                      get() { const v = d.get.call(this); send('get:' + name, v); return v; }
                    });
                  };
                  const wrapFn = (obj, prop, name=prop) => {
                    const orig = obj[prop]; if (typeof orig !== 'function') return;
                    obj[prop] = function(...args) { send('call:' + name, JSON.stringify(args)); return orig.apply(this, args); };
                  };
                
                  // Navigator / UA / Client Hints
                  wrapGetter(Navigator.prototype, 'webdriver', 'navigator.webdriver');
                  wrapGetter(Navigator.prototype, 'languages', 'navigator.languages');
                  wrapGetter(Navigator.prototype, 'platform', 'navigator.platform');
                  wrapGetter(Navigator.prototype, 'hardwareConcurrency', 'navigator.hardwareConcurrency');
                  wrapGetter(Navigator.prototype, 'deviceMemory', 'navigator.deviceMemory');
                  wrapGetter(Navigator.prototype, 'userAgent', 'navigator.userAgent');
                
                  if ('userAgentData' in Navigator.prototype) {
                    wrapGetter(Navigator.prototype, 'userAgentData', 'navigator.userAgentData');
                    const uad = Navigator.prototype.userAgentData;
                    if (uad) {
                      wrapFn(uad.__proto__, 'getHighEntropyValues', 'uaData.getHighEntropyValues');
                      wrapGetter(uad.__proto__, 'platform', 'uaData.platform');
                      wrapGetter(uad.__proto__, 'mobile', 'uaData.mobile');
                      wrapGetter(uad.__proto__, 'brands', 'uaData.brands');
                    }
                  }
                
                  // Screen / Window
                  wrapGetter(Screen.prototype, 'colorDepth', 'screen.colorDepth');
                  wrapGetter(Screen.prototype, 'pixelDepth', 'screen.pixelDepth');
                  wrapGetter(Screen.prototype, 'width', 'screen.width');
                  wrapGetter(Screen.prototype, 'height', 'screen.height');
                  wrapGetter(window, 'devicePixelRatio', 'devicePixelRatio');
                  wrapGetter(Performance.prototype, 'memory', 'performance.memory');
                
                  // Intl / Timezone / Locale
                  const _tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
                  send('intl.timeZone', _tz);
                  wrapFn(Intl.DateTimeFormat.prototype, 'resolvedOptions', 'Intl.DateTimeFormat.resolvedOptions');
                
                  // Permissions API
                  if (window.navigator.permissions) {
                    const origQuery = window.navigator.permissions.query.bind(window.navigator.permissions);
                    window.navigator.permissions.query = async function(p) {
                      send('permissions.query', p && p.name);
                      return origQuery(p);
                    };
                  }
                
                  // Canvas / Fonts
                  if (HTMLCanvasElement) {
                    wrapFn(HTMLCanvasElement.prototype, 'toDataURL', 'canvas.toDataURL');
                    wrapFn(HTMLCanvasElement.prototype, 'toBlob', 'canvas.toBlob');
                    if (CanvasRenderingContext2D) {
                      wrapFn(CanvasRenderingContext2D.prototype, 'getImageData', 'canvas2d.getImageData');
                      wrapFn(CanvasRenderingContext2D.prototype, 'measureText', 'canvas2d.measureText');
                    }
                    document.fonts && wrapFn(FontFaceSet.prototype, 'check', 'document.fonts.check');
                  }
                
                  // WebGL
                  const glNames = ['WebGLRenderingContext','WebGL2RenderingContext'];
                  glNames.forEach(n => {
                    const Ctor = window[n]; if (!Ctor) return;
                    wrapFn(Ctor.prototype, 'getParameter', n+'.getParameter');
                    wrapFn(Ctor.prototype, 'getExtension', n+'.getExtension');
                    wrapFn(Ctor.prototype, 'getSupportedExtensions', n+'.getSupportedExtensions');
                    wrapFn(Ctor.prototype, 'shaderSource', n+'.shaderSource');
                  });
                
                  // Audio fingerprint
                  if (window.AudioContext) {
                    wrapFn(AudioContext.prototype, 'createOscillator', 'audio.createOscillator');
                    wrapFn(AudioContext.prototype, 'createAnalyser', 'audio.createAnalyser');
                  }
                
                  // Storage / Quota
                  if (navigator.storage && navigator.storage.estimate) {
                    const orig = navigator.storage.estimate.bind(navigator.storage);
                    navigator.storage.estimate = async () => { send('storage.estimate',''); return orig(); };
                  }
                
                  // MediaDevices / WebRTC
                  if (navigator.mediaDevices) {
                    wrapFn(navigator.mediaDevices, 'enumerateDevices', 'media.enumerateDevices');
                    wrapFn(navigator.mediaDevices, 'getUserMedia', 'media.getUserMedia');
                  }
                  if (window.RTCPeerConnection) {
                    wrapFn(window, 'RTCPeerConnection', 'webrtc.RTCPeerConnection'); // конструктор
                    wrapFn(RTCPeerConnection.prototype, 'createDataChannel', 'webrtc.createDataChannel');
                  }
                
                  // Battery
                  if (navigator.getBattery) {
                    const orig = navigator.getBattery.bind(navigator);
                    navigator.getBattery = async () => { send('battery.getBattery',''); return orig(); };
                  }
                
                  // Network Information API
                  if (navigator.connection) {
                    wrapGetter(navigator.connection.__proto__, 'downlink', 'network.downlink');
                    wrapGetter(navigator.connection.__proto__, 'effectiveType', 'network.effectiveType');
                  }
                
                  // Notification / Clipboard
                  if ('permissions' in navigator) {
                    wrapGetter(Notification, 'permission', 'notification.permission');
                  }
                  if (navigator.clipboard) {
                    wrapFn(navigator.clipboard, 'readText', 'clipboard.readText');
                    wrapFn(navigator.clipboard, 'writeText', 'clipboard.writeText');
                  }
                
                  // Маленький пинг для контроля
                  send('init', 'probe-injected');
                })();
                """);

    }

    @Override
    public void destroy() {
        // Аккуратно закрываем Page/Context/Browser/Playwright в правильном порядке
        log.debug("destroy(): closing Playwright resources");
        try {
            if (ctx != null) ctx.close();
        } catch (Exception ignored) {
        }
        try {
            if (browser != null) browser.close();
        } catch (Exception ignored) {
        }
        try {
            if (pw != null) pw.close();
        } catch (Exception ignored) {
        }
    }

    /* ===================== Public API ===================== */

    public ProductInfo fetch(long sku) throws IOException, InterruptedException {
        // Старт сквозного запроса composer → возможный редирект → возможный antibot → парсинг
        log.debug("fetch(): start for sku={}", sku);

        String composerUrl = "https://api.ozon.ru/composer-api.bx/page/json/v2?url=/product/" + sku + "/";

        HttpResponse<String> r1 = sendComposer(composerUrl);
        // Первый ответ composer (без авто-редиректов)
        log.debug("composer r1: status={} url={}", r1.statusCode(), composerUrl);

        if (isRedirect(r1)) {
            // Composer ответил редиректом - обрабатываем Location вручную
            log.debug("composer redirect for sku={} code={}", sku, r1.statusCode());
            String loc = r1.headers().firstValue("location").orElse(null);
            if (loc == null) throw new IOException("307 без Location от composer");
            String rrUrl = URI.create(composerUrl).resolve(loc).toString();
            log.debug("composer rrUrl resolved: {}", rrUrl);

            HttpResponse<String> r2 = sendComposer(rrUrl);
            log.debug("composer r2: status={} url={}", r2.statusCode(), rrUrl);
            Incident inc = tryParseIncident(r2.body());
            if (inc != null) {
                // Получили инцидент на первом ответе - решаем и повторяем
                log.debug("ABT incident detected (direct) for sku={}, solving", sku);
                // Получили инцидент ABT - решаем через headless браузер
                log.debug("ABT incident detected for sku={}, solving via Playwright", sku);
                solveChallengeWithBrowser(inc.challengeURL());
                HttpResponse<String> r3 = sendComposer(composerUrl);
                if (r3.statusCode() != 200) r3 = sendComposer(rrUrl);
                if (r3.statusCode() != 200) {
                    // Часть недоступных/снятых товаров после ABT редиректит composer на /search с product_id=...
                    if (isRedirect(r3)) {
                        String loc3 = r3.headers().firstValue("location").orElse("");
                        try {
                            // Приводим Location к абсолютному URL относительно composerUrl
                            String absLoc3 = URI.create(composerUrl).resolve(loc3).toString();
                            URI u = URI.create(absLoc3);
                            String q = u.getQuery();
                            if (q != null) {
                                // Декодим полностью query и уже по нему достаем параметр url (во избежание обрезания)
                                String targetUrl = java.net.URLDecoder.decode(q, java.nio.charset.StandardCharsets.UTF_8);
                                if (targetUrl != null && targetUrl.startsWith("page_changed") &&
                                        (targetUrl.contains("product_id=" + sku) || targetUrl.contains("product_id="))) {
                                    log.debug("fetch(): search redirect for unavailable sku={} -> {}", sku, targetUrl);
                                    // Следуем по исходному Location (absLoc3) еще одним composer-запросом
                                    HttpResponse<String> r4 = sendComposer(absLoc3);
                                    if (r4.statusCode() == 200) {
                                        log.debug("fetch(): success via search redirect for sku={}", sku);
                                        return parser.parse(r4.body())
                                                .orElseThrow(() -> new IOException("Composer JSON от /search без нужных полей"));
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    log.debug("fetch(): non-200 after challenge for sku={} code={}", sku, r3.statusCode());
                    throw new IOException("Composer после челленджа HTTP " + r3.statusCode());
                }
                // Успешно после челленджа
                log.debug("fetch(): success after challenge for sku={}", sku);
                return parser.parse(r3.body())
                        .orElseThrow(() -> new IOException("Не удалось извлечь title/price из composer JSON (после челленджа)"));
            }

            if (r2.statusCode() == 200) {
                // Успех после редиректа
                log.debug("fetch(): success after redirect for sku={}", sku);
                return parser.parse(r2.body())
                        .orElseThrow(() -> new IOException("Composer JSON получен, но без нужных полей"));
            }
            // Неожиданный код после редиректа
            log.debug("fetch(): composer after redirect non-200 sku={} code={}", sku, r2.statusCode());
            throw new IOException("Composer (__rr) HTTP " + r2.statusCode());
        }

        if (r1.statusCode() == 200) {
            // Успех без редиректа
            log.debug("fetch(): success direct for sku={}", sku);
            return parser.parse(r1.body())
                    .orElseThrow(() -> new IOException("Composer JSON получен, но без нужных полей"));
        }

        Incident inc = tryParseIncident(r1.body());
        if (inc != null) {
            solveChallengeWithBrowser(inc.challengeURL());
            HttpResponse<String> r3 = sendComposer(composerUrl);
            if (r3.statusCode() != 200) throw new IOException("Composer после челленджа HTTP " + r3.statusCode());
            return parser.parse(r3.body())
                    .orElseThrow(() -> new IOException("Не удалось извлечь title/price из composer JSON (после челленджа)"));
        }

        log.debug("fetch(): first composer non-200 for sku={} code={}", sku, r1.statusCode());
        throw new IOException("Composer первый запрос HTTP " + r1.statusCode());
    }

    /* ===================== Challenge via Playwright ===================== */

    private void solveChallengeWithBrowser(String challengeUrl) throws IOException {
        // Решаем antibot-челлендж ABT в headless-браузере: переходим по URL и ждем /abt/result
        log.debug("solveChallenge(): start url={}", challengeUrl);
        ensureBrowser();

        if (DEBUG_CHALLENGE) {
            try {
                java.nio.file.Files.deleteIfExists(TRACE_PATH);
            } catch (Exception ignored) {
            }
            ctx.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(false));
        }

        try (Page page = ctx.newPage()) {

            page.setDefaultTimeout(35_000);
            page.onResponse(resp -> {
                Request req = resp.request();
                Map<String, String> h1 = resp.allHeaders();
                Map<String, String> h2 = new java.util.HashMap<>(req.headers());
                Map<String, String> h3 = req.allHeaders();
                if (resp.url().contains("/abt/result")) {
                    log.debug("[ABT] {} {}", resp.status(), resp.url());
                }
            });

            page.exposeFunction("jsProbe", (Object... args) -> {
                String what = args.length > 0 && args[0] != null ? args[0].toString() : "";
                String detail = args.length > 1 && args[1] != null ? args[1].toString() : "";
                if (what.contains("get:navigator.userAgentData")) {

                }
                log.debug("[JS-PROBE] {} {}", what, detail);
                return null;
            });

            page.onConsoleMessage(msg -> log.debug("[CONSOLE] {}", msg.text()));

            // Переходим на challengeURL и ждем событий сети
            page.navigate(challengeUrl);

            Response abtResp = null;
            try {
                abtResp = page.waitForResponse(
                        r -> r.url().startsWith("https://api.ozon.ru/abt/result") && r.status() >= 200 && r.status() < 300,
                        new Page.WaitForResponseOptions().setTimeout(15_000.0), () -> {
                        }
                );
            } catch (PlaywrightException ignored) {
            }

            if (abtResp == null) {
                try {
                    page.evaluate("""
                              () => fetch("https://api.ozon.ru/abt/result", {
                                method: "POST", credentials: "include",
                                headers: { "Content-Type": "text/plain;charset=UTF-8" }, body: ""
                              }).then(r => r.status).catch(() => -1)
                            """);
                } catch (PlaywrightException ignored) {
                }
            }

            List<Cookie> pwCookies = ctx.cookies(Arrays.asList("https://api.ozon.ru", "https://www.ozon.ru"));
            if (pwCookies.isEmpty()) throw new IOException("Challenge не выставил куки (cookies=0)");

            log.debug("solveChallenge(): cookies received count={}", pwCookies.size());

            CookieStore store = cookieManager.getCookieStore();
            for (Cookie c : pwCookies) {
                HttpCookie hc = new HttpCookie(c.name, c.value);
                hc.setDomain(c.domain.startsWith(".") ? c.domain : "." + c.domain);
                hc.setPath((c.path == null || c.path.isBlank()) ? "/" : c.path);
                if (c.expires != null && c.expires > 0) {
                    long maxAge = (long) Math.max(1, c.expires - ((double) System.currentTimeMillis() / 1000L));
                    hc.setMaxAge(maxAge);
                }
                hc.setSecure(true);
                hc.setHttpOnly(false);
                try {
                    store.add(new URI("https://api.ozon.ru/"), hc);
                    store.add(new URI("https://www.ozon.ru/"), hc);
                } catch (URISyntaxException ignored) {
                }
            }

            page.waitForTimeout(300); // на всякий
        } catch (PlaywrightException e) {
            throw new IOException("Не удалось пройти antibot challenge в браузере: " + e.getMessage(), e);
        } finally {
            if (DEBUG_CHALLENGE) {
                try {
                    ctx.tracing().stop(new Tracing.StopOptions().setPath(TRACE_PATH));
                    log.debug("Trace saved: {}", TRACE_PATH);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /* ===================== HTTP helpers ===================== */

    private HttpResponse<String> sendComposer(String url) throws IOException, InterruptedException {
        // Единообразная отправка composer-запроса (без авто-редиректов)
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(COMPOSER_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ru,en;q=0.8")
                .header("Origin", "https://api.ozon.ru")
                .header("Referer", "https://www.ozon.ru/")
                .build();
        HttpResponse<String> resp = httpNoRedirect.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        log.debug("sendComposer: {} -> HTTP {}", url, resp.statusCode());
        return resp;
    }

    private static boolean isRedirect(HttpResponse<?> r) {
        int sc = r.statusCode();
        return sc == 307 || sc == 302 || sc == 301;
    }

    /* ===================== Incident JSON ===================== */

    private record Incident(String incidentId, String challengeURL) {
    }

    private Incident tryParseIncident(String body) {
        // Пытаемся распознать ABT-инцидент в JSON: { incidentId, challengeURL }
        // Возвращаем null, если формат не похож на JSON или поля отсутствуют
        if (body == null) return null;
        String t = body.trim();
        if (!(t.startsWith("{") && t.endsWith("}"))) return null;
        try {
            JsonNode n = om.readTree(t);
            if (n.has("incidentId") && n.has("challengeURL")) {
                String id = n.get("incidentId").asText(null);
                String cu = n.get("challengeURL").asText(null);
                if (id != null && cu != null && !cu.isBlank()) return new Incident(id, cu);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
