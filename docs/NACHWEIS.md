# RefCard03–04 · Umsetzung und Nachweise

**Autor:** Tim Held · **Modul:** M324 · **Datum:** 06.11.2026

## Ausgangspunkt

Backend: `bbwlc/RefCard-03-be-SpringBootBusinessTrips`, Tag `v1-start`, Commit `e98f4afc1c0f9560682f8c25f6be07e620d49163`.
Frontend: `bbwlc/refcard-03-fe-react-vite`, Tag `v1-start`, Commit `ac9bf587aef94cfad1408540b9ee1eb77eecd3f0`.
Die Historie bleibt erhalten; die Lehrer-Repositories sind als `upstream` eingetragen. Eigene Arbeit liegt auf `main` in zwei neuen privaten Repositories.

## Stand der Übungen

| Übung | Implementierung | Bisher tatsächlich geprüft |
|---|---|---|
| EX-01 · EC2 und GitHub Actions | Getrennte Jobs für Test, Build und Deployment; Artefakte; gepinnter SSH-Hostkey; Healthcheck; Backend-Rollback; native MariaDB und systemd | Lokale Builds und Skript-Syntax; Live-Pipeline und EC2-Abnahme stehen noch aus |
| EX-02 · Docker Hub | Zwei Multi-Stage-Images; Tags `prod01`, `latest`, Commit-SHA; Veröffentlichung nur auf `main`; Buildx-Cache | Beide Images gebaut; Container laufen lokal; Veröffentlichung und erneuter Pull stehen noch aus |
| EX-03 · ECR, ECS, ALB, RDS | Infrastruktur als CloudFormation; private DB; zwei Services mit je zwei Tasks; Healthchecks; Circuit Breaker; Abgleich der tatsächlich laufenden Task-Revision | CloudFormation-Template von AWS validiert; Stack-Erstellung gestartet; laufende Tasks und Redeploy noch offen |
| EX-04 · Mitarbeiterfotos | Upload/Download; PNG/JPEG/WebP; 5 MiB; Signaturprüfung; UUID; Pfadprüfung; korrekter Content-Type; persistentes Volume; dauerhafter EC2-Pfad | Lokaler Upload/Download und Negativfälle erfolgreich; Foto nach Containerersetzung unverändert; EC2-Redeploy-Test noch offen |

Diese Tabelle trennt vorhandenen Code von einem erfolgreich beobachteten Deployment. Sie wird nach den Live-Prüfungen aktualisiert.

## Lokale Prüfungen

- Backend: vier JUnit-Tests erfolgreich, inklusive Storage-Sicherheitsfällen und Spring-Kontext.
- Frontend: vier Vitest-Tests erfolgreich, inklusive API-Fehler und Hinzufügen/Entfernen eines Trips in der Wishlist. Produktions-Build erfolgreich. ESLint ohne Fehler, eine vorhandene Fast-Refresh-Warnung im Context-Modul.
- Docker: Multi-Stage-Images für Backend und Frontend erfolgreich gebaut.
- Compose: MariaDB, Backend und nginx melden `healthy`.
- API über nginx: `/v1/trips` antwortet mit HTTP 200 und drei Trips.
- Upload/Download: PNG-Datei wird mit HTTP 200 und `image/png` ausgeliefert; Bytes stimmen überein.
- Negativfälle: falscher deklarierter MIME-Type und falsche Bildsignatur liefern 400, unbekannter Mitarbeiter 404, Datei über 5 MiB 413.
- Containerersetzung: `docker compose up -d --force-recreate --wait backend`; Bild weiterhin verfügbar, unveränderte Bytes und Datenbankreferenz.
- SHA256 des Testbilds: `4c5d573cfb85b8ab0a5659062b830a058d1a449e51a6d79c13b483a77ea7ed28`.

## EX-01 · Entscheidungen

Die Jar-Datei ist unabhängig von einer bestimmten Datenbank gebaut. Der MariaDB-Zugang wird erst zur Laufzeit eingelesen. Auf EC2 wird ein eigener Datenbankbenutzer für `db_biztrips` verwendet. Ein zufälliges Passwort wird auf der Instanz erzeugt und liegt nur in der geschützten `/etc/biztrips.env`.

Die Pipeline überträgt nur `app.jar.next`, ersetzt die aktuelle Jar und startet systemd neu. Wenn der Healthcheck fehlschlägt, wird die vorherige Jar wieder eingesetzt. Uploads liegen unter `/var/lib/biztrips/uploads` und werden durch den Jar-Transfer nie überschrieben. Das Frontend liegt separat unter `/var/www/biztrips`.

