# Portfolio Backend - Agent Guide

## Current State (2026-08-17)

### Tech Stack
- **Language**: Java 17 (runtime), GraalVM 21 for native image
- **Framework**: Spring Boot 3.3.5
- **Database**: PostgreSQL (Supabase) with Flyway migrations
- **Build**: Maven with native-maven-plugin
- **Deploy**: GitHub Actions → GHCR (native image) → Render (image runtime)

### Architecture
```
GitHub Push (main) → CI (tests, Java 17) → Native Image (GraalVM 21) → GHCR → Render Deploy Hook
```

### Key Components
| Component | Location | Purpose |
|-----------|----------|---------|
| GitHub API | `GithubRepositoryService` | Fetches repos, caches 6h, includes private repos (url=null) |
| Chat History | `ChatHistoryController` | Persists messages per session (50 max) |
| Contact Intents | `ContactIntentController` | Stores contact form submissions |
| Assistant Proxy | `AssistantProxyController` | Proxies `/api/assistant/*` to AI service |
| Runtime Hints | `PortfolioRuntimeHints` | GraalVM native-image reflection config |

### API Endpoints
| Endpoint | Method | Auth | Notes |
|----------|--------|------|-------|
| `/actuator/health` | GET | - | Health check |
| `/api/time` | GET | - | Server time |
| `/api/github/repositories` | GET | - | Public + private (url=null), 6h cache |
| `/api/github/knowledge` | GET | - | AI context sources |
| `/api/chat-messages` | GET/POST | - | Session-based chat history |
| `/api/contact-intents` | POST | - | Contact form |
| `/api/assistant/chat` | POST | - | Proxies to AI service |

### Build Commands
```bash
# Local dev
./mvnw spring-boot:run

# Native image build (requires GraalVM 21)
./mvnw -Pnative -DskipTests spring-boot:build-image

# Tests
./mvnw verify
```

### Environment Variables (Render)
| Key | Source | Required |
|-----|--------|----------|
| `SPRING_DATASOURCE_URL` | Dashboard | Yes |
| `SPRING_DATASOURCE_USERNAME` | Dashboard | Yes |
| `SPRING_DATASOURCE_PASSWORD` | Dashboard | Yes |
| `GITHUB_API_TOKEN` | Dashboard | Yes |
| `AI_SERVICE_URL` | Blueprint | Yes (https://portfolio-ai-dla4.onrender.com) |
| `RENDER_DEPLOY_HOOK_URL` | GitHub Secret | Yes (for native-image workflow) |

### Branching Strategy
```
develop → (PR + review) → staging → (manual promote) → main → (auto-deploy)
```

### Common Issues & Fixes
| Issue | Fix |
|-------|-----|
| Native image fails: class version 69 | Use GraalVM 21 (Java 21), not 25 |
| Missing `ACCESS_DECLARED_FIELDS` | Use `INVOKE_PUBLIC_METHODS` for records |
| Flyway migration fails | Check Supabase connection, migrations in `src/main/resources/db/migration/` |
| Private repos missing | Ensure `GithubRepositoryService` doesn't filter `isPrivate()` |

### Deploy Flow
1. Push to `main` triggers CI
2. CI passes → native-image workflow builds native binary
3. Image pushed to `ghcr.io/zorionten/portfolio-backend:main`
4. Workflow calls Render deploy hook
5. Render pulls new image, restarts service

### Render Service
- **URL**: https://portfolio-backend-lutt.onrender.com
- **Dashboard**: https://dashboard.render.com/web/srv-d9vajdegekts73f6kn30
- **Runtime**: image (pulls from GHCR)
- **Auto-deploy**: Disabled (uses deploy hook)