package Dao;

import Model.Inscripcion;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscripcionDAO {

    // ── INSERTAR INSCRIPCION ───────────────────────────────────
    // Inserta la inscripción y retorna el ID generado por Oracle
    // El trigger TRG_VALIDAR_CUPOS se dispara automáticamente
    // en este INSERT y descuenta un cupo de la experiencia
    public int insertar(Inscripcion ins) throws SQLException {
        String sql = """
            INSERT INTO inscripcion
                (num_reserva, fecha_inscripcion, costo_total,
                 saldo_pendiente, id_experiencia, id_cliente)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        // new String[]{"ID_INSCRIPCION"} recupera el ID generado
        try (PreparedStatement ps = Conexion.getConexion().prepareStatement(
                sql, new String[]{"ID_INSCRIPCION"})) {
            ps.setString(1, ins.getNumReserva());
            ps.setDate(2, ins.getFechaInscripcion());
            ps.setDouble(3, ins.getCostoTotal());
            ps.setDouble(4, ins.getSaldoPendiente());
            ps.setInt(5, ins.getIdExperiencia());
            ps.setInt(6, ins.getIdCliente());
            ps.executeUpdate();
            // Recuperar ID generado por Oracle
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    // ── INSERTAR SERVICIO EN INSCRIPCION ───────────────────────
    // Inserta una fila en la tabla inscxservicio
    // que relaciona una inscripción con un servicio específico
    // El trigger TRG_NOVEDAD_MODIFICACION se dispara aquí
    // y registra automáticamente la novedad en historial
    public void insertarServicio(int idIns, int idSrv, int cantidad,
                                 double precioUnitario,
                                 double impuesto) throws SQLException {
        String sql = """
            INSERT INTO inscxservicio
                (id_inscripcion, id_servicio, cantidad,
                 precio_unitario, impuesto_aplicado)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idIns);
            ps.setInt(2, idSrv);
            ps.setInt(3, cantidad);
            ps.setDouble(4, precioUnitario);
            ps.setDouble(5, impuesto);
            ps.executeUpdate();
        }
    }

    // ── BUSCAR POR RESERVA ─────────────────────────────────────
    // Se usa en HU2 cuando el coordinador ingresa el número de reserva
    // Usa JOIN con cliente y experiencia para traer los nombres
    // en una sola consulta — evita queries adicionales
    public Inscripcion buscarPorReserva(String numReserva) throws SQLException {
        String sql = """
            SELECT i.*,
                   c.nombre_completo AS nom_cliente,
                   e.descripcion     AS desc_experiencia
              FROM inscripcion i
              JOIN cliente     c ON i.id_cliente     = c.id_cliente
              JOIN experiencia e ON i.id_experiencia = e.id_experiencia
             WHERE i.num_reserva = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setString(1, numReserva);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null; // null = no existe esa reserva
    }

    // ── ACTUALIZAR COSTO TOTAL ─────────────────────────────────
    // Se llama en HU2 después de modificar servicios
    // Actualiza tanto el costo_total como el saldo_pendiente
    // con los nuevos valores calculados en el Service
    public void actualizarCostoTotal(int idIns, double nuevoCosto,
                                     double nuevoSaldo) throws SQLException {
        String sql = """
            UPDATE inscripcion
               SET costo_total = ?, saldo_pendiente = ?
             WHERE id_inscripcion = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setDouble(1, nuevoCosto);
            ps.setDouble(2, nuevoSaldo);
            ps.setInt(3, idIns);
            ps.executeUpdate();
        }
    }

    // ── ELIMINAR SERVICIO DE INSCRIPCION ───────────────────────
    // Borra una fila de inscxservicio — se usa en HU2
    // cuando el coordinador cambia los servicios de la inscripción
    // El trigger TRG_NOVEDAD_MODIFICACION se dispara en este DELETE
    // y registra automáticamente la novedad en historial
    public void eliminarServicio(int idIns,
                                 int idSrv) throws SQLException {
        String sql = """
            DELETE FROM inscxservicio
             WHERE id_inscripcion = ? AND id_servicio = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idIns);
            ps.setInt(2, idSrv);
            ps.executeUpdate();
        }
    }

    // ── LISTAR POR CLIENTE ─────────────────────────────────────
    // Trae el historial de todas las inscripciones de un cliente
    // Se muestra en HU2 en la tabla de historial del panel Resumen
    // ORDER BY fecha DESC → las más recientes primero
    public List<Inscripcion> listarPorCliente(int idCliente)
            throws SQLException {
        List<Inscripcion> lista = new ArrayList<>();
        String sql = """
            SELECT i.*,
                   c.nombre_completo AS nom_cliente,
                   e.descripcion     AS desc_experiencia
              FROM inscripcion i
              JOIN cliente     c ON i.id_cliente     = c.id_cliente
              JOIN experiencia e ON i.id_experiencia = e.id_experiencia
             WHERE i.id_cliente = ?
             ORDER BY i.fecha_inscripcion DESC
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── MAPEAR ─────────────────────────────────────────────────
    // Convierte fila del ResultSet en objeto Inscripcion
    // Los alias nom_cliente y desc_experiencia vienen de los JOINs
    private Inscripcion mapear(ResultSet rs) throws SQLException {
        Inscripcion i = new Inscripcion();
        i.setIdInscripcion(rs.getInt("id_inscripcion"));
        i.setNumReserva(rs.getString("num_reserva"));
        i.setFechaInscripcion(rs.getDate("fecha_inscripcion"));
        i.setCostoTotal(rs.getDouble("costo_total"));
        i.setSaldoPendiente(rs.getDouble("saldo_pendiente"));
        i.setIdExperiencia(rs.getInt("id_experiencia"));
        i.setIdCliente(rs.getInt("id_cliente"));
        // Estos vienen de los JOINs con alias
        i.setNombreCliente(rs.getString("nom_cliente"));
        i.setDescripcionExperiencia(rs.getString("desc_experiencia"));
        return i;
    }
}