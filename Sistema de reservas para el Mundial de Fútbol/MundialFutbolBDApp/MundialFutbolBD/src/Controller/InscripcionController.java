package Controller;

import Model.*;
import Service.InscripcionService;
import java.sql.SQLException;
import java.util.List;

public class InscripcionController {

    // ── INSTANCIA DEL SERVICE ──────────────────────────────────
    // El Controller SOLO habla con el Service
    // Nunca con DAOs directamente — eso viola la arquitectura
    // Si el profe pregunta: "¿por qué no llamas el DAO directo?"
    // Respuesta: porque la GUI no debe conocer la lógica de BD
    private final InscripcionService service = new InscripcionService();

    // =========================================================
    //  HU1 — REGISTRAR O BUSCAR CLIENTE
    //  Recibe los datos del formulario de la GUI
    //  Delega al Service y convierte SQLException en
    //  RuntimeException para que la GUI la muestre con JOptionPane
    //  sin necesidad de declarar throws en cada método de vista
    // =========================================================
    public Cliente registrarOBuscarCliente(String tipoDoc, String numDoc,
                                           String nombre, String correo,
                                           String telefono) {
        try {
            return service.registrarOBuscarCliente(
                    tipoDoc, numDoc, nombre, correo, telefono);
        } catch (SQLException e) {
            // Convierte SQLException (checked) en RuntimeException (unchecked)
            // para que la Vista no tenga que declarar throws SQLException
            throw new RuntimeException("Error al registrar cliente: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU1 — LISTAR EXPERIENCIAS DISPONIBLES
    //  La GUI llama este método al cargar el combo de experiencias
    //  El Controller no filtra ni transforma — solo delega
    // =========================================================
    public List<Experiencia> listarExperiencias() {
        try {
            return service.listarExperienciasDisponibles();
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar experiencias: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU1 — LISTAR SERVICIOS DE UNA EXPERIENCIA
    //  Se llama cuando el usuario selecciona una experiencia
    //  en el combo — carga los servicios disponibles para esa exp
    // =========================================================
    public List<Servicio> listarServicios(int idExperiencia) {
        try {
            return service.listarServiciosPorExperiencia(idExperiencia);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar servicios: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU1 — CALCULAR TOTAL
    //  Se llama cuando el usuario hace clic en "Calcular total"
    //  en el panel de pago — antes de confirmar la inscripción
    //  Recibe la experiencia y la lista de servicios marcados
    // =========================================================
    public double calcularTotal(Experiencia exp,
                                List<Servicio> servicios) {
        try {
            return service.calcularTotal(exp, servicios);
        } catch (SQLException e) {
            throw new RuntimeException("Error al calcular total: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU1 — REGISTRAR INSCRIPCIÓN COMPLETA
    //  Se llama cuando el usuario confirma la inscripción
    //  Pasa todos los datos recolectados en los 4 tabs
    //  Retorna la inscripción creada con número de reserva
    //  para mostrar en el comprobante
    // =========================================================
    public Inscripcion registrarInscripcion(Cliente cliente,
                                            Experiencia exp,
                                            List<Servicio> servicios,
                                            String tipoPago) {
        try {
            return service.registrarInscripcion(
                    cliente, exp, servicios, tipoPago);
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar inscripción: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — BUSCAR INSCRIPCIÓN POR NÚMERO DE RESERVA
    //  Se llama cuando el coordinador ingresa el número
    //  Lanza RuntimeException si no existe — la GUI muestra error
    //  con JOptionPane sin necesidad de lógica extra en la vista
    // =========================================================
    public Inscripcion buscarPorReserva(String numReserva) {
        try {
            Inscripcion ins = service.buscarPorReserva(numReserva);
            // Validación de negocio: si no existe lanzar error
            // así la GUI no tiene que verificar si es null
            if (ins == null)
                throw new RuntimeException(
                        "No se encontró inscripción con reserva: "
                                + numReserva);
            return ins;
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar inscripción: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — MODIFICAR SERVICIOS
    //  Se llama cuando el coordinador guarda los cambios
    //  en la pestaña "Modificar servicios"
    //  Retorna la inscripción actualizada con nuevo total
    // =========================================================
    public Inscripcion modificarServicios(Inscripcion ins,
                                          List<Servicio> nuevosServicios) {
        try {
            return service.modificarServicios(ins, nuevosServicios);
        } catch (SQLException e) {
            throw new RuntimeException("Error al modificar servicios: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — REGISTRAR NOVEDAD MANUAL
    //  El coordinador registra observaciones especiales
    //  Ej: "cliente requiere asistencia especial de movilidad"
    //  Las novedades automáticas (pagos, servicios) las hacen
    //  los triggers directamente en Oracle
    // =========================================================
    public void registrarNovedad(int idInscripcion, String descripcion,
                                 String tipo, String registradoPor) {
        try {
            service.registrarNovedad(
                    idInscripcion, descripcion, tipo, registradoPor);
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar novedad: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — OBTENER HISTORIAL DE NOVEDADES
    //  Trae todas las novedades de una inscripción
    //  incluyendo las automáticas de triggers y las manuales
    //  Se muestra en la pestaña "Novedades" de VentanaHU2
    // =========================================================
    public List<Novedad> obtenerHistorial(int idInscripcion) {
        try {
            return service.obtenerHistorial(idInscripcion);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener historial: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — OBTENER PAGOS
    //  Trae todos los pagos de una inscripción
    //  Se muestra en la pestaña "Pagos e impuestos" de VentanaHU2
    // =========================================================
    public List<Pago> obtenerPagos(int idInscripcion) {
        try {
            return service.obtenerPagos(idInscripcion);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener pagos: "
                    + e.getMessage());
        }
    }

    // =========================================================
    //  HU2 — HISTORIAL DE INSCRIPCIONES DEL CLIENTE
    //  Trae todas las inscripciones del cliente
    //  Se muestra en la tabla historial del panel Resumen de HU2
    // =========================================================
    public List<Inscripcion> listarInscripcionesCliente(int idCliente) {
        try {
            return service.listarInscripcionesPorCliente(idCliente);
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar inscripciones: "
                    + e.getMessage());
        }
    }
}