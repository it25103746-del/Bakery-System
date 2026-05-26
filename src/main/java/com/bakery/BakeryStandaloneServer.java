package com.bakery;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private static String adminDashboard() {
        return appLayout("admin", "Admin Dashboard", """
                <section class="hero-card"><p>Sweet Crumbs Bakery</p><h1>Admin Dashboard</h1><span>Manage bakery operations from one clean workspace.</span></section>
                <section class="tile-grid">%s</section>
                """.formatted(tiles("admin", MODULES)));
    }

    private static String customerDashboard() {
        List<Module> customerModules = MODULES.stream().filter(Module::customerAllowed).toList();
        return appLayout("customer", "Customer Dashboard", """
                <section class="hero-card"><p>Sweet Crumbs Bakery</p><h1>Customer Dashboard</h1><span>Browse bakery items, place orders, book custom cakes, and share feedback.</span></section>
                <section class="tile-grid">%s</section>
                """.formatted(tiles("customer", customerModules)));
    }

    private static String tiles(String role, List<Module> modules) {
        StringBuilder cards = new StringBuilder();
        for (Module module : modules) {
            cards.append("""
                    <a class="tile" href="/%s/modules/%s">
                        <div class="tile-icon">%s</div>
                        <div class="tile-content">
                            <strong>%s</strong>
                            <small>%s</small>
                        </div>
                    </a>
                    """.formatted(role, module.key(), module.iconEmoji(), escape(module.publicTitle()), escape(module.description())));
        }
        return cards.toString();
    }

    private static String loginPage(String error) {
        String notice = error.isBlank() ? "" : "<p class=\"error\">" + escape(error) + "</p>";
        return authLayout("Login", """
                <section class="auth-card">
                <div class="brand"><span>Sweet Crumbs</span><h1>Bakery Portal</h1><p>Welcome back</p></div>
                %s
                <form method="post" action="/login">
                <label>Role<select name="role" required><option value="admin">Admin</option><option value="customer">Customer</option></select></label>
                <label>Email<input name="email" type="email" placeholder="Email address" required></label>
                <label>Password<input name="password" type="password" placeholder="Password" required></label>
                <button class="button" type="submit">Login</button>
                </form>
                <a class="text-link" href="/create-account">Create customer account</a>
                </section>
                """.formatted(notice));
    }

    private static String accountPage(String error) {
        String notice = error.isBlank() ? "" : "<p class=\"error\">" + escape(error) + "</p>";
        return authLayout("Create Account", """
                <section class="auth-card">
                <div class="brand"><span>Customer Registration</span><h1>Create Account</h1><p>Join Sweet Crumbs Bakery</p></div>
                %s
                <form method="post" action="/create-account">
                <label>Name<input name="name" placeholder="Full name" required></label>
                <label>Email<input name="email" type="email" placeholder="name@gmail.com" required></label>
                <label>Phone Number<input name="phone" placeholder="Phone number" required></label>
                <label>Address<input name="address" placeholder="Address" required></label>
                <label>Password<input name="password" type="password" placeholder="Password" required></label>
                <label>Confirm Password<input name="confirmPassword" type="password" placeholder="Confirm password" required></label>
                <button class="button" type="submit">Create Account</button>
                </form>
                <a class="text-link" href="/login">Back to login</a>
                </section>
                """.formatted(notice));
    }

    private static String authLayout(String title, String body) {
        return """
                <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                <title>%s</title><style>
                :root { --primary: #c25c31; --primary-dark: #91401f; --bg-gradient-start: rgba(28,20,16,.9); --bg-gradient-end: rgba(65,34,20,.8); --card-bg: rgba(255,250,244,.98); --text-dark: #27211d; --text-muted: #6d625a; }
                body{min-height:100vh;margin:0;display:grid;place-items:center;padding:18px;background:linear-gradient(135deg,var(--bg-gradient-start),var(--bg-gradient-end)),url('https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1600&q=80') center/cover;font-family:'Inter',sans-serif;color:var(--text-dark)}.auth-card{width:min(420px,100%%);padding:32px;background:var(--card-bg);border:1px solid rgba(255,255,255,0.4);border-radius:16px;box-shadow:0 24px 64px rgba(0,0,0,0.4);backdrop-filter:blur(16px);transform:translateY(20px);animation:slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;}
                @keyframes slideUp { to { transform:translateY(0); } }
                .brand span{color:var(--primary);font-size:12px;font-weight:800;letter-spacing:1px;text-transform:uppercase}.brand h1{margin:8px 0 6px;font-size:32px;line-height:1.1;letter-spacing:-0.5px}.brand p{margin:0 0 24px;color:var(--text-muted);font-size:14px;font-weight:500}form{display:grid;gap:16px}label{display:grid;gap:6px;font-size:13px;font-weight:600;color:var(--text-dark)}input,select{width:100%%;box-sizing:border-box;min-height:44px;padding:10px 14px;border:1px solid #e0d0c4;border-radius:8px;font:inherit;font-size:14px;background:#fff;color:var(--text-dark);transition:all 0.2s ease}input:focus,select:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 4px rgba(194,92,49,0.15)}.button{min-height:46px;border:0;border-radius:8px;background:var(--primary);color:white;font-size:14px;font-weight:700;cursor:pointer;transition:all 0.2s ease;box-shadow:0 4px 12px rgba(194,92,49,0.3)}.button:hover{background:var(--primary-dark);transform:translateY(-1px);box-shadow:0 6px 16px rgba(194,92,49,0.4)}.text-link{display:inline-block;margin-top:20px;color:var(--primary-dark);font-size:14px;font-weight:600;text-decoration:none;transition:opacity 0.2s}.text-link:hover{opacity:0.8;text-decoration:underline}.error{display:none}.toast{position:fixed;top:20px;right:20px;max-width:340px;padding:16px;border-radius:12px;background:#2d211b;color:#fff;font-size:14px;font-weight:600;box-shadow:0 20px 40px rgba(0,0,0,0.3);z-index:5;animation:slideIn 0.4s ease forwards}.toast:empty{display:none}
                @keyframes slideIn { from { transform:translateX(20px);opacity:0; } to { transform:translateX(0);opacity:1; } }
                @media(max-width:520px){body{padding:16px;align-items:center}.auth-card{padding:24px}.brand h1{font-size:28px}.toast{left:16px;right:16px;top:16px;max-width:none}}
                </style><script>setTimeout(()=>{const t=document.querySelector('.toast');if(t)t.style.opacity='0';setTimeout(()=>t&&(t.style.display='none'),300)},4000)</script></head><body><div class="toast">%s</div>%s</body></html>
                """.formatted(escape(title), notificationFrom(body), body);
    }

    private static String notificationFrom(String body) {
        int start = body.indexOf("<p class=\"error\">");
        if (start < 0) {
            return "";
        }
        start += "<p class=\"error\">".length();
        int end = body.indexOf("</p>", start);
        return end > start ? body.substring(start, end) : "";
    }

    private static void createAccount(Map<String, String> form) throws IOException {
        Files.createDirectories(DATA_DIR);
        String id = "AC-" + Instant.now().toEpochMilli();
        String line = String.join("|",
                url(id),
                url(form.getOrDefault("name", "")),
                url(form.getOrDefault("email", "")),
                url(form.getOrDefault("password", "")),
                url(form.getOrDefault("phone", "")),
                url(form.getOrDefault("address", "")),
                url("Active"));
        Path file = DATA_DIR.resolve(ACCOUNT_FILE);
        List<String> lines = Files.exists(file) ? new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8)) : new ArrayList<>();
        lines.add(line);
        Files.write(file, lines, StandardCharsets.UTF_8);

        Module usersModule = findModule("users");
        List<Record> users = readRecords(usersModule);
        users.removeIf(record -> record.values().size() > 1
                && record.values().get(1).equalsIgnoreCase(form.getOrDefault("email", "")));
        users.add(new Record("USERS-" + Instant.now().toEpochMilli(), List.of(
                form.getOrDefault("name", ""),
                form.getOrDefault("email", ""),
                form.getOrDefault("phone", ""),
                form.getOrDefault("address", ""),
                "Active",
                ""
        )));
        writeRecords(usersModule, users);
    }

    private static String authenticate(Map<String, String> form) throws IOException {
        String email = form.getOrDefault("email", "");
        String password = form.getOrDefault("password", "");
        String role = form.getOrDefault("role", "customer");
        if ("admin".equals(role) && "admin@bakery.lk".equalsIgnoreCase(email) && "admin123".equals(password)) {
            return "admin";
        }
        Path file = DATA_DIR.resolve(ACCOUNT_FILE);
        if (!Files.exists(file)) {
            return null;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\|", -1);
            if ("customer".equals(role)
                    && parts.length >= 4
                    && decode(parts[2]).equalsIgnoreCase(email)
                    && decode(parts[3]).equals(password)
                    && (parts.length < 7 || "Active".equalsIgnoreCase(decode(parts[6])))) {
                return "customer";
            }
        }
        return null;
    }

    private static String loginName(String role, String email) throws IOException {
        if ("admin".equals(role)) {
            return "Admin";
        }
        Path file = DATA_DIR.resolve(ACCOUNT_FILE);
        if (!Files.exists(file)) {
            return "Customer";
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length >= 4 && decode(parts[2]).equalsIgnoreCase(email)) {
                return decode(parts[1]);
            }
        }
        return "Customer";
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static String autoTotalAmount(String orderedItem, String fallbackPrice) {
        String total = calculateOrderTotal(orderedItem);
        if (!"0".equals(total)) {
            return total;
        }
        return numberOnly(fallbackPrice);
    }

    private static String calculateOrderTotal(String orderedItems) {
        double total = 0;
        String[] lines = (orderedItems == null ? "" : orderedItems).split("\\R");
        for (String line : lines) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("x(\\d+)\\s*@\\s*(\\d+(?:\\.\\d{1,2})?)", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(line);
            if (matcher.find()) {
                total += parsePositiveInt(matcher.group(1)) * parseAmount(matcher.group(2));
            }
        }
        if (total == 0) {
            return "0";
        }
        return total == Math.rint(total) ? String.valueOf((long) total) : String.format(java.util.Locale.US, "%.2f", total);
    }

    private static String numberOnly(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d{1,2})?)").matcher(value == null ? "" : value);
        String last = "";
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last.isBlank() ? "0" : last;
    }

    private static double parseAmount(String value) {
        try {
            return Double.parseDouble(numberOnly(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static int parsePositiveInt(String value) {
        try {
            return Math.max(1, Integer.parseInt(value == null ? "1" : value.trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static int stockAmount(Record product) {
        if (product == null || product.values().size() < 6) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(product.values().get(5).trim()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String orderLine(String productName, String price, int quantity) {
        return productName + " x" + quantity + " @ " + numberOnly(price);
    }

    private static void updateProductStock(String productId, int stock) throws IOException {
        Module productsModule = findModule("products");
        List<Record> products = readRecords(productsModule);
        for (int i = 0; i < products.size(); i++) {
            Record product = products.get(i);
            if (product.id().equals(productId)) {
                List<String> values = normalizedValues(productsModule, product.values());
                values.set(5, String.valueOf(Math.max(0, stock)));
                if (stock <= 0) {
                    values.set(4, "Unavailable");
                }
                products.set(i, new Record(product.id(), values));
                break;
            }
        }
        writeRecords(productsModule, products);
    }

    private static String recordsPage(HttpExchange exchange, String role, Module module) throws IOException {
        boolean isFeedback = "feedback".equals(module.key());
        StringBuilder rows = new StringBuilder();
        for (Record record : visibleRecords(exchange, role, module)) {
            rows.append("<tr>");
            if (!isFeedback) {
                rows.append("<td>").append(escape(record.id())).append("</td>");
            }
            List<String> values = normalizedValues(module, record.values());
            for (int i = 0; i < module.fields().size(); i++) {
                String value = i < values.size() ? values.get(i) : "";
                if (isFeedback && i == 1) {
                    rows.append("<td class=\"stars\">").append(escape(formatStars(value))).append("</td>");
                } else {
                    rows.append("<td>").append(escape(value)).append("</td>");
                }
            }
            rows.append("""
                    <td class="actions">%s</td></tr>
                    """.formatted(actionButtons(role, module, record)));
        }

        StringBuilder headers = new StringBuilder();
        if (!isFeedback) {
            headers.append("<th>ID</th>");
        }
        module.fields().forEach(field -> headers.append("<th>").append(escape(field)).append("</th>"));
        headers.append("<th>Actions</th>");

        String body = """
                <section class="page-title"><div><p>%s</p><h1>%s</h1></div>%s</section>
                <div style="margin-bottom:12px; font-size:12px; color:var(--primary); font-weight:700; text-transform:uppercase; letter-spacing:1px; display:flex; align-items:center; gap:8px;">
                    <span style="width:8px; height:8px; background:var(--primary); border-radius:50%%; display:inline-block;"></span>
                    Sorted by Bubble Sort Algorithm
                </div>
                <input id="search" placeholder="Search records" oninput="filterRows(this.value)">
                <div class="table-scroll"><table><thead><tr>%s</tr></thead><tbody>%s</tbody></table></div>
                """.formatted(escape(module.description()), escape(module.publicTitle()), addButton(role, module), headers, rows);
        return appLayout(role, module.publicTitle(), body);
    }

    private static List<Record> visibleRecords(HttpExchange exchange, String role, Module module) throws IOException {
        List<Record> records = readRecords(module);
        List<Record> filtered;
        if (!"customer".equals(role) || "products".equals(module.key()) || "feedback".equals(module.key())) {
            filtered = new ArrayList<>(records);
        } else {
            UserSession session = currentSession(exchange);
            if (session == null) {
                return List.of();
            }
            filtered = records.stream()
                    .filter(record -> !record.values().isEmpty() && record.values().get(0).equalsIgnoreCase(session.name()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        // Apply Bubble Sort on the first value (index 0) of the records
        bubbleSort(filtered);
        return filtered;
    }

    private static void bubbleSort(List<Record> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                String val1 = list.get(j).values().isEmpty() ? "" : list.get(j).values().get(0).toLowerCase();
                String val2 = list.get(j + 1).values().isEmpty() ? "" : list.get(j + 1).values().get(0).toLowerCase();
                if (val1.compareTo(val2) > 0) {
                    Record temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }

    private static boolean ownsRecord(HttpExchange exchange, Record record) {
        UserSession session = currentSession(exchange);
        return record != null
                && session != null
                && !record.values().isEmpty()
                && record.values().get(0).equalsIgnoreCase(session.name());
    }

    private static String addButton(String role, Module module) {
        if ("customer".equals(role) && !module.customerCanAdd()) {
            return "";
        }
        return "<a class=\"button\" href=\"/" + role + "/modules/" + module.key() + "/new\">" + escape(module.createText()) + "</a>";
    }

    private static UserSession currentSession(HttpExchange exchange) {
        String token = sessionToken(exchange);
        return token == null ? null : SESSIONS.get(token);
    }

    private static String sessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String cookieHeader : cookies) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "bakery_session".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private static List<Record> readRecords(Module module) throws IOException {
        Path file = DATA_DIR.resolve(module.fileName());
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<Record> records = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            List<String> values = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                values.add(decode(parts[i]));
            }
            records.add(new Record(decode(parts[0]), values));
        }
        return records;
    }

    private static String appLayout(String role, String title, String body) {
        return """
                <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                <title>%s</title><style>
                :root { --primary: #c25c31; --primary-dark: #91401f; --bg-main: #fcfaf8; --surface: #ffffff; --text-dark: #27211d; --text-muted: #6d625a; --sidebar-bg: #1c1410; --border: #e8dcd0; }
                body{margin:0;background:var(--bg-main);color:var(--text-dark);font-family:'Inter',sans-serif;font-size:14px;line-height:1.5}.app{min-height:100vh;display:grid;grid-template-columns:250px minmax(0,1fr)}.sidebar{padding:24px;background:var(--sidebar-bg);color:#fff;display:flex;flex-direction:column;border-right:1px solid rgba(255,255,255,0.05)}.brand{margin-bottom:32px}.brand b{display:block;font-size:20px;font-weight:800;letter-spacing:-0.5px;color:var(--primary)}.brand span{color:#d9b89b;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;opacity:0.8}.nav{display:grid;gap:4px;flex:1}.nav a{padding:10px 16px;border-radius:10px;color:#d9b89b;text-decoration:none;font-weight:600;transition:all 0.2s ease;font-size:13px}.nav a:hover{background:rgba(194,92,49,0.1);color:#fff;transform:translateX(4px)}.signout{margin-top:auto;color:#f6c7b8!important;background:rgba(246,199,184,0.05)}.signout:hover{background:rgba(246,199,184,0.15);color:#fff!important}.content{min-width:0;padding:40px;overflow:auto;height:100vh;box-sizing:border-box}.hero-card,.page-title{display:flex;align-items:center;justify-content:space-between;gap:24px;margin-bottom:32px;padding:40px;background:linear-gradient(135deg,rgba(40,25,18,0.95),rgba(194,92,49,0.8)),url('https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1400&q=80') center/cover;border-radius:20px;color:#fff;box-shadow:0 15px 35px rgba(0,0,0,0.12)}.hero-card h1,.page-title h1{margin:8px 0;font-size:38px;line-height:1.1;letter-spacing:-1.5px;font-weight:800}.hero-card p,.page-title p{margin:0;font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:2px;color:var(--primary)}.hero-card span{font-size:16px;color:rgba(255,255,255,0.9);font-weight:500;max-width:500px}.tile-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:24px}.tile{background:white;border:1px solid var(--border);border-radius:18px;padding:28px;text-decoration:none;color:var(--text-dark);display:flex;align-items:center;gap:20px;transition:all 0.3s cubic-bezier(0.4, 0, 0.2, 1);box-shadow:0 4px 15px rgba(0,0,0,0.02)}.tile:hover{transform:translateY(-6px);box-shadow:0 20px 40px rgba(0,0,0,0.08);border-color:var(--primary)}.tile-icon{width:56px;height:56px;background:rgba(194,92,49,0.1);border-radius:14px;display:grid;place-items:center;font-size:24px;color:var(--primary);flex-shrink:0}.tile-content strong{display:block;font-size:18px;font-weight:700;margin-bottom:4px;letter-spacing:-0.4px}.tile-content small{color:var(--text-muted);font-size:13px;line-height:1.4;display:block}.button,.small,button{display:inline-flex;align-items:center;justify-content:center;min-height:44px;padding:0 20px;border:0;border-radius:10px;font-size:14px;font-weight:700;text-decoration:none;cursor:pointer;transition:all 0.2s}.button{color:white;background:var(--primary);box-shadow:0 8px 20px rgba(194,92,49,0.2)}.button:hover{background:var(--primary-dark);transform:translateY(-2px);box-shadow:0 12px 25px rgba(194,92,49,0.3)}.small{color:var(--primary);background:rgba(194,92,49,0.08);min-height:36px;padding:0 14px;font-size:12px}.small:hover{background:rgba(194,92,49,0.15)}button{color:white;background:#dc2626;min-height:36px}#search{width:100%%;box-sizing:border-box;min-height:48px;margin-bottom:24px;padding:0 20px;border:1px solid var(--border);border-radius:12px;font:inherit;background:white;transition:all 0.2s}#search:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 4px rgba(194,92,49,0.1)}table{width:100%%;border-collapse:separate;border-spacing:0;background:white;border:1px solid var(--border);border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.04)}th,td{padding:18px 20px;border-bottom:1px solid var(--border);text-align:left}th{background:#fdfaf7;color:var(--primary);font-weight:800;font-size:12px;text-transform:uppercase;letter-spacing:1px}tr:hover td{background:#fffbf8}.actions{display:flex;gap:10px}.form-wrap{max-width:800px;margin:0 auto}.form-wrap form{background:white;padding:40px;border-radius:20px;border:1px solid var(--border);box-shadow:0 20px 50px rgba(0,0,0,0.05);display:grid;gap:24px}label{font-weight:700;font-size:14px;color:var(--text-dark);display:grid;gap:8px}input,select,textarea{min-height:48px;padding:0 16px;border:1px solid var(--border);border-radius:10px;font:inherit;background:white;box-sizing:border-box;width:100%%}textarea{min-height:96px;padding:12px 16px}input[readonly],textarea[readonly]{background:#f7f1eb;color:var(--text-muted)}input:focus,select:focus,textarea:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 4px rgba(194,92,49,0.1)}.order-picker{display:grid;gap:10px}.order-row{display:grid;grid-template-columns:minmax(0,1fr) 110px;gap:10px}.price-panel{background:#fff7ef;border:1px solid #f1d4be;border-radius:14px;padding:18px;display:grid;gap:12px}.price-panel strong{color:var(--primary);font-size:13px;text-transform:uppercase;letter-spacing:1px}.price-grid{display:grid;grid-template-columns:1fr auto;gap:6px 16px;font-size:13px;color:var(--text-muted)}.price-grid span:nth-child(odd){font-weight:600;color:var(--text-dark)}.addons{display:grid;gap:6px;border-top:1px dashed #f1d4be;padding-top:10px}.addon{display:flex!important;align-items:center;gap:8px;font-weight:500;font-size:13px;margin:0}.addon input{min-height:auto!important;width:auto!important;padding:0!important;margin:0;border-radius:4px}.addon em{color:var(--primary);font-style:normal;font-weight:700;font-size:12px}.price-total{font-size:15px;font-weight:700;color:var(--text-dark);border-top:1px solid #f1d4be;padding-top:10px;text-align:right}.stars{color:#f5a623;font-weight:700;letter-spacing:2px;font-size:15px}@media(max-width:900px){.app{grid-template-columns:1fr}.sidebar{flex-direction:row;padding:12px 20px;position:sticky;top:0;z-index:100}.nav{display:none}.content{padding:20px}.hero-card{padding:30px;flex-direction:column;align-items:flex-start}.hero-card h1{font-size:30px}.order-row{grid-template-columns:1fr}}
                </style><script>
                function filterRows(q){q=q.toLowerCase();document.querySelectorAll('tbody tr').forEach(r=>r.hidden=!r.textContent.toLowerCase().includes(q));}
                function updateOrderDuplicates(){const selects=document.querySelectorAll('[data-order-product]');const taken=new Set();selects.forEach(s=>{if(s.value)taken.add(s.value);});selects.forEach(s=>{Array.from(s.options).forEach(opt=>{if(!opt.value){opt.disabled=false;return;}opt.disabled=opt.value!==s.value&&taken.has(opt.value);});});}
                function syncOrder(){const item=document.querySelector('[data-order-item]');const total=document.querySelector('[data-total-amount]');if(!item||!total)return;let lines=[],sum=0;document.querySelectorAll('[data-order-product]').forEach(select=>{const opt=select.selectedOptions[0];if(!opt||!select.value)return;const row=select.closest('.order-row');const qtyInput=row?row.querySelector('input[type="number"]'):null;const qty=Math.max(1,parseInt(qtyInput?.value||'1',10));const stock=Math.max(0,parseInt(opt.dataset.stock||'0',10));if(qtyInput)qtyInput.max=stock;const useQty=Math.min(qty,stock);const price=parseFloat(opt.dataset.price||'0');lines.push(`${opt.dataset.name} x${useQty} @ ${price}`);sum+=useQty*price;});item.value=lines.join('\\n');total.value=sum?String(Math.round(sum*100)/100):'0';}
                function addOrderRow(){const picker=document.querySelector('[data-order-picker]');const tmpl=document.querySelector('[data-row-template]');if(!picker||!tmpl)return;const rows=picker.querySelectorAll('.order-row');if(rows.length>=5)return;const idx=rows.length;const clone=tmpl.content.firstElementChild.cloneNode(true);const sel=clone.querySelector('select');const qty=clone.querySelector('input');sel.name='orderProduct'+idx;qty.name='orderQty'+idx;picker.appendChild(clone);updateOrderDuplicates();}
                const CAKE_BASE_PER_KG=3000;
                function parseKg(text){const m=(text||'').match(/([0-9]+(?:\\.[0-9]+)?)/);return m?parseFloat(m[1]):0;}
                function syncCake(){const panel=document.querySelector('[data-cake-pricing]');if(!panel)return;const sizeSelect=document.querySelector('select[name="field1"]');const kg=sizeSelect?parseKg(sizeSelect.value):0;let total=kg*CAKE_BASE_PER_KG;panel.querySelectorAll('[data-addon]').forEach(input=>{if(input.checked)total+=parseFloat(input.value||'0');});const out=panel.querySelector('[data-cake-total]');if(out)out.textContent='Rs. '+total.toLocaleString();}
                document.addEventListener('input',e=>{if(e.target.matches('[data-order-product],[data-order-qty]')){syncOrder();updateOrderDuplicates();}if(e.target.matches('[data-addon]')||e.target.name==='field1')syncCake();});
                document.addEventListener('change',e=>{if(e.target.matches('[data-order-product],[data-order-qty]')){syncOrder();updateOrderDuplicates();}if(e.target.matches('[data-addon]')||e.target.name==='field1')syncCake();});
                document.addEventListener('click',e=>{if(e.target.matches('[data-add-row]')){e.preventDefault();addOrderRow();}});
                document.addEventListener('DOMContentLoaded',()=>{syncOrder();updateOrderDuplicates();syncCake();});
                </script></head><body>%s</body></html>
                """.formatted(escape(title), "<div class=\"app\">" + sidebar(role) + "<main class=\"content\">" + body + "</main></div>");
    }

    private static List<String> normalizedValues(Module module, List<String> values) {
        List<String> normalized = new ArrayList<>(values);
        if ("feedback".equals(module.key()) && normalized.size() > module.fields().size()) {
            return new ArrayList<>(normalized.subList(0, module.fields().size()));
        }
        while (normalized.size() < module.fields().size()) {
            if ("products".equals(module.key()) && normalized.size() == 5) {
                normalized.add("10");
            } else {
                normalized.add("");
            }
        }
        return normalized;
    }

    private static boolean requireRole(HttpExchange exchange, String role) throws IOException {
        UserSession session = currentSession(exchange);
        if (session != null && role.equals(session.role())) {
            return true;
        }
        redirect(exchange, "/login");
        return false;
    }

    private static Record findRecord(Module module, String id) throws IOException {
        String decodedId = decode(id);
        return readRecords(module).stream().filter(record -> record.id().equals(decodedId)).findFirst().orElse(null);
    }

    private static void writeRecords(Module module, List<Record> records) throws IOException {
        Files.createDirectories(DATA_DIR);
        List<String> lines = new ArrayList<>();
        for (Record record : records) {
            List<String> parts = new ArrayList<>();
            parts.add(url(record.id()));
            record.values().forEach(value -> parts.add(url(value)));
            lines.add(String.join("|", parts));
        }
        Files.write(DATA_DIR.resolve(module.fileName()), lines, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
        }
        return values;
    }

    private static Module findModule(String key) {
        return MODULES.stream().filter(module -> module.key().equals(key)).findFirst().orElse(null);
    }

    private static void html(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String url(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record Module(String key, String title, String studentId, String studentName, String createText, String readText, String updateText, String deleteText, String fileName, List<String> fields) {
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

        String description() {
            return switch (key) {
                case "feedback" -> "Review customer ratings, comments, and moderation status.";
                case "admins" -> "Maintain staff access, stock duties, and operational records.";
                case "users" -> "Register customers and keep contact profiles up to date.";
                case "products" -> "Organize bakery items, categories, prices, and availability.";
                case "orders" -> "Track carts, checkout details, payment, and order progress.";
                case "custom-cakes" -> "Manage cake sizes, design notes, pickup dates, and booking status.";
                default -> "Manage bakery records.";
            };
        }

        String icon() {
            return switch (key) {
                case "feedback" -> "Reviews";
                case "admins" -> "Admin";
                case "users" -> "Customers";
                case "products" -> "Menu";
                case "orders" -> "Checkout";
                case "custom-cakes" -> "Cake Studio";
                default -> "Module";
            };
        }

        boolean customerAllowed() {
            return "feedback".equals(key) || "products".equals(key) || "orders".equals(key) || "custom-cakes".equals(key);
        }

        boolean customerCanAdd() {
            return "feedback".equals(key) || "orders".equals(key) || "custom-cakes".equals(key);
        }

        String singleName() {
            return switch (key) {
                case "feedback" -> "Customer Review";
                case "admins" -> "Staff Member";
                case "users" -> "Customer";
                case "products" -> "Menu Item";
                case "orders" -> "Order";
                case "custom-cakes" -> "Cake Booking";
                default -> "Record";
            };
        }

        String primaryFeature() {
            return switch (key) {
                case "feedback" -> "Customer review capture";
                case "admins" -> "Staff account setup";
                case "users" -> "Customer profile registration";
                case "products" -> "Menu item setup";
                case "orders" -> "Order placement";
                case "custom-cakes" -> "Personalized cake booking";
                default -> "Daily records";
            };
        }

        String insightFeature() {
            return switch (key) {
                case "feedback" -> "Ratings and comments";
                case "admins" -> "Operational reports";
                case "users" -> "Customer profiles";
                case "products" -> "Menu availability";
                case "orders" -> "Order history";
                case "custom-cakes" -> "Design and pickup details";
                default -> "Record details";
            };
        }

        String controlFeature() {
            return switch (key) {
                case "feedback" -> "Review moderation";
                case "admins" -> "Stock responsibility";
                case "users" -> "Profile maintenance";
                case "products" -> "Price and detail control";
                case "orders" -> "Progress tracking";
                case "custom-cakes" -> "Design adjustments";
                default -> "Record management";
            };
        }

        String iconEmoji() {
            return switch (key) {
                case "feedback" -> "\u2B50";
                case "admins" -> "\uD83D\uDD11";
                case "users" -> "\uD83D\uDC65";
                case "products" -> "\uD83C\uDF70";
                case "orders" -> "\uD83D\uDCE6";
                case "custom-cakes" -> "\uD83C\uDFA8";
                default -> "\uD83D\uDCC1";
            };
        }

        String cleanupFeature() {
            return switch (key) {
                case "feedback" -> "Keep reviews tidy";
                case "admins" -> "Archive expired access";
                case "users" -> "Retire inactive profiles";
                case "products" -> "Remove unavailable items";
                case "orders" -> "Clear cancelled orders";
                case "custom-cakes" -> "Clear cancelled bookings";
                default -> "Organized records";
            };
        }
    }

    private static void notFound(HttpExchange exchange) throws IOException {
        byte[] bytes = "Not found".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record Record(String id, List<String> values) {
    }

    private record UserSession(String role, String email, String name) {
    }

    private static String storefrontPage(HttpExchange exchange, String page) throws IOException {
        UserSession session = currentSession(exchange);
        boolean isLoggedIn = session != null && "customer".equals(session.role());
        String orderedMsg = "ordered=1".equals(exchange.getRequestURI().getQuery()) ? "<div class='toast'>Order placed successfully! Track it in your dashboard.</div>" : "";
        String stockMsg = "stock=0".equals(exchange.getRequestURI().getQuery()) ? "<div class='toast'>Not enough stock for that product.</div>" : "";

        StringBuilder content = new StringBuilder();

        if ("home".equals(page)) {
            content.append("<section class='hero'><h1>Freshly Baked Happiness</h1><p>Order our artisan cakes, cupcakes, and pastries. Crafted with love and the finest ingredients.</p><a href='/menu' class='button'>Explore Menu</a></section>");
            content.append("<section class='features' style='padding:80px 5%; text-align:center; display:grid; grid-template-columns:repeat(3,1fr); gap:40px;'>");
            content.append("<div class='feature-box'><h3>Fresh Daily</h3><p>Baked every morning with premium ingredients.</p></div>");
            content.append("<div class='feature-box'><h3>Custom Designs</h3><p>Personalized cakes for your special moments.</p></div>");
            content.append("<div class='feature-box'><h3>Fast Delivery</h3><p>Warm treats delivered straight to your door.</p></div>");
            content.append("</section>");
        } else if ("menu".equals(page)) {
            List<Record> products = readRecords(findModule("products"));
            StringBuilder productGrid = new StringBuilder();
            if (products.isEmpty()) {
                String[][] defaults = {
                        {"Artisan Cupcake", "Rs. 450", "/public/images/product_cupcake_1777824835243.png"},
                        {"Red Velvet Cake", "Rs. 3200", "/public/images/product_cake_1777824854074.png"},
                        {"Chocolate Cookies", "Rs. 250", "/public/images/product_cookies_1777825136738.png"},
                        {"Butter Croissant", "Rs. 350", "/public/images/product_croissant_1777825151387.png"},
                        {"Sourdough Bread", "Rs. 600", "/public/images/product_bread_1777825166311.png"},
                        {"Glazed Donuts", "Rs. 300", "/public/images/product_donuts_1777825181310.png"}
                };
                for (String[] d : defaults) {
                    productGrid.append("<div class='product-card'><div class='img-wrap'><img src='").append(d[2]).append("' alt='").append(d[0]).append("'></div><h3>").append(d[0]).append("</h3><p>").append(d[1]).append("</p>");
                    if (isLoggedIn) productGrid.append("<form method='post' action='/add-to-cart'><input type='hidden' name='product' value='").append(d[0]).append("'><input type='hidden' name='price' value='").append(d[1]).append("'><button type='submit' class='button small'>Order Now</button></form>");
                    else productGrid.append("<a href='/login' class='button small'>Login to Order</a>");
                    productGrid.append("</div>");
                }
            } else {
                for (Record p : products) {
                    List<String> values = normalizedValues(findModule("products"), p.values());
                    if (values.size() < 6 || !"Available".equalsIgnoreCase(values.get(4)) || stockAmount(new Record(p.id(), values)) <= 0) continue;
                    String name = escape(values.get(0));
                    String price = escape(values.get(2));
                    String stock = escape(values.get(5));
                    String img = "/public/images/product_cupcake_1777824835243.png";
                    if (name.toLowerCase().contains("cake") && !name.toLowerCase().contains("cup")) img = "/public/images/product_cake_1777824854074.png";
                    else if (name.toLowerCase().contains("cookie")) img = "/public/images/product_cookies_1777825136738.png";
                    else if (name.toLowerCase().contains("croissant")) img = "/public/images/product_croissant_1777825151387.png";
                    else if (name.toLowerCase().contains("bread")) img = "/public/images/product_bread_1777825166311.png";
                    else if (name.toLowerCase().contains("donut")) img = "/public/images/product_donuts_1777825181310.png";

                    productGrid.append("<div class='product-card'><div class='img-wrap'><img src='").append(img).append("' alt='").append(name).append("'></div><h3>").append(name).append("</h3><p>").append(price).append("</p><span class='stock-badge'>In stock: ").append(stock).append("</span>");
                    if (isLoggedIn) productGrid.append("<form method='post' action='/add-to-cart'><input type='hidden' name='productId' value='").append(escape(p.id())).append("'><input type='number' min='1' max='").append(stock).append("' name='quantity' value='1'><button type='submit' class='button small'>Order Now</button></form>");
                    else productGrid.append("<a href='/login' class='button small'>Login to Order</a>");
                    productGrid.append("</div>");
                }
            }
            content.append(stockMsg).append("<section class='products-sec'><h2>Our Menu</h2><div class='grid'>").append(productGrid).append("</div></section>");
        } else if ("about".equals(page)) {
            content.append("<section class='about-page'>");
            content.append("<h1>Our Story</h1>");
            content.append("<p>Sweet Crumbs Bakery started with a simple passion: bringing the warmth of fresh-baked goods to every home. We believe in the magic of high-quality ingredients and traditional techniques.</p>");
            content.append("<div class='about-img' style='background-image: url(\"https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=1200&q=80\")'></div>");
            content.append("</section>");
        } else if ("contact".equals(page)) {
            content.append("<section class='contact-page'>");
            content.append("<div class='contact-header'><h1>Contact Us</h1><p>Have a question or a special order? We'd love to hear from you!</p></div>");
            content.append("<div class='contact-container'>");
            content.append("<div class='contact-info'><h3>Get in Touch</h3><p>Visit us or send a message for custom cake orders and bulk bookings.</p><ul><li><b>Address:</b> 123 Bakery Lane, Colombo 07</li><li><b>Phone:</b> +94 11 234 5678</li><li><b>Email:</b> hello@sweetcrumbs.lk</li></ul></div>");
            content.append("<div class='contact-form'>");
            content.append("<label>Name<input placeholder='Your name'></label>");
            content.append("<label>Email<input type='email' placeholder='Your email'></label>");
            content.append("<label>Message<textarea placeholder='How can we help?'></textarea></label>");
            content.append("<button class='button'>Send Message</button>");
            content.append("</div></div></section>");
        }

        String navLinks = """
            <a href='/' class='nav-link %s'>Home</a>
            <a href='/menu' class='nav-link %s'>Menu</a>
            <a href='/about' class='nav-link %s'>About Us</a>
            <a href='/contact' class='nav-link %s'>Contact</a>
            """.formatted(
                "home".equals(page) ? "active" : "",
                "menu".equals(page) ? "active" : "",
                "about".equals(page) ? "active" : "",
                "contact".equals(page) ? "active" : ""
        );

        String userArea = isLoggedIn
                ? "<a href='/customer/dashboard' class='nav-link'>Dashboard</a> <a href='/logout' class='button small' style='margin-left:15px;'>Logout</a>"
                : "<a href='/login' class='nav-link'>Login</a> <a href='/create-account' class='button small' style='margin-left:15px;'>Sign Up</a>";

        String html = """
            <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
            <title>Sweet Crumbs Bakery</title><style>
            :root { --primary: #c25c31; --primary-dark: #91401f; --bg-main: #fcfaf8; --surface: #ffffff; --text-dark: #27211d; --text-muted: #6d625a; --border: #e8dcd0; --sidebar-bg: #221814; }
            body{margin:0;background:var(--bg-main);color:var(--text-dark);font-family:'Inter',sans-serif;font-size:15px;line-height:1.6;display:flex;flex-direction:column;min-height:100vh}
            header{display:flex;align-items:center;justify-content:space-between;padding:12px 5%%;background:var(--surface);box-shadow:0 2px 15px rgba(0,0,0,0.04);position:sticky;top:0;z-index:100;flex-wrap:wrap;gap:15px}
            .logo{display:flex;align-items:center;gap:10px;text-decoration:none;color:var(--primary);font-weight:800;font-size:22px;letter-spacing:-0.5px}
            .logo img{height:36px;width:36px;object-fit:contain}
            nav{display:flex;gap:5px;flex-wrap:wrap;justify-content:center}
            .nav-link{text-decoration:none;color:var(--text-dark);font-weight:600;margin:0 8px;transition:color 0.2s;font-size:14px;padding:5px 0}
            .nav-link:hover, .nav-link.active{color:var(--primary)}
            .button{display:inline-flex;align-items:center;justify-content:center;min-height:44px;padding:0 24px;border:0;border-radius:10px;background:var(--primary);color:white;font-size:14px;font-weight:700;text-decoration:none;cursor:pointer;transition:all 0.2s ease}
            .button.small{min-height:36px;padding:0 16px;font-size:13px;border-radius:8px}
            .button:hover{background:var(--primary-dark);transform:translateY(-1px);box-shadow:0 6px 16px rgba(194,92,49,0.3)}
            .hero{padding:120px 5%%;text-align:center;background:linear-gradient(135deg,rgba(40,25,18,0.85),rgba(90,45,25,0.8)),url('https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1600&q=80') center/cover;color:white}
            .hero h1{font-size:clamp(32px, 8vw, 58px);font-weight:800;margin:0 0 20px;letter-spacing:-2px;line-height:1.1}
            .hero p{font-size:clamp(16px, 4vw, 20px);max-width:700px;margin:0 auto 40px;color:rgba(255,255,255,0.9)}
            .feature-box{padding:30px;background:white;border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,0.03);border:1px solid var(--border);transition:all 0.3s}
            .feature-box:hover{transform:translateY(-5px);box-shadow:0 10px 30px rgba(0,0,0,0.06)}
            .feature-box h3{font-size:22px;margin:0 0 10px;color:var(--primary)}
            .products-sec{padding:80px 5%%;max-width:1100px;margin:0 auto}
            .products-sec h2{font-size:clamp(28px, 6vw, 38px);font-weight:800;margin-bottom:50px;text-align:center;letter-spacing:-1px}
            .grid{display:grid;grid-template-columns:repeat(auto-fit, minmax(280px, 1fr));gap:25px}
            .product-card{background:var(--surface);border-radius:14px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.04);transition:all 0.3s ease;display:flex;flex-direction:column;padding-bottom:16px;text-align:center;border:1px solid var(--border)}
            .product-card:hover{transform:translateY(-6px);box-shadow:0 15px 35px rgba(0,0,0,0.08)}
            .img-wrap{width:100%%;height:170px;overflow:hidden;background:#fdfaf7}
            .product-card img{width:100%%;height:100%%;object-fit:cover;transition:transform 0.5s}
            .product-card:hover img{transform:scale(1.08)}
            .product-card h3{margin:15px 15px 5px;font-size:18px;font-weight:700}
            .product-card p{margin:0 15px 15px;color:var(--primary);font-weight:800;font-size:16px}
            .stock-badge{margin:0 15px 14px;color:var(--text-muted);font-size:13px;font-weight:700}
            .product-card form{margin:auto 15px 0;display:grid;grid-template-columns:90px 1fr;gap:10px}
            .product-card input{min-height:36px;border:1px solid var(--border);border-radius:8px;padding:0 10px;font:inherit}
            .about-page{padding:80px 5%%;max-width:900px;margin:0 auto;text-align:center}
            .about-page h1{font-size:clamp(32px, 8vw, 48px);font-weight:800;color:var(--primary);margin-bottom:20px}
            .about-page p{font-size:18px;color:var(--text-muted);margin-bottom:50px}
            .about-img{width:100%%;height:450px;border-radius:24px;background-size:cover;background-position:center;box-shadow:0 20px 40px rgba(0,0,0,0.1)}
            .contact-page{padding:80px 5%%;max-width:1000px;margin:0 auto}
            .contact-header{text-align:center;margin-bottom:60px}
            .contact-header h1{font-size:48px;font-weight:800;color:var(--primary);margin-bottom:10px}
            .contact-container{display:grid;grid-template-columns:repeat(auto-fit, minmax(320px, 1fr));gap:40px;align-items:start}
            .contact-info{background:var(--sidebar-bg);color:white;padding:40px;border-radius:20px;box-shadow:0 15px 40px rgba(0,0,0,0.1)}
            .contact-info h3{font-size:24px;margin-bottom:20px;color:var(--primary)}
            .contact-info ul{list-style:none;padding:0;margin-top:30px}
            .contact-info li{margin-bottom:15px;font-size:16px;color:#d9b89b}
            .contact-form{background:white;padding:40px;border-radius:20px;box-shadow:0 10px 40px rgba(0,0,0,0.05);border:1px solid var(--border);display:grid;gap:20px}
            .contact-form label{font-weight:700;font-size:14px;display:grid;gap:8px}
            .contact-form input, .contact-form textarea{width:100%%;padding:12px;border:1px solid var(--border);border-radius:8px;font-family:inherit;font-size:15px;transition:all 0.2s}
            .contact-form input:focus, .contact-form textarea:focus{outline:none;border-color:var(--primary);box-shadow:0 0 0 3px rgba(194,92,49,0.1)}
            .contact-form textarea{min-height:120px;resize:vertical}
            footer{margin-top:auto;background:var(--sidebar-bg);padding:80px 5%% 40px;display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:40px;border-top:5px solid var(--primary)}
            .footer-box{padding:25px;background:rgba(255,255,255,0.03);border-radius:16px;border:1px solid rgba(255,255,255,0.05)}
            footer h4{color:var(--primary);font-size:18px;margin:0 0 20px;text-transform:uppercase;letter-spacing:1.5px;font-weight:800}
            footer p, footer a{color:#d9b89b;font-size:14px;text-decoration:none;margin-bottom:12px;display:block;font-weight:500;transition:color 0.2s}
            footer a:hover{color:white}
            .toast{position:fixed;top:20px;right:20px;padding:16px 24px;border-radius:12px;background:#221814;color:#fff;font-weight:600;box-shadow:0 10px 30px rgba(0,0,0,0.2);animation:slideIn 0.4s ease forwards;z-index:1000}
            @keyframes slideIn{from{transform:translateX(50px);opacity:0}to{transform:translateX(0);opacity:1}}
            @media(max-width:768px){
                header{justify-content:center;text-align:center;}
                .hero{padding:80px 20px;}
                .contact-info, .contact-form{padding:30px 20px;}
            }
            </style>
            <script>setTimeout(()=>{const t=document.querySelector('.toast');if(t)t.style.display='none'},4000)</script>
            </head><body>%s
            <header><a href="/" class="logo"><img src="/public/images/bakery_logo_1777824819116.png" alt="Logo">Sweet Crumbs</a><nav>%s</nav><div>%s</div></header>
            <main>%s</main>
            <footer>
                <div class='footer-box'><h4>About Us</h4><p>Sweet Crumbs Bakery is dedicated to providing the finest artisan breads and pastries in the city. Founded in 2024, we continue to serve happiness daily.</p></div>
                <div class='footer-box'><h4>Contact Info</h4><p>Address: 123 Bakery Lane, Colombo 07</p><p>Phone: +94 11 234 5678</p><p>Email: hello@sweetcrumbs.lk</p></div>
                <div class='footer-box'><h4>Quick Links</h4><a href='/menu'>Our Menu</a><a href='/about'>Our Story</a><a href='/login'>Staff Login</a></div>
                <div class='footer-box'><h4>Follow Us</h4><a href='#'>Instagram</a><a href='#'>Facebook</a><a href='#'>Twitter</a></div>
            </footer>
            <div style='background:var(--sidebar-bg); color:rgba(217,184,155,0.5); text-align:center; padding:20px; font-size:12px; border-top:1px solid rgba(255,255,255,0.05);'>&copy; 2026 Sweet Crumbs Bakery. All rights reserved.</div>
            </body></html>
            """;
        return html.formatted(orderedMsg, navLinks, userArea, content.toString());
    }


}