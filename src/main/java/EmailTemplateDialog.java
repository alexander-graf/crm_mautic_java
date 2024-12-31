import javax.swing.*;
import java.awt.*;

public class EmailTemplateDialog extends JDialog {
    private JTextArea templateArea;
    private boolean approved = false;
    private static final String HELP_TEXT = 
        "Verfügbare Platzhalter:\n" +
        "%%ANREDE%% - Anrede des Kontakts\n" +
        "%%VORNAME%% - Vorname des Kontakts\n" +
        "%%NACHNAME%% - Nachname des Kontakts\n" +
        "%%RECHNUNGSNUMMER%% - Nummer der Rechnung\n" +
        "%%RECHNUNGSDATUM%% - Datum der Rechnung";

    public EmailTemplateDialog(JFrame parent, String currentTemplate) {
        super(parent, "E-Mail Vorlage bearbeiten", true);
        
        setLayout(new BorderLayout());
        
        // Hilfetext oben
        JTextArea helpArea = new JTextArea(HELP_TEXT);
        helpArea.setEditable(false);
        helpArea.setBackground(null);
        add(new JScrollPane(helpArea), BorderLayout.NORTH);
        
        // Template-Editor
        templateArea = new JTextArea(currentTemplate, 10, 50);
        templateArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(templateArea), BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");
        
        saveButton.addActionListener(e -> {
            approved = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    public boolean showDialog() {
        setVisible(true);
        return approved;
    }
    
    public String getTemplate() {
        return templateArea.getText();
    }
} 