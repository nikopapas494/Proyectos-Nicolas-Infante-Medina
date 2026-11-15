package Dao;

import Model.Cliente;
import Util.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    // ── BUSCAR POR DOCUMENTO ───────────────────────────────────
    // Se usa en HU1 para verificar si el cliente ya existe en BD
    // antes de intentar registrarlo nuevamente
    // Retorna null si no existe → la GUI muestra "regístralo"
    public Cliente buscarPorDocumento(String tipoDoc,
                                      String numDoc) throws SQLException {
        // PreparedStatement evita SQL Injection
        // Los ? son parámetros que se llenan con setString()
        String sql = """
            SELECT * FROM cliente
             WHERE tipo_documento = ?
               AND numero_documento = ?
            """;
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setString(1, tipoDoc); // Primer ? = tipo_documento
            ps.setString(2, numDoc);  // Segundo ? = numero_documento
            ResultSet rs = ps.executeQuery();
            // rs.next() avanza al primer registro
            // si no hay ninguno retorna false → retornamos null
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    // ── INSERTAR CLIENTE NUEVO ─────────────────────────────────
    // Se usa en HU1 cuando el cliente no existe en BD
    // Retorna el ID que Oracle generó automáticamente con IDENTITY
    // para poder usarlo luego en la inscripción
    public int insertar(Cliente c) throws SQLException {
        String sql = """
            INSERT INTO cliente
                (nombre_completo, tipo_documento,
                 numero_documento, correo, telefono)
            VALUES (?, ?, ?, ?, ?)
            """;
        // new String[]{"ID_CLIENTE"} le dice a JDBC que queremos
        // recuperar el valor de la columna ID_CLIENTE generada por Oracle
        try (PreparedStatement ps = Conexion.getConexion().prepareStatement(
                sql, new String[]{"ID_CLIENTE"})) {
            ps.setString(1, c.getNombreCompleto());
            ps.setString(2, c.getTipoDocumento());
            ps.setString(3, c.getNumeroDocumento());
            ps.setString(4, c.getCorreo());
            ps.setString(5, c.getTelefono());
            ps.executeUpdate(); // Ejecuta el INSERT

            // getGeneratedKeys() trae el ID que Oracle asignó
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1; // Si algo falló retorna -1
    }

    // ── LISTAR TODOS ───────────────────────────────────────────
    // Trae todos los clientes ordenados alfabéticamente
    // Se usa para llenar combos en la GUI
    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente ORDER BY nombre_completo";
        // try-with-resources cierra automáticamente ps y rs al terminar
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // while recorre todos los registros del ResultSet
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── BUSCAR POR ID ──────────────────────────────────────────
    // Se usa en HU2 cuando necesitas los datos completos
    // del cliente a partir de un id_cliente conocido
    public Cliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM cliente WHERE id_cliente = ?";
        try (PreparedStatement ps =
                     Conexion.getConexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    // ── MAPEAR ─────────────────────────────────────────────────
    // Método privado que convierte una fila del ResultSet
    // en un objeto Cliente — evita repetir este código
    // en cada método que hace SELECT de clientes
    // rs.getString("nombre_columna") lee el valor de esa columna
    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("id_cliente"));
        c.setNombreCompleto(rs.getString("nombre_completo"));
        c.setTipoDocumento(rs.getString("tipo_documento"));
        c.setNumeroDocumento(rs.getString("numero_documento"));
        c.setCorreo(rs.getString("correo"));
        c.setTelefono(rs.getString("telefono"));
        return c;
    }
}