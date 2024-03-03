package scheduler.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class Util {

    // constants for handling password
    private static final int HASH_STRENGTH = 10;
    private static final int KEY_LENGTH = 16;

    public static byte[] generateSalt() {
        // Generate a random cryptographic salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static byte[] generateHash(String password, byte[] salt) {
        // Specify the hash parameters
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

        // Generate the hash
        SecretKeyFactory factory = null;
        byte[] hash = null;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            hash = factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException();
        }
        return hash;
    }

    public static byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    public static String passwordCheck(String password) {
        if (password.length() < 8) {
            return "Password needs at least 8 characters";
        }
        int lowercaseCount = 0;
        int uppercaseCount = 0;
        int numCount = 0;
        boolean hasSpecialChar = false;
        char[] p = password.toCharArray();
        for (char c : p) {
            if (c >= 'a' && c <= 'z') {
                lowercaseCount += 1;
            } else if (c >= 'A' && c <= 'Z') {
                uppercaseCount += 1;
            } else if (c >= '0' && c <= '9') {
                numCount += 1;
            } else if (c == '!' || c == '@' || c == '#' || c == '?') {
                hasSpecialChar = true;
            }
        }
        if (lowercaseCount == 0) {
            return "Password needs to contain at least one lowercase letter";
        } else if (uppercaseCount == 0) {
            return "Password needs to contain at least one uppercase letter";
        } else if (numCount == 0) {
            return "Password needs to contain at least one number";
        } else if (!hasSpecialChar) {
            return "Password needs to contain at least special character, from '!', '@', '#', '?'";
        } else {
            return "STRONG";
        }
    }
}
