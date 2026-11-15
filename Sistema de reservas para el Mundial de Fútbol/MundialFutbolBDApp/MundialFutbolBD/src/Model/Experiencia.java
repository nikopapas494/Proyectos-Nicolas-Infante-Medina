package Model;

import java.sql.Timestamp; // Tipo de Java para columnas TIMESTAMP de Oracle

public class Experiencia {

    // ── ATRIBUTOS DIRECTOS DE LA TABLA ────────────────────────
    private int       idExperiencia;    // PK
    private String    descripcion;      // Nombre/descripción de la experiencia
    private String    estado;           // 'disponible', 'agotada', 'cancelada'
    private double    precioBase;       // Precio sin servicios ni impuestos
    private int       cuposDisponibles; // Se decrementa con el trigger TRG_VALIDAR_CUPOS
    private Timestamp fechaHora;        // TIMESTAMP en Oracle → Timestamp en Java
    private int       idEstadio;        // FK → tabla estadio
    private int       idPartido;        // FK → tabla partido
    private int       idGuiaPrincipal;  // FK → tabla guia

    // ── ATRIBUTOS EXTRA (NO son columnas de la tabla) ──────────
    // Se llenan con JOINs en el DAO para no hacer consultas extra
    // Solo existen en memoria Java, no en la BD
    private String nombreEstadio; // Viene del JOIN con estadio
    private String nombrePartido; // "Colombia vs Portugal" — se arma en SQL
    private String nombreGuia;    // Nombre del guía principal

    // ── CONSTRUCTOR VACÍO ──────────────────────────────────────
    public Experiencia() {}

    // ── CONSTRUCTOR COMPLETO ───────────────────────────────────
    public Experiencia(int idExperiencia, String descripcion, String estado,
                       double precioBase, int cuposDisponibles,
                       Timestamp fechaHora, int idEstadio,
                       int idPartido, int idGuiaPrincipal) {
        this.idExperiencia    = idExperiencia;
        this.descripcion      = descripcion;
        this.estado           = estado;
        this.precioBase       = precioBase;
        this.cuposDisponibles = cuposDisponibles;
        this.fechaHora        = fechaHora;
        this.idEstadio        = idEstadio;
        this.idPartido        = idPartido;
        this.idGuiaPrincipal  = idGuiaPrincipal;
    }

    // ── GETTERS Y SETTERS ──────────────────────────────────────
    public int getIdExperiencia()              { return idExperiencia; }
    public void setIdExperiencia(int v)        { this.idExperiencia = v; }
    public String getDescripcion()             { return descripcion; }
    public void setDescripcion(String v)       { this.descripcion = v; }
    public String getEstado()                  { return estado; }
    public void setEstado(String v)            { this.estado = v; }
    public double getPrecioBase()              { return precioBase; }
    public void setPrecioBase(double v)        { this.precioBase = v; }
    public int getCuposDisponibles()           { return cuposDisponibles; }
    public void setCuposDisponibles(int v)     { this.cuposDisponibles = v; }
    public Timestamp getFechaHora()            { return fechaHora; }
    public void setFechaHora(Timestamp v)      { this.fechaHora = v; }
    public int getIdEstadio()                  { return idEstadio; }
    public void setIdEstadio(int v)            { this.idEstadio = v; }
    public int getIdPartido()                  { return idPartido; }
    public void setIdPartido(int v)            { this.idPartido = v; }
    public int getIdGuiaPrincipal()            { return idGuiaPrincipal; }
    public void setIdGuiaPrincipal(int v)      { this.idGuiaPrincipal = v; }
    public String getNombreEstadio()           { return nombreEstadio; }
    public void setNombreEstadio(String v)     { this.nombreEstadio = v; }
    public String getNombrePartido()           { return nombrePartido; }
    public void setNombrePartido(String v)     { this.nombrePartido = v; }
    public String getNombreGuia()              { return nombreGuia; }
    public void setNombreGuia(String v)        { this.nombreGuia = v; }

    // toString muestra descripción y precio en combos de la GUI
    @Override
    public String toString() { return descripcion + " - $" + precioBase; }
}