package Model; // Paquete que agrupa todos los objetos de datos (POJOs)

public class Cliente {

    // ── ATRIBUTOS ──────────────────────────────────────────────
    // Cada atributo mapea exactamente una columna de la tabla CLIENTE en Oracle
    private int    idCliente;        // PK generada por Oracle (GENERATED ALWAYS AS IDENTITY)
    private String nombreCompleto;   // nombre_completo VARCHAR2(150)
    private String tipoDocumento;    // tipo_documento  VARCHAR2(20) → CC, CE, PA
    private String numeroDocumento;  // numero_documento VARCHAR2(30)
    private String correo;           // correo VARCHAR2(100) — tiene UNIQUE en BD
    private String telefono;         // telefono VARCHAR2(20)

    // ── CONSTRUCTOR VACÍO ──────────────────────────────────────
    // JDBC lo necesita: crea el objeto vacío y luego llena
    // campo por campo usando los setters al leer un ResultSet
    public Cliente() {}

    // ── CONSTRUCTOR COMPLETO (con ID) ──────────────────────────
    // Se usa cuando traes un cliente YA EXISTENTE de la BD
    // El ID viene del ResultSet (Oracle ya lo generó antes)
    public Cliente(int idCliente, String nombreCompleto, String tipoDocumento,
                   String numeroDocumento, String correo, String telefono) {
        this.idCliente       = idCliente;
        this.nombreCompleto  = nombreCompleto;
        this.tipoDocumento   = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.correo          = correo;
        this.telefono        = telefono;
    }

    // ── CONSTRUCTOR SIN ID ─────────────────────────────────────
    // Se usa cuando vas a INSERTAR un cliente nuevo desde la GUI (HU1)
    // No pasas ID porque Oracle lo genera automáticamente con IDENTITY
    public Cliente(String nombreCompleto, String tipoDocumento,
                   String numeroDocumento, String correo, String telefono) {
        this.nombreCompleto  = nombreCompleto;
        this.tipoDocumento   = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.correo          = correo;
        this.telefono        = telefono;
    }

    // ── GETTERS Y SETTERS ──────────────────────────────────────
    // Getters: permiten leer el valor del atributo desde afuera de la clase
    // Setters: permiten modificar el valor — los usa JDBC al mapear ResultSet

    public int getIdCliente()               { return idCliente; }
    public void setIdCliente(int v)         { this.idCliente = v; }

    public String getNombreCompleto()       { return nombreCompleto; }
    public void setNombreCompleto(String v) { this.nombreCompleto = v; }

    public String getTipoDocumento()        { return tipoDocumento; }
    public void setTipoDocumento(String v)  { this.tipoDocumento = v; }

    public String getNumeroDocumento()      { return numeroDocumento; }
    public void setNumeroDocumento(String v){ this.numeroDocumento = v; }

    public String getCorreo()               { return correo; }
    public void setCorreo(String v)         { this.correo = v; }

    public String getTelefono()             { return telefono; }
    public void setTelefono(String v)       { this.telefono = v; }

    // ── TOSTRING ───────────────────────────────────────────────
    // Java llama este metodo automáticamente cuando el objeto
    // se muestra en un JComboBox o se imprime con System.out.println
    // Así el combo muestra "María López (CC: 52334455)"
    // en lugar de "Model.Cliente@7f9fcf7f"
    @Override
    public String toString() {
        return nombreCompleto + " (" + tipoDocumento
                + ": " + numeroDocumento + ")";
    }
}