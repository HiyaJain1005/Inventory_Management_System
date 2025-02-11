import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class InventoryManagementFinal {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField nameField, quantityField, priceField, searchField;
    private Connection connection;
    private JFrame loginFrame;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                InventoryManagementFinal window = new InventoryManagementFinal();
                window.loginFrame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public InventoryManagementFinal() {
        connectToDatabase();
        initializeLogin();
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/inventoryfinal", "root", "root");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeLogin() {
        loginFrame = new JFrame("Login");
        loginFrame.setBounds(100, 100, 400, 300); // Adjusted size
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST; // Align left

        // Username Label and TextField
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Bigger font
        loginPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 16)); // Bigger font
        loginPanel.add(usernameField, gbc);

        // Password Label and PasswordField
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Bigger font
        loginPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16)); // Bigger font
        loginPanel.add(passwordField, gbc);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span across both columns
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16)); // Bigger font
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateUser(username, password)) {
                loginFrame.dispose();
                showMainFrame();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials!");
            }
        });
        loginPanel.add(loginButton, gbc);

        // Register Button
        gbc.gridy = 3;
        JButton registerButton = new JButton("Register");
        registerButton.setFont(new Font("Arial", Font.BOLD, 16)); // Bigger font
        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            registerUser(username, password);
        });
        loginPanel.add(registerButton, gbc);

        // Add panel to frame
        loginFrame.add(loginPanel);
        loginFrame.setVisible(true);
    }

    private boolean authenticateUser(String username, String password) {
        try {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            return rs.next(); // returns true if user exists
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void registerUser(String username, String password) {
        try {
            PreparedStatement pst = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            pst.setString(1, username);
            pst.setString(2, password);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(loginFrame, "User registered successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(loginFrame, "Error registering user: " + e.getMessage());
        }
    }

    private void showMainFrame() {
        frame = new JFrame("Inventory Management");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Quantity", "Price"}, 0);
        table = new JTable(tableModel);
        frame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        panel.setLayout(new GridLayout(4, 4)); // Adjusted to add reorder button

        nameField = new JTextField();
        quantityField = new JTextField();
        priceField = new JTextField();
        searchField = new JTextField();

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantityField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);

        JButton addButton = new JButton("Add Product");
        addButton.addActionListener(new AddProductAction());
        panel.add(addButton);

        JButton updateButton = new JButton("Update Product");
        updateButton.addActionListener(new UpdateProductAction());
        panel.add(updateButton);
        JButton deleteButton = new JButton("Delete Product");
        deleteButton.addActionListener(new DeleteProductAction());
        panel.add(deleteButton);

        JButton reorderButton = new JButton("Reorder Products");
        reorderButton.addActionListener(e -> reorderProductsInGUI());
        panel.add(reorderButton);

        panel.add(new JLabel("Search:"));
        panel.add(searchField);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new SearchProductAction());
        panel.add(searchButton);

        loadData();
        frame.setVisible(true);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM products");
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getDouble("price")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class AddProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String name = nameField.getText();
            int quantity;
            double price;
            try {
                quantity = Integer.parseInt(quantityField.getText());
                price = Double.parseDouble(priceField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numeric values for quantity and price.");
                return;
            }

            try {
                PreparedStatement pst = connection.prepareStatement("INSERT INTO products (name, quantity, price) VALUES (?, ?, ?)");
                pst.setString(1, name);
                pst.setInt(2, quantity);
                pst.setDouble(3, price);
                pst.executeUpdate();
                loadData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error adding product: " + ex.getMessage());
            }

            clearFields();
        }
    }

    private class UpdateProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                String name = nameField.getText();
                int quantity;
                double price;
                try {
                    quantity = Integer.parseInt(quantityField.getText());
                    price = Double.parseDouble(priceField.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Please enter valid numeric values for quantity and price.");
                    return;
                }

                try {
                    PreparedStatement pst = connection.prepareStatement("UPDATE products SET name = ?, quantity = ?, price = ? WHERE id = ?");
                    pst.setString(1, name);
                    pst.setInt(2, quantity);
                    pst.setDouble(3, price);
                    pst.setInt(4, id);
                    pst.executeUpdate();
                    loadData();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error updating product: " + ex.getMessage());
                }

                clearFields();
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a product to update.");
            }
        }
    }

    private class DeleteProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int id = (int) tableModel.getValueAt(selectedRow, 0);
                try {
                    PreparedStatement pst = connection.prepareStatement("DELETE FROM products WHERE id = ?");
                    pst.setInt(1, id);
                    pst.executeUpdate();
                    renumberProductIDs();
                    loadData(); // Refresh the table
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error deleting product: " + ex.getMessage());
                }

                clearFields();
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a product to delete.");
            }
        }
    }

    private void renumberProductIDs() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY id");

            int newId = 1;
            while (rs.next()) {
                int currentId = rs.getInt("id");
                if (currentId != newId) {
                    PreparedStatement pst = connection.prepareStatement("UPDATE products SET id = ? WHERE id = ?");
                    pst.setInt(1, newId);
                    pst.setInt(2, currentId);
                    pst.executeUpdate();
                }
                newId++;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error renumbering product IDs: " + e.getMessage());
        }
    }

    private class SearchProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String searchTerm = searchField.getText();
            tableModel.setRowCount(0); // Clear the table
            try {
                PreparedStatement pst = connection.prepareStatement("SELECT * FROM products WHERE name LIKE ?");
                pst.setString(1, "%" + searchTerm + "%");
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    tableModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getDouble("price")});
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error searching for product: " + ex.getMessage());
            }
        }
    }

    private void reorderProductsInGUI() {
        loadData(); // Reload data
    }

    private void clearFields() {
        nameField.setText("");
        quantityField.setText("");
        priceField.setText("");
    }
}
