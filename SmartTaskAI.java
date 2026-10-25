import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartTaskAI {

    private static final String URL =
            "jdbc:mysql://localhost:3306/smarttask_db";
    private static final String USER = "root";

    // Set this in PowerShell with: setx DB_PASSWORD "your_mysql_password"
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_MODEL = "gpt-4.1-mini";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private static DefaultTableModel model;
    private static JTable table;
    private static JLabel totalLabel;
    private static JLabel pendingLabel;
    private static JLabel completedLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmartTaskAI::createGUI);
    }

    private static void createGUI() {

        JFrame frame = new JFrame("SmartTask AI - Personal Task Manager");
        frame.setSize(1050, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "SmartTask AI - Personal Task Manager",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));

        // Dashboard
        JPanel dashboard = new JPanel(new GridLayout(1, 3, 12, 12));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        totalLabel = new JLabel("Total Tasks: 0", SwingConstants.CENTER);
        pendingLabel = new JLabel("Pending Tasks: 0", SwingConstants.CENTER);
        completedLabel = new JLabel("Completed Tasks: 0", SwingConstants.CENTER);

        Font dashboardFont = new Font("Arial", Font.BOLD, 16);
        totalLabel.setFont(dashboardFont);
        pendingLabel.setFont(dashboardFont);
        completedLabel.setFont(dashboardFont);

        dashboard.add(totalLabel);
        dashboard.add(pendingLabel);
        dashboard.add(completedLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.NORTH);
        topPanel.add(dashboard, BorderLayout.CENTER);

        // Task form
        JTextField nameField = new JTextField(18);
        JTextField descriptionField = new JTextField(18);
        JTextField deadlineField = new JTextField(18);

        String[] priorities = {"High", "Medium", "Low"};
        JComboBox<String> priorityBox = new JComboBox<>(priorities);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Task Details"));

        formPanel.add(new JLabel("Task Name:"));
        formPanel.add(nameField);

        formPanel.add(new JLabel("Description:"));
        formPanel.add(descriptionField);

        formPanel.add(new JLabel("Deadline (DD-MM-YYYY):"));
        formPanel.add(deadlineField);

        formPanel.add(new JLabel("Priority:"));
        formPanel.add(priorityBox);

        JButton addButton = new JButton("Add Task");
        JButton clearButton = new JButton("Clear");

        formPanel.add(addButton);
        formPanel.add(clearButton);

        // Search
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Task:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);

        // Table
        String[] columns = {
                "ID", "Task Name", "Description",
                "Deadline", "Priority", "Status"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("My Tasks"));

        // Action buttons
        JButton completeButton = new JButton("Mark Completed");
        JButton updateButton = new JButton("Update Task");
        JButton deleteButton = new JButton("Delete Task");
        JButton aiScheduleButton = new JButton("AI Daily Schedule");
        JButton aiPriorityButton = new JButton("AI Prioritize Tasks");

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(completeButton);
        bottomPanel.add(updateButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(aiScheduleButton);
        bottomPanel.add(aiPriorityButton);

        aiScheduleButton.addActionListener(e -> runAiAssistant(frame, true));
        aiPriorityButton.addActionListener(e -> runAiAssistant(frame, false));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(formPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Add task
        addButton.addActionListener(e -> {

            String name = nameField.getText().trim();
            String description = descriptionField.getText().trim();
            String deadlineText = deadlineField.getText().trim();
            String priority = (String) priorityBox.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter a task name.");
                return;
            }

            java.sql.Date sqlDeadline = null;

            if (!deadlineText.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(
                            deadlineText, DATE_FORMAT);
                    sqlDeadline = java.sql.Date.valueOf(date);

                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Enter a valid deadline as DD-MM-YYYY.");
                    return;
                }
            }

            String sql = "INSERT INTO tasks " +
                    "(task_name, description, deadline, priority, status) " +
                    "VALUES (?, ?, ?, ?, 'Pending')";

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, description);

                if (sqlDeadline == null) {
                    ps.setNull(3, Types.DATE);
                } else {
                    ps.setDate(3, sqlDeadline);
                }

                ps.setString(4, priority);
                ps.executeUpdate();

                clearFields(nameField, descriptionField,
                        deadlineField, priorityBox);

                loadTasks("");
                updateDashboard();

                JOptionPane.showMessageDialog(frame,
                        "Task added successfully!");

            } catch (SQLException ex) {
                showDatabaseError(frame, ex);
            }
        });

        // Clear form
        clearButton.addActionListener(e ->
                clearFields(nameField, descriptionField,
                        deadlineField, priorityBox));

        // Mark completed
        completeButton.addActionListener(e -> {

            int row = getSelectedModelRow(frame);
            if (row == -1) {
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            String sql =
                    "UPDATE tasks SET status = 'Completed' WHERE id = ?";

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);

                if (ps.executeUpdate() > 0) {
                    loadTasks("");
                    updateDashboard();

                    JOptionPane.showMessageDialog(frame,
                            "Task marked as completed!");
                }

            } catch (SQLException ex) {
                showDatabaseError(frame, ex);
            }
        });

        // Update task
        updateButton.addActionListener(e -> {

            int row = getSelectedModelRow(frame);
            if (row == -1) {
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            JTextField editName = new JTextField(
                    String.valueOf(model.getValueAt(row, 1)));
            JTextField editDescription = new JTextField(
                    String.valueOf(model.getValueAt(row, 2)));
            JTextField editDeadline = new JTextField(
                    String.valueOf(model.getValueAt(row, 3)));

            JComboBox<String> editPriority =
                    new JComboBox<>(priorities);
            editPriority.setSelectedItem(model.getValueAt(row, 4));

            JPanel editPanel = new JPanel(new GridLayout(4, 2, 5, 5));
            editPanel.add(new JLabel("Task Name:"));
            editPanel.add(editName);
            editPanel.add(new JLabel("Description:"));
            editPanel.add(editDescription);
            editPanel.add(new JLabel("Deadline (DD-MM-YYYY):"));
            editPanel.add(editDeadline);
            editPanel.add(new JLabel("Priority:"));
            editPanel.add(editPriority);

            int result = JOptionPane.showConfirmDialog(
                    frame, editPanel, "Update Task",
                    JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String name = editName.getText().trim();
            String description = editDescription.getText().trim();
            String deadlineText = editDeadline.getText().trim();
            String priority = (String) editPriority.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Task name cannot be empty.");
                return;
            }

            java.sql.Date sqlDeadline = null;

            if (!deadlineText.isEmpty()) {
    try {
        LocalDate date = LocalDate.parse(
                deadlineText,
                DATE_FORMAT
        );

        sqlDeadline = java.sql.Date.valueOf(date);

    } catch (DateTimeParseException ex) {
        JOptionPane.showMessageDialog(
                frame,
                "Enter a valid deadline as DD-MM-YYYY."
        );
        return;
    }
}            // Update task in database
            String sql = "UPDATE tasks SET task_name=?, description=?, "
                    + "deadline=?, priority=? WHERE id=?";

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, description);

                if (sqlDeadline == null) {
                    ps.setNull(3, Types.DATE);
                } else {
                    ps.setDate(3, sqlDeadline);
                }

                ps.setString(4, priority);
                ps.setInt(5, id);

                if (ps.executeUpdate() > 0) {
                    loadTasks("");
                    updateDashboard();

                    JOptionPane.showMessageDialog(
                            frame,
                            "Task updated successfully!"
                    );
                }

            } catch (SQLException ex) {
                showDatabaseError(frame, ex);
            }
        });

        // Delete Task
        deleteButton.addActionListener(e -> {

            int row = getSelectedModelRow(frame);

            if (row == -1) {
                return;
            }

            int id = (int) model.getValueAt(row, 0);

            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to delete this task?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            String sql = "DELETE FROM tasks WHERE id=?";

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);

                if (ps.executeUpdate() > 0) {
                    loadTasks("");
                    updateDashboard();

                    JOptionPane.showMessageDialog(
                            frame,
                            "Task deleted successfully!"
                    );
                }

            } catch (SQLException ex) {
                showDatabaseError(frame, ex);
            }
        });

        // Search Task
        searchButton.addActionListener(e ->
                loadTasks(searchField.getText().trim())
        );

        // Show All Tasks
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadTasks("");
        });

        // Load existing tasks and update dashboard
        loadTasks("");
        updateDashboard();

        frame.setVisible(true);
    }

    // Establish MySQL connection
    private static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("MySQL password is not set. Set DB_PASSWORD in your environment, then restart VS Code.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // AI schedule and priority features use the OpenAI Responses API.
    private static void runAiAssistant(JFrame frame, boolean makeSchedule) {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            JOptionPane.showMessageDialog(frame,
                    "OPENAI_API_KEY is not set. Set it in PowerShell, then completely restart VS Code.",
                    "OpenAI Setup", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String tasks;
        try {
            tasks = getPendingTasksForAI();
        } catch (SQLException ex) {
            showDatabaseError(frame, ex);
            return;
        }

        if (tasks.isBlank()) {
            JOptionPane.showMessageDialog(frame,
                    "There are no pending tasks for the AI to analyze.");
            return;
        }

        String instruction = makeSchedule
                ? "You are a practical student productivity assistant. Based only on the pending tasks below, create a realistic daily schedule. Consider deadlines and priority, put urgent/high-priority work first, include short breaks, and clearly state any assumptions. Do not invent task deadlines. Use readable headings and bullet points."
                : "You are a practical student productivity assistant. Review the pending tasks below and recommend a priority order. For each task, give High, Medium, or Low priority, explain briefly why, and state the next task to work on first. Consider deadlines and existing priority. Do not invent deadlines. Use readable headings and bullet points.";

        String requestBody = "{\"model\":\"" + OPENAI_MODEL
                + "\",\"instructions\":\"" + jsonEscape(instruction)
                + "\",\"input\":\"" + jsonEscape("Pending tasks:\n" + tasks)
                + "\",\"max_output_tokens\":900}";

        JDialog loading = new JDialog(frame, "SmartTask AI", false);
        loading.add(new JLabel("AI is analyzing your tasks... Please wait.", SwingConstants.CENTER));
        loading.setSize(360, 100);
        loading.setLocationRelativeTo(frame);
        loading.setVisible(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/responses"))
                        .timeout(Duration.ofSeconds(90))
                        .header("Authorization", "Bearer " + OPENAI_API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("OpenAI API returned HTTP "
                            + response.statusCode() + ". Check API billing, key, and model access.");
                }
                String answer = extractFirstText(response.body());
                if (answer.isBlank()) {
                    throw new IllegalStateException("The AI response was empty. Please try again.");
                }
                return answer;
            }

            @Override
            protected void done() {
                loading.dispose();
                try {
                    JTextArea output = new JTextArea(get(), 22, 62);
                    output.setEditable(false);
                    output.setLineWrap(true);
                    output.setWrapStyleWord(true);
                    output.setCaretPosition(0);
                    JScrollPane scroll = new JScrollPane(output);
                    String title = makeSchedule ? "AI Daily Schedule" : "AI Task Priorities";
                    JOptionPane.showMessageDialog(frame, scroll, title,
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(frame,
                            "Could not get AI suggestions:\n" + cause.getMessage(),
                            "AI Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String getPendingTasksForAI() throws SQLException {
        StringBuilder tasks = new StringBuilder();
        String sql = "SELECT task_name, description, deadline, priority, status "
                + "FROM tasks WHERE status = 'Pending' ORDER BY deadline IS NULL, deadline ASC";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tasks.append("- Task: ").append(nullToEmpty(rs.getString("task_name")))
                        .append(" | Description: ").append(nullToEmpty(rs.getString("description")))
                        .append(" | Deadline: ");
                Date deadline = rs.getDate("deadline");
                tasks.append(deadline == null ? "Not set" : deadline.toLocalDate().format(DATE_FORMAT))
                        .append(" | Current priority: ").append(nullToEmpty(rs.getString("priority")))
                        .append(" | Status: ").append(nullToEmpty(rs.getString("status"))).append('\n');
            }
        }
        return tasks.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Extract and unescape the first JSON string value whose key is "text".
    private static String extractFirstText(String json) {
        Matcher matcher = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
        if (!matcher.find()) return "";
        String raw = matcher.group(1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(++i);
                switch (next) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'u' -> {
                        if (i + 4 < raw.length()) {
                            result.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                    }
                    default -> result.append(next);
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    // Get selected task row
    private static int getSelectedModelRow(JFrame frame) {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Please select a task first."
            );
            return -1;
        }

        return table.convertRowIndexToModel(viewRow);
    }

    // Load tasks from MySQL
    private static void loadTasks(String searchText) {

        model.setRowCount(0);

        String sql = "SELECT id, task_name, description, deadline, "
                + "priority, status FROM tasks "
                + "WHERE task_name LIKE ? ORDER BY id DESC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + searchText + "%");

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    java.sql.Date date = rs.getDate("deadline");

                    String formattedDate = date == null
                            ? ""
                            : date.toLocalDate().format(DATE_FORMAT);

                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("task_name"),
                            rs.getString("description"),
                            formattedDate,
                            rs.getString("priority"),
                            rs.getString("status")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not load tasks:\n" + ex.getMessage()
            );
        }
    }

    // Count tasks from database
    private static int getTaskCount(String condition)
            throws SQLException {

        String sql = "SELECT COUNT(*) FROM tasks " + condition;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;
        }
    }

    // Update dashboard counters
    private static void updateDashboard() {

        try {
            int total = getTaskCount("");
            int pending = getTaskCount(
                    "WHERE status = 'Pending'"
            );
            int completed = getTaskCount(
                    "WHERE status = 'Completed'"
            );

            totalLabel.setText("Total Tasks: " + total);
            pendingLabel.setText("Pending Tasks: " + pending);
            completedLabel.setText("Completed Tasks: " + completed);

        } catch (SQLException ex) {
            showDatabaseError(null, ex);
        }
    }

    // Clear the task form
    private static void clearFields(
            JTextField nameField,
            JTextField descriptionField,
            JTextField deadlineField,
            JComboBox<String> priorityBox) {

        nameField.setText("");
        descriptionField.setText("");
        deadlineField.setText("");
        priorityBox.setSelectedIndex(0);
    }

    // Display database errors
    private static void showDatabaseError(
            JFrame frame,
            SQLException ex) {

        JOptionPane.showMessageDialog(
                frame,
                "Database operation failed:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}