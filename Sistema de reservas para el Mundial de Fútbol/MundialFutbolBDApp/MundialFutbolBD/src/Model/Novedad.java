package Model;

import java.sql.Date;

public class Novedad {

    // ── ATRIBUTOS DE LA TABLA NOVEDAD ─────────────────────────
    private int    idNovedad;      // PK
    private int    idInscripcion;  // FK → inscripcion
    private String descripcion;    // Texto de la novedad
    private Date   fechaNovedad;   // DEFAULT SYSDATE en BD — Oracle la pone sola
    private String tipo;           // 'MODIFICACION_SERVICIO','PAGO','OBSERVACION','OTRO'
    private String registradoPor;  // DEFAULT 'SISTEMA' — o el nombre del coordinador

    public Novedad() {}

    // Constructor para insertar novedad manual desde HU2
    // La fecha la pone Oracle automáticamente con DEFAULT SYSDATE
    // por eso no se pasa en el constructor
    public Novedad(int idInscripcion, String descripcion,
                   String tipo, String registradoPor) {
        this.idInscripcion  = idInscripcion;
        this.descripcion    = descripcion;
        this.tipo           = tipo;
        this.registradoPor  = registradoPor;
    }

    public int getIdNovedad()               { return idNovedad; }
    public void setIdNovedad(int v)         { this.idNovedad = v; }
    public int getIdInscripcion()           { return idInscripcion; }
    public void setIdInscripcion(int v)     { this.idInscripcion = v; }
    public String getDescripcion()          { return descripcion; }
    public void setDescripcion(String v)    { this.descripcion = v; }
    public Date getFechaNovedad()           { return fechaNovedad; }
    public void setFechaNovedad(Date v)     { this.fechaNovedad = v; }
    public String getTipo()                 { return tipo; }
    public void setTipo(String v)           { this.tipo = v; }
    public String getRegistradoPor()        { return registradoPor; }
    public void setRegistradoPor(String v)  { this.registradoPor = v; }

    @Override
    public String toString() { return "[" + tipo + "] " + descripcion; }
}