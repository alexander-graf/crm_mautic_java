import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class Config {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.config";
    private static final String CONFIG_FILE = "letzte_rechnungsnummer_crm_java.json";
    private static final Path CONFIG_PATH = Paths.get(CONFIG_DIR, CONFIG_FILE);
    private static final Gson gson = new Gson();

    private int lastInvoiceNumber = 0;
    private int lastYear = LocalDate.now().getYear();
    private boolean isPreview = false;
    private int tempNextNumber;
    private String mauticApiUrl = "https://mautic.alexander-graf.de";
    private String templatePath = System.getProperty("user.home") + "/Vorlagen/rechnungs_vorlage_graf.tex";
    private String invoicePath = System.getProperty("user.home") + "/Nextcloud/IT-Service-Rechnungen";
    private String smtpHost = "w0196dc7.kasserver.com";
    private int smtpPort = 587;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private String senderEmail = "rechnung@alexander-graf.de";
    private String emailTemplate = "Sehr geehrte(r) %%ANREDE%% %%NACHNAME%%,\n\n" +
        "anbei erhalten Sie die Rechnung %%RECHNUNGSNUMMER%% vom %%RECHNUNGSDATUM%%.\n\n" +
        "Mit freundlichen Grüßen\n" +
        "Alexander Graf";

    public static Config load() throws IOException {
        Files.createDirectories(Paths.get(CONFIG_DIR));
        if (Files.exists(CONFIG_PATH)) {
            String json = Files.readString(CONFIG_PATH);
            return gson.fromJson(json, Config.class);
        }
        return new Config();
    }

    public void save() throws IOException {
        String json = gson.toJson(this);
        Files.writeString(CONFIG_PATH, json);
    }

    public int getNextInvoiceNumber(int requestedYear) {
        int currentYear = LocalDate.now().getYear();
        
        if (currentYear > lastYear && !isPreview) {
            lastYear = currentYear;
            lastInvoiceNumber = 1;
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 1;
        }

        if (!isPreview) {
            tempNextNumber = lastInvoiceNumber;
            return tempNextNumber;
        }
        return lastInvoiceNumber;
    }

    public void setLastInvoiceNumber(int number) {
        this.lastInvoiceNumber = number;
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastInvoiceNumber() {
        return lastInvoiceNumber;
    }

    public void setPreviewMode(boolean preview) {
        this.isPreview = preview;
    }

    public void confirmInvoiceNumber(int year) {
        if (!isPreview && year == LocalDate.now().getYear() && 
            tempNextNumber == lastInvoiceNumber) {
            lastInvoiceNumber = tempNextNumber + 1;
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMauticApiUrl() {
        return mauticApiUrl;
    }
    
    public void setMauticApiUrl(String url) {
        this.mauticApiUrl = url;
    }
    
    public String getTemplatePath() {
        return templatePath;
    }
    
    public void setTemplatePath(String path) {
        this.templatePath = path;
    }
    
    public String getInvoicePath() {
        return invoicePath;
    }
    
    public void setInvoicePath(String path) {
        this.invoicePath = path;
    }

    // Getter und Setter für E-Mail-Einstellungen
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String host) {
        this.smtpHost = host;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int port) {
        this.smtpPort = port;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String username) {
        this.smtpUsername = username;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String password) {
        this.smtpPassword = password;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String email) {
        this.senderEmail = email;
    }

    public String getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(String template) {
        this.emailTemplate = template;
    }
} 