import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

public class EditContactDialog extends JDialog {
    private JTextField firstnameField;
    private JTextField lastnameField;
    private JTextField emailField;
    private JTextField companyField;
    private JTextField streetField;
    private JTextField zipcodeField;
    private JTextField cityField;
    private boolean approved = false;
    private final MauticAPI mauticAPI;

    public EditContactDialog(JFrame parent, MauticAPI.Contact contact, MauticAPI mauticAPI) {
        super(parent, "Kontakt bearbeiten", true);
        this.mauticAPI = mauticAPI;
        
        // Panel mit GridBagLayout für das Formular
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Vorname:"), gbc);
        gbc.gridx = 1;
        firstnameField = new JTextField(contact.firstname, 20);
        panel.add(firstnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Nachname:"), gbc);
        gbc.gridx = 1;
        lastnameField = new JTextField(contact.lastname, 20);
        panel.add(lastnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("E-Mail:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(contact.email, 20);
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Firma:"), gbc);
        gbc.gridx = 1;
        companyField = new JTextField(contact.company, 20);
        panel.add(companyField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Straße und Nr.:"), gbc);
        gbc.gridx = 1;
        String fullStreet = contact.street + (contact.number.isEmpty() ? "" : " " + contact.number);
        streetField = new JTextField(fullStreet, 20);
        panel.add(streetField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("PLZ:"), gbc);
        gbc.gridx = 1;
        zipcodeField = new JTextField(contact.zipcode, 20);
        panel.add(zipcodeField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Stadt:"), gbc);
        gbc.gridx = 1;
        cityField = new JTextField(contact.city, 20);
        panel.add(cityField, gbc);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");

        saveButton.addActionListener(e -> {
            // Dialog NICHT sofort schließen
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            saveContact(getUpdatedContact(contact.id));
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

    public boolean showDialog() {
        setVisible(true);
        return approved;
    }

    public MauticAPI.Contact getUpdatedContact(int originalId) {
        return new MauticAPI.Contact(
            originalId,
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
        JLabel label = new JLabel("Speichere Änderungen...");
        label.setForeground(Color.BLACK);
        glassPane.add(label);
        glassPane.add(progress);
        setGlassPane(glassPane);
        glassPane.setVisible(true);

        // Starte API-Aufruf in separatem Thread
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    mauticAPI.updateContact(contact);
                    return true;
                } catch (IOException ex) {
                    // Prüfe, ob die Änderung trotz Timeout erfolgreich war
                    Thread.sleep(1000); // Kurz warten
                    List<MauticAPI.Contact> contacts = mauticAPI.getContacts();
                    for (MauticAPI.Contact c : contacts) {
                        if (c.id == contact.id) {
                            // Vergleiche die Felder
                            if (c.firstname.equals(contact.firstname) &&
                                c.lastname.equals(contact.lastname) &&
                                c.email.equals(contact.email) &&
                                c.company.equals(contact.company) &&
                                c.street.equals(contact.street) &&
                                c.zipcode.equals(contact.zipcode) &&
                                c.city.equals(contact.city)) {
                                return true;
                            }
                        }
                    }
                    throw ex;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        approved = true;
                        dispose();
                    }
                } catch (Exception ex) {
                    glassPane.setVisible(false);
                    setFieldsEnabled(true);
                    JOptionPane.showMessageDialog(EditContactDialog.this,
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