package com.lab;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.TitledBorder;

public class DocumentsPanel extends JPanel {

    private final Database db;
    private final AccessControl accessControl;
    private final DocumentsManager documentsManager;
    private final String currentUser;
    private final JFrame parentFrame;
    private final Runnable onLogout;

    private JTable documentsTable;
    private DocumentsTableModel tableModel;
    private JTextArea contentArea;
    private JButton viewButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton createButton;
    private JButton grantButton;
    private JButton backButton;

    private List<Document> allDocuments;
    private List<Document> accessibleDocuments;

    public DocumentsPanel(Database db, AccessControl accessControl,
                          DocumentsManager documentsManager,
                          String currentUser, JFrame parentFrame, Runnable onLogout) {
        this.db = db;
        this.accessControl = accessControl;
        this.documentsManager = documentsManager;
        this.currentUser = currentUser;
        this.parentFrame = parentFrame;
        this.onLogout = onLogout;

        setLayout(new BorderLayout());
        setBackground(new Color(45, 45, 60));

        initComponents();
        loadDocuments();
    }

    private void initComponents() {
        // Верхняя панель с информацией
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(45, 45, 60));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Управление документами");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(100, 200, 255));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JLabel userLabel = new JLabel("Пользователь: " + currentUser);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userLabel.setForeground(Color.LIGHT_GRAY);
        topPanel.add(userLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Центральная панель (таблица + область содержимого)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setBackground(new Color(45, 45, 60));

        // Левая панель: таблица документов
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(45, 45, 60));

