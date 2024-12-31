import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class ConfigBackup {
    private static final Logger logger = LogManager.getLogger(ConfigBackup.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.config/crm-gui";
    private static final String HOME_DIR = System.getProperty("user.home");

    // Struktur für Backup-Dateien mit Quelle und Ziel im ZIP
    private static class BackupFile {
        final Path sourcePath;
        final String zipPath;

        BackupFile(Path sourcePath, String zipPath) {
            this.sourcePath = sourcePath;
            this.zipPath = zipPath;
        }
    }

    public static void createBackup(Config config) throws IOException {
        // Erstelle Liste aller zu sichernden Dateien
        List<BackupFile> backupFiles = new ArrayList<>();

        // CRM-GUI Konfigurationsdateien
        backupFiles.add(new BackupFile(
            Paths.get(HOME_DIR, ".config/access_token.json"),
            "config/access_token.json"
        ));
        backupFiles.add(new BackupFile(
            Paths.get(HOME_DIR, ".config/letzte_rechnungsnummer_crm_java.json"),
            "config/letzte_rechnungsnummer_crm_java.json"
        ));
        backupFiles.add(new BackupFile(
            Paths.get(CONFIG_DIR, "config.json"),
            "config/config.json"
        ));

        // LaTeX Vorlage
        backupFiles.add(new BackupFile(
            Paths.get(config.getTemplatePath()),
            "templates/rechnungs_vorlage_graf.tex"
        ));

        // Log-Verzeichnis
        Path logsDir = Paths.get(CONFIG_DIR, "logs");
        if (Files.exists(logsDir)) {
            try (Stream<Path> paths = Files.walk(logsDir)) {
                paths.filter(path -> !Files.isDirectory(path))
                     .forEach(path -> backupFiles.add(new BackupFile(
                         path,
                         "logs/" + logsDir.relativize(path).toString()
                     )));
            }
        }

        // Erstelle Backup-Verzeichnis
        Path backupDir = Paths.get(config.getBackupPath());
        Files.createDirectories(backupDir);

        // Generiere Backup-Dateiname mit Zeitstempel
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String zipFileName = "config_backup_" + timestamp + ".zip";
        Path zipPath = backupDir.resolve(zipFileName);

        // Erstelle ZIP mit allen Dateien
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (BackupFile file : backupFiles) {
                if (Files.exists(file.sourcePath)) {
                    addToZip(file.sourcePath, file.zipPath, zos);
                } else {
                    logger.warn("Datei existiert nicht und wird übersprungen: " + file.sourcePath);
                }
            }
            logger.info("Backup erstellt: " + zipPath);
        }
    }

    private static void addToZip(Path file, String zipPath, ZipOutputStream zos) {
        try {
            ZipEntry entry = new ZipEntry(zipPath);
            zos.putNextEntry(entry);
            Files.copy(file, zos);
            zos.closeEntry();
            logger.debug("Datei zum Backup hinzugefügt: " + zipPath);
        } catch (IOException e) {
            logger.error("Fehler beim Hinzufügen von " + file + " zum Backup", e);
        }
    }

    public static void restoreBackup(File zipFile) throws IOException {
        // Erstelle temporäres Verzeichnis für die Extraktion
        Path tempDir = Files.createTempDirectory("config_restore_");
        
        // Extrahiere ZIP in temporäres Verzeichnis
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = tempDir.resolve(entry.getName());
                
                // Erstelle Verzeichnisse, falls nötig
                Files.createDirectories(targetPath.getParent());
                
                // Extrahiere Datei
                Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }

        // Stelle Dateien wieder her
        restoreFile(tempDir, "config/access_token.json", 
            Paths.get(HOME_DIR, ".config/access_token.json"));
        restoreFile(tempDir, "config/letzte_rechnungsnummer_crm_java.json", 
            Paths.get(HOME_DIR, ".config/letzte_rechnungsnummer_crm_java.json"));
        restoreFile(tempDir, "config/config.json", Paths.get(CONFIG_DIR, "config.json"));
        restoreFile(tempDir, "templates/rechnungs_vorlage_graf.tex", 
            Paths.get(HOME_DIR, "Vorlagen/rechnungs_vorlage_graf.tex"));

        // Stelle Logs wieder her
        Path logsSource = tempDir.resolve("logs");
        if (Files.exists(logsSource)) {
            Path logsTarget = Paths.get(CONFIG_DIR, "logs");
            try (Stream<Path> paths = Files.walk(logsSource)) {
                paths.filter(path -> !Files.isDirectory(path))
                     .forEach(source -> {
                         try {
                             Path target = logsTarget.resolve(logsSource.relativize(source));
                             Files.createDirectories(target.getParent());
                             Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                         } catch (IOException e) {
                             logger.error("Fehler beim Wiederherstellen von " + source, e);
                         }
                     });
            }
        }

        // Lösche temporäres Verzeichnis
        try (Stream<Path> walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.error("Fehler beim Löschen des temporären Verzeichnisses", e);
                    }
                });
        }
        
        logger.info("Backup wiederhergestellt von: " + zipFile);
    }

    private static void restoreFile(Path tempDir, String sourcePath, Path targetPath) throws IOException {
        Path source = tempDir.resolve(sourcePath);
        if (Files.exists(source)) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Datei wiederhergestellt: " + targetPath);
        } else {
            logger.warn("Datei nicht im Backup gefunden: " + sourcePath);
        }
    }
} 