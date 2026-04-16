import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final String credentialsFile;
    private final Map<String, String> userMap;

    public AuthService(String credentialsFile) throws IOException {
        this.credentialsFile = credentialsFile;
        this.userMap = new HashMap<>();
        loadCredentials();
    }

    private void loadCredentials() throws IOException {
        File file = new File(credentialsFile);
        if (!file.exists()) {
            throw new IOException("Credentials file not found: " + credentialsFile);
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                userMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        reader.close();
    }

    public boolean authenticate(String username, String password) {
        String storedHash = userMap.get(username);
        if (storedHash == null) return false;
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public boolean userExists(String username) {
        return userMap.containsKey(username);
    }
}
