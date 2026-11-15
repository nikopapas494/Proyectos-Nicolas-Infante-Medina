package Model;

import java.sql.Date;
import java.util.List; // Para la lista de servicios asociados

public class Inscripcion {

    // ── ATRIBUTOS DE LA TABLA INSCRIPCION ─────────────────────
    private int    idInscripcion;    // PK
    private String numReserva;       // Código único ej: "RES-2026-001"
    private Date   fechaInscripcion; // java.sql.Date mapea DATE de Oracle
    private double costoTotal;       // Precio base + servicios + impuestos
    private double saldoPendiente;   // Se actualiza con TRG_ACTUALIZAR_SALDO
    private int    idExperiencia;    // FK → experiencia
    private int    idCliente;        // FK → cliente

    // ── ATRIBUTOS EXTRA (vienen de JOINs en el DAO) ───────────
    // Se usan para mostrar info en la GUI sin queries adicionales
    private String        nombreCliente;          // De JOIN con cliente
    private String        descripcionExperiencia; // De JOIN con experiencia
    private List<Servicio> servicios;             // Lista de servicios inscritos

    public Inscripcion() {}

    // Constructor sin ID — para crear inscripción nueva desde HU1
    public Inscripcion(String numReserva, Date fechaInscripcion,
                       double costoTotal, double saldoPendiente,
                       int idExperiencia, int idCliente) {
        this.numReserva        = numReserva;
        this.fechaInscripcion  = fechaInscripcion;
        this.costoTotal        = costoTotal;
        this.saldoPendiente    = saldoPendiente;
        this.idExperiencia     = idExperiencia;
        this.idCliente         = idCliente;
    }

    public int getIdInscripcion()                   { return idInscripcion; }
    public void setIdInscripcion(int v)             { this.idInscripcion = v; }
    public String getNumReserva()                   { return numReserva; }
    public void setNumReserva(String v)             { this.numReserva = v; }
    public Date getFechaInscripcion()               { return fechaInscripcion; }
    public void setFechaInscripcion(Date v)         { this.fechaInscripcion = v; }
    public double getCostoTotal()                   { return costoTotal; }
    public void setCostoTotal(double v)             { this.costoTotal = v; }
    public double getSaldoPendiente()               { return saldoPendiente; }
    public void setSaldoPendiente(double v)         { this.saldoPendiente = v; }
    public int getIdExperiencia()                   { return idExperiencia; }
    public void setIdExperiencia(int v)             { this.idExperiencia = v; }
    public int getIdCliente()                       { return idCliente; }
    public void setIdCliente(int v)                 { this.idCliente = v; }
    public String getNombreCliente()                { return nombreCliente; }
    public void setNombreCliente(String v)          { this.nombreCliente = v; }
    public String getDescripcionExperiencia()       { return descripcionExperiencia; }
    public void setDescripcionExperiencia(String v) { this.descripcionExperiencia = v; }

    // La lista de servicios se llena en el Service después de buscar
    // la inscripción — no viene directamente del ResultSet
    public List<Servicio> getServicios()            { return servicios; }
    public void setServicios(List<Servicio> v)      { this.servicios = v; }

    @Override
    public String toString() { return numReserva + " - " + nombreCliente; }
}