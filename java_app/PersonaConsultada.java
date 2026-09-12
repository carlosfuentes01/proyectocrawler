/**
 * PersonaConsultada.java
 *
 * Datos de la persona (tabla 'persona') que el crawler necesita para
 * comparar contra el contenido de cada documento (RF5, RF7).
 */
public class PersonaConsultada {

    public int idPersona;
    public String nombreCompleto;
    public String aliasPersona;
    public String ciudadPersona;
    public String profesionPersona;
    public String empresaPersona;
    public String palabraClavePersona;

    public PersonaConsultada(int idPersona, String nombreCompleto, String aliasPersona,
                              String ciudadPersona, String profesionPersona,
                              String empresaPersona, String palabraClavePersona) {
        this.idPersona = idPersona;
        this.nombreCompleto = nombreCompleto;
        this.aliasPersona = aliasPersona;
        this.ciudadPersona = ciudadPersona;
        this.profesionPersona = profesionPersona;
        this.empresaPersona = empresaPersona;
        this.palabraClavePersona = palabraClavePersona;
    }
}
