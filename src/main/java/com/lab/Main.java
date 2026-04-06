package com.lab;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("  AU.log — Система Идентификации и");
        System.out.println("  Аутентификации (HMAC-SHA256 + SQLite)");
        System.out.println("=========================================");
        System.out.println("Для установки HMAC-ключа через переменную окружения:");
        System.out.println("  export AU_LOG_HMAC_KEY=ваш_ключ_в_base64");
        System.out.println("=========================================\n");

        SwingUtilities.invokeLater(() -> {
            Database db = new Database();
            AuthController authController = new AuthController(db);
            new AuthGUI(authController);
        });
    }
}