import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AgenteIA {

    private static final String OLLAMA_URL =
        System.getenv().getOrDefault("AGENTE_IA_URL", "http://servicio_ia:11434");

    private static final String MODELO =
        System.getenv().getOrDefault("AGENTE_IA_MODELO", "llama3.2:1b");

    private static final int TIMEOUT_SEGUNDOS = 180;

    private final HttpClient http;

    public AgenteIA() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ==================== RF7 ====================

    public String verificarIdentidad(PersonaConsultada persona, String contenidoDocumento) {
        String system = ""
            + "Eres un clasificador. Respondes UNICAMENTE con UNA de estas 4 etiquetas exactas:\n"
            + "MISMA_PERSONA\n"
            + "POSIBLE_COINCIDENCIA\n"
            + "PERSONA_DIFERENTE\n"
            + "NO_DETERMINADO\n"
            + "No agregues nada mas. Solo la etiqueta.";

        String user = ""
            + "Persona objetivo:\n"
            + "- nombre: " + safe(persona.nombreCompleto) + "\n"
            + "- alias: " + safe(persona.aliasPersona) + "\n"
            + "- ciudad: " + safe(persona.ciudadPersona) + "\n"
            + "- profesion: " + safe(persona.profesionPersona) + "\n"
            + "- empresa: " + safe(persona.empresaPersona) + "\n"
            + "- palabra clave: " + safe(persona.palabraClavePersona) + "\n\n"
            + "Documento:\n\"\"\"\n" + truncar(contenidoDocumento, 2500) + "\n\"\"\"\n\n"
            + "Pregunta: el documento trata sobre la persona objetivo?\n"
            + "Responde con UNA etiqueta:";

        return normalizarVerificacion(llamarOllama(system, user));
    }

    // ==================== RF8 ====================

    public String clasificarContexto(PersonaConsultada persona, String contenidoDocumento) {
        String system = ""
            + "Eres un clasificador. Respondes UNICAMENTE con UNA de estas 4 etiquetas exactas:\n"
            + "POSITIVO\n"
            + "NEUTRO\n"
            + "NEGATIVO\n"
            + "NO_DETERMINADO\n"
            + "No agregues nada mas. Solo la etiqueta.";

        String user = ""
            + "Persona: " + safe(persona.nombreCompleto) + "\n\n"
            + "Documento:\n\"\"\"\n" + truncar(contenidoDocumento, 2500) + "\n\"\"\"\n\n"
            + "El contexto en el que aparece la persona es positivo, neutro o negativo?\n"
            + "Responde con UNA etiqueta:";

        return normalizarClasificacion(llamarOllama(system, user));
    }

    // ==================== Interno ====================

    private String llamarOllama(String system, String user) {
        String body = "{"
            + "\"model\":\"" + escaparJson(MODELO) + "\","
            + "\"stream\":false,"
            + "\"options\":{\"temperature\":0.0,\"num_predict\":10},"
            + "\"messages\":["
            +   "{\"role\":\"system\",\"content\":\"" + escaparJson(system) + "\"},"
            +   "{\"role\":\"user\",\"content\":\"" + escaparJson(user) + "\"}"
            + "]"
            + "}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL + "/api/chat"))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEGUNDOS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return "NO_DETERMINADO";
            return extraerContent(res.body());
        } catch (Exception e) {
            return "NO_DETERMINADO";
        }
    }

    private String extraerContent(String json) {
        if (json == null) return "NO_DETERMINADO";
        int idx = json.indexOf("\"content\":\"");
        if (idx < 0) return "NO_DETERMINADO";
        int ini = idx + "\"content\":\"".length();
        int fin = ini;
        while (fin < json.length()) {
            char c = json.charAt(fin);
            if (c == '\\') { fin += 2; continue; }
            if (c == '"') break;
            fin++;
        }
        String content = json.substring(ini, fin);
        content = content.replace("\\n", "\n")
                         .replace("\\r", "\r")
                         .replace("\\t", "\t")
                         .replace("\\\"", "\"")
                         .replace("\\\\", "\\");
        return content;
    }

    private static String escaparJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "(no especificado)" : s;
    }

    private static String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String normalizarVerificacion(String v) {
        if (v == null) return "NO_DETERMINADO";
        String u = v.trim().toUpperCase().replace(" ", "_");
        if (u.contains("MISMA_PERSONA")) return "MISMA_PERSONA";
        if (u.contains("POSIBLE_COINCIDENCIA")) return "POSIBLE_COINCIDENCIA";
        if (u.contains("PERSONA_DIFERENTE")) return "PERSONA_DIFERENTE";
        return "NO_DETERMINADO";
    }

    private static String normalizarClasificacion(String c) {
        if (c == null) return "NO_DETERMINADO";
        String u = c.trim().toUpperCase();
        if (u.contains("POSITIVO")) return "POSITIVO";
        if (u.contains("NEGATIVO")) return "NEGATIVO";
        if (u.contains("NEUTRO")) return "NEUTRO";
        return "NO_DETERMINADO";
    }
}