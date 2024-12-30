import javax.swing.*;
import java.awt.*;
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
    
    public CRMGUI() {
        setTitle("CRM System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

        try {
            config = Config.load();  // Lade Konfiguration
            mauticAPI = new MauticAPI("https://mautic.alexander-graf.de");
            loadContacts();  // Initial laden
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

        contactMenu.add(new JMenuItem("Kontakt hinzufügen"));
        contactMenu.add(new JMenuItem("Kontakt bearbeiten"));
        contactMenu.add(new JMenuItem("Kontakt löschen"));

        JMenuItem reloadItem = new JMenuItem("Kontakte neu laden");
        reloadItem.addActionListener(e -> loadContacts());
        contactMenu.add(reloadItem);

        // Nur noch Rechnungsnummer-Dialog
        JMenuItem numberItem = new JMenuItem("Rechnungsnummer");
        numberItem.addActionListener(e -> {
            InvoiceNumberDialog dialog = new InvoiceNumberDialog(this, config);
            dialog.setVisible(true);
        });
        
        settingsMenu.add(numberItem);

        menuBar.add(fileMenu);
        menuBar.add(contactMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createContactPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Kontakte"));
        panel.setPreferredSize(new Dimension(250, 0));

        contactModel = new DefaultListModel<>();
        contactModel.addElement("Max Mustermann");
        contactModel.addElement("Erika Musterfrau");
        contactList = new JList<>(contactModel);

        JScrollPane scrollPane = new JScrollPane(contactList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(new JButton("Neu"));
        buttonPanel.add(new JButton("Bearbeiten"));
        buttonPanel.add(new JButton("Löschen"));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createServicePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Dienstleistungen"));

        // Gemeinsame GridBagConstraints für beide Panels
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
        serviceComboBox = new JComboBox<>(new String[]{
            "Beratung", "Entwicklung", "Design", "Support"
        });
        inputPanel.add(new JLabel("Dienstleistung:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(serviceComboBox, gbc);

        // Stunden
        hoursSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 0.5));
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Stunden:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(hoursSpinner, gbc);

        // Stundensatz
        rateField = new JTextField("85.00", 10);
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("€/Stunde:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(rateField, gbc);

        // Hinzufügen-Button
        JButton addButton = new JButton("Hinzufügen");
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
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

        return panel;
    }

    private void addServiceItem() {
        String service = (String) serviceComboBox.getSelectedItem();
        double hours = (Double) hoursSpinner.getValue();
        double rate = Double.parseDouble(rateField.getText());
        double total = hours * rate;

        String item = String.format("%s: %.1f Std. × %.2f € = %.2f €", 
            service, hours, rate, total);
        serviceModel.addElement(item);
        updatePreview();
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
        JScrollPane scrollPane = new JScrollPane(previewArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        previewButton = new JButton("PDF Vorschau");
        createButton = new JButton("Rechnung erstellen");
        buttonPanel.add(previewButton);
        buttonPanel.add(createButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addEventHandlers() {
        // Kontaktlisten-Handler
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedValue = contactList.getSelectedValue();
                System.out.println("\n=== Kontaktauswahl Debug ===");
                System.out.println("Ausgewählter Kontakt: " + selectedValue);
                
                try {
                    for (MauticAPI.Contact contact : mauticAPI.getContacts()) {
                        if (contact.toString().equals(selectedValue)) {
                            System.out.println("\n=== Kontaktdetails ===");
                            System.out.println("Vorname: " + contact.firstname);
                            System.out.println("Nachname: " + contact.lastname);
                            System.out.println("Firma: " + contact.company);
                            System.out.println("Straße: " + contact.street);
                            System.out.println("Hausnummer: " + contact.number);
                            System.out.println("PLZ: " + contact.zipcode);
                            System.out.println("Stadt: " + contact.city);
                            break;
                        }
                    }
                } catch (IOException ex) {
                    System.out.println("Fehler beim Laden der Kontaktdetails: " + ex.getMessage());
                    ex.printStackTrace();
                }
                
                updatePreview();
            }
        });

        // Service-Änderungs-Handler
        serviceComboBox.addActionListener(e -> updatePreview());
        hoursSpinner.addChangeListener(e -> updatePreview());
        rateField.addPropertyChangeListener("value", e -> updatePreview());

        // PDF-Button-Handler
        previewButton.addActionListener(e -> showPDFPreview());
        createButton.addActionListener(e -> createInvoice());
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
            
            // Extrahiere den Gesamtbetrag aus dem String
            try {
                String totalStr = item.substring(item.lastIndexOf("=") + 1, item.length() - 1).trim()
                    .replace("€", "").replace(",", ".").trim();
                total += Double.parseDouble(totalStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        preview.append("\nGesamtbetrag: ").append(String.format(Locale.GERMAN, "%.2f €", total));
        previewArea.setText(preview.toString());
    }

    private List<PDFGenerator.ServiceItem> parseServiceItems() {
        List<PDFGenerator.ServiceItem> items = new ArrayList<>();
        
        for (int i = 0; i < serviceModel.size(); i++) {
            String item = serviceModel.getElementAt(i);
            String[] parts = item.split(":");
            String service = parts[0].trim();
            String[] details = parts[1].split("×");
            double hours = Double.parseDouble(
                details[0].replace("Std.", "").trim().replace(",", "."));
            double rate = Double.parseDouble(
                details[1].split("=")[0].replace("€", "").trim().replace(",", "."));
            
            items.add(new PDFGenerator.ServiceItem(service, hours, rate));
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
            
            // Hole den ausgewählten Kontakt
            MauticAPI.Contact selectedContact = null;
            for (MauticAPI.Contact contact : mauticAPI.getContacts()) {
                if (contact.toString().equals(contactList.getSelectedValue())) {
                    selectedContact = contact;
                    break;
                }
            }

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
            // Hole den ausgewählten Kontakt
            MauticAPI.Contact selectedContact = null;
            for (MauticAPI.Contact contact : mauticAPI.getContacts()) {
                if (contact.toString().equals(contactList.getSelectedValue())) {
                    selectedContact = contact;
                    break;
                }
            }

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

    private void loadContacts() {
        try {
            List<MauticAPI.Contact> contacts = mauticAPI.getContacts();
            
            contactModel.clear();
            for (MauticAPI.Contact contact : contacts) {
                contactModel.addElement(contact.toString());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Laden der Kontakte: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
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