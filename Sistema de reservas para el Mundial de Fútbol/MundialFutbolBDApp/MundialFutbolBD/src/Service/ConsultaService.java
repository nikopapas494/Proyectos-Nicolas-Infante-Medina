package Service;

import Util.Conexion;
import java.sql.*;
import java.util.*;

public class ConsultaService {

    // =========================================================
    //  CONSULTA 1 — EXPERIENCIAS POR FILTRO
    //  Recibe el tipo de filtro y el valor a buscar
    //  Usa switch para armar la condición WHERE correcta
    //  Maneja ciudad, estadio y partido en un solo metodo
    // =========================================================
    public List<Map<String, Object>> experienciasPorFiltro(
            String filtro, String valor) throws SQLException {

        // switch expression arma el WHERE según el filtro elegido
        String condicion = switch (filtro) {
            case "ciudad"  -> "est.ciudad = ?";
            case "estadio" -> "est.nombre = ?";
            // Para partido necesita OR porque el equipo puede
            // ser local O visitante
            case "partido" -> "p.equipo_local = ? OR p.equipo_visitante = ?";
            default        -> "1=1"; // Sin filtro → trae toddo
        };

        String sql = """
            SELECT e.descripcion, e.estado, e.precio_base,
                   e.cupos_disponibles,
                   est.nombre AS estadio, est.ciudad, est.pais,
                   p.equipo_local || ' vs ' || p.equipo_visitante
                       AS partido,
                   e.fecha_hora
              FROM experiencia e
              JOIN estadio est ON e.id_estadio = est.id_estadio
              JOIN partido  p  ON e.id_partido  = p.id_partido
             WHERE e.estado = 'disponible'
               AND (""" + condicion + ")" + """
             ORDER BY est.ciudad, e.fecha_hora
            """;

        List<Map<String, Object>> resultado = new ArrayList<>();
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setString(1, valor);
            // Si es partido necesita dos parámetros para el OR
            if (filtro.equals("partido")) ps.setString(2, valor);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) resultado.add(mapearFila(rs));
        }
        return resultado;
    }

    // =========================================================
    //  CONSULTA 2 — CLIENTES CON MÁS INSCRIPCIONES
    //  COUNT agrupa por cliente y cuenta inscripciones
    //  ORDER BY total DESC → el que más tiene primero
    // =========================================================
    public List<Map<String, Object>> clientesConMasInscripciones()
            throws SQLException {
        String sql = """
            SELECT c.nombre_completo, c.correo,
                   COUNT(i.id_inscripcion) AS total_inscripciones,
                   SUM(i.costo_total)      AS gasto_total
              FROM cliente c
              JOIN inscripcion i ON c.id_cliente = i.id_cliente
             GROUP BY c.id_cliente, c.nombre_completo, c.correo
             ORDER BY total_inscripciones DESC, gasto_total DESC
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  CONSULTA 3 — INGRESOS POR GUÍA
    //  Cuenta experiencias lideradas e inscripciones generadas
    //  SUM(costo_total) = ingresos totales que ese guía generó
    // =========================================================
    public List<Map<String, Object>> ingresosPorGuia()
            throws SQLException {
        String sql = """
            SELECT g.nombre, g.nivel, g.valor_hora,
                   COUNT(DISTINCT ex.id_experiencia)
                       AS experiencias_lideradas,
                   COUNT(i.id_inscripcion)
                       AS total_inscripciones,
                   SUM(i.costo_total) AS ingresos_generados
              FROM guia g
              JOIN experiencia ex
                ON g.id_guia = ex.id_guia_principal
              JOIN inscripcion i
                ON ex.id_experiencia = i.id_experiencia
             GROUP BY g.id_guia, g.nombre, g.nivel, g.valor_hora
             ORDER BY ingresos_generados DESC
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  CONSULTA 4 — EXPERIENCIAS MÁS DEMANDADAS
    //  LEFT JOIN con inscripcion para incluir experiencias
    //  sin inscripciones (COUNT = 0) — así aparecen todas
    // =========================================================
    public List<Map<String, Object>> experienciasMasDemandadas()
            throws SQLException {
        String sql = """
            SELECT ex.descripcion, ex.estado, ex.precio_base,
                   est.ciudad, est.pais,
                   p.equipo_local || ' vs ' || p.equipo_visitante
                       AS partido,
                   COUNT(i.id_inscripcion)  AS total_inscripciones,
                   SUM(i.costo_total)       AS ingresos_experiencia
              FROM experiencia ex
              JOIN estadio  est ON ex.id_estadio = est.id_estadio
              JOIN partido  p   ON ex.id_partido  = p.id_partido
              LEFT JOIN inscripcion i
                ON ex.id_experiencia = i.id_experiencia
             GROUP BY ex.id_experiencia, ex.descripcion, ex.estado,
                      ex.precio_base, est.ciudad, est.pais,
                      p.equipo_local, p.equipo_visitante
             ORDER BY total_inscripciones DESC
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  CONSULTA 5 — IMPUESTOS POR PAÍS SEDE
    //  Agrupa ingresos por país de los 3 países sede
    //  WHERE filtra solo México, EEUU y Canadá
    // =========================================================
    public List<Map<String, Object>> impuestosPorPais()
            throws SQLException {
        String sql = """
            SELECT est.pais,
                   COUNT(DISTINCT ex.id_experiencia)
                       AS total_experiencias,
                   COUNT(i.id_inscripcion)
                       AS total_inscripciones,
                   SUM(i.costo_total)          AS ingresos_totales,
                   ROUND(AVG(i.costo_total),2) AS ingreso_promedio
              FROM estadio est
              JOIN experiencia ex
                ON est.id_estadio = ex.id_estadio
              JOIN inscripcion i
                ON ex.id_experiencia = i.id_experiencia
             WHERE est.pais IN ('México','Estados Unidos','Canadá')
             GROUP BY est.pais
             ORDER BY ingresos_totales DESC
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  CONSULTA 6 — CLIENTES CON PAGOS PENDIENTES
    //  WHERE saldo_pendiente > 0 filtra solo los que deben algo
    //  ORDER BY saldo DESC → el que más debe primero
    // =========================================================
    public List<Map<String, Object>> clientesConPagosPendientes()
            throws SQLException {
        String sql = """
            SELECT c.nombre_completo, c.correo, c.telefono,
                   i.num_reserva,
                   ex.descripcion AS experiencia,
                   i.costo_total, i.saldo_pendiente,
                   i.fecha_inscripcion
              FROM cliente c
              JOIN inscripcion  i
                ON c.id_cliente = i.id_cliente
              JOIN experiencia  ex
                ON i.id_experiencia = ex.id_experiencia
             WHERE i.saldo_pendiente > 0
             ORDER BY i.saldo_pendiente DESC
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  CONSULTA 7 — CUPOS DISPONIBLES POR EXPERIENCIA
    //  Muestra TODAS las experiencias con sus cupos actuales
    //  Los cupos se decrementan con TRG_VALIDAR_CUPOS
    //  ORDER BY cupos DESC → las que tienen más espacio primero
    // =========================================================
    public List<Map<String, Object>> cuposDisponibles()
            throws SQLException {
        String sql = """
            SELECT ex.descripcion, ex.estado, ex.precio_base,
                   ex.cupos_disponibles AS cupos_restantes,
                   est.nombre AS estadio, est.ciudad,
                   p.equipo_local || ' vs ' || p.equipo_visitante
                       AS partido,
                   ex.fecha_hora
              FROM experiencia ex
              JOIN estadio est ON ex.id_estadio = est.id_estadio
              JOIN partido  p  ON ex.id_partido  = p.id_partido
             ORDER BY ex.cupos_disponibles DESC, ex.fecha_hora
            """;
        return ejecutarConsulta(sql);
    }

    // =========================================================
    //  HELPER — EJECUTAR CONSULTA GENÉRICA
    //  Ejecuta cualquier SQL sin parámetros y retorna
    //  una lista de mapas donde cada mapa es una fila
    //  key = nombre columna, value = valor de la celda
    //  Esto permite mostrar cualquier resultado en un JTable
    //  genérico sin necesidad de crear clases específicas
    // =========================================================
    private List<Map<String, Object>> ejecutarConsulta(String sql)
            throws SQLException {
        List<Map<String, Object>> resultado = new ArrayList<>();
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // ResultSetMetaData trae info sobre las columnas:
            // nombres, tipos, cantidad — sin necesidad de saber
            // de antemano qué columnas retorna la consulta
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                // LinkedHashMap mantiene el orden de inserción
                // así las columnas aparecen en el mismo orden del SQL
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    // getColumnLabel trae el alias si lo tiene
                    // Ej: "total_inscripciones" en vez de "COUNT(...)"
                    fila.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                resultado.add(fila);
            }
        }
        return resultado;
    }

    // Helper para consultas CON parámetros (consulta 1)
    private Map<String, Object> mapearFila(ResultSet rs)
            throws SQLException {
        Map<String, Object> fila = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            fila.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return fila;
    }
}