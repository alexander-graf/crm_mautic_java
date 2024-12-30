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
            mauticAPI = new MauticAPI("https://mautic.alexander-graf.de");
            loadContacts();  // Initial laden
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Initialisieren der Mautic-API: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Datei");
        JMenu contactMenu = new JMenu("Kontakte");
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

        menuBar.add(fileMenu);
        menuBar.add(contactMenu);
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

        // Linke Seite: Eingabefelder
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Dienstleistungen
        serviceComboBox = new JComboBox<>(new String[]{
            "Beratung", "Entwicklung", "Design", "Support"
        });
        gbc.gridx = 0; gbc.gridy = 0;
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

    private void showPDFPreview() {
        if (contactList.getSelectedValue() == null || serviceModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte wählen Sie einen Kontakt und fügen Sie mindestens eine Dienstleistung hinzu.",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Path tempDir = Files.createTempDirectory("crm_preview");
            String tempPdfPath = tempDir.resolve("preview.pdf").toString();
            
            List<PDFGenerator.ServiceItem> items = parseServiceItems();
            PDFGenerator generator = new PDFGenerator();
            generator.generateInvoice(
                contactList.getSelectedValue(),
                items,
                tempPdfPath
            );

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

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF speichern");
        fileChooser.setSelectedFile(new File("Rechnung.pdf"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<PDFGenerator.ServiceItem> items = parseServiceItems();
                PDFGenerator generator = new PDFGenerator();
                generator.generateInvoice(
                    contactList.getSelectedValue(),
                    items,
                    fileChooser.getSelectedFile().getAbsolutePath()
                );

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