package com.lab;

import java.sql.*;
import java.util.UUID;

public class Database {
    // Файл БД создастся в папке проекта
    private static final String DB_URL = "jdbc:sqlite:auth.db";
    private Connection connection;

    public Database() {
        try {
            // Загружаем драйвер SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTable();
            insertTestData();
            System.out.println("[БД] SQLite подключена. Файл: auth.db");
        } catch (ClassNotFoundException e) {
            System.err.println("[БД] Драйвер SQLite не найден: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка подключения: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                salt TEXT NOT NULL,
                password_hmac TEXT NOT NULL,
                has_2fa INTEGER DEFAULT 0,
                role TEXT NOT NULL,
                failed_attempts INTEGER DEFAULT 0,
                locked_until TIMESTAMP
            )
            """;
        Statement stmt = connection.createStatement();
        stmt.execute(sql);
        System.out.println("[БД] Таблица users создана/проверена");
    }

    private void insertTestData() throws SQLException {
        // Проверяем, есть ли уже данные
        String checkSql = "SELECT COUNT(*) FROM users";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(checkSql);
        rs.next();
        if (rs.getInt(1) > 0) {
            System.out.println("[БД] Тестовые данные уже существуют");
            return;
        }

        CryptoUtils crypto = new CryptoUtils();

        // Тестовые пользователи: логин, пароль, роль
        String[][] testUsers = {
                {"admin", "admin123", "admin"},
                {"editor", "edit123", "editor"},
                {"user", "user123", "guest"}
        };

        String insertSql = "INSERT INTO users (login, salt, password_hmac, has_2fa, role) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = connection.prepareStatement(insertSql);

        for (String[] user : testUsers) {
            String salt = crypto.generateSalt();
            String hmac = crypto.hmacWithSalt(user[1], salt);

            pstmt.setString(1, user[0]);
            pstmt.setString(2, salt);
            pstmt.setString(3, hmac);
            pstmt.setInt(4, user[0].equals("user") ? 1 : 0); // только user имеет флаг 2FA
            pstmt.setString(5, user[2]);
            pstmt.executeUpdate();
        }

        System.out.println("[БД] Добавлены тестовые пользователи:");
        System.out.println("  - admin / admin123 (права: admin)");
        System.out.println("  - editor / edit123 (права: editor)");
        System.out.println("  - user / user123 (права: guest, 2FA=true)");
    }

    public boolean userExists(String login) {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка проверки пользователя: " + e.getMessage());
            return false;
        }
    }

    public UserData getUserData(String login) {
        String sql = "SELECT salt, password_hmac, role, has_2fa, failed_attempts, locked_until FROM users WHERE login = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UserData(
                        rs.getString("salt"),
                        rs.getString("password_hmac"),
                        rs.getString("role"),
                        rs.getInt("has_2fa") == 1,
                        rs.getInt("failed_attempts"),
                        rs.getString("locked_until")
                );
            }
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка получения данных: " + e.getMessage());
        }
        return null;
    }

    public void updateFailedAttempts(String login, int attempts, String lockedUntil) {
        String sql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE login = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, attempts);
            pstmt.setString(2, lockedUntil);
            pstmt.setString(3, login);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка обновления попыток: " + e.getMessage());
        }
    }

    public void resetFailedAttempts(String login) {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE login = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, login);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка сброса попыток: " + e.getMessage());
        }
    }

    public String getUserRole(String login) {
        String sql = "SELECT role FROM users WHERE login = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("[БД] Ошибка получения роли: " + e.getMessage());
        }
        return "unknown";
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Вспомогательный класс для хранения данных пользователя
    public static class UserData {
        public final String salt;
        public final String passwordHmac;
        public final String role;
        public final boolean has2fa;
        public final int failedAttempts;
        public final String lockedUntil;

        public UserData(String salt, String passwordHmac, String role, boolean has2fa,
                        int failedAttempts, String lockedUntil) {
            this.salt = salt;
            this.passwordHmac = passwordHmac;
            this.role = role;
            this.has2fa = has2fa;
            this.failedAttempts = failedAttempts;
            this.lockedUntil = lockedUntil;
        }
    }
}