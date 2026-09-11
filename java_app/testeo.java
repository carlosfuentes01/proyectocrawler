
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class testeo {

    // Hostname 'base_datos' = nombre del servicio en compose_taller1.yaml
    private static final String DB_URL =
        "jdbc:mysql://base_datos:3306/taller1_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root_password";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);

        // Flujo asincrono (lo usa calcular.php)
        server.createContext("/procesar", testeo::handleProcesar);
        // Alias directo para pruebas sin BD
        server.createContext("/sumar",    testeo::handleSumar);

        // Executor real: evita que com.sun.net.httpserver cierre conexiones raras
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.start();
        System.out.println("Servidor Java escuchando en 8083 (flujo asincrono con MySQL)");
    }

    // ---------- /procesar : flujo con BD ----------
    static void handleProcesar(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarError(exchange, 405, "Solo se acepta GET.");
                return;
            }

            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String numeroaStr = params.get("numeroa");
            String numerobStr = params.get("numerob");

            if (numeroaStr == null || numerobStr == null) {
                enviarError(exchange, 400, "Parametros 'numeroa' y 'numerob' requeridos.");
                return;
            }

            float numA, numB;
            try {
                numA = Float.parseFloat(numeroaStr);
                numB = Float.parseFloat(numerobStr);
            } catch (NumberFormatException nfe) {
                enviarError(exchange, 400, "numeroa/numerob deben ser numericos.");
                return;
            }

            int idGenerado = crearRegistroPendiente(numA, numB);

            String json = "{\"status\":\"PENDIENTE\",\"id\":" + idGenerado + "}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            final int id = idGenerado;
            new Thread(() -> calcularYActualizar(id, numA, numB)).start();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                enviarError(exchange, 500, "Error al crear el registro: " + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    // ---------- /sumar : sin BD, respuesta directa ----------
    static void handleSumar(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            float numA = parseFloatOrZero(params.get("numeroa"));
            float numB = parseFloatOrZero(params.get("numerob"));
            float resultado = numA + numB;

            System.out.println("[/sumar] numeroa=" + numA + " numerob=" + numB + " -> " + resultado);

            String json = "{"
                    + "\"id\":0,"
                    + "\"numeroa\":" + numA + ","
                    + "\"numerob\":" + numB + ","
                    + "\"resultado\":" + resultado + ","
                    + "\"estado\":\"COMPLETADO\""
                    + "}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                enviarError(exchange, 500, "Error: " + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    // ---------- Helpers BD ----------
    static int crearRegistroPendiente(float numA, float numB) throws SQLException {
        String sql = "INSERT INTO test (numeroa, numerob, estado) VALUES (?, ?, 'PENDIENTE')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setFloat(1, numA);
            stmt.setFloat(2, numB);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    static void calcularYActualizar(int id, float numA, float numB) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            int resultado = Math.round(numA + numB);
            Thread.sleep(1000); // simula trabajo pesado

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE test SET resultado = ?, estado = 'COMPLETADO' WHERE id = ?")) {
                stmt.setInt(1, resultado);
                stmt.setInt(2, id);
                stmt.executeUpdate();
            }
            System.out.println("[worker] Completado id=" + id + " -> " + resultado);
        } catch (Exception e) {
            System.err.println("Error procesando id " + id + ": " + e.getMessage());
            marcarError(id);
        }
    }

    static void marcarError(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE test SET estado = 'ERROR' WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ---------- Utilidades ----------
    static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=", 2);
            if (entry.length == 2) result.put(entry[0], entry[1]);
        }
        return result;
    }

    static float parseFloatOrZero(String s) {
        if (s == null) return 0f;
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return 0f; }
    }

    static void enviarError(HttpExchange exchange, int statusCode, String mensaje) throws IOException {
        String json = "{\"error\":\"" + mensaje.replace("\"", "'") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}