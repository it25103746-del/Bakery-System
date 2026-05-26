package com.bakery.service;

import com.bakery.model.AdminAccount;
import com.bakery.model.CustomCakeBooking;
import com.bakery.model.CustomerOrder;
import com.bakery.model.Feedback;
import com.bakery.model.Product;
import com.bakery.model.UserAccount;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ModuleCatalog {
    private final List<ModuleDefinition<?>> modules;

    public ModuleCatalog() {
        modules = List.of(
                new ModuleDefinition<>(
                        "feedback", "Feedback, Ratings & Documentation", "IT25103745", "Hettiarachchi H. H. K. S.",
                        "Feedback, Ratings & Documentation", "Create feedback", "Read reviews", "Update feedback", "Delete feedback",
                        "feedback.txt", Feedback::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("customerName", "Customer Name", Feedback::getCustomerName, Feedback::setCustomerName),
                                new ModuleDefinition.FieldDefinition<>("rating", "Rating", Feedback::getRating, Feedback::setRating),
                                new ModuleDefinition.FieldDefinition<>("comment", "Comment", Feedback::getComment, Feedback::setComment)
                        )
                ),
                new ModuleDefinition<>(
                        "admins", "Admin Management", "IT25103746", "Perera K. V. R.",
                        "Admin Management", "Add new admin account", "Read report", "Update Stock", "Delete expired records",
                        "admins.txt", AdminAccount::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("adminName", "Admin Name", AdminAccount::getAdminName, AdminAccount::setAdminName),
                                new ModuleDefinition.FieldDefinition<>("email", "Email", AdminAccount::getEmail, AdminAccount::setEmail),
                                new ModuleDefinition.FieldDefinition<>("role", "Role", AdminAccount::getRole, AdminAccount::setRole),
                                new ModuleDefinition.FieldDefinition<>("stockResponsibility", "Stock Responsibility", AdminAccount::getStockResponsibility, AdminAccount::setStockResponsibility),
                                new ModuleDefinition.FieldDefinition<>("expiryStatus", "Expiry Status", AdminAccount::getExpiryStatus, AdminAccount::setExpiryStatus)
                        )
                ),
                new ModuleDefinition<>(
                        "users", "User Interface / User Authentication", "IT25103747", "Nawarathne S. N.",
                        "User interface / User Authentication", "Register users", "View profiles", "Modify details", "Remove inactive accounts",
                        "users.txt", UserAccount::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("fullName", "Full Name", UserAccount::getFullName, UserAccount::setFullName),
                                new ModuleDefinition.FieldDefinition<>("email", "Email", UserAccount::getEmail, UserAccount::setEmail),
                                new ModuleDefinition.FieldDefinition<>("phone", "Phone", UserAccount::getPhone, UserAccount::setPhone),
                                new ModuleDefinition.FieldDefinition<>("address", "Address", UserAccount::getAddress, UserAccount::setAddress),
                                new ModuleDefinition.FieldDefinition<>("accountStatus", "Account Status", UserAccount::getAccountStatus, UserAccount::setAccountStatus),
                                new ModuleDefinition.FieldDefinition<>("temporaryPassword", "Temporary Password", UserAccount::getTemporaryPassword, UserAccount::setTemporaryPassword)
                        )
                ),
                new ModuleDefinition<>(
                        "products", "Product & Category Management", "IT25103748", "Samarakoon P. T. A. N.",
                        "Product & Category Management", "Create product", "Read listings", "Update prices/details", "Delete discontinued products",
                        "products.txt", Product::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("productName", "Product Name", Product::getProductName, Product::setProductName),
                                new ModuleDefinition.FieldDefinition<>("category", "Category", Product::getCategory, Product::setCategory),
                                new ModuleDefinition.FieldDefinition<>("price", "Price", Product::getPrice, Product::setPrice),
                                new ModuleDefinition.FieldDefinition<>("details", "Details", Product::getDetails, Product::setDetails),
                                new ModuleDefinition.FieldDefinition<>("availability", "Availability", Product::getAvailability, Product::setAvailability),
                                new ModuleDefinition.FieldDefinition<>("stockAmount", "Stock Amount", Product::getStockAmount, Product::setStockAmount)
                        )
                ),
                new ModuleDefinition<>(
                        "orders", "Cart, Checkout & Order Tracking", "IT25103749", "Siriwardhana R. D. T. D.",
                        "Handles cart, checkout, order tracking", "Create orders", "Read history", "Update status", "Delete cancelled orders",
                        "orders.txt", CustomerOrder::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("customerName", "Customer Name", CustomerOrder::getCustomerName, CustomerOrder::setCustomerName),
                                new ModuleDefinition.FieldDefinition<>("orderedItems", "Order Item", CustomerOrder::getOrderedItems, CustomerOrder::setOrderedItems),
                                new ModuleDefinition.FieldDefinition<>("totalAmount", "Total Amount", CustomerOrder::getTotalAmount, CustomerOrder::setTotalAmount),
                                new ModuleDefinition.FieldDefinition<>("orderStatus", "Order Status", CustomerOrder::getOrderStatus, CustomerOrder::setOrderStatus),
                                new ModuleDefinition.FieldDefinition<>("paymentStatus", "Payment Status", CustomerOrder::getPaymentStatus, CustomerOrder::setPaymentStatus)
                        )
                ),
                new ModuleDefinition<>(
                        "custom-cakes", "Custom Cake Design & Booking", "IT25103750", "Vishara A. M. H.",
                        "Custom Cake Design & Booking", "Create custom orders", "Read details", "Update designs", "Delete cancelled orders",
                        "custom-cakes.txt", CustomCakeBooking::new,
                        List.of(
                                new ModuleDefinition.FieldDefinition<>("customerName", "Customer Name", CustomCakeBooking::getCustomerName, CustomCakeBooking::setCustomerName),
                                new ModuleDefinition.FieldDefinition<>("cakeSize", "Cake Size", CustomCakeBooking::getCakeSize, CustomCakeBooking::setCakeSize),
                                new ModuleDefinition.FieldDefinition<>("designDetails", "Design Details", CustomCakeBooking::getDesignDetails, CustomCakeBooking::setDesignDetails),
                                new ModuleDefinition.FieldDefinition<>("pickupDate", "Pickup Date", CustomCakeBooking::getPickupDate, CustomCakeBooking::setPickupDate),
                                new ModuleDefinition.FieldDefinition<>("bookingStatus", "Booking Status", CustomCakeBooking::getBookingStatus, CustomCakeBooking::setBookingStatus)
                        )
                )
        );
    }

    public List<ModuleDefinition<?>> findAll() {
        return modules;
    }

    public ModuleDefinition<?> findByKey(String key) {
        Optional<ModuleDefinition<?>> module = modules.stream()
                .filter(definition -> definition.getKey().equals(key))
                .findFirst();
        return module.orElseThrow(() -> new IllegalArgumentException("Unknown module: " + key));
    }
}
