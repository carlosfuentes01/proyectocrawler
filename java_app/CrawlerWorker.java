import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerWorker implements Runnable {

    private static final Pattern PATRON_ENLACES =
        Pattern.compile("href=[\"']([^\"'#]+)[\"']", Pattern.CASE_INSENSITIVE);

    private final GestorCrawler gestorCrawler;
    private final PersonaConsultada personaConsultada;
    private final AgenteIA agenteIA;

    public CrawlerWorker(GestorCrawler gestorCrawler, PersonaConsultada personaConsultada) {
        this.gestorCrawler = gestorCrawler;
        this.personaConsultada = personaConsultada;
        this.agenteIA = new AgenteIA();
    }

    @Override
    public void run() {
        HttpClient clienteHttp = HttpClient.newHttpClient();

        while (true) {
            if (!gestorCrawler.hayTrabajoPendiente()) break;

            DocumentoCrawler documentoActual = gestorCrawler.obtenerSiguienteDocumentoPendiente();

            if (documentoActual == null) {
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            String nombreHilo = Thread.currentThread().getName();

            gestorCrawler.actualizarEstadoConHilo(
                    documentoActual.idDocumento, "EN_PROCESAMIENTO", null, nombreHilo);

            System.out.println("[" + nombreHilo + "] descargando doc "
                    + documentoActual.idDocumento + ": " + documentoActual.urlDocumento);

            try {
                HttpRequest peticion = HttpRequest.newBuilder(URI.create(documentoActual.urlDocumento))
                        .timeout(java.time.Duration.ofSeconds(15))
                        .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                          + "AppleWebKit/537.36 (KHTML, like Gecko) "
                          + "Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept-Language", "es-CO,es;q=0.9,en;q=0.8")
                        .GET()
                        .build();

                HttpResponse<String> respuesta = clienteHttp.send(
                        peticion, HttpResponse.BodyHandlers.ofString());
                String contenidoHtml = respuesta.body();
                String textoPlano = extraerTextoPlano(contenidoHtml);

                // ============ PASO 1: PRE-FILTRO POR TEXTO ============
                boolean coincideConTexto = gestorCrawler.elContenidoCoincideConLaPersona(
                        textoPlano, personaConsultada);

                String verificacion;
                String clasificacion = "NO_DETERMINADO";

                if (!coincideConTexto) {
                    // El texto no menciona nada de la persona. No perdemos tiempo con IA.
                    verificacion = "PERSONA_DIFERENTE";
                    System.out.println("[" + nombreHilo + "] doc " + documentoActual.idDocumento
                            + " -> PERSONA_DIFERENTE (texto no coincide)");
                } else {
                    // ============ PASO 2: LA IA CONFIRMA ============
                    System.out.println("[" + nombreHilo + "] RF7 consultando IA para doc "
                            + documentoActual.idDocumento + "...");
                    String iaResultado = agenteIA.verificarIdentidad(personaConsultada, textoPlano);

                    // ============ PASO 3: REGLA HIBRIDA ============
                    // Si el texto SI coincide, no dejamos que la IA lo descarte
                    // por un falso negativo. La IA solo puede "subir" la certeza.
                    if ("MISMA_PERSONA".equals(iaResultado)) {
                        verificacion = "MISMA_PERSONA";
                    } else if ("POSIBLE_COINCIDENCIA".equals(iaResultado)) {
                        verificacion = "POSIBLE_COINCIDENCIA";
                    } else {
                        // IA dice PERSONA_DIFERENTE o NO_DETERMINADO
                        // pero el texto SI contiene datos de la persona.
                        // Confiamos en el texto: es al menos posible coincidencia.
                        verificacion = "POSIBLE_COINCIDENCIA";
                    }

                    System.out.println("[" + nombreHilo + "] RF7 doc " + documentoActual.idDocumento
                            + " -> IA=" + iaResultado + " | final=" + verificacion);

                    // ============ PASO 4: RF8 si aplica ============
                    if ("MISMA_PERSONA".equals(verificacion)
                            || "POSIBLE_COINCIDENCIA".equals(verificacion)) {
                        System.out.println("[" + nombreHilo + "] RF8 consultando IA para doc "
                                + documentoActual.idDocumento + "...");
                        clasificacion = agenteIA.clasificarContexto(personaConsultada, textoPlano);
                        System.out.println("[" + nombreHilo + "] RF8 doc " + documentoActual.idDocumento
                                + " -> " + clasificacion);
                    }
                }

                boolean esMismaOPosible =
                        "MISMA_PERSONA".equals(verificacion)
                     || "POSIBLE_COINCIDENCIA".equals(verificacion);

                GestorCrawler.actualizarContenidoYRelacion(
                        documentoActual.idDocumento,
                        extraerTitulo(contenidoHtml),
                        contenidoHtml,
                        esMismaOPosible);

                if (esMismaOPosible) {
                    gestorCrawler.marcarDocumentoProcesado(documentoActual);
                    gestorCrawler.actualizarVerificacionClasificacionYEstado(
                            documentoActual.idDocumento,
                            verificacion,
                            clasificacion,
                            "PROCESADA",
                            null,
                            nombreHilo);

                    ArrayList<String> enlacesEncontrados = extraerEnlaces(contenidoHtml);
                    System.out.println("[" + nombreHilo + "] doc " + documentoActual.idDocumento
                            + " encolando " + enlacesEncontrados.size() + " enlaces");
                    for (int i = 0; i < enlacesEncontrados.size(); i++) {
                        gestorCrawler.agregarUrlSiEsNueva(
                                enlacesEncontrados.get(i),
                                documentoActual.idFuente,
                                documentoActual.idPersona);
                    }
                } else {
                    String motivo = "Descartado: " + verificacion;
                    gestorCrawler.marcarDocumentoDescartado(documentoActual, motivo);
                    gestorCrawler.actualizarVerificacionClasificacionYEstado(
                            documentoActual.idDocumento,
                            verificacion,
                            clasificacion,
                            "DESCARTADA",
                            motivo,
                            nombreHilo);
                }

            } catch (Exception excepcion) {
                gestorCrawler.marcarDocumentoConError(documentoActual);
                gestorCrawler.actualizarVerificacionClasificacionYEstado(
                        documentoActual.idDocumento,
                        "NO_DETERMINADO",
                        "NO_DETERMINADO",
                        "ERROR",
                        excepcion.getMessage(),
                        nombreHilo);
                System.err.println("Error procesando " + documentoActual.urlDocumento
                        + ": " + excepcion.getMessage());
            }
        }

        System.out.println("Worker terminado: " + Thread.currentThread().getName());
    }

    private ArrayList<String> extraerEnlaces(String contenidoHtml) {
        ArrayList<String> enlaces = new ArrayList<>();
        Matcher coincidencia = PATRON_ENLACES.matcher(contenidoHtml);
        while (coincidencia.find()) {
            String enlaceEncontrado = coincidencia.group(1);
            if (enlaceEncontrado.startsWith("http")) {
                enlaces.add(enlaceEncontrado);
            }
        }
        return enlaces;
    }

    private String extraerTitulo(String contenidoHtml) {
        Matcher coincidencia = Pattern.compile("<title>(.*?)</title>",
                Pattern.CASE_INSENSITIVE).matcher(contenidoHtml);
        return coincidencia.find() ? coincidencia.group(1) : "(sin titulo)";
    }

    private String extraerTextoPlano(String html) {
        if (html == null) return "";
        String sinScripts = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        String sinEstilos = sinScripts.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String sinTags = sinEstilos.replaceAll("(?is)<[^>]+>", " ");
        return sinTags.replaceAll("\\s+", " ").trim();
    }
}