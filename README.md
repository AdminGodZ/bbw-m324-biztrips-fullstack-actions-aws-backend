# Business Trips · Backend · RefCard03–04

Tim Held, M324. Java 21, Spring Boot, MariaDB, GitHub Actions, EC2, Docker Hub, ECS/Fargate und lokaler Foto-Storage.

## Schnellstart

Frontend als Nachbarordner `../frontend` auschecken, dann:

```bash
docker compose up -d --build --wait
```

Frontend: http://localhost:3000 · API: http://localhost:8080/v1/trips · Health: http://localhost:8080/actuator/health.

Ohne Docker: `./mvnw test` und `./mvnw spring-boot:run` verwenden die eingebettete H2-Datenbank. Für MariaDB `SPRING_PROFILES_ACTIVE=mariadb`, `DB_URL`, `DB_USER` und `DB_PASSWORD` setzen. Lokale Compose-Passwörter sind ausschliesslich Entwicklungswerte und lassen sich über `.env` überschreiben.

## Fotos

```bash
curl -f http://localhost:8080/v1/employees
curl -f -X POST http://localhost:8080/v1/employees/1/photo -F 'file=@test.png'
curl -f http://localhost:8080/v1/employees/1/photo -o downloaded.png
```

PNG, JPEG und WebP bis 5 MiB. `APP_STORAGE_LOCATION` bestimmt den Pfad. Compose mountet `uploads:/app/uploads`; EC2 verwendet `/var/lib/biztrips/uploads`. Der Container läuft ohne root. Datenbank und Uploads bleiben bei normalem `docker compose down` erhalten.

## GitHub Actions und AWS

Die Pipeline testet und baut jeden Pull Request und jeden Push auf `main`. Veröffentlichungen und Deployments laufen ausschliesslich auf `main`. Die drei Variablen `ENABLE_EC2`, `ENABLE_DOCKERHUB` und `ENABLE_ECS` werden erst nach der jeweiligen Einrichtung auf `true` gesetzt. Ein übersprungener Job ist kein erfolgreicher Deploy.

| Secret/Variable | Ort und Bedeutung |
|---|---|
| `AWS_CREDENTIALS` | Repository-Secret, vollständiger temporärer `[default]`-Block aus Learner Lab AWS Details |
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Repository-Secrets, Benutzer und Read/Write-Token für die Images |
| `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `EC2_KNOWN_HOSTS` | Secrets im Environment `production`; Host, Benutzer, PEM und gepinnter SSH-Hostkey |
| `EC2_SECURITY_GROUP` | Repository-Variable, Security Group für die kurzzeitige Runner-IP-Regel |
| `ENABLE_EC2`, `ENABLE_DOCKERHUB`, `ENABLE_ECS` | Repository-Variablen für die fertig eingerichteten Ziele |
| `AWS_ROLE_ARN` | Optionale Repository-Variable für OIDC im ECS-Job ausserhalb des Learner Labs |

Das eigene Infrastruktur-Template liegt in `deploy/infrastructure.json`. Es verwendet die vorhandenen Lab-Rollen und zwei Subnetze derselben VPC in unterschiedlichen Availability Zones. EC2 ist für EX-01/04 zuständig; ALB, ECR, RDS und ECS gehören zu EX-03. Das Template legt keine eigenen IAM-Rollen an. SSH-CIDR beim Erstellen auf die eigene IPv4-Adresse mit `/32` begrenzen. Die Actions öffnen und schliessen zusätzlich nur die jeweilige Runner-IP.

Credentials laufen mit der Lab-Session ab. Vor einem neuen Pipeline-Lauf den ganzen `AWS_CREDENTIALS`-Block in beiden Repositories erneuern. Keine Credentials in Dateien committen oder in Logs ausgeben.

## Nachweise

[Umsetzung, Prüfstand und Reflexion](docs/NACHWEIS.md). Die originalen Aufgaben bleiben unter `docs/exercises`; die ursprüngliche README liegt unter `docs/UPSTREAM-README.md`.

Fotos in ECS sind in RefCard04 bewusst flüchtig. Dauerhafter gemeinsamer Foto-Storage für mehrere Fargate-Tasks ist Gegenstand von RefCard05. Der Nachweis erklärt die Grenze.
