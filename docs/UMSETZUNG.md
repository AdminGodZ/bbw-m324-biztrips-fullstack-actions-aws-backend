# Technische Entscheidungen · RefCard03–04

Tim Held, M324. [Abnahme und Nachweise](NACHWEIS.md).

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

Das Learner Lab erlaubt keine eigene IAM-/OIDC-Rolle. Es werden die vorhandenen `LabRole` und `LabInstanceProfile` verwendet. Für GitHub liegt der temporäre Lab-Zugang als verschlüsseltes Secret `AWS_CREDENTIALS` vor. Nach Ablauf oder Neustart einer Lab-Session wird der komplette `[default]`-Block aus AWS Details in beiden Repos erneuert. Die Pipeline schreibt die drei Werte ohne Ausgabe in ihre Laufzeitumgebung. Der Parameter-Block wird beim privaten Einrichten auf echte Zeilenumbrüche normalisiert; im Secret steht ein gültiger INI-Block. In einem regulären AWS-Account wäre eine auf Repository, Branch und benötigte APIs beschränkte OIDC-Rolle vorzuziehen. Der ECS-Job unterstützt diese Alternative über `AWS_ROLE_ARN`.

## EX-04 · Entscheidungen und Grenzen

Die Datenbank speichert nur den generierten Dateinamen. Der Server verwendet weder den eingereichten Dateinamen noch dessen Endung als Speicherpfad. Er prüft MIME-Type und Dateisignatur, begrenzt die Grösse, verhindert Pfade ausserhalb des Storage-Verzeichnisses und lehnt Symlinks ab. Die Signaturprüfung ersetzt keine vollständige Bilddekodierung oder Bildaufbereitung.

PNG wird als PNG, JPEG als JPEG und WebP als WebP ausgeliefert. Leere oder ungültige Dateien werden abgelehnt. Wenn die Datenbankspeicherung scheitert, wird die gerade neu geschriebene Datei entfernt. Nach erfolgreichem Ersetzen wird das vorherige Foto aufgeräumt.

Ein benanntes Docker-Volume überlebt das Entfernen eines Containers, bleibt aber an seinen Docker-Host gebunden. Ein Fargate-Task hat ein eigenes flüchtiges Dateisystem. Ein Foto auf Task A ist auf Task B nicht vorhanden und geht bei Task-Ersetzung verloren. Auch ein einzelner Task löst den Verlust beim Redeploy nicht. Eine dauerhafte Lösung für mehrere Tasks benötigt einen gemeinsamen Speicher, beispielsweise S3 in RefCard05. Diese Erweiterung ist hier nicht implementiert.

Die offenen API-Endpunkte und Demo-Konten stammen aus dem Unterrichtsstarter. Die Veröffentlichung dient der Übung; reale personenbezogene Fotos werden dafür nicht verwendet.


