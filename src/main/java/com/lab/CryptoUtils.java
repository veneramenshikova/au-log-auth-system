package com.lab;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private final SecureRandom secureRandom;

    // Ключ HMAC — больше не захардкожен, генерируется или читается из переменной окружения
    private String hmacKey;

    public CryptoUtils() {
        this.secureRandom = new SecureRandom();
        // Инициализация urandom
        this.secureRandom.nextInt();
        loadOrGenerateKey();
        System.out.println("[КРИПТО] Инициализирован SecureRandom (urandom)");
    }

    private void loadOrGenerateKey() {
        // Пытаемся прочитать ключ из переменной окружения
        hmacKey = System.getenv("AU_LOG_HMAC_KEY");

        if (hmacKey == null || hmacKey.isEmpty()) {
            // Генерируем случайный ключ при первом запуске
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            hmacKey = Base64.getEncoder().encodeToString(keyBytes);
            System.out.println("[КРИПТО] Сгенерирован новый HMAC-ключ (сохраните для отладки)");
            System.out.println("[КРИПТО] KEY=" + hmacKey);
        } else {
            System.out.println("[КРИПТО] HMAC-ключ загружен из переменной окружения");
        }
    }

    /**
     * Генерация случайной соли для каждого пользователя
     */
    public String generateSalt() {
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * HMAC-SHA256 с солью (соль добавляется к паролю)
     * @param password пароль пользователя
     * @param salt уникальная соль
     */
    public String hmacWithSalt(String password, String salt) {
        String data = password + salt;  // Соль добавляется к паролю
        return hmacSha256(data, hmacKey);
    }

    /**
     * HMAC-SHA256 для произвольных данных
     */
    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                hexString.append(String.format("%02X", b));
            }

            return hexString.toString();

        } catch (Exception e) {
            System.err.println("[КРИПТО] Ошибка при вычислении HMAC: " + e.getMessage());
            return null;
        }
    }

    /**
     * Генерация случайных байтов (демонстрация urandom)
     * @param length количество байт
     */
    public byte[] generateRandomBytes(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        System.out.println("[КРИПТО] Сгенерировано " + length + " байт через urandom");
        return randomBytes;
    }

    /**
     * Вспомогательный метод для отображения HEX (только для демонстрации, без реальных секретов)
     */
    public void logRandomBytesDemo() {
        byte[] bytes = generateRandomBytes(16);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(4, bytes.length); i++) {
            hex.append(String.format("%02X", bytes[i]));
        }
        System.out.println("[КРИПТО] Пример случайных данных: " + hex + "...");
    }
}