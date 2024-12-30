import javax.swing.*;
import java.awt.*;

public class InvoiceNumberDialog extends JDialog {
    private final JSpinner numberSpinner;
    private final Config config;

    public InvoiceNumberDialog(JFrame parent, Config config) {
        super(parent, "Rechnungsnummer einstellen", true);
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        
        // Panel für das Eingabefeld
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Letzte Rechnungsnummer:"));
        numberSpinner = new JSpinner(new SpinnerNumberModel(
            config.getLastInvoiceNumber(), 0, 99999, 1));
        inputPanel.add(numberSpinner);
        
        // Panel für die Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");
        
        saveButton.addActionListener(e -> {
            config.setLastInvoiceNumber((Integer) numberSpinner.getValue());
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