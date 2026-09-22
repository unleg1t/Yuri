package ddlc.yuri.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class LocaltsClient {

    public static final String BASE_URL = "https://localts.store";
    private static final String USER_AGENT = "Yuri/1.8.9";
    private static final int MAX_ATTEMPTS = 4;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    private LocaltsClient() {
    }

    public static User getMe(String apiKey) throws LocaltsException {
        JsonObject json = execute("GET", "/v1/me", apiKey);
        return new User(requiredString(json, "username"), requiredNumber(json, "balance"));
    }

    public static List<Product> getProducts(String apiKey) throws LocaltsException {
        JsonObject json = execute("GET", "/v1/products", apiKey);
        List<Product> products = new ArrayList<>();
        for (JsonElement element : requiredArray(json, "products")) {
            if (!element.isJsonObject()) continue;
            JsonObject product = element.getAsJsonObject();

            List<String> tags = new ArrayList<>();
            JsonElement tagsElement = product.get("tags");
            if (tagsElement != null && tagsElement.isJsonArray()) {
                for (JsonElement tag : tagsElement.getAsJsonArray()) {
                    if (tag.isJsonPrimitive()) tags.add(tag.getAsString());
                }
            }

            TreeMap<Integer, Double> discounts = new TreeMap<>();
            JsonElement discountsElement = product.get("quantityDiscounts");
            if (discountsElement != null && discountsElement.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : discountsElement.getAsJsonObject().entrySet()) {
                    try {
                        discounts.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsDouble());
                    } catch (RuntimeException ignored) {
                    }
                }
            }

            products.add(new Product(
                    requiredString(product, "id"),
                    requiredString(product, "name"),
                    optionalString(product, "description"),
                    optionalString(product, "category"),
                    requiredNumber(product, "priceInCredits"),
                    requiredInt(product, "stock"),
                    optionalString(product, "type"),
                    Collections.unmodifiableList(tags),
                    Collections.unmodifiableMap(discounts)));
        }
        products.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return products;
    }

    public static String purchase(String apiKey, String productId, int amount) throws LocaltsException {
        JsonObject json = execute("POST", "/v1/products/" + urlEncode(productId) + "/purchase?amount=" + amount, apiKey);
        return requiredString(json, "orderId");
    }

    public static Order getOrder(String apiKey, String orderId) throws LocaltsException {
        JsonObject json = execute("GET", "/v1/orders/get-order?id=" + urlEncode(orderId), apiKey);
        List<OrderItem> items = new ArrayList<>();
        JsonElement itemsElement = json.get("items");
        if (itemsElement != null && itemsElement.isJsonArray()) {
            for (JsonElement element : itemsElement.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                items.add(new OrderItem(requiredString(item, "id"), requiredString(item, "content")));
            }
        }
        return new Order(orderId, requiredString(json, "status"), optionalString(json, "product-name"), Collections.unmodifiableList(items));
    }

    public static Order waitForOrder(String apiKey, String orderId, StatusListener status) throws LocaltsException, InterruptedException {
        int consecutiveErrors = 0;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                Order order = getOrder(apiKey, orderId);
                consecutiveErrors = 0;
                if (order.isPackaged()) return order;
                if (status != null) status.update("Order " + orderId + ": " + order.status + "...");
            } catch (LocaltsException e) {
                if (++consecutiveErrors >= 5) {
                    throw new LocaltsException("Could not read order " + orderId + ": " + e.getMessage());
                }
                if (status != null) status.update(e.getMessage());
                Thread.sleep(3000L * consecutiveErrors);
                continue;
            }
            Thread.sleep(2500L);
        }
        throw new LocaltsException("Order " + orderId + " timed out while packaging");
    }

    public static String extractRefreshToken(String content) {
        if (content == null) return "";
        String best = "";
        for (String line : content.split("[\\r\\n]+")) {
            for (String field : line.split("[\\s,;]+")) {
                for (String segment : field.split(":")) {
                    String candidate = strip(segment);
                    if (candidate.startsWith("M.") && candidate.length() > best.length()) best = candidate;
                }
            }
        }
        if (!best.isEmpty()) return best;

        for (String line : content.split("[\\r\\n]+")) {
            for (String field : line.split("[\\s,;]+")) {
                for (String segment : field.split(":")) {
                    String candidate = strip(segment);
                    if (candidate.length() >= 20 && candidate.length() > best.length()) best = candidate;
                }
            }
        }
        return best;
    }

    private static String strip(String value) {
        String stripped = value == null ? "" : value.trim();
        while (stripped.length() > 1 && (stripped.startsWith("\"") || stripped.startsWith("'"))) stripped = stripped.substring(1).trim();
        while (stripped.length() > 1 && (stripped.endsWith("\"") || stripped.endsWith("'") || stripped.endsWith(","))) stripped = stripped.substring(0, stripped.length() - 1).trim();
        return stripped;
    }

    private static JsonObject execute(String method, String path, String apiKey) throws LocaltsException {
        boolean retryable = "GET".equals(method);
        LocaltsException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return executeOnce(method, path, apiKey);
            } catch (RateLimitedException e) {
                last = e;
                if (!retryable || attempt == MAX_ATTEMPTS) break;
                long wait = e.retryAfterMillis > 0L ? e.retryAfterMillis : 1500L * (1L << (attempt - 1));
                try {
                    Thread.sleep(Math.min(wait, 15000L));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new LocaltsException("Interrupted while waiting for Localts");
                }
            }
        }
        throw last;
    }

    private static JsonObject executeOnce(String method, String path, String apiKey) throws LocaltsException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (apiKey != null && !apiKey.trim().isEmpty()) connection.setRequestProperty("X-API-Key", apiKey.trim());
            if ("POST".equals(method)) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(0);
                connection.getOutputStream().close();
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : readAll(stream);

            JsonObject json = new JsonObject();
            try {
                JsonElement parsed = new JsonParser().parse(body);
                if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } catch (RuntimeException ignored) {
            }

            if (status == 429) {
                throw new RateLimitedException(errorMessage(json, "Localts rate limit reached, retrying..."), retryAfterMillis(connection.getHeaderField("Retry-After")));
            }
            if (status == 408 || status == 425 || status >= 500) {
                throw new RateLimitedException(errorMessage(json, "Localts is temporarily unavailable (HTTP " + status + ")"), 0L);
            }
            if (status == 403 && isCloudflareBlock(body)) {
                throw new LocaltsException("Localts rejected this API key at Cloudflare (HTTP 403)");
            }
            if (status < 200 || status >= 300) {
                throw new LocaltsException(errorMessage(json, "Localts request failed (HTTP " + status + ")"));
            }
            if (json.entrySet().isEmpty()) {
                throw new LocaltsException("Localts returned a non-JSON response (HTTP " + status + ")");
            }
            if (!json.has("success") || !json.get("success").getAsBoolean()) {
                throw new LocaltsException(errorMessage(json, "Localts rejected the request"));
            }
            return json;
        } catch (IOException e) {
            throw new LocaltsException("Could not reach Localts: " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static long retryAfterMillis(String header) {
        if (header == null || header.trim().isEmpty()) return 0L;
        try {
            return Math.max(0L, (long) (Double.parseDouble(header.trim()) * 1000.0D));
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static boolean isCloudflareBlock(String body) {
        String value = body == null ? "" : body.toLowerCase(Locale.ROOT);
        return value.contains("cloudflare") && (value.contains("attention required") || value.contains("just a moment"));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (IOException e) {
            return value;
        }
    }

    private static String errorMessage(JsonObject json, String fallback) {
        String message = optionalString(json, "error");
        return message.trim().isEmpty() ? fallback : message;
    }

    private static String requiredString(JsonObject json, String field) throws LocaltsException {
        String value = optionalString(json, field);
        if (value.trim().isEmpty()) throw new LocaltsException("Localts response is missing " + field);
        return value;
    }

    private static String optionalString(JsonObject json, String field) {
        JsonElement value = json.get(field);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static double requiredNumber(JsonObject json, String field) throws LocaltsException {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive()) throw new LocaltsException("Localts response is missing " + field);
        return value.getAsDouble();
    }

    private static int requiredInt(JsonObject json, String field) throws LocaltsException {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive()) throw new LocaltsException("Localts response is missing " + field);
        return value.getAsInt();
    }

    private static JsonArray requiredArray(JsonObject json, String field) throws LocaltsException {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonArray()) throw new LocaltsException("Localts response is missing " + field);
        return value.getAsJsonArray();
    }

    public interface StatusListener {
        void update(String message);
    }

    public static final class User {
        public final String username;
        public final double balance;

        User(String username, double balance) {
            this.username = username;
            this.balance = balance;
        }
    }

    public static final class Product {
        public final String id, name, description, category, type;
        public final double priceInCredits;
        public final int stock;
        public final List<String> tags;
        public final Map<Integer, Double> quantityDiscounts;

        Product(String id, String name, String description, String category, double priceInCredits, int stock, String type,
                List<String> tags, Map<Integer, Double> quantityDiscounts) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.priceInCredits = priceInCredits;
            this.stock = stock;
            this.type = type;
            this.tags = tags;
            this.quantityDiscounts = quantityDiscounts;
        }

        public double discountFor(int amount) {
            double discount = 0.0D;
            for (Map.Entry<Integer, Double> tier : quantityDiscounts.entrySet()) {
                if (amount >= tier.getKey()) discount = Math.max(discount, tier.getValue());
            }
            return Math.max(0.0D, Math.min(100.0D, discount));
        }

        public double totalFor(int amount) {
            return priceInCredits * amount * (1.0D - discountFor(amount) / 100.0D);
        }

        public boolean isRefreshTokenProduct() {
            StringBuilder searchable = new StringBuilder();
            searchable.append(name).append(' ').append(description).append(' ').append(category).append(' ').append(type);
            for (String tag : tags) searchable.append(' ').append(tag);
            String text = searchable.toString().toLowerCase(Locale.ROOT);
            return text.contains("refresh token") || text.contains("refresh-token") || text.contains("oauth token");
        }

        public boolean isCookieProduct() {
            for (String tag : tags) if ("cookie".equalsIgnoreCase(tag.trim())) return true;
            return description.toLowerCase(Locale.ROOT).contains("format of a cookie");
        }

        public boolean isUnbanned() {
            for (String tag : tags) if ("unbanned".equalsIgnoreCase(tag.trim())) return true;
            return name.toLowerCase(Locale.ROOT).contains("unbanned");
        }
    }

    public static final class Order {
        public final String id, status, productName;
        public final List<OrderItem> items;

        Order(String id, String status, String productName, List<OrderItem> items) {
            this.id = id;
            this.status = status;
            this.productName = productName;
            this.items = items;
        }

        public boolean isPackaged() {
            return "PACKAGED".equalsIgnoreCase(status);
        }
    }

    public static final class OrderItem {
        public final String id, content;

        OrderItem(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    public static class LocaltsException extends Exception {
        public LocaltsException(String message) {
            super(message);
        }
    }

    public static final class RateLimitedException extends LocaltsException {
        final long retryAfterMillis;

        RateLimitedException(String message, long retryAfterMillis) {
            super(message);
            this.retryAfterMillis = retryAfterMillis;
        }
    }
}
