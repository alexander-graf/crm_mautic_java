import javax.swing.*;
import java.awt.*;
import java.net.URL;
import javax.swing.border.TitledBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.awt.Desktop;
import java.util.Locale;
import java.util.Date;
import com.toedter.calendar.JDateChooser;
import java.util.Properties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class CRMGUI extends JFrame {
    private JList<String> contactList;
    private DefaultListModel<String> contactModel;
    private JComboBox<String> serviceComboBox;
    private JSpinner hoursSpinner;
    private JTextField rateField;
    private JTextArea previewArea;
    private JButton previewButton;
    private JButton createButton;
    private MauticAPI mauticAPI;
    private DefaultListModel<String> serviceModel;
    private JList<String> serviceList;
    private JDateChooser invoiceDateChooser;  // Rechnungsdatum
    private JDateChooser serviceDateChooser;  // Leistungsdatum
    private Config config;  // Am Anfang der Klasse bei den anderen Variablen
    private JSpinner kmSpinner;  // Neues Feld für Kilometer
    private JPanel kmPanel;      // Panel für Kilometer-Eingabe
    private Map<String, MauticAPI.Contact> contactCache;  // Cache für Kontakte
    private static final Logger logger = LogManager.getLogger(CRMGUI.class);
    private JButton sendButton;
    
    public CRMGUI() {
        setTitle("CRM System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Icon setzen
        try {
            List<Image> icons = new ArrayList<>();
            String[] iconSizes = {"16", "32", "48", "64"};
            
            for (String size : iconSizes) {
                URL iconUrl = getClass().getResource("/icons/icon-" + size + ".png");
                if (iconUrl != null) {
                    icons.add(new ImageIcon(iconUrl).getImage());
                    logger.info("Icon geladen: icon-" + size + ".png");
                } else {
                    logger.warn("Icon nicht gefunden: icon-" + size + ".png");
                }
            }
            
            if (!icons.isEmpty()) {
                setIconImages(icons);
            } else {
                // Fallback: Versuche das Original-Icon
                URL iconUrl = getClass().getResource("/icons/digikam.png");
                if (iconUrl != null) {
                    setIconImage(new ImageIcon(iconUrl).getImage());
                    logger.info("Fallback-Icon geladen");
                } else {
                    logger.warn("Kein Icon gefunden");
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der App-Icons", e);
            e.printStackTrace();  // Für detailliertere Fehleranalyse
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Hauptlayout
        setLayout(new BorderLayout());

        // Menüleiste erstellen
        createMenuBar();

        // Linke Spalte (Kontakte)
        JPanel leftPanel = createContactPanel();
        add(leftPanel, BorderLayout.WEST);

        // Rechte Spalte
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Rechts oben (Dienstleistungen)
        JPanel servicePanel = createServicePanel();
        rightPanel.add(servicePanel, BorderLayout.NORTH);
        
        // Rechts unten (Vorschau)
        JPanel previewPanel = createPreviewPanel();
        rightPanel.add(previewPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.CENTER);

        addEventHandlers();

        contactCache = new HashMap<>();  // Cache initialisieren
        
        try {
            config = Config.load();
            mauticAPI = new MauticAPI("https://mautic.alexander-graf.de");
            loadContactsInitial();  // Initial laden mit Cache-Befüllung
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Laden der Konfiguration: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Datei");
        JMenu contactMenu = new JMenu("Kontakte");
        JMenu settingsMenu = new JMenu("Einstellungen");
        JMenu helpMenu = new JMenu("Hilfe");

        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(new JMenuItem("Neu"));
        fileMenu.add(new JMenuItem("Öffnen"));
        fileMenu.add(new JMenuItem("Speichern"));
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenuItem addContactItem = new JMenuItem("Kontakt hinzufügen");
        JMenuItem editContactItem = new JMenuItem("Kontakt bearbeiten");
        JMenuItem deleteContactItem = new JMenuItem("Kontakt löschen");
        deleteContactItem.addActionListener(e -> deleteContact());  // Verbinde mit der Löschfunktion

        contactMenu.add(addContactItem);
        contactMenu.add(editContactItem);
        contactMenu.add(deleteContactItem);

        JMenuItem reloadItem = new JMenuItem("Kontakte neu laden");
        reloadItem.addActionListener(e -> {
            try {
                loadContactsInitial();  // Nur wenn wirklich ein kompletter Reload gewünscht ist
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Laden der Kontakte: " + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        contactMenu.add(reloadItem);

        // Nur noch Rechnungsnummer-Dialog
        JMenuItem numberItem = new JMenuItem("Rechnungsnummer");
        numberItem.addActionListener(e -> {
            InvoiceNumberDialog dialog = new InvoiceNumberDialog(this, config);
            dialog.setVisible(true);
        });
        
        settingsMenu.add(numberItem);

        JMenuItem settingsItem = new JMenuItem("Einstellungen");
        settingsItem.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this, config);
            if (dialog.showDialog()) {
                // Speichere die neuen Einstellungen
                config.setMauticApiUrl(dialog.getApiUrl());
                config.setTemplatePath(dialog.getTemplatePath());
                config.setInvoicePath(dialog.getInvoicePath());
                try {
                    config.save();
                } catch (IOException ex) {
                    logger.error("Fehler beim Speichern der Konfiguration", ex);
                    JOptionPane.showMessageDialog(this,
                        "Fehler beim Speichern der Einstellungen: " + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        settingsMenu.add(settingsItem);

        menuBar.add(fileMenu);
        menuBar.add(contactMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        addContactItem.addActionListener(e -> createContact());
        editContactItem.addActionListener(e -> editContact());  // Verbinde mit der Bearbeitungsfunktion
    }

    private JPanel createContactPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Kontakte"));
        panel.setPreferredSize(new Dimension(320, 0));

        contactModel = new DefaultListModel<>();
        contactList = new JList<>(contactModel);

        JScrollPane scrollPane = new JScrollPane(contactList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button-Panel mit mehr Platz
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JButton newButton = new JButton("Neu");
        JButton editButton = new JButton("Bearbeiten");
        JButton deleteButton = new JButton("Löschen");
        
        newButton.addActionListener(e -> createContact());
        editButton.addActionListener(e -> editContact());
        deleteButton.addActionListener(e -> deleteContact());
        
        buttonPanel.add(newButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void deleteContact() {
        String selectedContact = contactList.getSelectedValue();
        if (selectedContact == null) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt zum Löschen aus.",
                "Kein Kontakt ausgewählt",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kontakt aus dem Cache holen
        MauticAPI.Contact contact = contactCache.get(selectedContact);
        if (contact != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Möchten Sie den Kontakt '" + selectedContact + "' wirklich löschen?",
                "Kontakt löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    mauticAPI.deleteContact(contact.id);
                    contactCache.remove(selectedContact);    // Aus Cache entfernen
                    contactModel.removeElement(selectedContact);  // Aus Liste entfernen
                    previewArea.setText("");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                        "Fehler beim Löschen des Kontakts: " + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }

    private JPanel createServicePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Dienstleistungen"));

        // Gemeinsame GridBagConstraints für alle Panels
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Datum Panel
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setBorder(new TitledBorder("Datum"));

        // Rechnungsdatum
        gbc.gridx = 0; gbc.gridy = 0;
        datePanel.add(new JLabel("Rechnungsdatum:"), gbc);
        invoiceDateChooser = new JDateChooser();
        invoiceDateChooser.setDate(new Date());
        invoiceDateChooser.setPreferredSize(new Dimension(150, 25));
        
        // PropertyChangeListener mit Flag
        final boolean[] isAdjusting = {false};
        invoiceDateChooser.getDateEditor().addPropertyChangeListener("date", e -> {
            if (!isAdjusting[0] && !isValidDate(invoiceDateChooser.getDate(), "Rechnungsdatum")) {
                isAdjusting[0] = true;
                invoiceDateChooser.setDate(new Date());
                isAdjusting[0] = false;
            }
        });
        gbc.gridx = 1;
        datePanel.add(invoiceDateChooser, gbc);

        // Leistungsdatum
        gbc.gridx = 0; gbc.gridy = 1;
        datePanel.add(new JLabel("Leistungsdatum:"), gbc);
        serviceDateChooser = new JDateChooser();
        serviceDateChooser.setDate(new Date());
        serviceDateChooser.setPreferredSize(new Dimension(150, 25));
        
        // PropertyChangeListener mit Flag
        final boolean[] isAdjustingService = {false};
        serviceDateChooser.getDateEditor().addPropertyChangeListener("date", e -> {
            if (!isAdjustingService[0]) {
                LocalDate serviceDate = serviceDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                LocalDate invoiceDate = invoiceDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                LocalDate today = LocalDate.now();

                isAdjustingService[0] = true;
                if (serviceDate.isAfter(today)) {
                    // Wenn Leistungsdatum in der Zukunft liegt, setze auf heute
                    serviceDateChooser.setDate(new Date());
                    JOptionPane.showMessageDialog(CRMGUI.this,
                        "Das Leistungsdatum darf nicht in der Zukunft liegen.",
                        "Ungültiges Datum",
                        JOptionPane.WARNING_MESSAGE);
                } else if (serviceDate.isAfter(invoiceDate)) {
                    // Wenn Leistungsdatum nach Rechnungsdatum liegt, setze auf Rechnungsdatum
                    serviceDateChooser.setDate(invoiceDateChooser.getDate());
                    JOptionPane.showMessageDialog(CRMGUI.this,
                        "Das Leistungsdatum wurde auf das Rechnungsdatum gesetzt.",
                        "Datum angepasst",
                        JOptionPane.INFORMATION_MESSAGE);
                }
                isAdjustingService[0] = false;
            }
        });
        gbc.gridx = 1;
        datePanel.add(serviceDateChooser, gbc);

        panel.add(datePanel, BorderLayout.NORTH);

        // Linke Seite: Eingabefelder
        JPanel inputPanel = new JPanel(new GridBagLayout());
        
        // Dienstleistungen
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.0;  // Label soll nicht wachsen
        gbc.fill = GridBagConstraints.HORIZONTAL;  // Füllt horizontal
        inputPanel.add(new JLabel("Dienstleistung:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;  // ComboBox soll den verfügbaren Platz nutzen
        serviceComboBox = new JComboBox<>(new String[]{
            "Fernwartung (60,00 €/Std)",
            "Vor-Ort-Service (60,00 €/Std)",
            "Anfahrt"
        });
        serviceComboBox.setPreferredSize(new Dimension(200, 25));
        serviceComboBox.setSelectedItem("Fernwartung (60,00 €/Std)");  // Setze initialen Wert
        inputPanel.add(serviceComboBox, gbc);

        // Kilometer-Panel
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        inputPanel.add(new JLabel("Kilometer:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        kmSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 200, 1));
        kmSpinner.setPreferredSize(new Dimension(80, 25));
        inputPanel.add(kmSpinner, gbc);

        // Stunden
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        inputPanel.add(new JLabel("Stunden:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        hoursSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 100.0, 0.5));
        hoursSpinner.setPreferredSize(new Dimension(80, 25));
        inputPanel.add(hoursSpinner, gbc);

        // Hinzufügen-Button
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = new JButton("Hinzufügen");
        inputPanel.add(addButton, gbc);

        panel.add(inputPanel, BorderLayout.WEST);

        // Rechte Seite: Liste der Posten
        serviceModel = new DefaultListModel<>();
        serviceList = new JList<>(serviceModel);
        JScrollPane scrollPane = new JScrollPane(serviceList);
        scrollPane.setPreferredSize(new Dimension(300, 0));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button-Panel für die Liste
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Entfernen");
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Event-Handler
        addButton.addActionListener(e -> addServiceItem());
        deleteButton.addActionListener(e -> deleteServiceItem());

        // Service-Auswahl Listener
        serviceComboBox.addActionListener(e -> {
            String selected = (String) serviceComboBox.getSelectedItem();
            boolean isAnfahrt = selected != null && selected.startsWith("Anfahrt");
            
            // Kilometer nur bei Anfahrt zeigen
            kmSpinner.setEnabled(isAnfahrt);
            kmSpinner.setVisible(isAnfahrt);
            inputPanel.getComponent(2).setVisible(isAnfahrt);  // "Kilometer:" Label
            
            // Stunden nur bei Fernwartung/Vor-Ort-Service zeigen
            hoursSpinner.setEnabled(!isAnfahrt);
            hoursSpinner.setVisible(!isAnfahrt);
            inputPanel.getComponent(4).setVisible(!isAnfahrt);  // "Stunden:" Label
            
            updatePreview();
        });

        // Initial die richtigen Komponenten verstecken
        kmSpinner.setVisible(false);
        kmSpinner.setEnabled(false);
        inputPanel.getComponent(2).setVisible(false);  // "Kilometer:" Label verstecken
        hoursSpinner.setVisible(true);
        hoursSpinner.setEnabled(true);

        return panel;
    }

    private void addServiceItem() {
        String selectedService = (String) serviceComboBox.getSelectedItem();
        
        if (selectedService.startsWith("Anfahrt")) {
            int km = (Integer) kmSpinner.getValue();
            double cost = calculateTravelCost(km);
            String item = String.format("Anfahrt (%d km): %.2f €", km, cost);
            serviceModel.addElement(item);
        } else {
            // Standardpreis für Dienstleistungen
            double rate = selectedService.startsWith("Fernwartung") ? 60.0 : 60.0;
            double hours = (Double) hoursSpinner.getValue();
            double total = hours * rate;

            String service = selectedService.split(" \\(")[0];  // Entferne Preisinfo
            String item = String.format("%s: %.1f Std. × %.2f € = %.2f €", 
                service, hours, rate, total);
            serviceModel.addElement(item);
        }
        updatePreview();
    }

    private double calculateTravelCost(int km) {
        if (km <= 10) {
            return 20.0;  // Pauschale bis 10 km
        }
        
        double cost = 20.0;  // Grundpauschale
        int remainingKm = km - 10;  // Verbleibende Kilometer

        if (remainingKm > 0) {
            if (km <= 20) {
                cost += remainingKm * 0.50;
            } else if (km <= 30) {
                cost += 10 * 0.50 + (remainingKm - 10) * 0.75;
            } else if (km <= 40) {
                cost += 10 * 0.50 + 10 * 0.75 + (remainingKm - 20) * 1.00;
            } else {
                cost += 10 * 0.50 + 10 * 0.75 + 10 * 1.00 + (remainingKm - 30) * 1.25;
            }
        }

        return cost;
    }

    private void deleteServiceItem() {
        int selectedIndex = serviceList.getSelectedIndex();
        if (selectedIndex != -1) {
            serviceModel.remove(selectedIndex);
            updatePreview();
        }
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Vorschau"));

        previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(previewArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        previewButton = new JButton("PDF Vorschau");
        createButton = new JButton("Rechnung erstellen");
        sendButton = new JButton("Erstellen & Senden");  // Hier initialisieren wir den Button
        
        buttonPanel.add(previewButton);
        buttonPanel.add(createButton);
        buttonPanel.add(sendButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addEventHandlers() {
        // Kontaktlisten-Handler
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedContactName = contactList.getSelectedValue();
                if (selectedContactName != null) {
                    // Kontakt aus dem Cache holen
                    MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);
                    if (selectedContact != null) {
                        // Debug-Ausgabe (optional)
                        System.out.println("\n=== Kontaktauswahl Debug ===");
                        System.out.println("Ausgewählter Kontakt: " + selectedContactName);
                        System.out.println("Aus Cache geladen:");
                        System.out.println("Vorname: " + selectedContact.firstname);
                        System.out.println("Nachname: " + selectedContact.lastname);
                        System.out.println("Firma: " + selectedContact.company);
                        System.out.println("Straße: " + selectedContact.street);
                        System.out.println("PLZ: " + selectedContact.zipcode);
                        System.out.println("Stadt: " + selectedContact.city);
                        
                        // Aktualisiere die Vorschau
                        updatePreview(selectedContact);
                    }
                } else {
                    previewArea.setText("");
                }
            }
        });

        // Service-Änderungs-Handler
        serviceComboBox.addActionListener(e -> updatePreview());
        hoursSpinner.addChangeListener(e -> updatePreview());

        // PDF-Button-Handler
        previewButton.addActionListener(e -> showPDFPreview());
        createButton.addActionListener(e -> createInvoice());

        // Kontaktauswahl-Listener
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedContactName = contactList.getSelectedValue();
                if (selectedContactName != null) {
                    // Kontakt aus dem Cache holen
                    MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);
                    if (selectedContact != null) {
                        // Aktualisiere die Vorschau oder andere UI-Elemente
                        updatePreview(selectedContact);
                    }
                }
            }
        });

        sendButton.addActionListener(e -> {
            if (validateInputs()) {
                try {
                    // Rechnung erstellen
                    String invoiceNumber = generateInvoiceNumber();
                    File pdfFile = generateInvoice(invoiceNumber, false);
                    
                    // E-Mail-Dialog anzeigen
                    MauticAPI.Contact selectedContact = contactCache.get(contactList.getSelectedValue());
                    EmailPreviewDialog dialog = new EmailPreviewDialog(
                        this, 
                        selectedContact, 
                        invoiceNumber, 
                        pdfFile,
                        config
                    );
                    
                    if (dialog.showDialog()) {
                        // Rechnungsnummer bestätigen nach erfolgreichem Versand
                        LocalDate invoiceDate = invoiceDateChooser.getDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                        config.confirmInvoiceNumber(invoiceDate.getYear());
                        
                        // Erfolgsmeldung
                        JOptionPane.showMessageDialog(this,
                            "Rechnung wurde erstellt und per E-Mail versendet.",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE);
                        resetForm();
                    }
                } catch (Exception ex) {
                    handleError("Fehler beim Erstellen/Senden der Rechnung", ex);
                }
            }
        });
    }

    private void updatePreview() {
        if (contactList.getSelectedValue() == null) {
            previewArea.setText("");
            return;
        }

        StringBuilder preview = new StringBuilder();
        preview.append("Rechnung für: ").append(contactList.getSelectedValue()).append("\n\n");
        
        double total = 0.0;
        for (int i = 0; i < serviceModel.size(); i++) {
            String item = serviceModel.getElementAt(i);
            preview.append(item).append("\n");
            
            try {
                if (item.contains("Anfahrt")) {
                    // Format: "Anfahrt (X km): YY,YY €"
                    String totalStr = item.substring(item.lastIndexOf(":") + 1)
                        .replace("€", "")  // Entferne €-Zeichen
                        .replace(",", ".")  // Ersetze Komma durch Punkt
                        .trim();           // Entferne Leerzeichen
                    total += Double.parseDouble(totalStr);
                } else {
                    // Format: "Service: X,X Std. × YY,YY € = ZZ,ZZ €"
                    String totalStr = item.substring(item.lastIndexOf("=") + 1)
                        .replace("€", "")  // Entferne €-Zeichen
                        .replace(",", ".")  // Ersetze Komma durch Punkt
                        .trim();           // Entferne Leerzeichen
                    total += Double.parseDouble(totalStr);
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Parsen von: " + item);
                e.printStackTrace();
            }
        }
        
        // Formatiere den Gesamtbetrag mit deutschem Format (Komma statt Punkt)
        preview.append("\nGesamtbetrag: ").append(String.format(Locale.GERMAN, "%.2f €", total));
        previewArea.setText(preview.toString());
    }

    private void updatePreview(MauticAPI.Contact contact) {
        // Aktualisiere die Vorschau mit den Kontaktdaten
        StringBuilder preview = new StringBuilder();
        preview.append("Kontaktdetails:\n\n");
        preview.append("Name: ").append(contact.firstname).append(" ").append(contact.lastname).append("\n");
        preview.append("E-Mail: ").append(contact.email).append("\n");
        preview.append("Firma: ").append(contact.company).append("\n");
        preview.append("Adresse:\n");
        preview.append(contact.street).append("\n");
        preview.append(contact.zipcode).append(" ").append(contact.city);
        
        previewArea.setText(preview.toString());
    }

    private List<PDFGenerator.ServiceItem> parseServiceItems() {
        List<PDFGenerator.ServiceItem> items = new ArrayList<>();
        
        for (int i = 0; i < serviceModel.size(); i++) {
            String item = serviceModel.getElementAt(i);
            
            if (item.contains("Anfahrt")) {
                // Format: "Anfahrt (X km): YY,YY €"
                String[] parts = item.split(":");
                String service = parts[0].trim();  // "Anfahrt (X km)"
                double cost = Double.parseDouble(
                    parts[1].replace("€", "").replace(",", ".").trim()
                );
                // Anfahrt als 1 Stunde mit dem berechneten Preis
                items.add(new PDFGenerator.ServiceItem(service, 1.0, cost));
            } else {
                // Format: "Service: X,X Std. × YY,YY € = ZZ,ZZ €"
                String[] parts = item.split(":");
                String service = parts[0].trim();
                String[] details = parts[1].split("×");
                double hours = Double.parseDouble(
                    details[0].replace("Std.", "").trim().replace(",", ".")
                );
                double rate = Double.parseDouble(
                    details[1].split("=")[0].replace("€", "").trim().replace(",", ".")
                );
                
                items.add(new PDFGenerator.ServiceItem(service, hours, rate));
            }
        }
        return items;
    }

    private boolean isValidDate(Date date, String fieldName) {
        LocalDate selectedDate = date.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();
        LocalDate today = LocalDate.now();
        
        // Prüfe auf Zukunftsdatum
        if (selectedDate.isAfter(today)) {
            JOptionPane.showMessageDialog(this,
                "Das " + fieldName + " darf nicht in der Zukunft liegen.",
                "Ungültiges Datum",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Prüfe Leistungsdatum im Verhältnis zum Rechnungsdatum
        if (fieldName.equals("Leistungsdatum")) {
            LocalDate invoiceDate = invoiceDateChooser.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            if (selectedDate.isAfter(invoiceDate)) {
                JOptionPane.showMessageDialog(this,
                    "Das Leistungsdatum darf nicht nach dem Rechnungsdatum liegen.",
                    "Ungültiges Datum",
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        
        return true;
    }

    private boolean isValidDates() {
        return isValidDate(invoiceDateChooser.getDate(), "Rechnungsdatum") &&
               isValidDate(serviceDateChooser.getDate(), "Leistungsdatum");
    }

    private void showPDFPreview() {
        if (contactList.getSelectedValue() == null || serviceModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt und fügen Sie mindestens eine Dienstleistung hinzu.",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidDates()) {
            return;
        }

        try {
            Path tempDir = Files.createTempDirectory("crm_preview");
            String tempPdfPath = tempDir.resolve("preview.pdf").toString();
            
            // Hole den ausgewählten Kontakt aus dem Cache
            String selectedContactName = contactList.getSelectedValue();
            MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);

            if (selectedContact == null) {
                throw new IOException("Kontakt nicht gefunden");
            }
            
            List<PDFGenerator.ServiceItem> items = parseServiceItems();
            config.setPreviewMode(true);
            PDFGenerator generator = new PDFGenerator(config);
            generator.generateInvoice(
                selectedContact,
                items,
                tempPdfPath,
                invoiceDateChooser.getDate(),
                serviceDateChooser.getDate()
            );
            config.setPreviewMode(false);
            
            // Öffne PDF mit dem Standardprogramm des Systems
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(tempPdfPath));
            } else {
                throw new IOException("Desktop-Integration nicht unterstützt");
            }

            // Registriere Aufräum-Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walk(tempDir)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler bei der PDF-Vorschau: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void createInvoice() {
        if (contactList.getSelectedValue() == null || serviceModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt und fügen Sie mindestens eine Dienstleistung hinzu.",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidDates()) {
            return;
        }

        try {
            // Hole den ausgewählten Kontakt aus dem Cache
            String selectedContactName = contactList.getSelectedValue();
            MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);
            
            if (selectedContact == null) {
                throw new IOException("Kontakt nicht gefunden");
            }

            // Erstelle Jahresordner
            int year = LocalDate.now().getYear();
            Path invoicePath = Paths.get(System.getProperty("user.home"), 
                "Nextcloud/IT-Service-Rechnungen", String.valueOf(year));
            Files.createDirectories(invoicePath);

            // Generiere Rechnungsnummer für Dateinamen
            config.setPreviewMode(false);
            PDFGenerator generator = new PDFGenerator(config);
            String invoiceNumber = generator.generateInvoiceNumber(selectedContact, invoiceDateChooser.getDate());

            // Generiere Dateinamen
            String fileName = String.format("Graf-Computer-Rechnung-%s-%s-%s-%s.pdf",
                invoiceNumber,
                selectedContact.lastname,
                selectedContact.firstname,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            Path pdfPath = invoicePath.resolve(fileName);

            // Generiere PDF
            generator.generateInvoice(
                selectedContact,
                parseServiceItems(),
                pdfPath.toString(),
                invoiceDateChooser.getDate(),
                serviceDateChooser.getDate()
            );

            // Öffne die erstellte PDF
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfPath.toFile());
            }

            // Speichere die erhöhte Rechnungsnummer erst jetzt
            // Übergebe das Jahr des Rechnungsdatums
            LocalDate invoiceDate = invoiceDateChooser.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            config.confirmInvoiceNumber(invoiceDate.getYear());

            JOptionPane.showMessageDialog(this,
                "PDF wurde erfolgreich erstellt!",
                "Erfolg",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Erstellen der PDF: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void editContact() {
        String selectedContact = contactList.getSelectedValue();
        if (selectedContact == null) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt zum Bearbeiten aus.",
                "Kein Kontakt ausgewählt",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        MauticAPI.Contact contact = contactCache.get(selectedContact);
        if (contact != null) {
            EditContactDialog dialog = new EditContactDialog(this, mauticAPI, contact);
            if (dialog.showDialog()) {
                try {
                    loadContactsInitial();
                } catch (IOException ex) {
                    logger.error("Fehler beim Neuladen der Kontakte", ex);
                }
            }
        }
    }

    private void createContact() {
        NewContactDialog dialog = new NewContactDialog(this, mauticAPI);
        if (dialog.showDialog()) {
            try {
                loadContactsInitial();
            } catch (IOException ex) {
                logger.error("Fehler beim Neuladen der Kontakte", ex);
            }
        }
    }

    private void loadContactsInitial() throws IOException {
        List<MauticAPI.Contact> contacts = mauticAPI.getContacts();
        contactModel.clear();
        contactCache.clear();
        
        for (MauticAPI.Contact contact : contacts) {
            String displayName = contact.toString();
            contactModel.addElement(displayName);
            contactCache.put(displayName, contact);  // Im Cache speichern
        }
    }

    private boolean validateInputs() {
        if (contactList.getSelectedValue() == null) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt aus.",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (serviceModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte fügen Sie mindestens eine Dienstleistung hinzu.",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return isValidDates();  // Diese Methode existiert bereits
    }

    private String generateInvoiceNumber() {
        // Hole den ausgewählten Kontakt aus dem Cache
        String selectedContactName = contactList.getSelectedValue();
        MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);
        
        config.setPreviewMode(false);
        PDFGenerator generator = new PDFGenerator(config);
        return generator.generateInvoiceNumber(selectedContact, invoiceDateChooser.getDate());
    }

    private File generateInvoice(String invoiceNumber, boolean isPreview) throws IOException {
        // Hole den ausgewählten Kontakt aus dem Cache
        String selectedContactName = contactList.getSelectedValue();
        MauticAPI.Contact selectedContact = contactCache.get(selectedContactName);
        
        if (isPreview) {
            Path tempDir = Files.createTempDirectory("crm_preview");
            String tempPdfPath = tempDir.resolve("preview.pdf").toString();
            config.setPreviewMode(true);
            PDFGenerator generator = new PDFGenerator(config);
            generator.generateInvoice(
                selectedContact,
                parseServiceItems(),
                tempPdfPath,
                invoiceDateChooser.getDate(),
                serviceDateChooser.getDate()
            );
            config.setPreviewMode(false);
            return new File(tempPdfPath);
        } else {
            // Erstelle Jahresordner
            int year = LocalDate.now().getYear();
            Path invoicePath = Paths.get(config.getInvoicePath(), String.valueOf(year));
            Files.createDirectories(invoicePath);

            // Generiere Dateinamen
            String fileName = String.format("Graf-Computer-Rechnung-%s-%s-%s-%s.pdf",
                invoiceNumber,
                selectedContact.lastname,
                selectedContact.firstname,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            Path pdfPath = invoicePath.resolve(fileName);
            
            PDFGenerator generator = new PDFGenerator(config);
            generator.generateInvoice(
                selectedContact,
                parseServiceItems(),
                pdfPath.toString(),
                invoiceDateChooser.getDate(),
                serviceDateChooser.getDate()
            );
            
            return pdfPath.toFile();
        }
    }

    private void resetForm() {
        if (serviceModel != null) {
            serviceModel.clear();
        }
        if (hoursSpinner != null) {
            hoursSpinner.setValue(0.0);  // Double für Stunden
        }
        if (kmSpinner != null) {
            kmSpinner.setValue(0);  // Integer für Kilometer
        }
        if (rateField != null) {
            rateField.setText("");
        }
        if (previewArea != null) {
            previewArea.setText("");
        }
    }

    private void handleError(String message, Exception ex) {
        logger.error(message, ex);
        JOptionPane.showMessageDialog(this,
            message + ": " + ex.getMessage(),
            "Fehler",
            JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new CRMGUI().setVisible(true);
        });
    }
} 