package me.lerch.alexa.state.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUtils {
    public static String encryptSha1(final String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        if (input == null) return "";

        final MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        final byte[] buffer = input.getBytes("UTF-8");
        md.update(buffer);
        final byte[] digest = md.digest();

        String hexStr = "";
        for (byte aDigest : digest) {
            hexStr += Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1);
        }
        return hexStr;
    }
}
