import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.github.javafaker.Faker;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class ContactGeneratorTest {
    private MauticAPI mauticAPI;
    private Faker faker;
    private Config config;
    
    @BeforeEach
    void setUp() throws IOException {
        config = new Config();  // Lade Konfiguration
        config.load();  // Lade die Konfiguration aus der Datei
        mauticAPI = new MauticAPI(config.getMauticApiUrl());
        faker = new Faker(new Locale("de")); // Deutsche Fake-Daten
    }
    
    @Test
    void generateAndCreateContacts() throws IOException {
        List<MauticAPI.Contact> contacts = new ArrayList<>();
        
        // Generiere 100 Kontakte
        for (int i = 0; i < 100; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = faker.internet().emailAddress();
            String company = faker.company().name();
            String street = faker.address().streetName();
            String number = faker.address().buildingNumber();
            String zipcode = faker.address().zipCode();
            String city = faker.address().city();
            
            MauticAPI.Contact contact = new MauticAPI.Contact(
                firstName,
                lastName,
                email,
                company,
                street,
                number,
                zipcode,
                city
            );
            
            // Speichere Kontakt in Mautic
            mauticAPI.createContact(contact);
            
            // Warte kurz zwischen den Anfragen
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.printf("Kontakt %d erstellt: %s %s (%s)%n", 
                i + 1, firstName, lastName, email);
        }
    }
    
    @Test
    void testEmailValidation() {
        // Teste gültige E-Mail-Adressen
        String[] validEmails = {
            "test@example.com",
            "user@domain.de",
            "name.surname@company.org",
            "info@sub.domain.com"
        };
        
        for (String email : validEmails) {
            assert Validator.isValidEmail(email) : 
                "E-Mail sollte gültig sein: " + email;
        }
        
        // Teste ungültige E-Mail-Adressen
        String[] invalidEmails = {
            "test@domain",
            "test@.com",
            "test@domain.",
            "@domain.com",
            "test@",
            "test",
            ""
        };
        
        for (String email : invalidEmails) {
            assert !Validator.isValidEmail(email) : 
                "E-Mail sollte ungültig sein: " + email;
        }
    }
} 