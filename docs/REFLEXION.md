# Reflexionsfragen · RefCard03–04

Tim Held, M324. Antworten zur eigenen Lösung. [Abnahme und Nachweise](NACHWEIS.md).

## EX-01

1. `production`-Secrets sind nur für Jobs mit diesem Environment verfügbar. Am Environment können zusätzlich Branch-Regeln und Freigaben eingerichtet werden. Ein Environment allein ersetzt keine tatsächlich konfigurierte Schutzregel.
2. Die Jar liest Datenbank und Profil beim Start. Ein Vite-Build bindet `VITE_*` in JavaScript ein. Hier verwendet das Frontend eine relative API-Adresse und nginx einen separat konfigurierbaren Proxy.
3. Ein PR-Deploy könnte ungeprüften Code veröffentlichen. Die Bedingung auf `main` und den Ereignistyp lässt PRs prüfen, ohne den Deploy-Job auszuführen. Fork-PRs bekommen GitHub-Secrets standardmässig ohnehin nicht.
4. Aufräumen muss auch nach einem Fehler laufen. Unser Shell-`trap ... EXIT` löscht den temporären Key und entfernt die Runner-IP-Regel bei regulärem Verlassen des Skripts. Das erfüllt denselben Zweck wie ein separater `if: always()`-Schritt. Bei hart abgebrochenen Runnern kann eine Regel zurückbleiben und muss kontrolliert entfernt werden.

## EX-02

1. Im finalen Image fehlen Compiler, Maven/Node und Build-Cache. Backend: JRE plus Jar; Frontend: nginx plus statische Dateien.
2. `latest` kann überschrieben werden. Der Commit-Tag und Image-Digest dokumentieren eindeutig, welcher Quellcode beziehungsweise welches Image gemeint ist.
3. Das Backend benötigt DB-Werte erst beim Start. Dasselbe Image kann deshalb unverändert gegen eine lokale MariaDB oder RDS laufen. Vite-Variablen werden dagegen beim Build gebunden.
4. Beim Jar-Deploy muss EC2 Java und Dienstkonfiguration bereitstellen. Docker kapselt die Runtime mit der Anwendung und erleichtert reproduzierbares Starten; Container-Netzwerk, Registry und persistente Volumes müssen dafür bewusst eingerichtet werden. Im Frontend entfällt auf dem Ziel eine eigene Node-Buildumgebung.

## EX-03

1. ECS zieht Images, startet Ersatz-Tasks, kontrolliert deren Zustand und rollt neue Revisionen aus. Die Anwendung und Task-Definition müssen weiterhin passend konfiguriert sein.
2. OIDC liefert kurzlebige Credentials, deren Vertrauensregel auf Repo und Branch beschränkt werden kann. Ein gespeicherter SSH-Key oder langfristiger AWS-Key bleibt bis zur Sperrung verwendbar. Im Learner Lab wird stattdessen der kurzlebige Session-Zugang bewusst erneuert.
3. RDS verwaltet dauerhaften DB-Storage, Backups und Patching unabhängig von ersetzten Tasks. Eine Datenbank im flüchtigen Fargate-Dateisystem würde Daten beim Task-Wechsel verlieren.
4. Die DB-Adresse ist eine Konfiguration; das Passwort ist ein Geheimnis. ECS löst dessen Secret-ARN beim Start auf. In unserer Umsetzung heissen die Laufzeitvariablen `DB_URL`, `DB_USER` und `DB_PASSWORD`.
5. Beim Rolling Deployment starten neue Tasks, werden geprüft und lösen alte Tasks schrittweise ab. Zwei Tasks und die Mindestanzahl gesunder Tasks erlauben dabei weiterlaufenden Betrieb; die alte EC2-Einzelinstanz hat beim Prozessneustart eine Unterbrechung.
6. Ein Fargate-Task besitzt eine eigene Netzwerk-IP, keine von uns verwaltete EC2-Instance-ID. Deshalb registriert die Target Group IP-Adressen.

## EX-04

1. Kleine Dateireferenzen halten normale DB-Abfragen und Backups schlanker. BLOBs würden Bilder in DB-Storage, Übertragung und Backups einschliessen.
2. Die normalisierte Parent-Prüfung verhindert, dass Pfade wie `../../etc/passwd` ausserhalb des Storage-Verzeichnisses landen. Unsere zusätzliche Referenzprüfung lässt nur generierte Namen zu und lehnt Symlinks ab.
3. Das benannte Volume existiert unabhängig vom Container, aber auf genau dessen Docker-Host. Ein anderer Host hat dieses lokale Volume nicht automatisch.
4. Entscheidend ist, ob der Deploy den Ordner löscht. Ein `rsync --delete` über das ganze Arbeitsverzeichnis könnte benachbarte Uploads entfernen. Ein Transfer nur der Jar würde sie auch dort erhalten. Der separate Pfad `/var/lib/biztrips/uploads` vermeidet diese Kopplung dauerhaft.
5. Ein Task beseitigt lediglich die gleichzeitige Inkonsistenz zwischen A und B. Beim Ersatz oder Redeploy gehen lokale Dateien weiterhin verloren; Redundanz und Weiterbetrieb bei einem Task-Ausfall wären ebenfalls reduziert.
6. Konstruktor und Implementierung von `store`/`load` müssten einen gemeinsamen Speicher statt `Files` verwenden. `store(MultipartFile) -> String` und `load(String) -> Resource` können als Schnittstellen bestehen bleiben. In dieser RefCard wird S3 noch nicht angebunden.

