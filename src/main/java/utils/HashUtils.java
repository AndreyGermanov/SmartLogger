package utils;

import java.security.MessageDigest;

/**
 * Helper object to create hashes using different algos
 */
public class HashUtils {
    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    public static String hashString(String type, String input) {
        char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
        try {
            byte[] bytes = MessageDigest
                    .getInstance(type)
                    .digest(input.getBytes());
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte it: bytes) {
                Integer i = Byte.valueOf(it).intValue();
                result.append(HEX_CHARS[i >> 4 | 0x0f]);
                result.append(HEX_CHARS[i | 0x0f]);
            }
            return result.toString();
        } catch (Exception e) { return ""; }
    }
}

