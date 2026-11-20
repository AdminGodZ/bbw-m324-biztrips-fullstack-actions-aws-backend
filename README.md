# Business Trips · Backend · RefCard05 (Full Stack auf AWS)

Tim Held, M324. Spring Boot 4 (Java 21) · Amazon RDS MariaDB · Amazon S3 · ECR · ECS Fargate · GitHub Actions.

**→ Dokumentation RefCard05: [docs/REFCARD05.md](docs/REFCARD05.md)** (Architektur, Pipeline, Versionierung, RDS, S3/IAM Least Privilege, ECS, Secrets/OIDC, Nachweise)

| | |
|---|---|
| Frontend-Repo | <https://github.com/AdminGodZ/bbw-m324-biztrips-fullstack-actions-aws-frontend> |
| Lokale Entwicklung (Compose) | <https://github.com/AdminGodZ/bbw-m324-biztrips-fullstack-compose> |
| Pipeline | [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml): build → test → Image → ECR → ECS + Verifikation |
| Infrastruktur | [`deploy/infrastructure.yaml`](deploy/infrastructure.yaml) (CloudFormation-Stack `bbw-m324-refcard05`) |
| Releases | Git-Tags `vX.Y.Z` → ECR-Image `X.Y.Z` |

## Schnellstart

```bash
./mvnw test              # 17 Tests, ohne Datenbank oder AWS (H2 + gemockter S3-Client)
./mvnw spring-boot:run   # H2, Fotos in ./uploads → http://localhost:8080/v1/trips
```

Das ganze System (Frontend, Backend, MariaDB, S3-Mock) läuft lokal über das [Compose-Repo](https://github.com/AdminGodZ/bbw-m324-biztrips-fullstack-compose).

## Konfiguration (nur Umgebungsvariablen, keine Zugangsdaten im Code)

| Profil | Datenbank | Fotos | Variablen |
|---|---|---|---|
| *(keins)* | H2 | lokal (`APP_STORAGE_LOCATION`) | – |
| `mariadb` | MariaDB | `APP_STORAGE_TYPE=local\|s3` | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| `aws` | RDS (TLS verify-full) | S3 | zusätzlich `APP_STORAGE_S3_BUCKET`, `AWS_REGION`. `DB_USER`/`DB_PASSWORD` aus Secrets Manager |

## API

`GET /v1/trips`, `GET /v1/employees`, `POST|GET /v1/employees/{id}/photo` (PNG/JPEG/WebP ≤ 5 MiB) · `GET /actuator/health[/readiness]` · `GET /actuator/info` (Version)

## Ältere Stände

RefCard03/04 (EC2, Docker Hub, ECS mit lokalem Foto-Storage): [docs/NACHWEIS.md](docs/NACHWEIS.md), Aufträge unter `docs/exercises`, ursprüngliche README unter [docs/UPSTREAM-README.md](docs/UPSTREAM-README.md).
