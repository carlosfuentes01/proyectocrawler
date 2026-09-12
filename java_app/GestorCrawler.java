import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * GestorCrawler.java
 *
 * Guarda en memoria (ArrayList, recorrido con for/break, nada de
 * estructuras concurrentes complejas) la cola de documentos de UNA
 * ejecucion del crawler, y sincroniza el acceso entre varios hilos
 * con la palabra clave 'synchronized'.
 *
 * Limites de la nota del taller:
 *   MAXIMO_DOCUMENTOS_TOTAL = 100 (procesados + descartados)
 *   MAXIMO_DOCUMENTOS_DESCARTADOS = 50
 */
public class GestorCrawler {

    public static final int MAXIMO_DOCUMENTOS_TOTAL = 100;
    public static final int MAXIMO_DOCUMENTOS_DESCARTADOS = 50;

    private static final String DB_URL =
        "jdbc:mysql://base_datos:3306/taller1_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root_password";

    private final ArrayList<DocumentoCrawler> colaDocumentos = new ArrayList<>();
    private final ArrayList<String> urlsYaVistas = new ArrayList<>();

    private int totalDocumentosProcesados = 0;
    private int totalDocumentosDescartados = 0;

    /**
     * Agrega una URL nueva a la cola SOLO si nunca se habia visto antes.
     * Recorre urlsYaVistas con un for simple; en cuanto encuentra
     * coincidencia, hace break (ya no sigue buscando, esta repetida).
     */
    public synchronized void agregarUrlSiEsNueva(String urlNueva, int idFuente, int idPersona) {
        boolean urlYaExiste = false;

        for (int indice = 0; indice < urlsYaVistas.size(); indice++) {
            if (urlsYaVistas.get(indice).equals(urlNueva)) {
                urlYaExiste = true;
                break; // ya la encontramos, no hace falta seguir recorriendo
            }
        }

        if (urlYaExiste) {
            return; // no se agrega, ya estaba
        }

        // Se respetan los limites: si ya se alcanzo el maximo, no se agregan mas
        if ((totalDocumentosProcesados + totalDocumentosDescartados) >= MAXIMO_DOCUMENTOS_TOTAL) {
            return;
        }

        urlsYaVistas.add(urlNueva);

        DocumentoCrawler nuevoDocumento = new DocumentoCrawler(idFuente, idPersona, urlNueva);
        int idGenerado = insertarDocumentoPendiente(nuevoDocumento);
        nuevoDocumento.idDocumento = idGenerado;

        colaDocumentos.add(nuevoDocumento);
    }

    /**
     * Le entrega a un worker la siguiente URL en estado PENDIENTE.
     * El primer hilo que llama a este metodo despues de desocuparse
     * es el que se queda con la siguiente URL disponible (no hay
     * turnos fijos por hilo).
     * Devuelve null si no hay ninguna pendiente (el worker termina).
     */
    public synchronized DocumentoCrawler obtenerSiguienteDocumentoPendiente() {

        if (totalDocumentosDescartados >= MAXIMO_DOCUMENTOS_DESCARTADOS) {
            return null; // ya se llego al tope de descartados, se detiene el crawling
        }
        if ((totalDocumentosProcesados + totalDocumentosDescartados) >= MAXIMO_DOCUMENTOS_TOTAL) {
            return null; // tope total alcanzado
        }

        for (int indice = 0; indice < colaDocumentos.size(); indice++) {
            DocumentoCrawler documentoActual = colaDocumentos.get(indice);
            if (documentoActual.estadoUrl.equals("PENDIENTE")) {
                documentoActual.estadoUrl = "EN_PROCESAMIENTO";
                actualizarEstadoUrl(documentoActual.idDocumento, "EN_PROCESAMIENTO", null);
                return documentoActual; // se encontro una, se entrega y se para de buscar
            }
        }

        return null; // no hay ninguna PENDIENTE en este momento
    }

    /**
     * Revisa el contenido textual buscando la palabra "colombia"
     * (sin importar mayusculas/minusculas). Si no aparece, el
     * documento se descarta de inmediato (regla explicita del taller).
     */
    public boolean elContenidoMencionaColombia(String contenidoTextual) {
        if (contenidoTextual == null) {
            return false;
        }
        String contenidoEnMinusculas = contenidoTextual.toLowerCase();
        return contenidoEnMinusculas.contains("colombia");
    }

    /**
     * RF5: compara el contenido contra los datos de la persona
     * (nombre, alias, ciudad, profesion, empresa, palabra clave).
     * Con que UNO solo coincida, ya se considera relacionado.
     */
    public boolean elContenidoCoincideConLaPersona(String contenidoTextual, PersonaConsultada persona) {
        if (contenidoTextual == null) {
            return false;
        }
        String contenidoEnMinusculas = contenidoTextual.toLowerCase();

        String[] datosDeLaPersona = {
            persona.nombreCompleto,
            persona.aliasPersona,
            persona.ciudadPersona,
            persona.profesionPersona,
            persona.empresaPersona,
            persona.palabraClavePersona
        };

        for (int indice = 0; indice < datosDeLaPersona.length; indice++) {
            String dato = datosDeLaPersona[indice];
            if (dato != null && !dato.isBlank() && contenidoEnMinusculas.contains(dato.toLowerCase())) {
                return true; // encontro coincidencia, no hace falta seguir comparando
            }
        }
        return false;
    }

