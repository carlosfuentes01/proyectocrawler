/**
 * DocumentoCrawler.java
 *
 * Representa UNA url/documento dentro del proceso del crawler, desde que
 * se descubre hasta que queda PROCESADA o DESCARTADA. Es solo un
 * contenedor de datos (sin logica adentro) para que sea facil de leer:
 * toda la logica vive en GestorCrawler y CrawlerWorker.
 *
 * Estos mismos campos son los que se le pasaran al agente de IA cuando
 * se agregue: verificacionIdentidad y clasificacionContextual.
 */
public class DocumentoCrawler {

    public int idDocumento;              // -1 mientras no se ha insertado en MySQL
    public int idFuente;
    public int idPersona;

    public String urlDocumento;
    public String tituloDocumento;
    public String paisDocumento;
    public String contenidoTextual;

    // Valores posibles: PENDIENTE, EN_PROCESAMIENTO, PROCESADA, DESCARTADA, ERROR
    public String estadoUrl;

    // Se llenan cuando exista el agente de IA (por ahora quedan NO_DETERMINADO)
    public String verificacionIdentidad;
    public String clasificacionContextual;

    public String motivoDescarte; // solo se llena si estadoUrl = DESCARTADA

    public DocumentoCrawler(int idFuente, int idPersona, String urlDocumento) {
        this.idDocumento = -1;
        this.idFuente = idFuente;
        this.idPersona = idPersona;
        this.urlDocumento = urlDocumento;
        this.estadoUrl = "PENDIENTE";
        this.verificacionIdentidad = "NO_DETERMINADO";
        this.clasificacionContextual = "NO_DETERMINADO";
        this.motivoDescarte = null;
    }
}
