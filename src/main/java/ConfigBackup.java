import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConfigBackup {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static class BackupConfig {
        public String mauticApiUrl;
        public String templatePath;
        public String invoicePath;
        public String smtpHost;
        public int smtpPort;
        public String smtpUsername;
        public String senderEmail;
        public String emailTemplate;
        // Sensitive Daten werden nicht gesichert:
        // - access_token.json
        // - smtpPassword
    }
    
    public static void createBackup(Config config, String backupPath) throws IOException {
        // Backup-Konfiguration erstellen
        BackupConfig backupConfig = new BackupConfig();
        backupConfig.mauticApiUrl = config.getMauticApiUrl();
        backupConfig.templatePath = config.getTemplatePath();
        backupConfig.invoicePath = config.getInvoicePath();
        backupConfig.smtpHost = config.getSmtpHost();
        backupConfig.smtpPort = config.getSmtpPort();
        backupConfig.smtpUsername = config.getSmtpUsername();
        backupConfig.senderEmail = config.getSenderEmail();
        backupConfig.emailTemplate = config.getEmailTemplate();

        // Temporäres Verzeichnis für Backup erstellen
        Path tempDir = Files.createTempDirectory("crm_backup");
        
        try {
            // Config-JSON speichern
            String configJson = gson.toJson(backupConfig);
            Files.writeString(tempDir.resolve("config.json"), configJson);
            
            // Rechnungsnummer-Datei kopieren
            Path rechnungsnummerSrc = Paths.get(System.getProperty("user.home"), 
                ".config", "letzte_rechnungsnummer_crm_java.json");
            if (Files.exists(rechnungsnummerSrc)) {
                Files.copy(rechnungsnummerSrc, 
                    tempDir.resolve("letzte_rechnungsnummer_crm_java.json"));
            }
            
            // Access Token kopieren
            Path accessTokenSrc = Paths.get(System.getProperty("user.home"), 
                ".config", "access_token.json");
            if (Files.exists(accessTokenSrc)) {
                Files.copy(accessTokenSrc, 
                    tempDir.resolve("access_token.json"));
            }
            
            // LaTeX-Vorlage kopieren
            Path templateSrc = Paths.get(config.getTemplatePath());
            if (Files.exists(templateSrc)) {
                Files.copy(templateSrc, 
                    tempDir.resolve("rechnungs_vorlage_graf.tex"));
            }

            // ZIP erstellen
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String zipName = "crm_backup_" + timestamp + ".zip";
            Path zipPath = Paths.get(backupPath, zipName);
            
            try (ZipOutputStream zos = new ZipOutputStream(
                    new FileOutputStream(zipPath.toFile()))) {
                Files.walk(tempDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            ZipEntry zipEntry = new ZipEntry(
                                tempDir.relativize(path).toString());
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            }
        } finally {
            // Temporäres Verzeichnis aufräumen
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    public static void restoreBackup(String zipPath, Config config) throws IOException {
        Path tempDir = Files.createTempDirectory("crm_restore");
        
        try {
            // ZIP entpacken
            try (ZipInputStream zis = new ZipInputStream(
                    new FileInputStream(zipPath))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path targetPath = tempDir.resolve(zipEntry.getName());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                }
            }
            
            // Config wiederherstellen
            String configJson = Files.readString(tempDir.resolve("config.json"));
            BackupConfig backupConfig = gson.fromJson(configJson, BackupConfig.class);
            
            config.setMauticApiUrl(backupConfig.mauticApiUrl);
            config.setTemplatePath(backupConfig.templatePath);
            config.setInvoicePath(backupConfig.invoicePath);
            config.setSmtpHost(backupConfig.smtpHost);
            config.setSmtpPort(backupConfig.smtpPort);
            config.setSmtpUsername(backupConfig.smtpUsername);
            config.setSenderEmail(backupConfig.senderEmail);
            config.setEmailTemplate(backupConfig.emailTemplate);
            
            // Dateien wiederherstellen
            Path configDir = Paths.get(System.getProperty("user.home"), ".config");
            Path crmConfigDir = configDir.resolve("crm-gui");
            Files.createDirectories(crmConfigDir);
            
            // Rechnungsnummer wiederherstellen
            Path rechnungsnummerSrc = tempDir.resolve(
                "letzte_rechnungsnummer_crm_java.json");
            if (Files.exists(rechnungsnummerSrc)) {
                Files.copy(rechnungsnummerSrc, 
                    configDir.resolve("letzte_rechnungsnummer_crm_java.json"), 
                    StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Access Token wiederherstellen
            Path accessTokenSrc = tempDir.resolve("access_token.json");
            if (Files.exists(accessTokenSrc)) {
                Files.copy(accessTokenSrc, 
                    configDir.resolve("access_token.json"), 
                    StandardCopyOption.REPLACE_EXISTING);
            }
            
            // LaTeX-Vorlage wiederherstellen
            Path templateSrc = tempDir.resolve("rechnungs_vorlage_graf.tex");
            if (Files.exists(templateSrc)) {
                Path templateTarget = Paths.get(backupConfig.templatePath);
                Files.createDirectories(templateTarget.getParent());
                Files.copy(templateSrc, templateTarget, 
                    StandardCopyOption.REPLACE_EXISTING);
            }
            
            config.save();
        } finally {
            // Temporäres Verzeichnis aufräumen
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
} 