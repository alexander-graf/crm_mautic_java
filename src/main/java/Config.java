import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.config";
    private static final String CONFIG_FILE = "letzte_rechnungsnummer_crm_java.json";
    private static final Path CONFIG_PATH = Paths.get(CONFIG_DIR, CONFIG_FILE);
    private static final Gson gson = new Gson();

    private int lastInvoiceNumber = 0;
    private String invoicePrefix = "RB";  // Default-Wert
    private boolean isPreview = false;  // Neues Flag für Vorschau
    private int tempNextNumber;

    public static Config load() throws IOException {
        // Erstelle .config Verzeichnis falls nicht vorhanden
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

    public int getNextInvoiceNumber() {
        if (!isPreview) {
            tempNextNumber = lastInvoiceNumber;
            return tempNextNumber;
        }
        return lastInvoiceNumber;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String prefix) {
        this.invoicePrefix = prefix;
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void confirmInvoiceNumber() {
        if (!isPreview && tempNextNumber == lastInvoiceNumber) {
            lastInvoiceNumber = tempNextNumber + 1;
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
} 