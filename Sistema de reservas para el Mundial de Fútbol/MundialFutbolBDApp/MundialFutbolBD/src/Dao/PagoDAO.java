package Dao;

import Model.Pago;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

    // ── INSERTAR PAGO ──────────────────────────────────────────
    // Registra un pago en la tabla PAGO
    // Al ejecutarse este INSERT Oracle dispara automáticamente
    // el trigger TRG_ACTUALIZAR_SALDO que:
    //   1. Lee el saldo_pendiente actual de la inscripción
    //   2. Resta el valor_pagado
    //   3. Actualiza inscripcion.saldo_pendiente
    //   4. Inserta una novedad de tipo 'PAGO' en la tabla novedad
    public void insertar(Pago p) throws SQLException {
        String sql = """
            INSERT INTO pago
                (id_inscripcion, referencia, valor_pagado,
                 fecha_pago, tipo_pago)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, p.getIdInscripcion());
            ps.setString(2, p.getReferencia());
            ps.setDouble(3, p.getValorPagado());
            ps.setDate(4, p.getFechaPago());
            ps.setString(5, p.getTipoPago());
            ps.executeUpdate(); // Aquí se dispara TRG_ACTUALIZAR_SALDO
        }
    }

    // ── LISTAR POR INSCRIPCION ─────────────────────────────────
    // Trae todos los pagos de una inscripción ordenados por fecha
    // Se usa en HU2 en la pestaña "Pagos e impuestos"
    // para mostrar el detalle de pagos para impresión
    public List<Pago> listarPorInscripcion(int idIns) throws SQLException {
        List<Pago> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM pago
             WHERE id_inscripcion = ?
             ORDER BY fecha_pago
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idIns);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── MAPEAR ─────────────────────────────────────────────────
    private Pago mapear(ResultSet rs) throws SQLException {
        Pago p = new Pago();
        p.setIdPago(rs.getInt("id_pago"));
        p.setIdInscripcion(rs.getInt("id_inscripcion"));
        p.setReferencia(rs.getString("referencia"));
        p.setValorPagado(rs.getDouble("valor_pagado"));
        p.setFechaPago(rs.getDate("fecha_pago"));
        p.setTipoPago(rs.getString("tipo_pago"));
        return p;
    }
}