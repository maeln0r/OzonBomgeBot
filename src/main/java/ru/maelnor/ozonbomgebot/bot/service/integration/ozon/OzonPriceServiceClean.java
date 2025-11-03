package ru.maelnor.ozonbomgebot.bot.service.integration.ozon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
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

/**
 * "Clean" версия сервиса: без HAR, трейсинга и JS-пробов.
 * Поведение и алгоритм полностью повторяют debug-реализацию.
 */
@Service
@ConditionalOnExpression("!'${logging.level.root:INFO}'.equalsIgnoreCase('DEBUG')")
public class OzonPriceServiceClean implements DisposableBean, OzonPriceService {

    /* ===================== Settings & constants ===================== */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration COMPOSER_TIMEOUT = Duration.ofSeconds(15);

    /* ===================== Deps ===================== */
    private final OzonProductParser parser;

    public OzonPriceServiceClean(OzonProductParser parser) {
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
        if (pw != null) return;
        pw = Playwright.create();
        browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage"))
        );
        ctx = browser.newContext(new Browser.NewContextOptions()
                .setLocale("ru-RU")
                .setUserAgent(USER_AGENT)
                .setViewportSize(1280, 900)
        );
        ctx.setExtraHTTPHeaders(Map.of("Origin", "https://api.ozon.ru", "Referer", "https://www.ozon.ru/"));

        // Минимально необходимая маскировка окружения: platform + webdriver (+пара мелочей)
        ctx.addInitScript("""
                  (() => {
                    const proto = Object.getPrototypeOf(navigator);
                    try { Object.defineProperty(proto, 'platform',  { get() { return 'Win32'; }, configurable: true }); } catch {}
                    try { Object.defineProperty(proto, 'webdriver', { get() { return undefined; }, configurable: true }); } catch {}
                    try { Object.defineProperty(navigator, 'languages', { get() { return ['ru-RU','ru','en-US','en']; }, configurable: true }); } catch {}
                
                
                    try { Object.defineProperty(navigator, 'plugins', { get() { return [1,2,3]; }, configurable: true }); } catch {}
                    try {
                      const getParameter = WebGLRenderingContext.prototype.getParameter;
                      WebGLRenderingContext.prototype.getParameter = function(p) {
                        if (p === 37445) return 'Intel Inc.';
                        if (p === 37446) return 'Intel Iris OpenGL';
                        return getParameter.call(this, p);
                      };
                    } catch {}
                
                
                    try {
                      if ('userAgentData' in navigator) {
                        const brands = [
                          { brand: 'Chromium', version: '126' },
                          { brand: 'Not(A:Brand', version: '24' },
                          { brand: 'Google Chrome', version: '126' }
                        ];
                        const uach = { brands, mobile: false, platform: 'Windows' };
                        const hi = {
                          architecture: 'x86', bitness: '64', model: '', platform: 'Windows',
                          platformVersion: '15.0.0', uaFullVersion: '126.0.0.0',
                          fullVersionList: brands.map(b => ({ brand: b.brand, version: b.version })), wow64: false
                        };
                        Object.defineProperty(navigator, 'userAgentData', {
                          get() {
                            return {
                              get brands() { return uach.brands; },
                              get mobile() { return uach.mobile; },
                              get platform() { return uach.platform; },
                              getHighEntropyValues: async (hints) => {
                                const out = {}; for (const k of hints || []) if (k in hi) out[k] = hi[k]; return out;
                              },
                              toJSON: () => uach
                            };
                          }, configurable: true
                        });
                      }
                    } catch {}
                  })();
                """);
    }

    @Override
    public void destroy() {
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
    @Override
    public ProductInfo fetch(long sku) throws IOException, InterruptedException {
        final String composerUrl = "https://api.ozon.ru/composer-api.bx/page/json/v2?url=/product/" + sku + "/";

        HttpResponse<String> r1 = sendComposer(composerUrl);
        if (isRedirect(r1)) {
            String loc = r1.headers().firstValue("location").orElse(null);
            if (loc == null) throw new IOException("307 без Location от composer");
            String rrUrl = URI.create(composerUrl).resolve(loc).toString();

            HttpResponse<String> r2 = sendComposer(rrUrl);
            Incident inc = tryParseIncident(r2.body());
            if (inc != null) {
                solveChallengeWithBrowser(inc.challengeURL());
                HttpResponse<String> r3 = sendComposer(composerUrl);
                if (r3.statusCode() != 200) r3 = sendComposer(rrUrl);
                if (r3.statusCode() != 200) {
                    if (isRedirect(r3)) {
                        String loc3 = r3.headers().firstValue("location").orElse("");
                        try {
                            String absLoc3 = URI.create(composerUrl).resolve(loc3).toString();
                            URI u = URI.create(absLoc3);
                            String q = u.getQuery();
                            if (q != null) {
                                String targetUrl = java.net.URLDecoder.decode(q, java.nio.charset.StandardCharsets.UTF_8);
                                if (targetUrl != null && targetUrl.startsWith("page_changed") &&
                                        (targetUrl.contains("product_id=" + sku) || targetUrl.contains("product_id="))) {
                                    HttpResponse<String> r4 = sendComposer(absLoc3);
                                    if (r4.statusCode() == 200) {
                                        return parser.parse(r4.body())
                                                .orElseThrow(() -> new IOException("Composer JSON от /search без нужных полей"));
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    throw new IOException("HTTP " + r3.statusCode());
                }
                return parser.parse(r3.body())
                        .orElseThrow(() -> new IOException("Не удалось извлечь title/price"));
            }

            if (r2.statusCode() == 200) {
                return parser.parse(r2.body())
                        .orElseThrow(() -> new IOException("JSON получен, но без нужных полей"));
            }
            throw new IOException("HTTP " + r2.statusCode());
        }

        if (r1.statusCode() == 200) {
            return parser.parse(r1.body())
                    .orElseThrow(() -> new IOException("Composer JSON получен, но без нужных полей"));
        }

        Incident inc = tryParseIncident(r1.body());
        if (inc != null) {
            solveChallengeWithBrowser(inc.challengeURL());
            HttpResponse<String> r3 = sendComposer(composerUrl);
            if (r3.statusCode() != 200) throw new IOException("HTTP " + r3.statusCode());
            return parser.parse(r3.body())
                    .orElseThrow(() -> new IOException("Не удалось извлечь title/price из composer JSON (после челленджа)"));
        }

        throw new IOException("HTTP " + r1.statusCode());
    }

    /* ===================== Challenge via Playwright ===================== */
    private void solveChallengeWithBrowser(String challengeUrl) throws IOException {
        ensureBrowser();
        try (Page page = ctx.newPage()) {
            page.setDefaultTimeout(35_000);
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

            page.waitForTimeout(300);
        } catch (PlaywrightException e) {
            throw new IOException("Не удалось пройти antibot challenge в браузере: " + e.getMessage(), e);
        }
    }

    /* ===================== HTTP helpers ===================== */
    private HttpResponse<String> sendComposer(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(COMPOSER_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ru,en;q=0.8")
                .header("Origin", "https://api.ozon.ru")
                .header("Referer", "https://www.ozon.ru/")
                .build();
        return httpNoRedirect.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static boolean isRedirect(HttpResponse<?> r) {
        int sc = r.statusCode();
        return sc == 307 || sc == 302 || sc == 301;
    }

    /* ===================== Incident JSON ===================== */
    private record Incident(String incidentId, String challengeURL) {
    }

    private Incident tryParseIncident(String body) {
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
