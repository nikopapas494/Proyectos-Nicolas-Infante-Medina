package Dao;

import Model.Novedad;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NovedadDAO {

    // ── INSERTAR NOVEDAD MANUAL ────────────────────────────────
    // Se usa en HU2 cuando el coordinador registra una observación
    // manualmente — ej: "cliente requiere asistencia de movilidad"
    // Las novedades automáticas (PAGO, MODIFICACION_SERVICIO)
    // las insertan directamente los triggers en Oracle
    // sin pasar por este metodo
    public void insertar(Novedad n) throws SQLException {
        String sql = """
            INSERT INTO novedad
                (id_inscripcion, descripcion, tipo, registrado_por)
            VALUES (?, ?, ?, ?)
            """;
        // Nota: no se inserta fecha_novedad porque Oracle
        // la pone automáticamente con DEFAULT SYSDATE
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, n.getIdInscripcion());
            ps.setString(2, n.getDescripcion());
            ps.setString(3, n.getTipo());
            ps.setString(4, n.getRegistradoPor());
            ps.executeUpdate();
        }
    }

    // ── LISTAR POR INSCRIPCION ─────────────────────────────────
    // Trae TODAS las novedades de una inscripción
    // incluyendo las automáticas de los triggers (PAGO,
    // MODIFICACION_SERVICIO) y las manuales (OBSERVACION, OTRO)
    // ORDER BY fecha DESC → las más recientes primero
    public List<Novedad> listarPorInscripcion(int idIns) throws SQLException {
        List<Novedad> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM novedad
             WHERE id_inscripcion = ?
             ORDER BY fecha_novedad DESC
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
    private Novedad mapear(ResultSet rs) throws SQLException {
        Novedad n = new Novedad();
        n.setIdNovedad(rs.getInt("id_novedad"));
        n.setIdInscripcion(rs.getInt("id_inscripcion"));
        n.setDescripcion(rs.getString("descripcion"));
        // getDate lee la fecha_novedad que Oracle puso con SYSDATE
        n.setFechaNovedad(rs.getDate("fecha_novedad"));
        n.setTipo(rs.getString("tipo"));
        n.setRegistradoPor(rs.getString("registrado_por"));
        return n;
    }
}