        tableModel = new DocumentsTableModel();
        documentsTable = new JTable(tableModel);
        documentsTable.setBackground(new Color(60, 60, 75));
        documentsTable.setForeground(Color.WHITE);
        documentsTable.setSelectionBackground(new Color(100, 130, 200));
        documentsTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        documentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onDocumentSelected();
            }
        });

        JScrollPane tableScroll = new JScrollPane(documentsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Доступные документы",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), Color.LIGHT_GRAY
        ));
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        // Правая панель: содержимое документа
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(45, 45, 60));

        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setBackground(new Color(60, 60, 75));
        contentArea.setForeground(new Color(180, 255, 180));
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Содержимое документа",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), Color.LIGHT_GRAY
        ));
        rightPanel.add(contentScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Нижняя панель с кнопками
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(new Color(45, 45, 60));

        viewButton = new JButton("👁️ Просмотреть");
        viewButton.setBackground(new Color(70, 130, 200));
        viewButton.setForeground(Color.WHITE);
        viewButton.addActionListener(e -> viewDocument());

        editButton = new JButton("✏️ Редактировать");
        editButton.setBackground(new Color(70, 130, 200));
        editButton.setForeground(Color.WHITE);
        editButton.addActionListener(e -> editDocument());

        deleteButton = new JButton("🗑️ Удалить");
        deleteButton.setBackground(new Color(200, 70, 70));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> deleteDocument());

        createButton = new JButton("➕ Создать документ");
        createButton.setBackground(new Color(70, 130, 200));
        createButton.setForeground(Color.WHITE);
        createButton.addActionListener(e -> createDocument());

        grantButton = new JButton("🔓 Выдать доступ");
        grantButton.setBackground(new Color(70, 130, 200));
        grantButton.setForeground(Color.WHITE);
        grantButton.addActionListener(e -> grantPermission());

        backButton = new JButton("🚪 Выйти");
        backButton.setBackground(new Color(70, 70, 70));
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        bottomPanel.add(viewButton);
        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(createButton);
        bottomPanel.add(grantButton);
        bottomPanel.add(backButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadDocuments() {
        allDocuments = documentsManager.getAllDocuments();
        accessibleDocuments = new ArrayList<>();

        for (Document doc : allDocuments) {
            if (accessControl.canRead(currentUser, doc)) {
                accessibleDocuments.add(doc);
                System.out.println("[DocumentsPanel] Доступен: " + doc.getTitle());
            } else {
                System.out.println("[DocumentsPanel] НЕТ доступа: " + doc.getTitle());
            }
        }

        tableModel.fireTableDataChanged();

        if (!accessibleDocuments.isEmpty()) {
            documentsTable.setRowSelectionInterval(0, 0);
            onDocumentSelected();
        }
    }

    private void onDocumentSelected() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < accessibleDocuments.size()) {
            Document doc = accessibleDocuments.get(selectedRow);
            contentArea.setText(doc.getContentPlain());

            // Обновляем состояние кнопок
            boolean canWrite = accessControl.canWrite(currentUser, doc);
            boolean canDelete = accessControl.canDelete(currentUser, doc);
            boolean isOwner = doc.getOwnerId().equals(currentUser);

            editButton.setEnabled(canWrite);
            deleteButton.setEnabled(canDelete);
            grantButton.setEnabled(isOwner || "admin".equals(getUserRole(currentUser)));
        }
    }

    private String getUserRole(String login) {
        // Получение роли через Database
        return db.getUserRole(login);
    }

    private void viewDocument() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            Document doc = accessibleDocuments.get(selectedRow);
            JOptionPane.showMessageDialog(this,
                    "Название: " + doc.getTitle() + "\n\n" +
                            "Содержимое:\n" + doc.getContentPlain() + "\n\n" +
                            "Владелец: " + doc.getOwnerId() + "\n" +
                            "Уровень секретности: " + doc.getClearanceLevel() + "\n" +
                            "Отдел: " + doc.getDepartment(),
                    "Просмотр документа", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editDocument() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            Document doc = accessibleDocuments.get(selectedRow);

            if (!accessControl.canWrite(currentUser, doc)) {
                JOptionPane.showMessageDialog(this, "У вас нет прав на редактирование этого документа!",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String newTitle = JOptionPane.showInputDialog(this, "Введите новое название:", doc.getTitle());
            if (newTitle == null || newTitle.trim().isEmpty()) return;

            String newContent = JOptionPane.showInputDialog(this, "Введите новое содержимое:", doc.getContentPlain());
            if (newContent == null) return;

            if (documentsManager.updateDocument(doc.getId(), newTitle, newContent)) {
                JOptionPane.showMessageDialog(this, "Документ обновлён!");
                loadDocuments();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка обновления документа!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteDocument() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            Document doc = accessibleDocuments.get(selectedRow);

            if (!accessControl.canDelete(currentUser, doc)) {
                JOptionPane.showMessageDialog(this, "У вас нет прав на удаление этого документа!",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите удалить документ \"" + doc.getTitle() + "\"?",
                    "Подтверждение удаления", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (documentsManager.deleteDocument(doc.getId())) {
                    JOptionPane.showMessageDialog(this, "Документ удалён!");
                    loadDocuments();
                } else {
                    JOptionPane.showMessageDialog(this, "Ошибка удаления документа!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void createDocument() {
        JTextField titleField = new JTextField(20);
        JTextArea contentField = new JTextArea(5, 20);
        JComboBox<String> levelBox = new JComboBox<>(new String[]{"1 (обычный)", "2 (секретно)", "3 (сверхсекретно)"});
        JTextField deptField = new JTextField(15);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Название:"), gbc);
        gbc.gridx = 1;
        panel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Содержимое:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(contentField), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Уровень секретности:"), gbc);
        gbc.gridx = 1;
        panel.add(levelBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Отдел:"), gbc);
        gbc.gridx = 1;
        panel.add(deptField, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Создание нового документа",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String content = contentField.getText();
            int level = levelBox.getSelectedIndex() + 1;
            String dept = deptField.getText().trim().isEmpty() ? "common" : deptField.getText().trim();

            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Название не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (documentsManager.createDocument(title, content, currentUser, level, dept)) {
                JOptionPane.showMessageDialog(this, "Документ создан!");
                loadDocuments();
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка создания документа!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void grantPermission() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Выберите документ!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Document doc = accessibleDocuments.get(selectedRow);

        if (!doc.getOwnerId().equals(currentUser) && !"admin".equals(getUserRole(currentUser))) {
            JOptionPane.showMessageDialog(this, "Только владелец документа может выдавать права доступа!",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String targetUser = JOptionPane.showInputDialog(this, "Введите логин пользователя для выдачи доступа:");
        if (targetUser == null || targetUser.trim().isEmpty()) return;

        if (!db.userExists(targetUser)) {
            JOptionPane.showMessageDialog(this, "Пользователь не найден!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JCheckBox readBox = new JCheckBox("Чтение", true);
        JCheckBox writeBox = new JCheckBox("Запись", false);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(readBox);
        panel.add(writeBox);

        int result = JOptionPane.showConfirmDialog(this, panel, "Выдача прав для " + targetUser,
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            if (accessControl.grantPermission(currentUser, targetUser, doc.getId(), readBox.isSelected(), writeBox.isSelected())) {
                JOptionPane.showMessageDialog(this, "Права успешно выданы!");
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка выдачи прав!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Модель таблицы
    class DocumentsTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Название", "Владелец", "Уровень", "Отдел"};

        @Override
        public int getRowCount() {
            return accessibleDocuments.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Document doc = accessibleDocuments.get(rowIndex);
            switch (columnIndex) {
                case 0: return doc.getId();
                case 1: return doc.getTitle();
                case 2: return doc.getOwnerId();
                case 3: return doc.getClearanceLevel();
                case 4: return doc.getDepartment();
                default: return null;
            }
        }
    }
}