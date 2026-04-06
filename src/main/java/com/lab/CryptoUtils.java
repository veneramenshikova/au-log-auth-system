package com.lab;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private final SecureRandom secureRandom;
    private String hmacKey;
    private static final String KEY_FILE = "hmac_key.secret";

    public CryptoUtils() {
        this.secureRandom = new SecureRandom();
        this.secureRandom.nextInt();
        loadOrGenerateKey();
        System.out.println("[КРИПТО] Инициализирован SecureRandom (urandom)");
    }

    private void loadOrGenerateKey() {
        Path keyPath = Paths.get(KEY_FILE);
        
        try {
            // Пробуем прочитать существующий ключ из файла
            if (Files.exists(keyPath)) {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                hmacKey = Base64.getEncoder().encodeToString(keyBytes);
                System.out.println("[КРИПТО] HMAC-ключ загружен из файла: " + KEY_FILE);
                return;
            }
            
            // Генерируем новый ключ и сохраняем его
            byte[] newKeyBytes = new byte[32];
            secureRandom.nextBytes(newKeyBytes);
            Files.write(keyPath, newKeyBytes);
            hmacKey = Base64.getEncoder().encodeToString(newKeyBytes);
            System.out.println("[КРИПТО] Сгенерирован и сохранён новый HMAC-ключ");
            System.out.println("[КРИПТО] Файл ключа: " + KEY_FILE + " (НЕ КОММИТЬ В GIT!)");
            
        } catch (Exception e) {
            System.err.println("[КРИПТО] Ошибка работы с файлом ключа: " + e.getMessage());
            // Фолбэк: генерируем ключ в памяти (будет несовместим между запусками)
            byte[] fallbackKey = new byte[32];
            secureRandom.nextBytes(fallbackKey);
            hmacKey = Base64.getEncoder().encodeToString(fallbackKey);
            System.err.println("[КРИПТО] ВНИМАНИЕ: Используется временный ключ (несовместим с БД!)");
        }
    }

    public String generateSalt() {
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public String hmacWithSalt(String password, String salt) {
        String data = password + salt;
        return hmacSha256(data, hmacKey);
    }

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

    public byte[] generateRandomBytes(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        System.out.println("[КРИПТО] Сгенерировано " + length + " байт через urandom");
        return randomBytes;
    }

    public void logRandomBytesDemo() {
        byte[] bytes = generateRandomBytes(16);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(4, bytes.length); i++) {
            hex.append(String.format("%02X", bytes[i]));
        }
        System.out.println("[КРИПТО] Пример случайных данных: " + hex + "...");
    }
}
