import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmailService {
    private final Config config;
    
    public EmailService(Config config) {
        this.config = config;
    }
    
    public void sendInvoice(MauticAPI.Contact contact, File pdfFile, String invoiceNumber) 
            throws MessagingException, IOException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    config.getSmtpUsername(), 
                    config.getSmtpPassword()
                );
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getSenderEmail()));
        message.setRecipients(Message.RecipientType.TO, 
            InternetAddress.parse(contact.email));
        message.setSubject("Rechnung " + invoiceNumber);

        // E-Mail-Text mit Platzhaltern ersetzen
        String emailText = config.getEmailTemplate()
            .replace("%%ANREDE%%", "Herr/Frau")  // TODO: Geschlecht berücksichtigen
            .replace("%%VORNAME%%", contact.firstname)
            .replace("%%NACHNAME%%", contact.lastname)
            .replace("%%RECHNUNGSNUMMER%%", invoiceNumber)
            .replace("%%RECHNUNGSDATUM%%", LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        // Multipart Message erstellen
        Multipart multipart = new MimeMultipart();

        // Text-Teil
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(emailText);
        multipart.addBodyPart(textPart);

        // PDF-Anhang
        MimeBodyPart pdfPart = new MimeBodyPart();
        try {
            pdfPart.attachFile(pdfFile);
            multipart.addBodyPart(pdfPart);
        } catch (IOException e) {
            throw new IOException("Fehler beim Anhängen der PDF-Datei: " + e.getMessage(), e);
        }

        message.setContent(multipart);
        Transport.send(message);
    }
} 