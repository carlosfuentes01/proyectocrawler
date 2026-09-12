import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class testeo {

    private static final String DB_URL =
        "jdbc:mysql://base_datos:3306/taller1_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root_password";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);

        server.createContext("/procesar", testeo::handleProcesar);
        server.createContext("/sumar", testeo::handleSumar);

        server.createContext("/crawler/iniciar", testeo::handleCrawlerIniciar);
        server.createContext("/crawler/metricas", testeo::handleCrawlerMetricas);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Servidor Java escuchando en 8083 (con testeo + crawler)");
    }

    static void handleCrawlerIniciar(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            Map<String, String> parametros = queryToMap(exchange.getRequestURI().getQuery());
            int idFuente = Integer.parseInt(parametros.getOrDefault("idFuente", "0"));
            int idPersona = Integer.parseInt(parametros.getOrDefault("idPersona", "0"));
            String urlSemilla = parametros.get("urlSemilla");
            int numeroDeHilos = Integer.parseInt(parametros.getOrDefault("numeroHilos", "2"));

            if (urlSemilla == null || idFuente == 0 || idPersona == 0) {
                enviarError(exchange, 400, "idFuente, idPersona y urlSemilla son requeridos.");
                return;
            }

            responderJson(exchange, "{\"status\":\"CRAWLING_INICIADO\",\"numeroHilos\":" + numeroDeHilos + "}");

            new Thread(() -> ejecutarCrawlingCompleto(idFuente, idPersona, urlSemilla, numeroDeHilos)).start();

        } catch (Exception excepcion) {
            try { enviarError(exchange, 500, excepcion.getMessage()); } catch (IOException ignorado) {}
        }
    }

    private static void ejecutarCrawlingCompleto(int idFuente, int idPersona, String urlSemilla, int numeroDeHilos) {
        PersonaConsultada persona = GestorCrawler.cargarPersonaDesdeBaseDatos(idPersona);
        if (persona == null) {
            System.err.println("No se encontro la persona con id " + idPersona);
            return;
        }

        GestorCrawler gestorCrawler = new GestorCrawler(idFuente, idPersona);
        gestorCrawler.agregarUrlSiEsNueva(urlSemilla, idFuente, idPersona);

        long tiempoInicio = System.currentTimeMillis();

        System.out.println("Iniciando crawling con " + numeroDeHilos + " hilos...");

        Thread[] hilosDelCrawler = new Thread[numeroDeHilos];
        for (int indice = 0; indice < numeroDeHilos; indice++) {
            hilosDelCrawler[indice] = new Thread(new CrawlerWorker(gestorCrawler, persona));
            hilosDelCrawler[indice].setName("worker-" + (indice + 1));
            hilosDelCrawler[indice].start();
        }

        for (int indice = 0; indice < numeroDeHilos; indice++) {
            try {
                hilosDelCrawler[indice].join();
            } catch (InterruptedException ignorado) {}
        }

        long duracionTotalEnMilisegundos = System.currentTimeMillis() - tiempoInicio;
        int totalProcesadas = gestorCrawler.obtenerTotalDocumentosProcesados()
                + gestorCrawler.obtenerTotalDocumentosDescartados();

        GestorCrawler.registrarMetricaConcurrencia(numeroDeHilos, totalProcesadas, duracionTotalEnMilisegundos);

        System.out.println("Crawling terminado: " + totalProcesadas + " urls en "
                + duracionTotalEnMilisegundos + " ms con " + numeroDeHilos + " hilos.");
    }

    static void handleCrawlerMetricas(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            ArrayList<String> filasComoJson = new ArrayList<>();
            try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(
                         "SELECT id_metrica, num_workers, urls_procesadas, duracion_ms, fecha "
                       + "FROM metrica_concurrencia ORDER BY id_metrica DESC LIMIT 20")) {
                ResultSet resultado = sentenciaPreparada.executeQuery();
                while (resultado.next()) {
                    filasComoJson.add("{"
                        + "\"idMetrica\":" + resultado.getInt("id_metrica") + ","
                        + "\"numWorkers\":" + resultado.getInt("num_workers") + ","
                        + "\"urlsProcesadas\":" + resultado.getInt("urls_procesadas") + ","
                        + "\"duracionMs\":" + resultado.getLong("duracion_ms") + ","
                        + "\"fecha\":\"" + resultado.getString("fecha") + "\""
                        + "}");
                }
            }

            String json = "[" + String.join(",", filasComoJson) + "]";
            responderJson(exchange, json);

        } catch (Exception excepcion) {
            try { enviarError(exchange, 500, excepcion.getMessage()); } catch (IOException ignorado) {}
        }
    }

    static void handleProcesar(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            Map<String, String> parametros = queryToMap(exchange.getRequestURI().getQuery());
            float numeroA = parseFloatOrZero(parametros.get("numeroa"));
            float numeroB = parseFloatOrZero(parametros.get("numerob"));

            int idGenerado = crearRegistroPendiente(numeroA, numeroB);
            responderJson(exchange, "{\"status\":\"PENDIENTE\",\"id\":" + idGenerado + "}");

            new Thread(() -> calcularYActualizar(idGenerado, numeroA, numeroB)).start();

        } catch (Exception excepcion) {
            try { enviarError(exchange, 500, excepcion.getMessage()); } catch (IOException ignorado) {}
        }
    }

    static void handleSumar(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            Map<String, String> parametros = queryToMap(exchange.getRequestURI().getQuery());
            float numeroA = parseFloatOrZero(parametros.get("numeroa"));
            float numeroB = parseFloatOrZero(parametros.get("numerob"));
            float resultado = numeroA + numeroB;

            responderJson(exchange, "{\"numeroa\":" + numeroA + ",\"numerob\":" + numeroB
                    + ",\"resultado\":" + resultado + "}");
        } catch (Exception excepcion) {
            try { enviarError(exchange, 500, excepcion.getMessage()); } catch (IOException ignorado) {}
        }
    }

    static int crearRegistroPendiente(float numeroA, float numeroB) throws SQLException {
        String sql = "INSERT INTO test (numeroa, numerob, estado) VALUES (?, ?, 'PENDIENTE')";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentenciaPreparada.setFloat(1, numeroA);
            sentenciaPreparada.setFloat(2, numeroB);
            sentenciaPreparada.executeUpdate();
            try (ResultSet resultado = sentenciaPreparada.getGeneratedKeys()) {
                return resultado.next() ? resultado.getInt(1) : -1;
            }
        }
    }

    static void calcularYActualizar(int id, float numeroA, float numeroB) {
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            int resultado = Math.round(numeroA + numeroB);
            Thread.sleep(1000);
            try (PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(
                    "UPDATE test SET resultado = ?, estado = 'COMPLETADO' WHERE id = ?")) {
                sentenciaPreparada.setInt(1, resultado);
                sentenciaPreparada.setInt(2, id);
                sentenciaPreparada.executeUpdate();
            }
        } catch (Exception excepcion) {
            System.err.println("Error procesando id " + id + ": " + excepcion.getMessage());
        }
    }

    static Map<String, String> queryToMap(String query) {
        Map<String, String> resultado = new HashMap<>();
        if (query == null) return resultado;
        for (String parametro : query.split("&")) {
            String[] partes = parametro.split("=", 2);
            if (partes.length == 2) resultado.put(partes[0], partes[1]);
        }
        return resultado;
    }

    static float parseFloatOrZero(String texto) {
        if (texto == null) return 0f;
        try { return Float.parseFloat(texto); } catch (NumberFormatException excepcion) { return 0f; }
    }

    static void responderJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    static void enviarError(HttpExchange exchange, int codigoEstado, String mensaje) throws IOException {
        String json = "{\"error\":\"" + mensaje.replace("\"", "'") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(codigoEstado, bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }
}