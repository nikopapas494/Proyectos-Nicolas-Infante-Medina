package Model;

public class Impuesto {

    // ── ATRIBUTOS DE LA TABLA IMPUESTO ────────────────────────
    private int    idImpuesto;  // PK
    private String nombre;      // Ej: "IVA México"
    private String tipo;        // 'IVA','Turístico','Local','Otros'
    private double porcentaje;  // Ej: 16.00 → significa 16%

    public Impuesto() {}

    public Impuesto(int idImpuesto, String nombre,
                    String tipo, double porcentaje) {
        this.idImpuesto  = idImpuesto;
        this.nombre      = nombre;
        this.tipo        = tipo;
        this.porcentaje  = porcentaje;
    }

    public int getIdImpuesto()              { return idImpuesto; }
    public void setIdImpuesto(int v)        { this.idImpuesto = v; }
    public String getNombre()               { return nombre; }
    public void setNombre(String v)         { this.nombre = v; }
    public String getTipo()                 { return tipo; }
    public void setTipo(String v)           { this.tipo = v; }
    public double getPorcentaje()           { return porcentaje; }
    public void setPorcentaje(double v)     { this.porcentaje = v; }

    // Muestra "IVA México (16.0%)" en combos o logs
    @Override
    public String toString() { return nombre + " (" + porcentaje + "%)"; }
}