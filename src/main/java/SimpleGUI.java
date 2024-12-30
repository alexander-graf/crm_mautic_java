import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SimpleGUI extends JFrame {
    private JTextField textField1;
    private JTextField textField2;
    private JButton button;

    public SimpleGUI() {
        // Fenster-Einstellungen
        setTitle("Mein erstes GUI");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout Manager
        setLayout(new FlowLayout());

        // Komponenten erstellen
        textField1 = new JTextField(20);
        textField2 = new JTextField(20);
        button = new JButton("Klick mich!");

        // Komponenten zum Fenster hinzufügen
        add(new JLabel("Erste Eingabe:"));
        add(textField1);
        add(new JLabel("Zweite Eingabe:"));
        add(textField2);
        add(button);

        // Button-Aktion hinzufügen
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text1 = textField1.getText();
                String text2 = textField2.getText();
                JOptionPane.showMessageDialog(null, 
                    "Eingabe 1: " + text1 + "\nEingabe 2: " + text2);
            }
        });
    }

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();  // Für dunkles Theme
            // ODER
            // com.formdev.flatlaf.FlatLightLaf.setup();  // Für helles Theme
        } catch (Exception e) {
            e.printStackTrace();
        }

        // GUI im Event-Dispatch-Thread starten
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SimpleGUI().setVisible(true);
            }
        });
    }
} 