import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import okhttp3.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MauticAPI {
    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private String accessToken;

    public MauticAPI(String baseUrl) throws IOException {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient();
        this.gson = new Gson();
        loadAccessToken();
    }

    private void loadAccessToken() throws IOException {
        String homeDir = System.getProperty("user.home");
        Path tokenPath = Paths.get(homeDir, ".config", "access_token.json");
        String tokenJson = Files.readString(tokenPath);
        JsonObject tokenData = gson.fromJson(tokenJson, JsonObject.class);
        accessToken = tokenData.get("access_token").getAsString();
    }

    public List<Contact> getContacts() throws IOException {
        if (accessToken == null) {
            throw new IllegalStateException("Kein Access Token verfügbar");
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/contacts")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Fehler " + response);
            
            String responseBody = response.body().string();
            System.out.println("\n=== API Response Debug ===");
            System.out.println(responseBody);
            
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            List<Contact> result = new ArrayList<>();
            
            if (jsonResponse.has("contacts")) {
                JsonObject contactsObj = jsonResponse.getAsJsonObject("contacts");
                contactsObj.entrySet().forEach(entry -> {
                    JsonObject contactObj = entry.getValue().getAsJsonObject();
                    System.out.println("\n=== Kontakt Debug ===");
                    System.out.println("Contact ID: " + entry.getKey());
                    System.out.println(gson.toJson(contactObj));
                    
                    if (contactObj.has("fields") && 
                        contactObj.getAsJsonObject("fields").has("core")) {
                        JsonObject core = contactObj.getAsJsonObject("fields").getAsJsonObject("core");
                        
                        System.out.println("\n=== Core Fields Debug ===");
                        core.entrySet().forEach(field -> {
                            System.out.println(field.getKey() + ": " + field.getValue());
                        });

                        // Sicheres Abrufen der Felder
                        String firstname = getFieldValueSafe(core, "firstname");
                        String lastname = getFieldValueSafe(core, "lastname");
                        String company = getFieldValueSafe(core, "company");
                        String street = getFieldValueSafe(core, "address1");
                        String number = getFieldValueSafe(core, "address2");
                        String zipcode = getFieldValueSafe(core, "zipcode");
                        String city = getFieldValueSafe(core, "city");
                        
                        System.out.println("\n=== Extrahierte Felder ===");
                        System.out.println("Vorname: " + firstname);
                        System.out.println("Nachname: " + lastname);
                        System.out.println("Firma: " + company);
                        System.out.println("Straße: " + street);
                        System.out.println("Hausnummer: " + number);
                        System.out.println("PLZ: " + zipcode);
                        System.out.println("Stadt: " + city);
                        
                        if (!firstname.isEmpty() || !lastname.isEmpty()) {
                            result.add(new Contact(firstname, lastname, company, street, number, zipcode, city));
                        }
                    }
                });
            }
            
            return result;
        }
    }

    private String getFieldValueSafe(JsonObject core, String fieldName) {
        try {
            if (core.has(fieldName)) {
                JsonElement fieldElement = core.get(fieldName);
                if (!fieldElement.isJsonNull() && 
                    fieldElement.isJsonObject() && 
                    fieldElement.getAsJsonObject().has("value")) {
                    
                    JsonElement valueElement = fieldElement.getAsJsonObject().get("value");
                    if (!valueElement.isJsonNull()) {
                        return valueElement.getAsString();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Lesen von Feld " + fieldName + ": " + e.getMessage());
        }
        return "";
    }

    public static class Contact {
        public final String firstname;
        public final String lastname;
        public final String company;
        public final String street;
        public final String number;
        public final String zipcode;
        public final String city;

        public Contact(String firstname, String lastname, String company, 
                      String street, String number, String zipcode, String city) {
            this.firstname = firstname;
            this.lastname = lastname;
            this.company = company;
            this.street = street;
            this.number = number;
            this.zipcode = zipcode;
            this.city = city;
        }

        @Override
        public String toString() {
            return firstname + " " + lastname;
        }
    }
} 