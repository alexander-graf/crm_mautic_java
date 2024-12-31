import java.util.regex.Pattern;

public class Validator {
    // Strengeres Muster, das eine TLD mit mindestens 2 Zeichen erfordert
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return pattern.matcher(email).matches();
    }
} 