# Portfolio Backend

Minimal Spring Boot service for the portfolio application. Render builds it from `Dockerfile` and checks `/actuator/health` before routing traffic.

## Local Development

Requirements: Java 17 and Docker.

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`. Run its tests with:

```bash
./mvnw verify
```

## Docker

```bash
docker build -t portfolio-backend .
docker run --rm -p 8080:8080 portfolio-backend
```

## Render

This repository owns `render.yaml`. The Blueprint creates one Render project named `portfolio` with two Docker services sourced from:

- `ZorionTen/portfolio-backend`
- `ZorionTen/portfolio-ai`

[Deploy the Blueprint to Render](https://render.com/deploy?repo=https://github.com/ZorionTen/portfolio-backend)

Render prompts for `GROQ_API_KEY` during the first Blueprint deployment. Never commit that value.