SSH prüft einen vorher hinterlegten Hostkey. Der GitHub-Runner erhält nur für seine aktuelle IPv4-Adresse eine temporäre Port-22-Regel, die beim Verlassen des Deploy-Skripts wieder entfernt wird. Die normale SSH-Regel ist auf die eigene IP beschränkt. MariaDB-Port 3306 und Spring-Port 8080 werden auf EC2 nicht öffentlich geöffnet; nginx stellt die Anwendung auf Port 80 bereit.

## EX-02 · Entscheidungen

Im Backend-Image baut ein JDK die Jar, während das Laufzeit-Image nur JRE und Jar enthält und als eigener Benutzer läuft. Das Frontend wird mit Node gebaut und mit nginx ausgeliefert. Build-Caches und Quellcode gelangen nicht ins finale Laufzeit-Image.

`latest` und `prod01` sind bewegliche Verweise. Der volle Git-SHA verknüpft ein veröffentlichtes Image mit seinem Quellcode. Die React-App verwendet `/v1` auf demselben Origin. nginx leitet diese Anfragen an das Backend weiter. `BACKEND_ORIGIN` konfiguriert den Proxy beim Containerstart; das ist bewusst unabhängig von Vites Build-Variablen.

## EX-03 · Entscheidungen

RDS ist ausschliesslich für die Task-Security-Group auf Port 3306 erreichbar. Backend-Port 8080 und Frontend-Port 80 akzeptieren eingehend ausschliesslich die ALB-Security-Group. Die Tasks erhalten öffentliche IPs für ausgehende Registry- und Log-Verbindungen, haben jedoch keine öffentliche Eingangsregel. Die DB erhält keine öffentliche IP.

ALB leitet `/v1/*` und `/actuator/health` ans Backend, alle übrigen Pfade ans Frontend. Healthchecks prüfen die laufenden Container. Das Backend prüft über Actuator auch seine Datenbank. Beim ersten Service-Start wird zunächst ein Task gesund gestartet, damit die Demo-Daten einmal angelegt werden; danach wird auf zwei Tasks skaliert.

ECS ersetzt Tasks bei einem Rolling Deployment. Der Deploy-Code wartet auf Stabilität und vergleicht anschliessend die aktive Task-Definition mit der gerade registrierten Revision. Ein automatischer Rollback kann dadurch nicht als erfolgreicher neuer Release erscheinen.

Das Learner Lab erlaubt keine eigene IAM-/OIDC-Rolle. Es werden die vorhandenen `LabRole` und `LabInstanceProfile` verwendet. Für GitHub liegt der temporäre Lab-Zugang als verschlüsseltes Secret `AWS_CREDENTIALS` vor. Nach Ablauf oder Neustart einer Lab-Session wird der komplette `[default]`-Block aus AWS Details in beiden Repos erneuert. Die Pipeline schreibt die drei Werte ohne Ausgabe in ihre Laufzeitumgebung. In einem regulären AWS-Account wäre eine auf Repository, Branch und benötigte APIs beschränkte OIDC-Rolle vorzuziehen. Der ECS-Job unterstützt diese Alternative über `AWS_ROLE_ARN`.

## EX-04 · Entscheidungen und Grenzen

Die Datenbank speichert nur den generierten Dateinamen. Der Server verwendet weder den eingereichten Dateinamen noch dessen Endung als Speicherpfad. Er prüft MIME-Type und Dateisignatur, begrenzt die Grösse, verhindert Pfade ausserhalb des Storage-Verzeichnisses und lehnt Symlinks ab. Die Signaturprüfung ersetzt keine vollständige Bilddekodierung oder Bildaufbereitung.

PNG wird als PNG, JPEG als JPEG und WebP als WebP ausgeliefert. Leere oder ungültige Dateien werden abgelehnt. Wenn die Datenbankspeicherung scheitert, wird die gerade neu geschriebene Datei entfernt. Nach erfolgreichem Ersetzen wird das vorherige Foto aufgeräumt.

Ein benanntes Docker-Volume überlebt das Entfernen eines Containers, bleibt aber an seinen Docker-Host gebunden. Ein Fargate-Task hat ein eigenes flüchtiges Dateisystem. Ein Foto auf Task A ist auf Task B nicht vorhanden und geht bei Task-Ersetzung verloren. Auch ein einzelner Task löst den Verlust beim Redeploy nicht. Eine dauerhafte Lösung für mehrere Tasks benötigt einen gemeinsamen Speicher, beispielsweise S3 in RefCard05. Diese Erweiterung ist hier nicht implementiert.

Die offenen API-Endpunkte und Demo-Konten stammen aus dem Unterrichtsstarter. Die Veröffentlichung dient der Übung; reale personenbezogene Fotos werden dafür nicht verwendet.
