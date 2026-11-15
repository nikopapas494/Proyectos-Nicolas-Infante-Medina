package Model;

public class Servicio {

    // ── ATRIBUTOS DE LA TABLA SERVICIO ────────────────────────
    private int    idServicio;     // PK
    private String nombre;         // Ej: "Transporte aeropuerto"
    private String descripcion;    // Descripción larga del servicio
    private double costoAdicional; // Precio base del servicio sin impuestos

    // ── ATRIBUTOS CALCULADOS (no son columnas) ─────────────────
    // Se usan al armar el detalle de una inscripción específica
    // cantidad viene de inscxservicio.cantidad
    // impuestoAplicado se calcula en ServicioDAO.calcularImpuesto()
    private int    cantidad;          // Cuántas veces se agrega el servicio
    private double impuestoAplicado;  // Total de impuestos para este servicio

    public Servicio() {}

    public Servicio(int idServicio, String nombre,
                    String descripcion, double costoAdicional) {
        this.idServicio     = idServicio;
        this.nombre         = nombre;
        this.descripcion    = descripcion;
        this.costoAdicional = costoAdicional;
    }

    public int getIdServicio()              { return idServicio; }
    public void setIdServicio(int v)        { this.idServicio = v; }
    public String getNombre()               { return nombre; }
    public void setNombre(String v)         { this.nombre = v; }
    public String getDescripcion()          { return descripcion; }
    public void setDescripcion(String v)    { this.descripcion = v; }
    public double getCostoAdicional()       { return costoAdicional; }
    public void setCostoAdicional(double v) { this.costoAdicional = v; }

    // cantidad e impuestoAplicado se setean desde el DAO
    // al leer inscxservicio con JOIN a servicio
    public int getCantidad()                { return cantidad; }
    public void setCantidad(int v)          { this.cantidad = v; }
    public double getImpuestoAplicado()     { return impuestoAplicado; }
    public void setImpuestoAplicado(double v){ this.impuestoAplicado = v; }

    @Override
    public String toString() { return nombre + " - $" + costoAdicional; }
}