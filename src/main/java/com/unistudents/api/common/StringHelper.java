package com.unistudents.api.common;

import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class StringHelper {

    public static String removeTones(String string) {
        string = string.replace("Ά", "Α");
        string = string.replace("Έ", "Ε");
        string = string.replace("Ή", "Η");
        string = string.replace("Ί", "Ι");
        string = string.replace("Ό", "Ο");
        string = string.replace("Ύ", "Υ");
        string = string.replace("Ώ", "Ω");
        return string;
    }

    public static String getRandomHashcode() {
        try {
            String text;
            int targetStringLength = 64;
            Random random = new Random();
            StringBuilder buffer = new StringBuilder(targetStringLength);
            for (int i = 0; i < targetStringLength; i++) {
                int randomLimitedInt = 97 + (int) (random.nextFloat() * (122 - 97 + 1));
                buffer.append((char) randomLimitedInt);
            }
            text = buffer.toString();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static void write(String fileName, String str) {
        try (OutputStreamWriter writer =
                     new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)) {
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
