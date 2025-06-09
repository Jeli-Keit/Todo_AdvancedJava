import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class TodoApp extends JFrame {
    private JTextField taskField;
    private JButton addButton, completeButton, deleteButton;
    private JTable table;
    private DefaultTableModel model;
    private Connection conn;

    public TodoApp() {
        setTitle("To-Do List with Date & Time");
        setSize(800, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Font setup
        Font font = new Font("Segoe UI", Font.PLAIN, 14);
        Font headerFont = new Font("Segoe UI", Font.BOLD, 15);

        // Input field with tooltip and font
        taskField = new JTextField(25);
        taskField.setFont(font);
        taskField.setToolTipText("Enter your task here");

        // Buttons with font and tooltip
        addButton = new JButton("Add Task");
        addButton.setFont(font);
        addButton.setToolTipText("Add a new task to the list");

        completeButton = new JButton("Mark as Complete");
        completeButton.setFont(font);
        completeButton.setToolTipText("Mark selected task as completed");

        deleteButton = new JButton("Delete Task");
        deleteButton.setFont(font);
        deleteButton.setToolTipText("Delete selected task");

        // Table model and JTable - make cells non-editable
        model = new DefaultTableModel(new String[]{"ID", "Task", "Status", "Date & Time"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;  // No direct editing in table
            }
        };
        table = new JTable(model);
        table.setFont(font);
        table.setRowHeight(28);

        // Header styling
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(headerFont);

        // Column width tweaks
        table.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(300); // Task
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(3).setPreferredWidth(180); // Date & Time

        // Custom cell renderer to color code status column
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                                                           boolean isSelected, boolean hasFocus, 
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                if ("Completed".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(0, 128, 0)); // Dark green
                } else if ("Pending".equalsIgnoreCase(status)) {
                    c.setForeground(new Color(255, 140, 0)); // Dark orange
                } else {
                    c.setForeground(Color.BLACK);
                }
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        // Layout with GridBagLayout for top panel
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;

        // Clone gbc for each component to avoid reusing the same instance
        GridBagConstraints gbcLabel = (GridBagConstraints) gbc.clone();
        gbcLabel.gridx = 0;
        topPanel.add(new JLabel("Task:"), gbcLabel);

        GridBagConstraints gbcField = (GridBagConstraints) gbc.clone();
        gbcField.gridx = 1;
        gbcField.fill = GridBagConstraints.HORIZONTAL;
        gbcField.weightx = 1.0;  // allow horizontal expansion
        topPanel.add(taskField, gbcField);

        GridBagConstraints gbcAddBtn = (GridBagConstraints) gbc.clone();
        gbcAddBtn.gridx = 2;
        gbcAddBtn.fill = GridBagConstraints.NONE;
        gbcAddBtn.weightx = 0;
        topPanel.add(addButton, gbcAddBtn);

        GridBagConstraints gbcCompleteBtn = (GridBagConstraints) gbc.clone();
        gbcCompleteBtn.gridx = 3;
        topPanel.add(completeButton, gbcCompleteBtn);

        GridBagConstraints gbcDeleteBtn = (GridBagConstraints) gbc.clone();
        gbcDeleteBtn.gridx = 4;
        topPanel.add(deleteButton, gbcDeleteBtn);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        connectDB();
        loadTasks();

        addButton.addActionListener(e -> addTask());
        completeButton.addActionListener(e -> markAsComplete());
        deleteButton.addActionListener(e -> deleteTask());

        setVisible(true);
    }

    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/todo_db", "root", "");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to DB: " + e.getMessage());
            System.exit(1);
        }
    }

    private void loadTasks() {
        model.setRowCount(0);
        String query = "SELECT id, task, status, created_at FROM todos ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("task"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toString()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
        }
    }

    private void addTask() {
        String task = taskField.getText().trim();
        if (task.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task.");
            return;
        }

        String sql = "INSERT INTO todos (task, status) VALUES (?, 'Pending')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task);
            ps.executeUpdate();
            taskField.setText("");
            loadTasks();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding task: " + e.getMessage());
        }
    }

    private void markAsComplete() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a task to mark as complete.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        try (PreparedStatement ps = conn.prepareStatement("UPDATE todos SET status='Completed' WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadTasks();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating task: " + e.getMessage());
        }
    }

    private void deleteTask() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a task to delete.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM todos WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadTasks();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting task: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TodoApp::new);
    }
}
