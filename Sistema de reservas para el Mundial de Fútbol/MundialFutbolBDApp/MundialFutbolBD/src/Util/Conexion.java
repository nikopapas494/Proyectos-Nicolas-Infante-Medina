package Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String URL    = "jdbc:oracle:thin:@10.34.1.33:1521/LAB";
    private static final String USUARIO = "is331509"; //  usuario Oracle
    private static final String CLAVE   = "J93GT$TDNNqMbIK";  //  clave Oracle

    private static Connection instancia = null;

    private Conexion() {}

    public static Connection getConexion() throws SQLException {
        if (instancia == null || instancia.isClosed()) {
            instancia = DriverManager.getConnection(URL, USUARIO, CLAVE);
        }
        return instancia;
    }

    public static void cerrar() {
        try {
            if (instancia != null && !instancia.isClosed()) {
                instancia.close();
                instancia = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}