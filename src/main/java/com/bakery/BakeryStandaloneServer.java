package com.bakery;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", BakeryStandaloneServer::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("Bakery app running at http://localhost:" + port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ("GET".equals(method) && path.startsWith("/public/")) {
            Path file = Path.of(".").resolve(path.substring(1)).normalize();
            if (Files.exists(file) && !Files.isDirectory(file)) {
                String mime = "application/octet-stream";
                if (file.toString().endsWith(".jpg")) mime = "image/jpeg";
                if (file.toString().endsWith(".png")) mime = "image/png";
                if (file.toString().endsWith(".css")) mime = "text/css";
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, Files.size(file));
                Files.copy(file, exchange.getResponseBody());
                exchange.getResponseBody().close();
                return;
            }
        }

        if ("GET".equals(method) && "/".equals(path)) {
            html(exchange, storefrontPage(exchange, "home"));
            return;
        }

        if ("GET".equals(method) && "/menu".equals(path)) {
            html(exchange, storefrontPage(exchange, "menu"));
            return;
        }

        if ("GET".equals(method) && "/about".equals(path)) {
            html(exchange, storefrontPage(exchange, "about"));
            return;
        }

        if ("GET".equals(method) && "/contact".equals(path)) {
            html(exchange, storefrontPage(exchange, "contact"));
            return;
        }

        if ("GET".equals(method) && "/login".equals(path)) {
            String message = "";
            if ("created=1".equals(query)) {
                message = "Customer account created. Please login.";
            } else if ("denied=1".equals(query)) {
                message = "Please login with the correct role.";
            }
            html(exchange, loginPage(message));
            return;
        }
        if ("GET".equals(method) && "/create-account".equals(path)) {
            html(exchange, accountPage(""));
            return;
        }
        if ("POST".equals(method) && "/create-account".equals(path)) {
            Map<String, String> form = parseForm(exchange);
            String password = form.getOrDefault("password", "");
            String confirmPassword = form.getOrDefault("confirmPassword", "");
            String email = form.getOrDefault("email", "");
            if (!isValidEmail(email)) {
                html(exchange, accountPage("Please enter a valid email address."));
                return;
            }
            if (password.length() < 6) {
                html(exchange, accountPage("Password must be at least 6 characters."));
                return;
            }
            if (!password.equals(confirmPassword)) {
                html(exchange, accountPage("Password and confirm password must match."));
                return;
            }
            createAccount(form);
            redirect(exchange, "/login?created=1");
            return;
        }
        if ("POST".equals(method) && "/login".equals(path)) {
            Map<String, String> form = parseForm(exchange);
            String role = authenticate(form);
            if (role != null) {
                String token = UUID.randomUUID().toString();
                SESSIONS.put(token, new UserSession(role, form.getOrDefault("email", ""), loginName(role, form.getOrDefault("email", ""))));
                exchange.getResponseHeaders().add("Set-Cookie", "bakery_session=" + token + "; Path=/; HttpOnly");
                redirect(exchange, "/" + role + "/dashboard");
            } else {
                html(exchange, loginPage("Wrong role, email, or password."));
            }
            return;
        }
        if ("GET".equals(method) && "/logout".equals(path)) {
            String token = sessionToken(exchange);
            if (token != null) {
                SESSIONS.remove(token);
            }
            exchange.getResponseHeaders().add("Set-Cookie", "bakery_session=; Path=/; Max-Age=0");
            redirect(exchange, "/");
            return;
        }
        if ("POST".equals(method) && "/add-to-cart".equals(path)) {
            UserSession session = currentSession(exchange);
            if (session == null || !"customer".equals(session.role())) {
                redirect(exchange, "/login");
                return;
            }
            Map<String, String> form = parseForm(exchange);
            String productId = form.getOrDefault("productId", "");
            int quantity = parsePositiveInt(form.getOrDefault("quantity", "1"));
            Record productRecord = findRecord(findModule("products"), productId);
            if (productRecord != null) {
                productRecord = new Record(productRecord.id(), normalizedValues(findModule("products"), productRecord.values()));
            }
            if (productRecord == null || stockAmount(productRecord) < quantity) {
                redirect(exchange, "/menu?stock=0");
                return;
            }
            String product = productRecord.values().get(0);
            int remainingStock = stockAmount(productRecord) - quantity;
            updateProductStock(productId, remainingStock);
            String price = productRecord.values().get(2);
            String orderedItems = orderLine(product, price, quantity);

            // Just create an order directly for simplicity in this demo app
            Module orderModule = findModule("orders");
            List<Record> orders = readRecords(orderModule);
            String id = "ORDERS-" + Instant.now().toEpochMilli();
            orders.add(new Record(id, List.of(session.name(), orderedItems, calculateOrderTotal(orderedItems), "Processing", "Pending Approval")));
            writeRecords(orderModule, orders);

            redirect(exchange, "/?ordered=1");
            return;
        }

        if ("GET".equals(method) && "/admin/dashboard".equals(path)) {
            if (!requireRole(exchange, "admin")) return;
            html(exchange, adminDashboard());
            return;
        }
        if ("GET".equals(method) && "/customer/dashboard".equals(path)) {
            if (!requireRole(exchange, "customer")) return;
            html(exchange, customerDashboard());
            return;
        }

        if (path.startsWith("/admin/modules/") || path.startsWith("/customer/modules/")) {
            String role = path.startsWith("/admin/") ? "admin" : "customer";
            if (!requireRole(exchange, role)) return;
            String[] parts = path.substring(1).split("/");
            Module module = findModule(parts.length > 2 ? parts[2] : "");
            if (module == null) {
                notFound(exchange);
                return;
            }
            if ("customer".equals(role) && !module.customerAllowed()) {
                notFound(exchange);
                return;
            }

            if ("GET".equals(method) && parts.length == 3) {
                html(exchange, recordsPage(exchange, role, module));
                return;
            }
            if ("GET".equals(method) && parts.length == 4 && "new".equals(parts[3])) {
                if ("customer".equals(role) && !module.customerCanAdd()) {
                    notFound(exchange);
                    return;
                }
                html(exchange, formPage(exchange, role, module, null));
                return;
            }
            if ("GET".equals(method) && parts.length == 5 && "edit".equals(parts[4])) {
                if ("customer".equals(role) && !("orders".equals(module.key()) || "custom-cakes".equals(module.key()))) {
                    notFound(exchange);
                    return;
                }
                Record record = findRecord(module, parts[3]);
                if ("customer".equals(role) && !ownsRecord(exchange, record)) {
                    notFound(exchange);
                    return;
                }
                html(exchange, formPage(exchange, role, module, record));
                return;
            }
            if ("POST".equals(method) && parts.length == 3) {
                Map<String, String> form = parseForm(exchange);
                saveRecord(exchange, role, module, form);
                redirect(exchange, "/" + role + "/modules/" + module.key());
                return;
            }
            if ("POST".equals(method) && parts.length == 5 && "delete".equals(parts[4])) {
                deleteRecord(exchange, role, module, parts[3]);
                redirect(exchange, "/" + role + "/modules/" + module.key());
                return;
            }
        }

        notFound(exchange);
    }

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