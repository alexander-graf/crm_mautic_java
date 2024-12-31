import javax.swing.*;
import java.awt.*;

public class NewContactDialog extends JDialog {
    private JTextField firstnameField;
    private JTextField lastnameField;
    private JTextField emailField;
    private JTextField companyField;
    private JTextField streetField;
    private JTextField zipcodeField;
    private JTextField cityField;
    private boolean approved = false;
    private final MauticAPI mauticAPI;

    public NewContactDialog(JFrame parent, MauticAPI mauticAPI) {
        super(parent, "Neuer Kontakt", true);
        this.mauticAPI = mauticAPI;
        
        // Panel mit GridBagLayout für das Formular
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Formularfelder
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Vorname:*"), gbc);
        gbc.gridx = 1;
        firstnameField = new JTextField(20);
        panel.add(firstnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Nachname:*"), gbc);
        gbc.gridx = 1;
        lastnameField = new JTextField(20);
        panel.add(lastnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("E-Mail:*"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Firma:"), gbc);
        gbc.gridx = 1;
        companyField = new JTextField(20);
        panel.add(companyField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Straße und Nr.:"), gbc);
        gbc.gridx = 1;
        streetField = new JTextField(20);
        panel.add(streetField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("PLZ:"), gbc);
        gbc.gridx = 1;
        zipcodeField = new JTextField(20);
        panel.add(zipcodeField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Stadt:"), gbc);
        gbc.gridx = 1;
        cityField = new JTextField(20);
        panel.add(cityField, gbc);

        // Pflichtfeld-Hinweis
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(new JLabel("* Pflichtfelder"), gbc);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");

        saveButton.addActionListener(e -> {
            if (validateInput()) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                saveContact(getNewContact());
            }
        });
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Layout zusammenbauen
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private boolean validateInput() {
        if (firstnameField.getText().trim().isEmpty() && 
            lastnameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie mindestens Vor- oder Nachnamen ein.",
                "Fehlende Eingabe",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie eine E-Mail-Adresse ein.",
                "Fehlende Eingabe",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public boolean showDialog() {
        setVisible(true);
        return approved;
    }

    public MauticAPI.Contact getNewContact() {
        return new MauticAPI.Contact(
            firstnameField.getText().trim(),
            lastnameField.getText().trim(),
            emailField.getText().trim(),
            companyField.getText().trim(),
            streetField.getText().trim(),
            "",
            zipcodeField.getText().trim(),
            cityField.getText().trim()
        );
    }

    private void saveContact(MauticAPI.Contact contact) {
        // Deaktiviere alle Eingabefelder während des Speicherns
        setFieldsEnabled(false);
        
        // Zeige Ladekringel
        JPanel glassPane = new JPanel(new GridBagLayout());
        glassPane.setOpaque(false);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        JLabel label = new JLabel("Erstelle neuen Kontakt...");
        label.setForeground(Color.BLACK);
        glassPane.add(label);
        glassPane.add(progress);
        setGlassPane(glassPane);
        glassPane.setVisible(true);

        // Starte API-Aufruf in separatem Thread
        new SwingWorker<MauticAPI.Contact, Void>() {
            @Override
            protected MauticAPI.Contact doInBackground() throws Exception {
                return mauticAPI.createContact(contact);
            }

            @Override
            protected void done() {
                try {
                    MauticAPI.Contact newContact = get();
                    if (newContact != null && newContact.id > 0) {
                        approved = true;
                        dispose();
                    }
                } catch (Exception ex) {
                    glassPane.setVisible(false);
                    setFieldsEnabled(true);
                    JOptionPane.showMessageDialog(NewContactDialog.this,
                        "Fehler beim Speichern: " + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setFieldsEnabled(boolean enabled) {
        firstnameField.setEnabled(enabled);
        lastnameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        companyField.setEnabled(enabled);
        streetField.setEnabled(enabled);
        zipcodeField.setEnabled(enabled);
        cityField.setEnabled(enabled);
    }
} 