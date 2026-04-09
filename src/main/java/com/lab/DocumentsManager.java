package com.lab;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentsManager {

    private final Connection connection;
    private final AesUtil aesUtil;

    public DocumentsManager(Connection connection) {
        this.connection = connection;
        this.aesUtil = new AesUtil();
        createDocumentsTable();
    }

    private void createDocumentsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT,
                owner_id TEXT NOT NULL,
                clearance_level INTEGER DEFAULT 1,
                department TEXT DEFAULT 'common',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (owner_id) REFERENCES users(login)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("[DocumentsManager] Таблица документов создана");
            insertTestDocuments();
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка создания таблицы: " + e.getMessage());
        }
    }

    private void insertTestDocuments() {
        // Проверяем, есть ли уже документы
        String checkSql = "SELECT COUNT(*) FROM documents";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("[DocumentsManager] Тестовые документы уже существуют");
                return;
            }
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка проверки: " + e.getMessage());
        }

        // Тестовые документы
        Object[][] testDocs = {
                {"Общий документ", "Это открытый документ для всех", "admin", 1, "common"},
                {"Секретный документ", "Это секретный документ уровня 2", "admin", 2, "it"},
                {"Сверхсекретный документ", "Документ высшего уровня секретности", "admin", 3, "security"},
                {"Документ отдела IT", "Только для IT отдела", "editor", 2, "it"},
                {"Личный документ пользователя", "Только для user", "user", 1, "common"}
        };

        String insertSql = "INSERT INTO documents (title, content, owner_id, clearance_level, department) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Object[] doc : testDocs) {
                String plainContent = (String) doc[1];
                String encryptedContent = aesUtil.encrypt(plainContent);

                pstmt.setString(1, (String) doc[0]);
                pstmt.setString(2, encryptedContent);
                pstmt.setString(3, (String) doc[2]);
                pstmt.setInt(4, (Integer) doc[3]);
                pstmt.setString(5, (String) doc[4]);
                pstmt.executeUpdate();
            }
            System.out.println("[DocumentsManager] Добавлено 5 тестовых документов");
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка вставки: " + e.getMessage());
        }
    }

    public List<Document> getAllDocuments() {
        List<Document> docs = new ArrayList<>();
        String sql = "SELECT * FROM documents ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                docs.add(extractDocument(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка получения документов: " + e.getMessage());
        }
        return docs;
    }

    public Document getDocumentById(int id) {
        String sql = "SELECT * FROM documents WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractDocument(rs);
            }
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка получения документа: " + e.getMessage());
        }
        return null;
    }

    private Document extractDocument(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String encryptedContent = rs.getString("content");
        String ownerId = rs.getString("owner_id");
        int clearanceLevel = rs.getInt("clearance_level");
        String department = rs.getString("department");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        Document doc = new Document(id, title, encryptedContent, ownerId,
                clearanceLevel, department, createdAt);
        // Расшифровываем содержимое для отображения
        String decrypted = aesUtil.decrypt(encryptedContent);
        doc.setContentPlain(decrypted);

        return doc;
    }

    public boolean createDocument(String title, String content, String ownerId,
                                  int clearanceLevel, String department) {
        String encryptedContent = aesUtil.encrypt(content);
        String sql = "INSERT INTO documents (title, content, owner_id, clearance_level, department) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, encryptedContent);
            pstmt.setString(3, ownerId);
            pstmt.setInt(4, clearanceLevel);
            pstmt.setString(5, department);
            pstmt.executeUpdate();
            System.out.println("[DocumentsManager] Создан документ: " + title);
            return true;
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка создания: " + e.getMessage());
            return false;
        }
    }

    public boolean updateDocument(int id, String title, String content) {
        String encryptedContent = aesUtil.encrypt(content);
        String sql = "UPDATE documents SET title = ?, content = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, encryptedContent);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
            System.out.println("[DocumentsManager] Обновлён документ ID: " + id);
            return true;
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка обновления: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDocument(int id) {
        String sql = "DELETE FROM documents WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("[DocumentsManager] Удалён документ ID: " + id);
            return true;
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка удаления: " + e.getMessage());
            return false;
        }
    }

    public List<Document> getDocumentsByOwner(String ownerId) {
        List<Document> docs = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE owner_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                docs.add(extractDocument(rs));
            }
        } catch (SQLException e) {
            System.err.println("[DocumentsManager] Ошибка: " + e.getMessage());
        }
        return docs;
    }
}