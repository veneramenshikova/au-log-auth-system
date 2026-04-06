package com.lab;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuthController {
    private final Database database;
    private final CryptoUtils crypto;

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_SECONDS = 30;

    public AuthController(Database db) {
        this.database = db;
        this.crypto = new CryptoUtils();
    }

    public boolean identify(String login) {
        System.out.println("\n[ИДЕНТИФИКАЦИЯ] Проверка логина: '" + login + "'");

        if (login == null || login.trim().isEmpty()) {
            System.out.println("[ИДЕНТИФИКАЦИЯ] Ошибка: пустой логин");
            return false;
        }

        boolean exists = database.userExists(login);

        if (exists) {
            System.out.println("[ИДЕНТИФИКАЦИЯ] Результат: пользователь найден");
        } else {
            System.out.println("[ИДЕНТИФИКАЦИЯ] Результат: пользователь НЕ найден");
        }

        return exists;
    }

    public boolean authenticate(String login, String password) {
        System.out.println("\n[АУТЕНТИФИКАЦИЯ] Проверка для пользователя: " + login);

        if (password == null || password.isEmpty()) {
            System.out.println("[АУТЕНТИФИКАЦИЯ] Ошибка: пустой пароль");
            return false;
        }

        Database.UserData userData = database.getUserData(login);
        if (userData == null) {
            System.out.println("[АУТЕНТИФИКАЦИЯ] Ошибка: данные пользователя не найдены");
            return false;
        }

        // Проверка блокировки
        if (isAccountLocked(userData)) {
            System.out.println("[АУТЕНТИФИКАЦИЯ] ОШИБКА: учётная запись временно заблокирована");
            return false;
        }

        // Вычисляем HMAC с солью
        String computedHmac = crypto.hmacWithSalt(password, userData.salt);

        // Сравниваем (без вывода HMAC в лог!)
        boolean passwordMatches = computedHmac.equals(userData.passwordHmac);

        if (passwordMatches) {
            System.out.println("[АУТЕНТИФИКАЦИЯ] Результат: пароль верный ✅");
            database.resetFailedAttempts(login);

            // Демонстрация поля 2FA (требование лабораторной)
            if (userData.has2fa) {
                System.out.println("[АУТЕНТИФИКАЦИЯ] Двухфакторная аутентификация: поле активировано (требуется код)");
                // Здесь в реальном приложении был бы запрос кода
            }

            return true;
        } else {
            System.out.println("[АУТЕНТИФИКАЦИЯ] Результат: неверный пароль ❌");
            handleFailedAttempt(login, userData.failedAttempts + 1);
            return false;
        }
    }

    private boolean isAccountLocked(Database.UserData userData) {
        if (userData.lockedUntil == null || userData.lockedUntil.isEmpty()) {
            return false;
        }

        try {
            LocalDateTime lockedUntil = LocalDateTime.parse(userData.lockedUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (LocalDateTime.now().isBefore(lockedUntil)) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("[АУТЕНТИФИКАЦИЯ] Ошибка парсинга времени блокировки");
        }
        return false;
    }

    private void handleFailedAttempt(String login, int attempts) {
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusSeconds(LOCKOUT_DURATION_SECONDS);
            String lockedUntilStr = lockedUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            database.updateFailedAttempts(login, attempts, lockedUntilStr);
            System.out.println("[АУТЕНТИФИКАЦИЯ] Учётная запись заблокирована на " + LOCKOUT_DURATION_SECONDS + " секунд");
        } else {
            database.updateFailedAttempts(login, attempts, null);
            System.out.println("[АУТЕНТИФИКАЦИЯ] Осталось попыток: " + (MAX_FAILED_ATTEMPTS - attempts));
        }
    }

    public String authorize(String login) {
        System.out.println("\n[АВТОРИЗАЦИЯ] Запрос прав для пользователя: " + login);
        String role = database.getUserRole(login);
        System.out.println("[АВТОРИЗАЦИЯ] Выданы права: " + role);

        // Демонстрация генерации случайных данных (требование лабораторной)
        crypto.logRandomBytesDemo();

        return role;
    }

    // Для тестирования: сброс блокировки
    public void forceResetLockout(String login) {
        database.resetFailedAttempts(login);
        System.out.println("[АУТЕНТИФИКАЦИЯ] Блокировка для " + login + " сброшена принудительно");
    }

    public void generateRandomData() {
        crypto.logRandomBytesDemo();
    }
}