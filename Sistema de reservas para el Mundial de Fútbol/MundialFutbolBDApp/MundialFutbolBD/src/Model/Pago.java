package Model;

import java.sql.Date;

public class Pago {

    // ── ATRIBUTOS DE LA TABLA PAGO ────────────────────────────
    private int    idPago;         // PK
    private int    idInscripcion;  // FK → inscripcion
    private String referencia;     // Código único del pago ej: "REF-001-2026"
    private double valorPagado;    // Cuánto pagó en este pago
    private Date   fechaPago;      // java.sql.Date mapea DATE de Oracle
    private String tipoPago;       // 'crédito','débito','efectivo','billetera digital'

    public Pago() {}

    // Constructor sin ID — para registrar pago nuevo desde HU1
    // Al insertar en BD el trigger TRG_ACTUALIZAR_SALDO
    // se dispara automáticamente y descuenta del saldo_pendiente
    public Pago(int idInscripcion, String referencia,
                double valorPagado, Date fechaPago, String tipoPago) {
        this.idInscripcion = idInscripcion;
        this.referencia    = referencia;
        this.valorPagado   = valorPagado;
        this.fechaPago     = fechaPago;
        this.tipoPago      = tipoPago;
    }

    public int getIdPago()              { return idPago; }
    public void setIdPago(int v)        { this.idPago = v; }
    public int getIdInscripcion()       { return idInscripcion; }
    public void setIdInscripcion(int v) { this.idInscripcion = v; }
    public String getReferencia()       { return referencia; }
    public void setReferencia(String v) { this.referencia = v; }
    public double getValorPagado()      { return valorPagado; }
    public void setValorPagado(double v){ this.valorPagado = v; }
    public Date getFechaPago()          { return fechaPago; }
    public void setFechaPago(Date v)    { this.fechaPago = v; }
    public String getTipoPago()         { return tipoPago; }
    public void setTipoPago(String v)   { this.tipoPago = v; }

    @Override
    public String toString() { return referencia + " - $" + valorPagado; }
}