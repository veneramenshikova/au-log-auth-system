package com.lab;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccessControl {

    private final Database db;
    private final Connection connection;

    public AccessControl(Database db) {
        this.db = db;
        this.connection = db.getConnection();
        createPermissionsTable();
    }

    private void createPermissionsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS doc_permissions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                doc_id INTEGER NOT NULL,
                user_id TEXT NOT NULL,
                can_read INTEGER DEFAULT 0,
                can_write INTEGER DEFAULT 0,
                FOREIGN KEY (doc_id) REFERENCES documents(id),
                UNIQUE(doc_id, user_id)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("[AccessControl] Таблица разрешений создана");
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка создания таблицы: " + e.getMessage());
        }
    }

    /**
     * Получение уровня доступа пользователя
     */
    private int getUserClearance(String login) {
        String sql = "SELECT clearance_level FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("clearance_level");
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка получения уровня: " + e.getMessage());
        }
        return 1;
    }

    /**
     * Получение отдела пользователя
     */
    private String getUserDepartment(String login) {
        String sql = "SELECT department FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("department");
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка получения отдела: " + e.getMessage());
        }
        return "common";
    }

    /**
     * Проверка, является ли пользователь админом
     */
    private boolean isAdmin(String login) {
        String sql = "SELECT role FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "admin".equals(rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка проверки роли: " + e.getMessage());
        }
        return false;
    }

    /**
     * Проверка, является ли пользователь владельцем документа
     */
    private boolean isOwner(String login, Document doc) {
        return doc.getOwnerId().equals(login);
    }

    /**
     * Проверка явного разрешения (дискреционная модель)
     */
    private boolean hasExplicitPermission(String login, int docId, String permissionType) {
        String sql = "SELECT " + permissionType + " FROM doc_permissions WHERE doc_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, docId);
            pstmt.setString(2, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка проверки разрешения: " + e.getMessage());
        }
        return false;
    }

    // ==================== МОДЕЛИ ДОСТУПА ====================

    /**
     * 1. МАНДАТНАЯ МОДЕЛЬ: проверка по уровню секретности
     */
    private boolean checkMandatory(String login, Document doc) {
        int userLevel = getUserClearance(login);
        boolean hasAccess = userLevel >= doc.getClearanceLevel();
        System.out.println("[Mandatory] Пользователь уровень=" + userLevel +
                ", документ уровень=" + doc.getClearanceLevel() +
                " → " + (hasAccess ? "ДОСТУПЕН" : "ЗАКРЫТ"));
        return hasAccess;
    }

    /**
     * 2. АТРИБУТНАЯ МОДЕЛЬ: проверка по отделу
     * (пользователь видит документы своего отдела, если уровень не выше 2)
     */
    private boolean checkAttribute(String login, Document doc) {
        String userDept = getUserDepartment(login);
        // Админ или владелец игнорируют атрибутную модель
        if (isAdmin(login) || isOwner(login, doc)) {
            return true;
        }
        // Документы уровня 3 видны только админу и владельцу
        if (doc.getClearanceLevel() >= 3) {
            return false;
        }
        boolean hasAccess = userDept.equals(doc.getDepartment()) || doc.getDepartment().equals("common");
        System.out.println("[Attribute] Пользователь отдел=" + userDept +
                ", документ отдел=" + doc.getDepartment() +
                " → " + (hasAccess ? "ДОСТУПЕН" : "ЗАКРЫТ"));
        return hasAccess;
    }

    /**
     * 3. ДИСКРЕЦИОННАЯ МОДЕЛЬ: явное разрешение от владельца
     */
    private boolean checkDiscretionary(String login, Document doc, String action) {
        if (isAdmin(login) || isOwner(login, doc)) {
            return true;
        }
        String permType = "can_read".equals(action) ? "can_read" : "can_write";
        boolean hasAccess = hasExplicitPermission(login, doc.getId(), permType);
        System.out.println("[Discretionary] Явное разрешение на " + action + " → " +
                (hasAccess ? "ЕСТЬ" : "НЕТ"));
        return hasAccess;
    }

    /**
     * 4. РОЛЕВАЯ МОДЕЛЬ: базовая проверка роли
     */
    private boolean checkRole(String login, Document doc, String action) {
        if (isAdmin(login)) {
            return true;
        }
        String role = getUserRole(login);
        if ("editor".equals(role) && "write".equals(action)) {
            return true;
        }
        if ("guest".equals(role) && "read".equals(action)) {
            return true;
        }
        System.out.println("[Role] Роль " + role + " не имеет права на " + action);
        return false;
    }

    private String getUserRole(String login) {
        String sql = "SELECT role FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка: " + e.getMessage());
        }
        return "guest";
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ПРОВЕРКИ ====================

    /**
     * Проверка на чтение документа (все модели)
     */
    public boolean canRead(String login, Document doc) {
        System.out.println("\n[AccessControl] Проверка чтения для: " + login);

        if (doc == null) return false;

        // Комбинация всех моделей (кроме ролевой для админа)
        boolean mandatory = checkMandatory(login, doc);
        if (!mandatory) return false;

        boolean attribute = checkAttribute(login, doc);
        if (!attribute) return false;

        boolean discretionary = checkDiscretionary(login, doc, "can_read");
        if (discretionary) return true;

        boolean role = checkRole(login, doc, "read");
        return role;
    }

    /**
     * Проверка на запись (редактирование) документа
     */
    public boolean canWrite(String login, Document doc) {
        System.out.println("\n[AccessControl] Проверка записи для: " + login);

        if (doc == null) return false;

        boolean mandatory = checkMandatory(login, doc);
        if (!mandatory) return false;

        boolean attribute = checkAttribute(login, doc);
        if (!attribute) return false;

        boolean discretionary = checkDiscretionary(login, doc, "can_write");
        if (discretionary) return true;

        boolean role = checkRole(login, doc, "write");
        return role;
    }

    /**
     * Проверка на удаление (только админ или владелец)
     */
    public boolean canDelete(String login, Document doc) {
        return isAdmin(login) || isOwner(login, doc);
    }

    /**
     * Выдача явного разрешения другому пользователю (дискреционная модель)
     */
    public boolean grantPermission(String ownerLogin, String targetLogin, int docId, boolean canRead, boolean canWrite) {
        // Только владелец или админ может выдавать права
        String sql = "SELECT owner_id FROM documents WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, docId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String owner = rs.getString("owner_id");
                if (!owner.equals(ownerLogin) && !isAdmin(ownerLogin)) {
                    System.out.println("[AccessControl] Только владелец или админ может выдавать права");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка: " + e.getMessage());
            return false;
        }

        String insertSql = """
            INSERT INTO doc_permissions (doc_id, user_id, can_read, can_write) 
            VALUES (?, ?, ?, ?) 
            ON CONFLICT(doc_id, user_id) DO UPDATE SET can_read = ?, can_write = ?
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setInt(1, docId);
            pstmt.setString(2, targetLogin);
            pstmt.setInt(3, canRead ? 1 : 0);
            pstmt.setInt(4, canWrite ? 1 : 0);
            pstmt.setInt(5, canRead ? 1 : 0);
            pstmt.setInt(6, canWrite ? 1 : 0);
            pstmt.executeUpdate();
            System.out.println("[AccessControl] Выданы права для " + targetLogin +
                    " (чтение=" + canRead + ", запись=" + canWrite + ")");
            return true;
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка выдачи прав: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получение списка пользователей, имеющих доступ к документу
     */
    public List<String> getUsersWithAccess(int docId) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT user_id FROM doc_permissions WHERE doc_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, docId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            System.err.println("[AccessControl] Ошибка: " + e.getMessage());
        }
        return users;
    }
}