    public synchronized void marcarDocumentoProcesado(DocumentoCrawler documento) {
        documento.estadoUrl = "PROCESADA";
        totalDocumentosProcesados++;
        actualizarEstadoUrl(documento.idDocumento, "PROCESADA", null);
    }

    public synchronized void marcarDocumentoDescartado(DocumentoCrawler documento, String motivo) {
        documento.estadoUrl = "DESCARTADA";
        documento.motivoDescarte = motivo;
        totalDocumentosDescartados++;
        actualizarEstadoUrl(documento.idDocumento, "DESCARTADA", motivo);
    }

    public synchronized void marcarDocumentoConError(DocumentoCrawler documento) {
        documento.estadoUrl = "ERROR";
        actualizarEstadoUrl(documento.idDocumento, "ERROR", null);
    }

    public synchronized boolean sePuedeSeguirProcesando() {
        return totalDocumentosDescartados < MAXIMO_DOCUMENTOS_DESCARTADOS
                && (totalDocumentosProcesados + totalDocumentosDescartados) < MAXIMO_DOCUMENTOS_TOTAL;
    }

    public synchronized int obtenerTotalDocumentosProcesados() {
        return totalDocumentosProcesados;
    }

    public synchronized int obtenerTotalDocumentosDescartados() {
        return totalDocumentosDescartados;
    }

    // ---------------- Persistencia en MySQL (JDBC) ----------------

    private int insertarDocumentoPendiente(DocumentoCrawler documento) {
        String sql = "INSERT INTO documento (id_fuente, id_persona, url, estado_url, relacion) "
                + "VALUES (?, ?, ?, 'PENDIENTE', 'NO_RELACIONADO')";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentenciaPreparada.setInt(1, documento.idFuente);
            sentenciaPreparada.setInt(2, documento.idPersona);
            sentenciaPreparada.setString(3, documento.urlDocumento);
            sentenciaPreparada.executeUpdate();
            try (ResultSet resultado = sentenciaPreparada.getGeneratedKeys()) {
                return resultado.next() ? resultado.getInt(1) : -1;
            }
        } catch (Exception excepcion) {
            System.err.println("Error insertando documento pendiente: " + excepcion.getMessage());
            return -1;
        }
    }

    private void actualizarEstadoUrl(int idDocumento, String nuevoEstado, String motivoDescarte) {
        String sql = "UPDATE documento SET estado_url = ?, motivo_descarte = ? WHERE id_documento = ?";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql)) {
            sentenciaPreparada.setString(1, nuevoEstado);
            sentenciaPreparada.setString(2, motivoDescarte);
            sentenciaPreparada.setInt(3, idDocumento);
            sentenciaPreparada.executeUpdate();
        } catch (Exception excepcion) {
            System.err.println("Error actualizando estado del documento: " + excepcion.getMessage());
        }
    }

    public static void actualizarContenidoYRelacion(int idDocumento, String titulo, String contenido,
                                                     boolean estaRelacionado) {
        String sql = "UPDATE documento SET titulo = ?, contenido_textual = ?, relacion = ? WHERE id_documento = ?";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql)) {
            sentenciaPreparada.setString(1, titulo);
            sentenciaPreparada.setString(2, contenido);
            sentenciaPreparada.setString(3, estaRelacionado ? "RELACIONADO" : "NO_RELACIONADO");
            sentenciaPreparada.setInt(4, idDocumento);
            sentenciaPreparada.executeUpdate();
        } catch (Exception excepcion) {
            System.err.println("Error actualizando contenido del documento: " + excepcion.getMessage());
        }
    }

    public static PersonaConsultada cargarPersonaDesdeBaseDatos(int idPersona) {
        String sql = "SELECT id_persona, nombre, alias, ciudad, profesion, empresa, palabra_clave "
                + "FROM persona WHERE id_persona = ?";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql)) {
            sentenciaPreparada.setInt(1, idPersona);
            try (ResultSet resultado = sentenciaPreparada.executeQuery()) {
                if (resultado.next()) {
                    return new PersonaConsultada(
                        resultado.getInt("id_persona"),
                        resultado.getString("nombre"),
                        resultado.getString("alias"),
                        resultado.getString("ciudad"),
                        resultado.getString("profesion"),
                        resultado.getString("empresa"),
                        resultado.getString("palabra_clave")
                    );
                }
            }
        } catch (Exception excepcion) {
            System.err.println("Error cargando persona: " + excepcion.getMessage());
        }
        return null;
    }

    public static void registrarMetricaConcurrencia(int numeroDeHilos, int urlsProcesadas, long duracionEnMilisegundos) {
        String sql = "INSERT INTO metrica_concurrencia (num_workers, urls_procesadas, duracion_ms) VALUES (?, ?, ?)";
        try (Connection conexionBaseDatos = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement sentenciaPreparada = conexionBaseDatos.prepareStatement(sql)) {
            sentenciaPreparada.setInt(1, numeroDeHilos);
            sentenciaPreparada.setInt(2, urlsProcesadas);
            sentenciaPreparada.setLong(3, duracionEnMilisegundos);
            sentenciaPreparada.executeUpdate();
        } catch (Exception excepcion) {
            System.err.println("Error registrando metrica: " + excepcion.getMessage());
        }
    }
}
