package Dao;

import Model.Experiencia;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExperienciaDAO {

    // ── LISTAR DISPONIBLES ─────────────────────────────────────
    // Trae todas las experiencias con estado='disponible'
    // Usa JOIN para traer nombre del estadio, partido y guía
    // en una sola consulta — evita hacer 3 queries separadas
    public List<Experiencia> listarDisponibles() throws SQLException {
        List<Experiencia> lista = new ArrayList<>();
        String sql = """
            SELECT e.*,
                   est.nombre AS nom_estadio,
                   p.equipo_local || ' vs ' || p.equipo_visitante
                       AS nom_partido,
                   g.nombre AS nom_guia
              FROM experiencia e
              JOIN estadio est ON e.id_estadio        = est.id_estadio
              JOIN partido  p  ON e.id_partido         = p.id_partido
              JOIN guia     g  ON e.id_guia_principal  = g.id_guia
             WHERE e.estado = 'disponible'
             ORDER BY e.fecha_hora
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── BUSCAR POR ID ──────────────────────────────────────────
    // Se usa en HU2 al cargar una inscripción existente
    // necesitamos los datos de la experiencia para recalcular
    public Experiencia buscarPorId(int id) throws SQLException {
        String sql = """
            SELECT e.*,
                   est.nombre AS nom_estadio,
                   p.equipo_local || ' vs ' || p.equipo_visitante
                       AS nom_partido,
                   g.nombre AS nom_guia
              FROM experiencia e
              JOIN estadio est ON e.id_estadio        = est.id_estadio
              JOIN partido  p  ON e.id_partido         = p.id_partido
              JOIN guia     g  ON e.id_guia_principal  = g.id_guia
             WHERE e.id_experiencia = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    // ── MAPEAR ─────────────────────────────────────────────────
    // Convierte fila del ResultSet en objeto Experiencia
    // También llena los atributos extra (estadio, partido, guía)
    // que vienen de los JOINs con alias AS nom_estadio, etc.
    private Experiencia mapear(ResultSet rs) throws SQLException {
        Experiencia e = new Experiencia();
        e.setIdExperiencia(rs.getInt("id_experiencia"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setEstado(rs.getString("estado"));
        e.setPrecioBase(rs.getDouble("precio_base"));
        e.setCuposDisponibles(rs.getInt("cupos_disponibles"));
        // getTimestamp lee columnas TIMESTAMP de Oracle
        e.setFechaHora(rs.getTimestamp("fecha_hora"));
        e.setIdEstadio(rs.getInt("id_estadio"));
        e.setIdPartido(rs.getInt("id_partido"));
        e.setIdGuiaPrincipal(rs.getInt("id_guia_principal"));
        // Atributos extra de los JOINs
        e.setNombreEstadio(rs.getString("nom_estadio"));
        e.setNombrePartido(rs.getString("nom_partido"));
        e.setNombreGuia(rs.getString("nom_guia"));
        return e;
    }
}