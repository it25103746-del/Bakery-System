package com.bakery;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BakeryStandaloneServer {
    private static final Path DATA_DIR = Path.of("data");
    private static final String ACCOUNT_FILE = "system-accounts.txt";
    private static final Map<String, UserSession> SESSIONS = new HashMap<>();
    private static final List<Module> MODULES = List.of(
            new Module("feedback", "Feedback, Ratings & Documentation", "IT25103745", "Hettiarachchi H. H. K. S.", "Create feedback", "Read reviews", "Update feedback", "Delete feedback", "feedback.txt", List.of("Customer Name", "Rating", "Comment")),
            new Module("admins", "Admin Management", "IT25103746", "Perera K. V. R.", "Add a new admin account", "Read report", "Update Stock", "Delete expired records", "admins.txt", List.of("Admin Name", "Email", "Role", "Stock Responsibility", "Expiry Status")),
            new Module("users", "User interface /User Authentication", "IT25103747", "Nawarathne S. N.", "Register users", "View profiles", "Modify details", "Remove inactive accounts", "users.txt", List.of("Full Name", "Email", "Phone", "Address", "Account Status", "Temporary Password")),
            new Module("products", "Product & Category Management", "IT25103748", "Samarakoon P. T. A. N.", "Create product", "Read listings", "Update prices/details", "Delete discontinued products", "products.txt", List.of("Product Name", "Category", "Price", "Details", "Availability", "Stock Amount")),
            new Module("orders", "Handles cart, checkout, order tracking", "IT25103749", "Siriwardhana R. D. T. D.", "Create orders", "Read history", "Update status", "Delete cancelled orders", "orders.txt", List.of("Customer Name", "Order Item", "Total Amount", "Order Status", "Payment Status")),
            new Module("custom-cakes", "Custom Cake Design & Booking", "IT25103750", "Vishara A. M. H.", "Create custom orders", "Read details", "Update designs", "Delete cancelled orders", "custom-cakes.txt", List.of("Customer Name", "Cake Size", "Design Details", "Pickup Date", "Booking Status"))
    );

    private record Module(String key, String title, String studentId, String studentName, String createText,
                          String readText, String updateText, String deleteText, String fileName, List<String> fields) {
        String publicTitle() {
            return switch (key) {
                case "feedback" -> "Customer Feedback";
                case "admins" -> "Admin Workspace";
                case "users" -> "Customer Accounts";
                case "products" -> "Products & Categories";
                case "orders" -> "Orders & Checkout";
                case "custom-cakes" -> "Custom Cake Bookings";
                default -> title;
            };
        }
    }
}