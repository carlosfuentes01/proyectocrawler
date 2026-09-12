import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CrawlerWorker.java
 *
 * Un hilo (Runnable) del crawler. Repite: pide la siguiente url pendiente
 * al GestorCrawler, la descarga, aplica las reglas de descarte (RF5),
 * guarda enlaces nuevos, y sigue hasta que ya no quede nada pendiente
 * o se llegue a los limites (100 documentos / 50 descartados).
 */
public class CrawlerWorker implements Runnable {

    private static final Pattern PATRON_ENLACES =
        Pattern.compile("href=[\"']([^\"'#]+)[\"']", Pattern.CASE_INSENSITIVE);

    private final GestorCrawler gestorCrawler;
    private final PersonaConsultada personaConsultada;

    public CrawlerWorker(GestorCrawler gestorCrawler, PersonaConsultada personaConsultada) {
        this.gestorCrawler = gestorCrawler;
        this.personaConsultada = personaConsultada;
    }

    @Override
    public void run() {
        HttpClient clienteHttp = HttpClient.newHttpClient();
        DocumentoCrawler documentoActual;

        while ((documentoActual = gestorCrawler.obtenerSiguienteDocumentoPendiente()) != null) {
            try {
                HttpRequest peticion = HttpRequest.newBuilder(URI.create(documentoActual.urlDocumento))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("User-Agent", "TallerCrawler/1.0")
                        .GET()
                        .build();

                HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
                String contenidoHtml = respuesta.body();

                // Paso 1: regla explicita de pais (si no dice "Colombia", se descarta ya)
                if (!gestorCrawler.elContenidoMencionaColombia(contenidoHtml)) {
                    gestorCrawler.marcarDocumentoDescartado(documentoActual,
                        "El contenido no menciona explicitamente a Colombia.");
                    continue;
                }

                // Paso 2: RF5, coincidencia con los datos de la persona
                boolean estaRelacionado = gestorCrawler.elContenidoCoincideConLaPersona(contenidoHtml, personaConsultada);

                GestorCrawler.actualizarContenidoYRelacion(
                    documentoActual.idDocumento, extraerTitulo(contenidoHtml), contenidoHtml, estaRelacionado);

                if (!estaRelacionado) {
                    gestorCrawler.marcarDocumentoDescartado(documentoActual,
                        "El contenido no coincide con los datos de la persona (RF5).");
                    continue;
                }

                // Relacionado: queda PROCESADA. La verificacion_identidad y
                // clasificacion_contextual las llenara el agente de IA (proximamente).
                gestorCrawler.marcarDocumentoProcesado(documentoActual);

                // Encolar enlaces nuevos encontrados en esta pagina
                ArrayList<String> enlacesEncontrados = extraerEnlaces(contenidoHtml);
                for (int indice = 0; indice < enlacesEncontrados.size(); indice++) {
                    gestorCrawler.agregarUrlSiEsNueva(
                        enlacesEncontrados.get(indice), documentoActual.idFuente, documentoActual.idPersona);
                }

            } catch (Exception excepcion) {
                gestorCrawler.marcarDocumentoConError(documentoActual);
                System.err.println("Error procesando " + documentoActual.urlDocumento + ": " + excepcion.getMessage());
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
        Matcher coincidencia = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(contenidoHtml);
        return coincidencia.find() ? coincidencia.group(1) : "(sin titulo)";
    }
}
