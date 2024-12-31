import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmailPreviewDialog extends JDialog {
    private JTextArea emailPreview;
    private boolean approved = false;
    private final MauticAPI.Contact contact;
    private final String invoiceNumber;
    private final File pdfFile;
    private final Config config;

    public EmailPreviewDialog(JFrame parent, MauticAPI.Contact contact, String invoiceNumber, File pdfFile, Config config) {
        super(parent, "E-Mail Vorschau", true);
        this.contact = contact;
        this.invoiceNumber = invoiceNumber;
        this.pdfFile = pdfFile;
        this.config = config;
        
        setLayout(new BorderLayout());
        
        // Header-Panel
        JPanel headerPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headerPanel.add(new JLabel("An:"));
        headerPanel.add(new JLabel(contact.email));
        headerPanel.add(new JLabel("Betreff:"));
        headerPanel.add(new JLabel("Rechnung " + invoiceNumber));
        headerPanel.add(new JLabel("Anhang:"));
        headerPanel.add(new JLabel(pdfFile.getName()));
        add(headerPanel, BorderLayout.NORTH);
        
        // E-Mail Text Vorschau
        String emailText = config.getEmailTemplate()
            .replace("%%ANREDE%%", "Herr/Frau")  // TODO: Geschlecht berücksichtigen
            .replace("%%VORNAME%%", contact.firstname)
            .replace("%%NACHNAME%%", contact.lastname)
            .replace("%%RECHNUNGSNUMMER%%", invoiceNumber)
            .replace("%%RECHNUNGSDATUM%%", LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                
        emailPreview = new JTextArea(emailText);
        emailPreview.setEditable(true); // Erlaubt letzte Anpassungen
        emailPreview.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(emailPreview), BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendButton = new JButton("Senden");
        JButton cancelButton = new JButton("Abbrechen");
        
        sendButton.addActionListener(e -> {
            try {
                EmailService emailService = new EmailService(config);
                emailService.sendInvoice(contact, pdfFile, invoiceNumber, emailPreview.getText());
                approved = true;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Fehler beim Senden: " + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }
    
    public boolean showDialog() {
        setVisible(true);
        return approved;
    }
} 