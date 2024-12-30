import javax.swing.*;
import java.awt.*;
import javax.swing.border.TitledBorder;

public class CRMGUI extends JFrame {
    private JList<String> contactList;
    private DefaultListModel<String> contactModel;
    private JComboBox<String> serviceComboBox;
    private JSpinner hoursSpinner;
    private JTextField rateField;
    private JTextArea previewArea;
    private JButton previewButton;
    private JButton createButton;
    
    public CRMGUI() {
        setTitle("CRM System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Datei");
        JMenu contactMenu = new JMenu("Kontakte");
        JMenu helpMenu = new JMenu("Hilfe");

        fileMenu.add(new JMenuItem("Neu"));
        fileMenu.add(new JMenuItem("Öffnen"));
        fileMenu.add(new JMenuItem("Speichern"));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Beenden"));

        contactMenu.add(new JMenuItem("Kontakt hinzufügen"));
        contactMenu.add(new JMenuItem("Kontakt bearbeiten"));
        contactMenu.add(new JMenuItem("Kontakt löschen"));

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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Dienstleistungen"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Dienstleistungen
        serviceComboBox = new JComboBox<>(new String[]{
            "Beratung", "Entwicklung", "Design", "Support"
        });
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Dienstleistung:"), gbc);
        gbc.gridx = 1;
        panel.add(serviceComboBox, gbc);

        // Stunden
        hoursSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 0.5));
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Stunden:"), gbc);
        gbc.gridx = 1;
        panel.add(hoursSpinner, gbc);

        // Stundensatz
        rateField = new JTextField("85.00", 10);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("€/Stunde:"), gbc);
        gbc.gridx = 1;
        panel.add(rateField, gbc);

        return panel;
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

        double hours = (Double) hoursSpinner.getValue();
        double rate = Double.parseDouble(rateField.getText());
        String service = (String) serviceComboBox.getSelectedItem();

        StringBuilder preview = new StringBuilder();
        preview.append("Rechnung für: ").append(contactList.getSelectedValue()).append("\n\n");
        preview.append("Dienstleistung: ").append(service).append("\n");
        preview.append("Stunden: ").append(String.format("%.2f", hours)).append("\n");
        preview.append("Stundensatz: ").append(String.format("%.2f €", rate)).append("\n");
        preview.append("Gesamtbetrag: ").append(String.format("%.2f €", hours * rate));

        previewArea.setText(preview.toString());
    }

    private void showPDFPreview() {
        // Hier später PDF-Vorschau implementieren
        JOptionPane.showMessageDialog(this, 
            "PDF-Vorschau wird implementiert, sobald Mautic-API angebunden ist.");
    }

    private void createInvoice() {
        // Hier später PDF erstellen und speichern
        JOptionPane.showMessageDialog(this, 
            "PDF-Erstellung wird implementiert, sobald Mautic-API angebunden ist.");
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