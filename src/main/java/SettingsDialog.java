import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SettingsDialog extends JDialog {
    private JTextField apiUrlField;
    private JTextField templatePathField;
    private JTextField invoicePathField;
    private JTextField backupPathField;
    private JTextField smtpHostField;
    private JSpinner smtpPortSpinner;
    private JTextField smtpUsernameField;
    private JPasswordField smtpPasswordField;
    private JTextField senderEmailField;
    private boolean approved = false;
    private static final Logger logger = LogManager.getLogger(SettingsDialog.class);
    private JButton testConnectionButton;
    private JButton testEmailButton;
    private final Config config;

    public SettingsDialog(JFrame parent, Config config) {
        super(parent, "Einstellungen", true);
        this.config = config;
        
        setLayout(new BorderLayout());
        
        // Hauptpanel mit GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // API URL
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Mautic API URL:"), gbc);
        
        gbc.gridx = 1;
        apiUrlField = new JTextField(config.getMauticApiUrl(), 30);
        mainPanel.add(apiUrlField, gbc);
        
        // Template Pfad
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("LaTeX Template:"), gbc);
        
        gbc.gridx = 1;
        JPanel templatePanel = new JPanel(new BorderLayout());
        templatePathField = new JTextField(config.getTemplatePath(), 25);
        templatePanel.add(templatePathField, BorderLayout.CENTER);
        JButton templateBrowse = new JButton("...");
        templateBrowse.addActionListener(e -> browseFile(templatePathField, "LaTeX Template auswählen"));
        templatePanel.add(templateBrowse, BorderLayout.EAST);
        mainPanel.add(templatePanel, gbc);
        
        // Rechnungspfad
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Rechnungsordner:"), gbc);
        
        gbc.gridx = 1;
        JPanel invoicePanel = new JPanel(new BorderLayout());
        invoicePathField = new JTextField(config.getInvoicePath(), 25);
        invoicePanel.add(invoicePathField, BorderLayout.CENTER);
        JButton invoiceBrowse = new JButton("...");
        invoiceBrowse.addActionListener(e -> browseDirectory(invoicePathField, "Rechnungsordner auswählen"));
        invoicePanel.add(invoiceBrowse, BorderLayout.EAST);
        mainPanel.add(invoicePanel, gbc);
        
        // Backup-Pfad
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Backup-Verzeichnis:"), gbc);
        gbc.gridx = 1;
        JPanel backupPanel = new JPanel(new BorderLayout());
        backupPathField = new JTextField(config.getBackupPath(), 25);
        JButton backupBrowse = new JButton("...");
        backupBrowse.addActionListener(e -> browseDirectory(backupPathField, "Backup-Verzeichnis auswählen"));
        backupPanel.add(backupPathField, BorderLayout.CENTER);
        backupPanel.add(backupBrowse, BorderLayout.EAST);
        mainPanel.add(backupPanel, gbc);
        
        // E-Mail Einstellungen
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("E-Mail Einstellungen"), gbc);
        gbc.gridx = 1;
        mainPanel.add(createEmailSettingsPanel(), gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");
        
        saveButton.addActionListener(e -> {
            if (validateSettings()) {
                // Bestehende Einstellungen speichern
                config.setMauticApiUrl(getApiUrl());
                config.setTemplatePath(getTemplatePath());
                config.setInvoicePath(getInvoicePath());
                config.setBackupPath(getBackupPath());
                
                // E-Mail-Einstellungen speichern
                config.setSmtpHost(smtpHostField.getText());
                config.setSmtpPort((Integer) smtpPortSpinner.getValue());
                config.setSmtpUsername(smtpUsernameField.getText());
                config.setSmtpPassword(new String(smtpPasswordField.getPassword()));
                config.setSenderEmail(senderEmailField.getText());
                
                approved = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(parent);

        // E-Mail Einstellungen initialisieren
        smtpHostField.setText(config.getSmtpHost());
        smtpPortSpinner.setValue(config.getSmtpPort());
        smtpUsernameField.setText(config.getSmtpUsername());
        smtpPasswordField.setText(config.getSmtpPassword());
        senderEmailField.setText(config.getSenderEmail());
    }
    
    private void browseFile(JTextField field, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void browseDirectory(JTextField field, String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    public boolean showDialog() {
        setVisible(true);
        return approved;
    }
    
    public String getApiUrl() {
        return apiUrlField.getText();
    }
    
    public String getTemplatePath() {
        return templatePathField.getText();
    }
    
    public String getInvoicePath() {
        return invoicePathField.getText();
    }
    
    public String getBackupPath() {
        return backupPathField.getText();
    }
    
    private boolean validateSettings() {
        // API URL Validierung
        String apiUrl = apiUrlField.getText().trim();
        if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            showError("Die API-URL muss mit http:// oder https:// beginnen");
            return false;
        }

        // Teste API-Verbindung
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();
            Request request = new Request.Builder()
                .url(apiUrl)
                .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                showWarning("API-URL scheint nicht erreichbar zu sein (Status: " + response.code() + ")");
            }
        } catch (Exception e) {
            logger.error("Fehler beim Testen der API-URL", e);
            showWarning("API-URL konnte nicht getestet werden: " + e.getMessage());
        }

        // Template-Pfad Validierung
        String templatePath = templatePathField.getText().trim();
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            showError("Die LaTeX-Vorlage existiert nicht: " + templatePath);
            return false;
        }
        if (!templateFile.canRead()) {
            showError("Keine Leserechte für die LaTeX-Vorlage");
            return false;
        }
        if (!templatePath.toLowerCase().endsWith(".tex")) {
            showWarning("Die LaTeX-Vorlage sollte die Endung .tex haben");
        }

        // Rechnungsordner Validierung
        String invoicePath = invoicePathField.getText().trim();
        File invoiceDir = new File(invoicePath);
        if (!invoiceDir.exists()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Der Rechnungsordner existiert nicht. Soll er erstellt werden?",
                "Ordner erstellen?",
                JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                if (!invoiceDir.mkdirs()) {
                    showError("Konnte den Rechnungsordner nicht erstellen");
                    return false;
                }
            } else {
                return false;
            }
        }
        if (!invoiceDir.canWrite()) {
            showError("Keine Schreibrechte im Rechnungsordner");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        logger.error(message);
        JOptionPane.showMessageDialog(this,
            message,
            "Fehler",
            JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        logger.warn(message);
        JOptionPane.showMessageDialog(this,
            message,
            "Warnung",
            JOptionPane.WARNING_MESSAGE);
    }

    private JPanel createEmailSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("E-Mail Einstellungen"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // SMTP Host
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("SMTP Server:"), gbc);
        gbc.gridx = 1;
        smtpHostField = new JTextField(20);
        panel.add(smtpHostField, gbc);

        // SMTP Port
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("SMTP Port:"), gbc);
        gbc.gridx = 1;
        smtpPortSpinner = new JSpinner(new SpinnerNumberModel(587, 1, 65535, 1));
        panel.add(smtpPortSpinner, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Benutzername:"), gbc);
        gbc.gridx = 1;
        smtpUsernameField = new JTextField(20);
        panel.add(smtpUsernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Passwort:"), gbc);
        gbc.gridx = 1;
        smtpPasswordField = new JPasswordField(20);
        panel.add(smtpPasswordField, gbc);

        // Sender Email
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Absender:"), gbc);
        gbc.gridx = 1;
        senderEmailField = new JTextField(20);
        panel.add(senderEmailField, gbc);

        // Test-Buttons Panel
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;  // Über beide Spalten
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        testConnectionButton = new JButton("Verbindung testen");
        testConnectionButton.addActionListener(e -> testConnection());
        testPanel.add(testConnectionButton);
        
        testEmailButton = new JButton("Test-E-Mail senden");
        testEmailButton.addActionListener(e -> sendTestEmail());
        testPanel.add(testEmailButton);
        
        panel.add(testPanel, gbc);

        return panel;
    }

    private void testConnection() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHostField.getText());
            props.put("mail.smtp.port", smtpPortSpinner.getValue());

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        smtpUsernameField.getText(),
                        new String(smtpPasswordField.getPassword())
                    );
                }
            });

            // Versuche eine Verbindung aufzubauen
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();

            JOptionPane.showMessageDialog(this,
                "Verbindung erfolgreich hergestellt!",
                "Erfolg",
                JOptionPane.INFORMATION_MESSAGE);
            logger.info("SMTP-Verbindungstest erfolgreich");

        } catch (MessagingException ex) {
            logger.error("SMTP-Verbindungstest fehlgeschlagen", ex);
            JOptionPane.showMessageDialog(this,
                "Verbindung fehlgeschlagen: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendTestEmail() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHostField.getText());
            props.put("mail.smtp.port", smtpPortSpinner.getValue());

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        smtpUsernameField.getText(),
                        new String(smtpPasswordField.getPassword())
                    );
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmailField.getText()));
            message.setRecipients(Message.RecipientType.TO, 
                InternetAddress.parse(senderEmailField.getText())); // An sich selbst
            message.setSubject("CRM Test-E-Mail");

            String testText = "Dies ist eine Test-E-Mail vom CRM-System.\n\n" +
                "SMTP-Server: " + smtpHostField.getText() + "\n" +
                "Port: " + smtpPortSpinner.getValue() + "\n" +
                "Benutzername: " + smtpUsernameField.getText() + "\n" +
                "Absender: " + senderEmailField.getText() + "\n\n" +
                "Wenn Sie diese E-Mail sehen, funktioniert der E-Mail-Versand korrekt.";

            message.setText(testText);
            Transport.send(message);

            JOptionPane.showMessageDialog(this,
                "Test-E-Mail wurde versendet an: " + senderEmailField.getText(),
                "Erfolg",
                JOptionPane.INFORMATION_MESSAGE);
            logger.info("Test-E-Mail erfolgreich versendet");

        } catch (MessagingException ex) {
            logger.error("Fehler beim Versenden der Test-E-Mail", ex);
            JOptionPane.showMessageDialog(this,
                "E-Mail-Versand fehlgeschlagen: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getSmtpHost() {
        return smtpHostField.getText();
    }
    
    public int getSmtpPort() {
        return (Integer) smtpPortSpinner.getValue();
    }
    
    public String getSmtpUsername() {
        return smtpUsernameField.getText();
    }
    
    public String getSmtpPassword() {
        return new String(smtpPasswordField.getPassword());
    }
    
    public String getSenderEmail() {
        return senderEmailField.getText();
    }
} 