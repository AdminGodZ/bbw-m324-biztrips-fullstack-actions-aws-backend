# RefCard03-04 - GitHub: BE, FE, Actions, deploy

## Ausgangslage

GitHub Actions, test, build and deploy. Sie kennen bereits das einfache React-Projekt https://github.com/bbwlc/refcard-03-fe-react-vite/tree/v1-start mit dem SpringBoot Backend https://github.com/bbwlc/RefCard-03-be-SpringBootBusinessTrips/tree/v1-start

Sie nutzen CI/CD von GitHub, die GitHub Actions um das Projekt nach einem Commit und Push automatisiert zu builden, testen und als Image auf Docker-Hub abzuspeichern.

| | |
|---|---|
| **Zeitbedarf:** | 2 Lektionen |
| **Hilfsmittel:** | React, GitHub Actions, AWS-Academy |
| **Methode/Sozialform:** | – |
| **Lernziele:** | ReactJS – GitHub Actions<br>Sie können eine CI-CD Pipeline auf GitHub mittels GitHub Action aufsetzen.<br>Sie können ein Projekt automatisiert builden, testen und auf AWS-EC2 deployen.<br>RefCard04: Sie können einen lokalen Dateistorage für Mitarbeiterfotos erstellen (Vorstufe zu S3)<br>Sie können das Image automatisiert auf AWS/ECR abspeichern.<br>Sie können die Zugangsdaten für AWS-ECR/ECS als Secrets schützen.<br>Deployment einer Docker-Anwendung auf AWS ECR auf ECS (Fargate) mit GitHub Actions aktualisieren und versionieren |

**Legende:** 👤 Einzelarbeit, 👥 Partnerarbeit, 📄 Dokumentation, 💻 Code, 🎤 Präsentation

### Inhalt

1. EX-01 – Deployment des Spring-Boot-Backends (mit MariaDB) auf AWS EC2 mit GitHub Actions
2. EX-02 – Docker-Image bauen, taggen und auf Docker Hub veröffentlichen
3. EX-03 – Deployment einer Docker-Anwendung auf AWS ECS (Fargate) mit GitHub Actions
4. RefCard04 Zusatz: EX-04 – Lokaler Datei-Storage für Mitarbeiterfotos (Vorstufe zu S3)

### Überblick über die Sequenz der Arbeiten

| RefCard | Status | Neues Thema | Frontend | Backend | Datenbank | Storage |
|---|---|---|---|---|---|---|
| 02 | 🟢 fertig | GitHub Actions, EC2, Docker, ECS | React + Vite | – | – | – |
| 03 (dieses Repo) | 🟡 in Arbeit | Spring Boot Backend | React + Vite | Spring Boot | MariaDB | – |
| 04 | 🟡 in Arbeit | Storage (Bilder) | React + Vite | Spring Boot | MariaDB | lokal/Volume → Vorstufe zu S3 |
| 05 | ⚪ geplant | Fullstack auf AWS | React + Vite | Spring Boot | MariaDB | S3 |

---

## EX-01 – Deployment des Spring-Boot-Backends (mit MariaDB) auf AWS EC2 mit GitHub Actions

**Lernziele:** Nach dieser Übung könnt ihr:

- ein bestehendes lokales Git-Repository mit einem leeren GitHub-Repository verbinden und pushen
- eine EC2-Instanz für das Hosting eines Spring-Boot-Backends samt MariaDB vorbereiten
- eine GitHub-Actions-Pipeline mit den Jobs test → build → deploy nachvollziehen
- die für den Deploy-Job nötigen Secrets und Variables in GitHub konfigurieren
- einen Workflow-Run beobachten, Fehler lesen und gezielt beheben

**Aufgabe:**

Nutzen Sie das oben genannte Projekt. Wenden Sie CI-CD mit GitHub Action an und berücksichtigen Sie dabei die obigen Ziele.

**Vorgehen:**

- Suchen Sie ein einfaches Tutorial
- Detaillierte Angaben siehe Projekt `EX-01-deploy-AWS-EC2.md`

**Ergebnis festhalten:**

