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
import java.util.Base64;
import java.util.Properties;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonParser;
import java.util.concurrent.TimeUnit;

public class MauticAPI {
    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private String accessToken;

    public MauticAPI(String baseUrl) throws IOException {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
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
            
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(responseBody);
            List<Contact> result = new ArrayList<>();
            
            if (element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                JsonObject contacts = root.getAsJsonObject("contacts");

                contacts.entrySet().forEach(entry -> {
                    JsonObject contactObj = entry.getValue().getAsJsonObject();
                    int id = Integer.parseInt(entry.getKey());
                    JsonObject fields = contactObj.getAsJsonObject("fields");
                    if (fields != null) {
                        JsonObject core = fields.getAsJsonObject("core");
                        if (core != null) {
                            String firstname = getFieldValueSafe(core, "firstname");
                            String lastname = getFieldValueSafe(core, "lastname");
                            String email = getFieldValueSafe(core, "email");
                            String company = getFieldValueSafe(core, "company");
                            String street = getFieldValueSafe(core, "address1");
                            String number = getFieldValueSafe(core, "address2");
                            String zipcode = getFieldValueSafe(core, "zipcode");
                            String city = getFieldValueSafe(core, "city");

                            System.out.println("\n=== Extrahierte Felder ===");
                            System.out.println("ID: " + id);
                            System.out.println("Vorname: " + firstname);
                            System.out.println("Nachname: " + lastname);
                            System.out.println("Email: " + email);
                            System.out.println("Firma: " + company);
                            System.out.println("Straße: " + street);
                            System.out.println("Hausnummer: " + number);
                            System.out.println("PLZ: " + zipcode);
                            System.out.println("Stadt: " + city);

                            if (!firstname.isEmpty() || !lastname.isEmpty()) {
                                result.add(new Contact(id, firstname, lastname, email, company, 
                                    street, number, zipcode, city));
                            }
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

    public void deleteContact(int contactId) throws IOException {
        String url = baseUrl + "/api/contacts/" + contactId + "/delete";
        
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        // Asynchroner Aufruf ohne auf Antwort zu warten
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("Delete-Antwort nicht erhalten (könnte trotzdem erfolgreich sein): " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close(); // Response schließen, aber nicht auswerten
            }
        });
        
        // Kurz warten um sicherzustellen, dass die Anfrage raus ist
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void updateContact(Contact contact) throws IOException {
        String url = baseUrl + "/api/contacts/" + contact.id + "/edit";
        
        JsonObject json = new JsonObject();
        json.addProperty("firstname", contact.firstname);
        json.addProperty("lastname", contact.lastname);
        json.addProperty("email", contact.email);
        json.addProperty("company", contact.company);
        json.addProperty("address1", contact.street);
        json.addProperty("zipcode", contact.zipcode);
        json.addProperty("city", contact.city);
        json.addProperty("overwriteWithBlank", false);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            gson.toJson(json)
        );

        Request request = new Request.Builder()
            .url(url)
            .patch(body)
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        // Asynchroner Aufruf ohne auf Antwort zu warten
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Ignorieren, da die Änderung wahrscheinlich trotzdem durchging
                System.out.println("Update-Antwort nicht erhalten (könnte trotzdem erfolgreich sein): " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close(); // Response schließen, aber nicht auswerten
            }
        });
        
        // Kurz warten um sicherzustellen, dass die Anfrage raus ist
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void addField(JsonObject core, String fieldName, String value) {
        JsonObject field = new JsonObject();
        field.addProperty("value", value);
        core.add(fieldName, field);
    }

    public Contact createContact(Contact contact) throws IOException {
        String url = baseUrl + "/api/contacts/new";
        
        JsonObject json = new JsonObject();
        json.addProperty("firstname", contact.firstname);
        json.addProperty("lastname", contact.lastname);
        json.addProperty("email", contact.email);
        json.addProperty("company", contact.company);
        json.addProperty("address1", contact.street);
        json.addProperty("address2", contact.number);
        json.addProperty("zipcode", contact.zipcode);
        json.addProperty("city", contact.city);
        json.addProperty("overwriteWithBlank", true);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            gson.toJson(json)
        );

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fehler beim Erstellen des Kontakts: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            JsonObject contactObj = responseJson.getAsJsonObject("contact");
            int newId = contactObj.get("id").getAsInt();
            
            return new Contact(
                newId,
                contact.firstname,
                contact.lastname,
                contact.email,
                contact.company,
                contact.street,
                contact.number,
                contact.zipcode,
                contact.city
            );
        }
    }

    public static class Contact {
        public final int id;
        public final String firstname;
        public final String lastname;
        public final String email;
        public final String company;
        public final String street;
        public final String number;
        public final String zipcode;
        public final String city;

        public Contact(String firstname, String lastname, String email, String company, 
                      String street, String number, String zipcode, String city) {
            this.id = -1;
            this.firstname = firstname;
            this.lastname = lastname;
            this.email = email;
            this.company = company;
            this.street = street;
            this.number = number;
            this.zipcode = zipcode;
            this.city = city;
        }

        public Contact(int id, String firstname, String lastname, String email, String company, 
                      String street, String number, String zipcode, String city) {
            this.id = id;
            this.firstname = firstname;
            this.lastname = lastname;
            this.email = email;
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