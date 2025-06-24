# Система управления банковскими картами

Данный репозиторий представляет собой REST API для управления с банковскими картами.

## Предварительные требования

- Установленный [Docker](https://docs.docker.com/get-docker/)
- Установленный [Docker Compose](https://docs.docker.com/compose/install/)

## Инструкция по запуску

1. Клонировать репозиторий на своё устройство и перейти в директорию проекта
   ```
   git clone <Repository_URL>
   cd Bank_REST
   ```

2. Собрать образы и запустить контейнеры
   ```
   docker-compose up --build
   ```

Когда все контейнеры будут запущены, можно получить доступ к:
* REST API по адресу `http://localhost:8080`
* Swagger UI по адресу `http://localhost:8080/swagger-ui.html`