- Halten Sie die Schritte Ihres Vorgehens kurz in einem Markdown im Projekt fest.
- Beschränken Sie sich auf die Essenz, kurz, knapp und verständlich.
- Ergänzen Sie ins Markdown Bilder, die Ihr Ergebnis zeigen. Achten Sie darauf, dass Ihre individuelle Lösung eindeutig erkennbar ist. (Commits, Workflows, Dockerhub, Container, Web-Site, …)

**Erweiterung:**

Nur Veränderungen auf dem main branch führen dazu, dass das Image auf docker-hub landet.

---

## EX-02 – Docker-Image bauen, taggen und auf Docker Hub veröffentlichen

**Lernziele:** Nach dieser Übung könnt ihr:

- ein Multi-Stage-Dockerfile für eine React/Vite-App schreiben
- ein Docker-Image lokal bauen und mit einer sinnvollen Tagging-Strategie versehen (`prod01`, `latest`, Git-Kurz-SHA)
- ein Docker-Hub-Repository anlegen und euch per CLI dort anmelden
- ein Image auf Docker Hub pushen und wieder herunterladen
- den Container lokal starten und testen

**Aufgabe:**

Detaillierte Aufgabe im Projekt: `EX-02-create-Docker-Image-DockerHub.md`

Verwenden Sie die Dockerhub.

**Ergebnis festhalten:** Wie oben.

- Halten Sie die Schritte Ihres Vorgehens kurz in einem Markdown im Projekt fest.
- Beschränken Sie sich auf die Essenz, kurz, knapp und verständlich.
- Ergänzen Sie ins Markdown Bilder, die Ihr Ergebnis zeigen. Achten Sie darauf, dass Ihre individuelle Lösung eindeutig erkennbar ist (Commits, Workflows, ECR).

---

## EX-03 – Deployment einer Docker-Anwendung auf AWS ECS (Fargate) mit GitHub Actions

**Lernziele:** Nach dieser Übung könnt ihr:

- den Unterschied zwischen einem direkten EC2-Deployment (EX-01) und einem containerbasierten ECS-Deployment einordnen
- ein Amazon ECR (Elastic Container Registry) Repository anlegen und ein Image dorthin pushen
- eine ECS-Task-Definition, einen Cluster und einen Service (Fargate-Launch-Type) anlegen
- einen Application Load Balancer vor einen ECS-Service schalten
- eine GitHub-Actions-Pipeline schreiben, die bei jedem Push auf main ein neues Image baut, nach ECR pusht und den ECS-Service aktualisiert (Rolling Deployment)
- AWS-Zugangsdaten per OIDC (statt langlebiger Access Keys) an GitHub Actions vergeben

**Aufgabe:**

Detaillierte Aufgabe im Projekt: `EX-03-deploy-AWS-ECS.md`

Verwenden Sie die ECR.

**Ergebnis festhalten:** Wie oben.

- Ergänzen Sie ins Markdown Bilder, die Ihr Ergebnis zeigen. Achten Sie darauf, dass Ihre individuelle Lösung eindeutig erkennbar ist (ECS, Änderung → Wirkung).

---

## RefCard04 Zusatz: EX-04 – Lokaler Datei-Storage für Mitarbeiterfotos (Vorstufe zu S3)

**Lernziele:** Nach dieser Übung könnt ihr:

- einen Spring-Boot-Endpoint für Datei-Upload (multipart/form-data) und -Download schreiben
- Dateien sicher auf einem Dateisystem ablegen (Grössen-/Typ-Validierung, Schutz vor Path Traversal, kollisionsfreie Dateinamen)
- den Ablagepfad über eine Property nach aussen konfigurierbar machen, statt ihn im Code fest zu verdrahten
- ein Docker-Volume für persistenten Storage einrichten und von einem zustandslosen Container-Dateisystem abgrenzen
- den EC2-Deploy aus EX-01 so anpassen, dass hochgeladene Dateien einen Redeploy überleben
- begründen, warum genau dieser Ansatz nicht unverändert nach ECS/Fargate (EX-03) migriert werden kann — das ist die Brücke zu RefCard05 (S3)

**Aufgabe:**

Detaillierte Aufgabe im Projekt: `EX-04-RefCard04-Storage.md`

Verwenden Sie die EC2 (nur hier geht es).

**Ergebnis festhalten:** Wie oben.
