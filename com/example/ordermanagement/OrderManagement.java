package com.example.ordermanagement;
import java.sql.*;
import java.util.Scanner;

public class OrderManagement {

    static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    static final String USER = "root";
    static final String PASSWORD = "YOUR_PASSWORD";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Database Connected Successfully!");

            while (true) {
                System.out.println("\n===== ORDER MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert Orders");
                System.out.println("2. Search Orders by Status");
                System.out.println("3. Increase Order Quantity");
                System.out.println("4. Update Order Status");
                System.out.println("5. Total Value and Delete Cancelled Order");
                System.out.println("6. Exit");

                System.out.print("Enter choice: ");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1:
                        insertOrders(con);
                        break;

                    case 2:
                        searchOrders(con);
                        break;

                    case 3:
                        increaseQuantity(con);
                        break;

                    case 4:
                        updateStatus(con);
                        break;

                    case 5:
                        totalAndDelete(con);
                        break;

                    case 6:
                        con.close();
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // A. Insert at least three orders
    static void insertOrders(Connection con) throws SQLException {

        String sql = "INSERT INTO Orders VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            int[] ids = {101, 102, 103};
            String[] customers = {"Rahul", "Aman", "Priya"};
            String[] products = {"Laptop", "Mobile", "Headphones"};
            int[] quantities = {3, 2, 5};
            double[] prices = {50000, 20000, 1500};
            String[] statuses = {"Pending", "Shipped", "Cancelled"};

            for (int i = 0; i < 3; i++) {

                ps.setInt(1, ids[i]);
                ps.setString(2, customers[i]);
                ps.setString(3, products[i]);
                ps.setInt(4, quantities[i]);
                ps.setDouble(5, prices[i]);
                ps.setString(6, statuses[i]);

                try {
                    ps.executeUpdate();
                    System.out.println("Order inserted: " + ids[i]);

                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Duplicate Order ID: " + ids[i]);
                }
            }
        }
    }

    // B. Search orders by status and quantity > 2
    static void searchOrders(Connection con) throws SQLException {

        System.out.print("Enter order status: ");
        String status = sc.next();

        String sql = "SELECT * FROM Orders WHERE status = ? "
                   + "AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    displayOrder(rs);
                }
            }
        }
    }

    // C. Increase existing order quantity
    static void increaseQuantity(Connection con) throws SQLException {

        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();

        System.out.print("Enter additional quantity: ");
        int qty = sc.nextInt();

        String sql = "UPDATE Orders SET quantity = quantity + ? "
                   + "WHERE order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Quantity updated successfully!");
            else
                System.out.println("Order does not exist.");
        }
    }

    // D. Update status only if valid
    static void updateStatus(Connection con) throws SQLException {

        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();

        System.out.print("Enter new status: ");
        String status = sc.next();

        if (!status.equalsIgnoreCase("Pending") &&
            !status.equalsIgnoreCase("Shipped") &&
            !status.equalsIgnoreCase("Delivered")) {

            System.out.println("Invalid status!");
            return;
        }

        String check = "SELECT * FROM Orders WHERE order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(check)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String sql = "UPDATE Orders SET status = ? "
                               + "WHERE order_id = ?";

                    try (PreparedStatement update = con.prepareStatement(sql)) {

                        update.setString(1, status.substring(0, 1).toUpperCase()
                                + status.substring(1).toLowerCase());
                        update.setInt(2, id);
                        update.executeUpdate();
                    }

                    System.out.println("Updated Order Details:");

                    try (PreparedStatement show = con.prepareStatement(check)) {
                        show.setInt(1, id);

                        try (ResultSet result = show.executeQuery()) {
                            if (result.next())
                                displayOrder(result);
                        }
                    }

                } else {
                    System.out.println("Order not found!");
                }
            }
        }
    }

    // E. Calculate total value and delete cancelled order
    static void totalAndDelete(Connection con) throws SQLException {

        String total = "SELECT SUM(quantity * unit_price) AS total "
                     + "FROM Orders";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(total)) {

            if (rs.next())
                System.out.println("Total Order Value: " + rs.getDouble("total"));
        }

        System.out.print("Enter Order ID to delete: ");
        int id = sc.nextInt();

        String check = "SELECT status FROM Orders WHERE order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(check)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    if (rs.getString("status").equalsIgnoreCase("Cancelled")) {

                        String sql = "DELETE FROM Orders WHERE order_id = ?";

                        try (PreparedStatement del = con.prepareStatement(sql)) {
                            del.setInt(1, id);
                            del.executeUpdate();
                        }

                        System.out.println("Cancelled order deleted!");

                    } else {
                        System.out.println("Cannot delete: Order is not Cancelled.");
                    }

                } else {
                    System.out.println("Order not found!");
                }
            }
        }
    }

    // Display order details
    static void displayOrder(ResultSet rs) throws SQLException {

        System.out.println(
            rs.getInt("order_id") + " | " +
            rs.getString("customer_name") + " | " +
            rs.getString("product_name") + " | " +
            rs.getInt("quantity") + " | " +
            rs.getDouble("unit_price") + " | " +
            rs.getString("status")
        );
    }
}
