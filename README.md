Для linux окружения сбилдить образ с Playwright:

```bash
docker build -t ozonbomgebot .
```

Положить рядом с `docker-compose.yml` образ и `.env`

```dotenv
APP_BOT_NAME=MyOzonBot
APP_BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
APP_DATA_DIR=/app/data
LOGGING_LEVEL_ROOT=INFO
```

Запустить

```bash
docker compose up -d
```