package Dao;

import Model.Servicio;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioDAO {

    // ── LISTAR POR EXPERIENCIA ─────────────────────────────────
    // Trae los servicios disponibles para UNA experiencia específica
    // Se usa en HU1 al seleccionar experiencia → muestra servicios opcionales
    // JOIN con srvxexperiencia filtra solo los servicios de esa experiencia
    public List<Servicio> listarPorExperiencia(int idExp) throws SQLException {
        List<Servicio> lista = new ArrayList<>();
        String sql = """
            SELECT s.*
              FROM servicio s
              JOIN srvxexperiencia sxe
                ON s.id_servicio = sxe.id_servicio
             WHERE sxe.id_experiencia = ?
             ORDER BY s.nombre
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idExp);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── LISTAR POR INSCRIPCION ─────────────────────────────────
    // Trae los servicios que YA están inscritos en una inscripción
    // Se usa en HU2 para mostrar qué servicios tiene actualmente
    // Lee cantidad e impuesto_aplicado de la tabla inscxservicio
    public List<Servicio> listarPorInscripcion(int idIns) throws SQLException {
        List<Servicio> lista = new ArrayList<>();
        String sql = """
            SELECT s.*, ixs.cantidad, ixs.impuesto_aplicado
              FROM servicio s
              JOIN inscxservicio ixs
                ON s.id_servicio = ixs.id_servicio
             WHERE ixs.id_inscripcion = ?
             ORDER BY s.nombre
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idIns);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Servicio s = mapear(rs);
                // Llenar atributos extra que vienen de inscxservicio
                s.setCantidad(rs.getInt("cantidad"));
                s.setImpuestoAplicado(rs.getDouble("impuesto_aplicado"));
                lista.add(s);
            }
        }
        return lista;
    }

    // ── CALCULAR IMPUESTO ──────────────────────────────────────
    // Calcula el total de impuestos para UN servicio
    // SUM(porcentaje) suma todos los impuestos que aplican al servicio
    // Se usa en InscripcionService.calcularTotal() al armar el resumen
    public double calcularImpuesto(int idServicio,
                                   double costoBase) throws SQLException {
        String sql = """
            SELECT SUM(i.porcentaje) AS total_pct
              FROM impuesto i
              JOIN srvximpuesto sxi
                ON i.id_impuesto = sxi.id_impuesto
             WHERE sxi.id_servicio = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idServicio);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double pct = rs.getDouble("total_pct");
                // Fórmula: costoBase * porcentaje / 100
                // Ej: 150000 * 21.0 / 100 = 31500
                return costoBase * pct / 100.0;
            }
        }
        return 0; // Si no tiene impuestos retorna 0
    }

    // ── MAPEAR ─────────────────────────────────────────────────
    private Servicio mapear(ResultSet rs) throws SQLException {
        Servicio s = new Servicio();
        s.setIdServicio(rs.getInt("id_servicio"));
        s.setNombre(rs.getString("nombre"));
        s.setDescripcion(rs.getString("descripcion"));
        s.setCostoAdicional(rs.getDouble("costo_adicional"));
        return s;
    }
}