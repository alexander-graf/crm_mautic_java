import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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
            
            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            List<Contact> result = new ArrayList<>();
            
            if (jsonResponse.has("contacts")) {
                JsonObject contactsObj = jsonResponse.getAsJsonObject("contacts");
                contactsObj.entrySet().forEach(entry -> {
                    JsonObject contactObj = entry.getValue().getAsJsonObject();
                    String firstname = "";
                    String lastname = "";
                    
                    if (contactObj.has("fields") && 
                        contactObj.getAsJsonObject("fields").has("core")) {
                        JsonObject core = contactObj.getAsJsonObject("fields").getAsJsonObject("core");
                        
                        if (core.has("firstname") && 
                            core.getAsJsonObject("firstname").has("value")) {
                            firstname = core.getAsJsonObject("firstname")
                                          .get("value").getAsString();
                        }
                        
                        if (core.has("lastname") && 
                            core.getAsJsonObject("lastname").has("value")) {
                            lastname = core.getAsJsonObject("lastname")
                                         .get("value").getAsString();
                        }
                    }
                    
                    if (!firstname.isEmpty() || !lastname.isEmpty()) {
                        result.add(new Contact(firstname, lastname));
                    }
                });
            }
            
            return result;
        }
    }

    public static class Contact {
        private final String firstname;
        private final String lastname;

        public Contact(String firstname, String lastname) {
            this.firstname = firstname;
            this.lastname = lastname;
        }

        @Override
        public String toString() {
            return firstname + " " + lastname;
        }
    }
} 