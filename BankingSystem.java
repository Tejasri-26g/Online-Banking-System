import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BankingSystem extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/banking_system";
    private static final String DB_USER = "root"; 
    private static final String DB_PASS = "Tejasri@2006";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private int currentUserId = -1;
    private String currentUserName = "";
    private double userSavingsGoal = 0.0; 

    private JProgressBar goalBar;
    private JLabel goalLabel;

    public BankingSystem() {
        setTitle("Secure Digital Bank");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createDashboardPanel(), "Dashboard");

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("ONLINE BANKING LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        
        JTextField loginId = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register New Account");

        loginBtn.setBackground(new Color(52, 152, 219));
        loginBtn.setForeground(Color.WHITE);
        registerBtn.setBackground(new Color(44, 62, 80));
        registerBtn.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("Name or ID:"), gbc);
        gbc.gridx = 1;
        panel.add(loginId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);
        
        gbc.gridy = 4;
        panel.add(registerBtn, gbc);

        loginBtn.addActionListener(e -> {
            if (authenticate(loginId.getText().trim(), new String(passField.getPassword()))) {
                promptForGoal(false);
                cardLayout.show(mainPanel, "Dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "Login Failed.");
            }
        });

        registerBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter Full Name:");
            if (name == null || name.trim().isEmpty()) return;
            String pass = JOptionPane.showInputDialog(this, "Set Password:");
            if (pass == null || pass.trim().isEmpty()) return;
            registerAccount(name.trim(), pass);
        });

        return panel;
    }

    private void promptForGoal(boolean isReset) {
        String msg = isReset ? "Congratulations! You hit your goal. Set your next target:" : "Set your savings goal for this session:";
        String input = JOptionPane.showInputDialog(this, msg, "Savings Goal", JOptionPane.QUESTION_MESSAGE);
        try {
            userSavingsGoal = (input == null || input.isEmpty()) ? 1000.0 : Double.parseDouble(input);
            updateGoalProgress();
        } catch (NumberFormatException e) {
            userSavingsGoal = 1000.0;
        }
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel goalPanel = new JPanel(new BorderLayout(5, 5));
        goalLabel = new JLabel("Loading Goal...");
        goalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        goalBar = new JProgressBar(0, 100);
        goalBar.setStringPainted(true);
        goalBar.setForeground(new Color(46, 204, 113));
        goalBar.setPreferredSize(new Dimension(300, 35));
        
        goalPanel.add(goalLabel, BorderLayout.NORTH);
        goalPanel.add(goalBar, BorderLayout.CENTER);
        goalPanel.setBorder(BorderFactory.createTitledBorder("Savings Tracker"));

        JPanel btnPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        String[] labels = {"Balance Inquiry", "Deposit Money", "Withdraw Money", "Fund Transfer", "Logout"};
        JButton[] btns = new JButton[5];

        for (int i = 0; i < 5; i++) {
            btns[i] = new JButton(labels[i]);
            btnPanel.add(btns[i]);
        }

        panel.add(goalPanel, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.CENTER);

        btns[0].addActionListener(e -> showBalance());
        btns[1].addActionListener(e -> performTransaction("deposit"));
        btns[2].addActionListener(e -> performTransaction("withdraw"));
        btns[3].addActionListener(e -> performTransfer());
        btns[4].addActionListener(e -> { cardLayout.show(mainPanel, "Login"); });

        return panel;
    }

    private void updateGoalProgress() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE acc_no = ?");
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double bal = rs.getDouble("balance");
                int progress = (int) ((bal / userSavingsGoal) * 100);
                goalBar.setValue(Math.min(progress, 100));
                goalLabel.setText("Hi " + currentUserName + " | Goal: $" + userSavingsGoal);
                
                if (bal >= userSavingsGoal && userSavingsGoal > 0) {
                    SwingUtilities.invokeLater(() -> promptForGoal(true));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating goal: " + ex.getMessage());
        }
    }

    private boolean authenticate(String identifier, String pass) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT * FROM accounts WHERE (acc_no = ? OR name = ?) AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            try { ps.setInt(1, Integer.parseInt(identifier)); } catch (Exception e) { ps.setInt(1, -1); }
            ps.setString(2, identifier);
            ps.setString(3, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("acc_no");
                currentUserName = rs.getString("name");
                return true;
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return false;
    }

    private void registerAccount(String name, String password) {
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO accounts (name, password, balance) VALUES (?, ?, 0.0)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name); ps.setString(2, password);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Success! ID: " + id));
                }
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void showBalance() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE acc_no = ?");
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) JOptionPane.showMessageDialog(this, "Balance: $" + rs.getDouble("balance"));
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void performTransaction(String type) {
        String input = JOptionPane.showInputDialog("Amount to " + type + ":");
        if (input == null || input.isEmpty()) return;
        double amount = Double.parseDouble(input);
        new Thread(() -> {
            synchronized (this) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    conn.setAutoCommit(false);
                    String sql = type.equals("deposit") ? "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?" : "UPDATE accounts SET balance = balance - ? WHERE acc_no = ? AND balance >= ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setDouble(1, amount); ps.setInt(2, currentUserId);
                    if (type.equals("withdraw")) ps.setDouble(3, amount);
                    
                    if (ps.executeUpdate() > 0) {
                        
                        PreparedStatement logPs = conn.prepareStatement("INSERT INTO transactions (acc_no, type, amount) VALUES (?, ?, ?)");
                        logPs.setInt(1, currentUserId);
                        logPs.setString(2, type);
                        logPs.setDouble(3, amount);
                        logPs.executeUpdate();
                        conn.commit();
                        SwingUtilities.invokeLater(() -> { updateGoalProgress(); JOptionPane.showMessageDialog(this, "Success!"); });
                    } else {
                        conn.rollback();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed."));
                    }
                } catch (SQLException ex) { 
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    private void performTransfer() {
        String targetStr = JOptionPane.showInputDialog("Recipient ID:");
        String amountStr = JOptionPane.showInputDialog("Amount:");
        if (targetStr == null || amountStr == null) return;
        int targetId = Integer.parseInt(targetStr);
        double amount = Double.parseDouble(amountStr);
        new Thread(() -> {
            synchronized (this) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    conn.setAutoCommit(false);
                    PreparedStatement ps1 = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE acc_no = ? AND balance >= ?");
                    ps1.setDouble(1, amount); ps1.setInt(2, currentUserId); ps1.setDouble(3, amount);
                    PreparedStatement ps2 = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE acc_no = ?");
                    ps2.setDouble(1, amount); ps2.setInt(2, targetId);
                    
                    if (ps1.executeUpdate() > 0 && ps2.executeUpdate() > 0) {
                        // Log for sender
                        PreparedStatement log1 = conn.prepareStatement("INSERT INTO transactions (acc_no, type, amount) VALUES (?, 'Transfer Out', ?)");
                        log1.setInt(1, currentUserId); log1.setDouble(2, amount); log1.executeUpdate();
                        // Log for receiver
                        PreparedStatement log2 = conn.prepareStatement("INSERT INTO transactions (acc_no, type, amount) VALUES (?, 'Transfer In', ?)");
                        log2.setInt(1, targetId); log2.setDouble(2, amount); log2.executeUpdate();

                        conn.commit();
                        SwingUtilities.invokeLater(() -> { updateGoalProgress(); JOptionPane.showMessageDialog(this, "Transfer Sent!"); });
                    } else {
                        conn.rollback();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Transfer Failed."));
                    }
                } catch (SQLException ex) { 
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankingSystem().setVisible(true));
    }
}