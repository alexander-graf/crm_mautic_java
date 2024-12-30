import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class PDFGenerator {
    private final String templatePath;
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    public PDFGenerator() {
        this.templatePath = System.getProperty("user.home") + "/Vorlagen/rechnungs_vorlage_graf.tex";
    }

    public void generateInvoice(MauticAPI.Contact contact, List<ServiceItem> items, 
            String outputPath, Date invoiceDate, Date serviceDate) throws IOException {
        System.out.println("=== PDF Generierung startet ===");
        System.out.println("Template Pfad: " + templatePath);
        System.out.println("Output Pfad: " + outputPath);
        System.out.println("Kunde: " + contact.firstname + " " + contact.lastname);
        
        String template = Files.readString(Paths.get(templatePath));
        System.out.println("Template geladen, Länge: " + template.length());
        
        // Formatiere die Daten
        String invoiceDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
            .format(invoiceDate);
        String serviceDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
            .format(serviceDate);
        
        // Ersetze Platzhalter
        String tex = template
            .replace("\\newcommand{\\rechnungsdatum}{\\VAR{01.06.2023}}", 
                    "\\newcommand{\\rechnungsdatum}{\\VAR{" + invoiceDateStr + "}}")
            .replace("\\newcommand{\\leistungsdatum}{\\VAR{15.05.2023}}", 
                    "\\newcommand{\\leistungsdatum}{\\VAR{" + serviceDateStr + "}}")
            .replace("\\newcommand{\\rechnungsnummer}{\\VAR{RE2023-001}}", 
                    "\\newcommand{\\rechnungsnummer}{\\VAR{" + generateInvoiceNumber() + "}}");

        // Kundenname aufteilen
        String[] nameParts = (contact.firstname + " " + contact.lastname).split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Anrede generieren
        String anrede = "Sehr geehrte";
        if (firstName.endsWith("a") || firstName.endsWith("e")) {
            anrede += " Frau";
        } else {
            anrede += "r Herr";
        }

        tex = tex
            .replace("\\newcommand{\\anrede}{\\VAR{Sehr geehrter Herr Mustermann,}}", 
                    "\\newcommand{\\anrede}{\\VAR{" + anrede + " " + contact.lastname + ",}}")
            .replace("\\newcommand{\\vorname}{\\VAR{Max}}", 
                    "\\newcommand{\\vorname}{\\VAR{" + contact.firstname + "}}")
            .replace("\\newcommand{\\name}{\\VAR{Mustermann}}", 
                    "\\newcommand{\\name}{\\VAR{" + contact.lastname + "}}")
            // Adressfelder mit den tatsächlichen Werten
            .replace("\\newcommand{\\firma}{\\VAR{}}",
                    "\\newcommand{\\firma}{\\VAR{" + (contact.company != null ? contact.company : "") + "}}")
            .replace("\\newcommand{\\strasse}{\\VAR{Musterstraße}}",
                    "\\newcommand{\\strasse}{\\VAR{" + (contact.street != null ? contact.street : "") + "}}")
            .replace("\\newcommand{\\hausnummer}{\\VAR{123}}",
                    "\\newcommand{\\hausnummer}{\\VAR{" + (contact.number != null ? contact.number : "") + "}}")
            .replace("\\newcommand{\\postleitzahl}{\\VAR{12345}}",
                    "\\newcommand{\\postleitzahl}{\\VAR{" + (contact.zipcode != null ? contact.zipcode : "") + "}}")
            .replace("\\newcommand{\\ort}{\\VAR{Musterstadt}}",
                    "\\newcommand{\\ort}{\\VAR{" + (contact.city != null ? contact.city : "") + "}}");

        // Debug-Ausgabe der Adressfelder
        System.out.println("\n=== Adressfelder für LaTeX ===");
        System.out.println("Firma: " + contact.company);
        System.out.println("Straße: " + contact.street);
        System.out.println("Hausnummer: " + contact.number);
        System.out.println("PLZ: " + contact.zipcode);
        System.out.println("Ort: " + contact.city);

        // Dienstleistungen eintragen
        AtomicInteger pos = new AtomicInteger(1);
        double total = 0.0;

        // Leere alle Positionen zuerst
        for (char c = 'A'; c <= 'D'; c++) {
            String posChar = String.valueOf(c);
            tex = tex
                .replace("\\newcommand{\\pos" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\pos" + posChar + "}{\\VAR{}}")
                .replace("\\newcommand{\\bez" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\bez" + posChar + "}{\\VAR{}}")
                .replace("\\newcommand{\\anz" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\anz" + posChar + "}{\\VAR{}}")
                .replace("\\newcommand{\\einzel" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\einzel" + posChar + "}{\\VAR{}}")
                .replace("\\newcommand{\\sum" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\sum" + posChar + "}{\\VAR{}}");
        }

        // Fülle die vorhandenen Positionen
        for (int i = 0; i < items.size() && i < 4; i++) {
            ServiceItem item = items.get(i);
            String posChar = String.valueOf((char)('A' + i));
            
            tex = tex
                .replace("\\newcommand{\\pos" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\pos" + posChar + "}{" + pos.getAndIncrement() + "}")
                .replace("\\newcommand{\\bez" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\bez" + posChar + "}{" + item.service + "}")
                .replace("\\newcommand{\\anz" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\anz" + posChar + "}{" + String.format(Locale.GERMAN, "%.1f", item.hours) + "}")
                .replace("\\newcommand{\\einzel" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\einzel" + posChar + "}{" + String.format(Locale.GERMAN, "%.2f", item.rate) + "}")
                .replace("\\newcommand{\\sum" + posChar + "}{\\VAR{}}", 
                        "\\newcommand{\\sum" + posChar + "}{" + String.format(Locale.GERMAN, "%.2f", item.total) + "}");
            
            total += item.total;
        }

        tex = tex.replace("\\newcommand{\\Endsumme}{\\VAR{400,00 €}}", 
                         "\\newcommand{\\Endsumme}{\\VAR{" + String.format(Locale.GERMAN, "%.2f", total) + "}}");

        // Debug-Ausgabe für die Positionen
        System.out.println("\n=== Positionen ===");
        for (int i = 0; i < items.size(); i++) {
            ServiceItem item = items.get(i);
            System.out.printf("Position %d: %s, %.1f Std, %.2f €%n", 
                i+1, item.service, item.hours, item.rate);
        }

        // Vor dem Schreiben der .tex Datei
        System.out.println("\n=== Generierte .tex Datei ===");
        System.out.println("Erste 500 Zeichen:");
        System.out.println(tex.substring(0, Math.min(500, tex.length())));
        System.out.println("...");
        System.out.println("Letzte 500 Zeichen:");
        System.out.println(tex.substring(Math.max(0, tex.length() - 500)));

        // Speichere temporäre .tex Datei
        Path tempTexPath = Paths.get(outputPath.replace(".pdf", ".tex"));
        Files.writeString(tempTexPath, tex);
        System.out.println("\n=== .tex Datei geschrieben ===");
        System.out.println("Pfad: " + tempTexPath.toAbsolutePath());

        // Kompiliere PDF mit xelatex (zwei Durchläufe)
        System.out.println("\n=== XeLaTeX Ausführung (1. Durchlauf) ===");
        ProcessBuilder pb = new ProcessBuilder(
            "xelatex",
            "--interaction=nonstopmode",
            "--output-directory=" + tempTexPath.getParent().toString(),
            tempTexPath.toString()
        );
        pb.directory(tempTexPath.getParent().toFile());
        
        System.out.println("Arbeitsverzeichnis: " + pb.directory().getAbsolutePath());
        System.out.println("Kommando: " + String.join(" ", pb.command()));
        
        try {
            // Erster Durchlauf
            Process p = pb.start();
            readProcessOutput(p);
            int exitCode = p.waitFor();
            
            if (exitCode != 0) {
                handleXeLatexError(exitCode, tempTexPath);
            }

            // Zweiter Durchlauf
            System.out.println("\n=== XeLaTeX Ausführung (2. Durchlauf) ===");
            p = pb.start();
            readProcessOutput(p);
            exitCode = p.waitFor();
            
            if (exitCode != 0) {
                handleXeLatexError(exitCode, tempTexPath);
            }

            System.out.println("\n=== Aufräumen ===");
            // Aufräumen - temporäre Dateien löschen
            String baseName = tempTexPath.toString().replace(".tex", "");
            System.out.println("Lösche: " + baseName + ".tex");
            Files.deleteIfExists(Paths.get(baseName + ".tex"));
            System.out.println("Lösche: " + baseName + ".aux");
            Files.deleteIfExists(Paths.get(baseName + ".aux"));
            System.out.println("Lösche: " + baseName + ".log");
            Files.deleteIfExists(Paths.get(baseName + ".log"));

        } catch (InterruptedException e) {
            System.out.println("\n=== Prozess unterbrochen ===");
            Thread.currentThread().interrupt();
            throw new IOException("PDF generation interrupted", e);
        }
        
        System.out.println("\n=== PDF Generierung abgeschlossen ===");
    }

    private void readProcessOutput(Process p) throws IOException {
        // Lese die Standardausgabe
        System.out.println("\n=== XeLaTeX Standardausgabe ===");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("STDOUT: " + line);
            }
        }
        
        // Lese die Fehlerausgabe
        System.out.println("\n=== XeLaTeX Fehlerausgabe ===");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("STDERR: " + line);
            }
        }
    }

    private void handleXeLatexError(int exitCode, Path tempTexPath) throws IOException {
        // Speichere die Log-Datei für Debugging
        String logContent = "";
        Path logFile = tempTexPath.resolveSibling(tempTexPath.getFileName().toString().replace(".tex", ".log"));
        if (Files.exists(logFile)) {
            System.out.println("\n=== XeLaTeX Log Datei ===");
            logContent = Files.readString(logFile);
            System.out.println(logContent);
        } else {
            System.out.println("Keine Log-Datei gefunden!");
        }
        
        throw new IOException("XeLaTeX compilation failed with exit code: " + exitCode + 
            "\nLog file content: " + logContent);
    }

    private String generateInvoiceNumber() {
        return "RE" + LocalDate.now().getYear() + 
               "-" + String.format("%03d", getNextInvoiceNumber());
    }

    private int getNextInvoiceNumber() {
        // TODO: Implementiere Rechnungsnummern-Verwaltung
        return 1;
    }

    public static class ServiceItem {
        final String service;
        final double hours;
        final double rate;
        final double total;

        public ServiceItem(String service, double hours, double rate) {
            this.service = service;
            this.hours = hours;
            this.rate = rate;
            this.total = hours * rate;
        }
    }
} 