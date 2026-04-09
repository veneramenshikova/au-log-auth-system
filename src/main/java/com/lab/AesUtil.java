package com.lab;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class AesUtil {

    private static final String AES_KEY_FILE = "aes_key.secret";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_SIZE = 256;

    private SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesUtil() {
        this.secureRandom = new SecureRandom();
        loadOrGenerateKey();
        System.out.println("[AES] Инициализирован AES-GCM шифровальщик");
    }

    private void loadOrGenerateKey() {
        Path keyPath = Paths.get(AES_KEY_FILE);

        try {
            if (Files.exists(keyPath)) {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                secretKey = new SecretKeySpec(keyBytes, "AES");
                System.out.println("[AES] Ключ загружен из файла: " + AES_KEY_FILE);
                return;
            }

            // Генерация нового ключа
            byte[] keyBytes = new byte[KEY_SIZE / 8];
            secureRandom.nextBytes(keyBytes);
            Files.write(keyPath, keyBytes);
            secretKey = new SecretKeySpec(keyBytes, "AES");
            System.out.println("[AES] Сгенерирован новый AES-ключ: " + AES_KEY_FILE);

        } catch (Exception e) {
            System.err.println("[AES] Ошибка загрузки ключа: " + e.getMessage());
        }
    }

    /**
     * Шифрование текста
     * @param plainText открытый текст
     * @return зашифрованная строка в Base64 (IV + ciphertext)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Объединяем IV и зашифрованный текст
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            System.err.println("[AES] Ошибка шифрования: " + e.getMessage());
            return null;
        }
    }

    /**
     * Расшифрование текста
     * @param cipherText зашифрованная строка в Base64
     * @return расшифрованный текст
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return "";
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            // Извлекаем IV и зашифрованный текст
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("[AES] Ошибка расшифрования: " + e.getMessage());
            return "[Данные зашифрованы и не могут быть прочитаны]";
        }
    }
}