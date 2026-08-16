# Portfolio Backend

Minimal Spring Boot service for the portfolio application. GitHub Actions publishes a GraalVM native image for Render, which checks `/actuator/health` before routing traffic.

## Local Development

Requirements: Java 17 and Docker. Copy `.env.example` to `.env`, then configure the Supabase datasource and GitHub token values.

`GITHUB_API_TOKEN` is used only by the backend to retrieve repository metadata. To include private and collaborator repositories, use either:

- A classic personal access token with the `repo` scope.
- A fine-grained token with access to every repository being displayed and read-only Metadata and Contents permissions.

The public portfolio API intentionally returns private repository names, descriptions, languages, and last-commit dates. It never returns private repository URLs or the token. GitHub responses are cached for six hours.

`GET /api/github/knowledge` is the AI-facing retrieval boundary. Java fetches repository metadata and READMEs from GitHub, keeps public project context, and reduces all private repository content to an anonymous technology set. The Python service never receives the GitHub token, private repository identity, or private README text.

Chat history is persisted through `GET` and `POST /api/chat-messages`. Messages are associated with a random browser-session UUID and store the role, content, AI source labels, and timestamp. No contact identity is attached. Each session retains and returns its latest 50 messages in chronological order, providing a clean base for a future authenticated analytics interface.

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

This repository owns `render.yaml`. The Blueprint creates one Render project named `portfolio` with these services:

- `portfolio-backend`, pulled as a prebuilt native image from GHCR.
- `portfolio-ai`, built from `ZorionTen/portfolio-ai` on Render.

[Deploy the Blueprint to Render](https://render.com/deploy?repo=https://github.com/ZorionTen/portfolio-backend)

Render prompts for the Supabase credentials and `GITHUB_API_TOKEN` for the backend, plus `GROQ_API_KEY` for the AI service, during Blueprint setup. Never commit those values.

The `Native Image` workflow runs after `CI` succeeds on `main`. It builds the backend with Spring AOT and GraalVM, then publishes these images to GitHub Container Registry:

- `ghcr.io/zorionten/portfolio-backend:<commit-sha>` for traceability and rollbacks.
- `ghcr.io/zorionten/portfolio-backend:main` as the Blueprint default.

Complete these one-time deployment steps for the initial rollout:

1. Disable Blueprint Auto Sync in Render before merging the runtime change, because the GHCR image does not exist yet.
2. Merge the change and wait for the first `Native Image` workflow to publish the package.
3. In the package settings on GitHub, change `portfolio-backend` visibility to public so Render can pull it without registry credentials. GitHub does not allow changing a public package back to private.
4. Manually sync the Render Blueprint so `portfolio-backend` changes from a Git-backed Docker service to the prebuilt image.
5. Copy the backend service deploy hook from Render and add it as the `RENDER_DEPLOY_HOOK_URL` Actions secret in GitHub.
6. Re-enable Blueprint Auto Sync.

The workflow resolves the published image digest and passes that immutable digest to the deploy hook, so production never depends on a stale mutable tag. It also refuses to publish a delayed CI result if `main` has moved to a newer commit. If the deploy-hook secret is absent, image publication still succeeds and the workflow reports that automatic deployment was skipped.
