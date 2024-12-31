import javax.swing.*;
import java.awt.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class EditContactDialog extends JDialog {
    private JTextField firstnameField;
    private JTextField lastnameField;
    private JTextField emailField;
    private JTextField companyField;
    private JTextField streetField;
    private JTextField numberField;
    private JTextField zipcodeField;
    private JTextField cityField;
    private boolean approved = false;
    private final MauticAPI mauticAPI;
    private final MauticAPI.Contact contact;
    private JButton saveButton;

    public EditContactDialog(JFrame parent, MauticAPI mauticAPI, MauticAPI.Contact contact) {
        super(parent, "Kontakt bearbeiten", true);
        this.mauticAPI = mauticAPI;
        this.contact = contact;
        
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
        firstnameField.setText(contact.firstname);
        panel.add(firstnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Nachname:*"), gbc);
        gbc.gridx = 1;
        lastnameField = new JTextField(20);
        lastnameField.setText(contact.lastname);
        panel.add(lastnameField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("E-Mail:*"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        emailField.setText(contact.email);
        emailField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { validateFields(); }
            public void removeUpdate(DocumentEvent e) { validateFields(); }
            public void insertUpdate(DocumentEvent e) { validateFields(); }
        });
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Firma:"), gbc);
        gbc.gridx = 1;
        companyField = new JTextField(20);
        companyField.setText(contact.company);
        panel.add(companyField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Straße:"), gbc);
        gbc.gridx = 1;
        streetField = new JTextField(20);
        streetField.setText(contact.street);
        panel.add(streetField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Hausnummer:"), gbc);
        gbc.gridx = 1;
        numberField = new JTextField(20);
        numberField.setText(contact.number);
        panel.add(numberField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("PLZ:"), gbc);
        gbc.gridx = 1;
        zipcodeField = new JTextField(20);
        zipcodeField.setText(contact.zipcode);
        panel.add(zipcodeField, gbc);

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Stadt:"), gbc);
        gbc.gridx = 1;
        cityField = new JTextField(20);
        cityField.setText(contact.city);
        panel.add(cityField, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");
        
        saveButton.addActionListener(e -> {
            if (validateInput()) {
                saveContact();
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
        
        // Initial validation
        validateFields();
    }

    private void validateFields() {
        String email = emailField.getText().trim();
        boolean isValid = !email.isEmpty() && Validator.isValidEmail(email);
        saveButton.setEnabled(isValid);
        
        if (!isValid && !email.isEmpty()) {
            emailField.setBackground(new Color(255, 200, 200));
            emailField.setToolTipText("Bitte geben Sie eine gültige E-Mail-Adresse ein");
        } else {
            emailField.setBackground(UIManager.getColor("TextField.background"));
            emailField.setToolTipText(null);
        }
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
        
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie eine E-Mail-Adresse ein.",
                "Fehlende Eingabe",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (!Validator.isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                "Bitte geben Sie eine gültige E-Mail-Adresse ein.",
                "Ungültige E-Mail",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        return true;
    }

    private void saveContact() {
        try {
            MauticAPI.Contact updatedContact = new MauticAPI.Contact(
                contact.id,  // ID im Konstruktor übergeben
                firstnameField.getText().trim(),
                lastnameField.getText().trim(),
                emailField.getText().trim(),
                companyField.getText().trim(),
                streetField.getText().trim(),
                numberField.getText().trim(),
                zipcodeField.getText().trim(),
                cityField.getText().trim()
            );
            
            mauticAPI.updateContact(updatedContact);
            approved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Fehler beim Speichern: " + ex.getMessage(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean showDialog() {
        setVisible(true);
        return approved;
    }
} 