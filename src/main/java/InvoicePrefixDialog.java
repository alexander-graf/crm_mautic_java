import javax.swing.*;
import java.awt.*;

public class InvoicePrefixDialog extends JDialog {
    private final JTextField prefixField;
    private final Config config;

    public InvoicePrefixDialog(JFrame parent, Config config) {
        super(parent, "Rechnungsnummernkreis", true);
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        
        // Panel für das Eingabefeld
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Präfix:"));
        prefixField = new JTextField(config.getInvoicePrefix(), 10);
        inputPanel.add(prefixField);
        
        // Panel für die Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");
        
        saveButton.addActionListener(e -> {
            config.setInvoicePrefix(prefixField.getText().trim());
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Füge Panels zum Dialog hinzu
        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Dialog-Eigenschaften
        pack();
        setLocationRelativeTo(parent);
    }
} 