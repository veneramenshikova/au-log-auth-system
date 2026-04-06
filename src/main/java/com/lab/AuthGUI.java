package com.lab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.border.TitledBorder;

public class AuthGUI {
    private final AuthController authController;

    // Компоненты окна
    private JFrame frame;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JTextArea logArea;
    private JButton loginButton;
    private JButton clearButton;
    private JPanel mainPanel;
    private JPanel menuPanel;
    private CardLayout cardLayout;

    // Для отслеживания состояния аутентификации
    private String currentUser = null;
    private int authAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    // Секретная комбинация (Alt+5+Esc)
    private boolean altPressed = false;
    private boolean fivePressed = false;

    public AuthGUI(AuthController authController) {
        this.authController = authController;
        initialize();
    }

    private void initialize() {
        frame = new JFrame("AU.log — Система идентификации, аутентификации и авторизации");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        // Используем CardLayout для переключения между экранами
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Создаём экраны
        mainPanel.add(createLoginPanel(), "login");
        mainPanel.add(createAuthPanel(), "auth");
        mainPanel.add(createAccessPanel(), "access");

        frame.add(mainPanel);
        frame.setVisible(true);

        // Логируем запуск
        addLog("[СИСТЕМА] AU.log запущена");
        addLog("[СИСТЕМА] Используется HMAC-SHA256 для аутентификации");
        addLog("[СИСТЕМА] Генератор случайных чисел: SecureRandom (urandom)");
        addLog("[СИСТЕМА] База данных: H2 (файл userdb.mv.db)");
        addLog("[СИСТЕМА] Ожидание ввода логина...");
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(45, 45, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Заголовок
        JLabel titleLabel = new JLabel("AU.log");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(new Color(100, 200, 255));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Идентификация • Аутентификация • Авторизация");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        gbc.gridy = 1;
        panel.add(subtitleLabel, gbc);

        // Логин
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel loginLabel = new JLabel("Логин:");
        loginLabel.setForeground(Color.WHITE);
        loginLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(loginLabel, gbc);

        gbc.gridx = 1;
        loginField = new JTextField(20);
        loginField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        loginField.addKeyListener(new SecretKeyListener());
        panel.add(loginField, gbc);

        // Кнопки
        gbc.gridy = 3;
        gbc.gridx = 0;
        loginButton = new JButton("Идентификация");
        loginButton.setBackground(new Color(70, 130, 200));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.addActionListener(e -> performIdentification());
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> loginField.setText(""));
        panel.add(clearButton, gbc);

        // Область лога
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 40));
        logArea.setForeground(new Color(180, 255, 180));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Журнал событий (поэтапный вывод)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), Color.LIGHT_GRAY
        ));
        panel.add(scrollPane, gbc);

        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(45, 45, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Аутентификация");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 200, 100));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel();
        userLabel.setForeground(Color.WHITE);
        gbc.gridy = 1;
        panel.add(userLabel, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        JLabel passLabel = new JLabel("Пароль:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        passwordField.addActionListener(e -> performAuthentication());
        panel.add(passwordField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        JButton authButton = new JButton("Аутентификация");
        authButton.setBackground(new Color(70, 130, 200));
        authButton.setForeground(Color.WHITE);
        authButton.setFont(new Font("Arial", Font.BOLD, 14));
        authButton.addActionListener(e -> performAuthentication());
        panel.add(authButton, gbc);

        gbc.gridx = 1;
        JButton backButton = new JButton("Назад");
        backButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "login");
            authAttempts = 0;
            addLog("[АУТЕНТИФИКАЦИЯ] Возврат к идентификации");
        });
        panel.add(backButton, gbc);

        // Лог для этого экрана используем общий
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        JTextArea logArea2 = new JTextArea();
        logArea2.setEditable(false);
        logArea2.setBackground(new Color(30, 30, 40));
        logArea2.setForeground(new Color(180, 255, 180));
        logArea2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane2 = new JScrollPane(logArea2);
        scrollPane2.setBorder(BorderFactory.createTitledBorder("Журнал событий"));
        panel.add(scrollPane2, gbc);

        // Синхронизируем лог
        Timer timer = new Timer(100, e -> {
            logArea2.setText(logArea.getText());
        });
        timer.start();

        return panel;
    }

    private JPanel createAccessPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 60));

        JLabel titleLabel = new JLabel("Панель доступа", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(100, 255, 150));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(new Color(45, 45, 60));
        panel.add(menuPanel, BorderLayout.CENTER);

        JButton logoutButton = new JButton("Выход из системы");
        logoutButton.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "login");
            loginField.setText("");
            addLog("[СИСТЕМА] Пользователь вышел из системы");
        });
        panel.add(logoutButton, BorderLayout.SOUTH);

        // Контейнер для динамических кнопок меню
        JPanel dynamicMenu = new JPanel(new GridLayout(0, 1, 10, 10));
        dynamicMenu.setBackground(new Color(45, 45, 60));
        dynamicMenu.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        menuPanel.add(dynamicMenu);

        // Метод для обновления меню по роли
        panel.putClientProperty("dynamicMenu", dynamicMenu);

        return panel;
    }

    private void performIdentification() {
        String login = loginField.getText().trim();

        addLog("\n=== ЭТАП 1: ИДЕНТИФИКАЦИЯ ===");
        addLog("[ИДЕНТИФИКАЦИЯ] Введён логин: '" + login + "'");

        if (login.isEmpty()) {
            addLog("[ИДЕНТИФИКАЦИЯ] Ошибка: логин не может быть пустым");
            JOptionPane.showMessageDialog(frame, "Введите логин!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean exists = authController.identify(login);

        if (exists) {
            addLog("[ИДЕНТИФИКАЦИЯ] ✅ Пользователь найден в системе");
            addLog("[ИДЕНТИФИКАЦИЯ] Переход к этапу аутентификации...");
            currentUser = login;
            authAttempts = 0;
            cardLayout.show(mainPanel, "auth");
        } else {
            addLog("[ИДЕНТИФИКАЦИЯ] ❌ Пользователь НЕ найден в системе");
            JOptionPane.showMessageDialog(frame,
                    "Пользователь '" + login + "' не найден в системе!",
                    "Ошибка идентификации",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performAuthentication() {
        String password = new String(passwordField.getPassword());

        addLog("\n=== ЭТАП 2: АУТЕНТИФИКАЦИЯ ===");
        addLog("[АУТЕНТИФИКАЦИЯ] Пользователь: " + currentUser);
        addLog("[АУТЕНТИФИКАЦИЯ] Попытка " + (authAttempts + 1) + " из " + MAX_ATTEMPTS);

        boolean authenticated = authController.authenticate(currentUser, password);

        if (authenticated) {
            addLog("[АУТЕНТИФИКАЦИЯ] ✅ Пароль верный!");
            addLog("[АУТЕНТИФИКАЦИЯ] Переход к этапу авторизации...");

            // Авторизация
            addLog("\n=== ЭТАП 3: АВТОРИЗАЦИЯ ===");
            String role = authController.authorize(currentUser);
            addLog("[АВТОРИЗАЦИЯ] ✅ Пользователю '" + currentUser + "' выданы права: " + role);

            // Демонстрация генерации случайных данных (требование лабораторной)
            authController.generateRandomData();

            showAccessMenu(role);
        } else {
            authAttempts++;
            addLog("[АУТЕНТИФИКАЦИЯ] ❌ Неверный пароль!");

            if (authAttempts >= MAX_ATTEMPTS) {
                addLog("[АУТЕНТИФИКАЦИЯ] Превышено количество попыток (" + MAX_ATTEMPTS + ")");
                addLog("[АУТЕНТИФИКАЦИЯ] Возврат к идентификации");
                JOptionPane.showMessageDialog(frame,
                        "Превышено количество попыток ввода пароля!",
                        "Доступ заблокирован",
                        JOptionPane.WARNING_MESSAGE);
                cardLayout.show(mainPanel, "login");
                loginField.setText("");
                authAttempts = 0;
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Неверный пароль! Осталось попыток: " + (MAX_ATTEMPTS - authAttempts),
                        "Ошибка аутентификации",
                        JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        }
    }

    private void showAccessMenu(String role) {
        JPanel accessPanel = (JPanel) mainPanel.getComponent(2);
        JPanel dynamicMenu = (JPanel) accessPanel.getClientProperty("dynamicMenu");
        dynamicMenu.removeAll();

        JLabel roleLabel = new JLabel("Роль: " + role.toUpperCase());
        roleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roleLabel.setForeground(new Color(255, 200, 100));
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dynamicMenu.add(roleLabel);

        // Кнопки в зависимости от роли
        if (role.equals("admin")) {
            addMenuButton(dynamicMenu, "🔧 Просмотр всех пользователей", "admin_view");
            addMenuButton(dynamicMenu, "⚙️ Редактирование настроек", "admin_edit");
            addMenuButton(dynamicMenu, "👥 Управление пользователями", "admin_users");
            addMenuButton(dynamicMenu, "👤 Просмотр своего профиля", "user_profile");
        } else if (role.equals("editor")) {
            addMenuButton(dynamicMenu, "📄 Просмотр данных", "editor_view");
            addMenuButton(dynamicMenu, "✏️ Редактирование контента", "editor_edit");
            addMenuButton(dynamicMenu, "👤 Просмотр своего профиля", "user_profile");
        } else {
            addMenuButton(dynamicMenu, "👁️ Просмотр данных (только чтение)", "guest_view");
            addMenuButton(dynamicMenu, "👤 Просмотр своего профиля", "user_profile");
        }

        dynamicMenu.revalidate();
        dynamicMenu.repaint();

        addLog("\n=== МЕНЮ ДОСТУПА ===");
        addLog("[АВТОРИЗАЦИЯ] Открыто меню с правами: " + role);

        cardLayout.show(mainPanel, "access");


        JButton logoutButton = new JButton("🚪 Выйти из системы");
        logoutButton.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "login");
            loginField.setText("");
            passwordField.setText("");
            addLog("[СИСТЕМА] Пользователь вышел из системы");
        });
        dynamicMenu.add(logoutButton);
    }

    private void addMenuButton(JPanel panel, String text, String action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setBackground(new Color(70, 130, 200));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            addLog("[ДОСТУП] Выполнение действия: " + text);
            JOptionPane.showMessageDialog(frame,
                    "Выполнение: " + text + "\nПрава доступа подтверждены.",
                    "Доступ разрешён",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        panel.add(button);
    }

    private void addLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // Класс для обработки секретной комбинации Alt+5+Esc
    private class SecretKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ALT) {
                altPressed = true;
            } else if (e.getKeyCode() == KeyEvent.VK_5) {
                fivePressed = true;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (altPressed && fivePressed) {
                    showCatMeme();
                    altPressed = false;
                    fivePressed = false;
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ALT) {
                altPressed = false;
            } else if (e.getKeyCode() == KeyEvent.VK_5) {
                fivePressed = false;
            }
        }

        private void showCatMeme() {
            addLog("\n=========================================");
            addLog("  🐱 СЮРПРИЗ! МЕМ С КОТЁНКОМ 🐱");
            addLog("=========================================");
            addLog("      /\\_/\\");
            addLog("     ( o.o )");
            addLog("      > ^ <");
            addLog("    АУТЕНТИФИКАЦИЯ ПРОЙДЕНА С ЛЮБОВЬЮ!");
            addLog("=========================================");

            JOptionPane.showMessageDialog(frame,
                    "      /\\_/\\\n" +
                            "     ( o.o )\n" +
                            "      > ^ <\n\n" +
                            "  АУТЕНТИФИКАЦИЯ ПРОЙДЕНА С ЛЮБОВЬЮ!",
                    "🐱 Секретный мем с котёнком",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}