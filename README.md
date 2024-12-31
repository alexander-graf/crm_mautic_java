# CRM & Rechnungssystem

Ein Java-basiertes CRM-System zur Kontaktverwaltung und automatischen Rechnungserstellung mit PDF-Export und E-Mail-Versand.

## Features

- **Kontaktverwaltung**
  - Integration mit Mautic CRM
  - Kontakte erstellen, bearbeiten und löschen
  - Automatische Synchronisation

- **Rechnungserstellung**
  - Automatische Rechnungsnummern-Generierung
  - PDF-Generierung basierend auf LaTeX-Vorlagen
  - Vorschau-Funktion
  - Jahresweise Archivierung

- **Dienstleistungsverwaltung**
  - Vordefinierte Dienstleistungen (Fernwartung, Vor-Ort-Service)
  - Stundensatz- und Kilometerberechnung
  - Flexible Preisgestaltung

- **E-Mail-Integration**
  - Automatischer Rechnungsversand
  - Konfigurierbare E-Mail-Templates
  - SMTP-Unterstützung
  - Vorschau vor Versand

## Voraussetzungen

- Java 11 oder höher
- LaTeX-Installation (z.B. TexLive)
- Mautic CRM-Installation
- Maven für die Kompilierung

### Benötigte Verzeichnisse
Die folgenden Verzeichnisse werden benötigt:

- `~/.config/crm-gui/` - Konfigurationsverzeichnis
  - `letzte_rechnungsnummer_crm_java.json` - Speichert die letzte Rechnungsnummer
  - `access_token.json` - Mautic API Token
  - `logs/` - Log-Dateien

- `~/Vorlagen/` - LaTeX-Vorlagen
  - `rechnungs_vorlage_graf.tex` - Template für Rechnungen

- `~/Nextcloud/IT-Service-Rechnungen/` - Rechnungsarchiv
  - Jahresweise Unterverzeichnisse (z.B. `2024/`)
  - PDF-Rechnungen werden hier automatisch abgelegt

## Installation

1. Repository klonen:
   ```bash
   git clone https://github.com/alexandergraf/crm-gui.git
   ```

2. Mit Maven kompilieren:
   ```bash
   mvn clean package
   ```

3. JAR-Datei nach `~/bin` kopieren:
   ```bash
   cp target/crm-gui-1.0-jar-with-dependencies.jar ~/bin/
   ```

4. Ausführbare Datei erstellen:
   ```bash
   cp crm ~/bin/
   chmod +x ~/bin/crm
   ```

5. Konfigurationsverzeichnis anlegen:
   ```bash
   mkdir -p ~/.config/crm-gui/logs
   ```

6. Mautic API Token in `~/.config/crm-gui/access_token.json` hinterlegen

## Konfiguration

### Mautic-API
- URL: Ihre Mautic-Installation (z.B. https://mautic.example.com)
- API-Token in `access_token.json` hinterlegen

### E-Mail-Einstellungen
In den Einstellungen konfigurieren:
- SMTP-Server
- Port (Standard: 587)
- Benutzername
- Passwort
- Absender-E-Mail
- E-Mail-Template

## Verwendung

Das Programm kann über das Terminal gestartet werden:
```bash
crm
```

Oder durch Ausführen der JAR-Datei:
```bash
java -jar ~/bin/crm-gui-1.0-jar-with-dependencies.jar
```

### Workflow
1. **Kontakt auswählen** oder neu anlegen
2. **Dienstleistungen hinzufügen**
   - Art der Dienstleistung wählen
   - Stunden/Kilometer eingeben
   - Stundensatz anpassen (optional)
3. **Rechnung erstellen**
   - "PDF Vorschau" für Vorschau
   - "Rechnung erstellen" für finale PDF
   - "Erstellen & Senden" für PDF-Erstellung und E-Mail-Versand

## Entwicklung

Das Projekt verwendet:
- Java Swing für die GUI
- FlatLaf für modernes Look & Feel
- JavaMail für E-Mail-Funktionalität
- PDFLaTeX für PDF-Generierung
- Log4j für Logging

## Lizenz

MIT License

## Autor

Alexander Graf