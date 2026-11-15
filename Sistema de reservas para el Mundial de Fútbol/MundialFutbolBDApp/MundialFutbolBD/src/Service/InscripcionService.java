package Service;

import Dao.*;
import Model.*;
import Util.Conexion;
import java.sql.*;
import java.util.List;

public class InscripcionService {

    // ── INSTANCIAS DE DAOs ─────────────────────────────────────
    // El Service NO habla directamente con la BD, asi funciona el modelo ncapas
    // Delega toda la persistencia a los DAOs
    // Esto cumple el principio de separación de capas del enunciado
    private final ClienteDAO     clienteDAO  = new ClienteDAO();
    private final ExperienciaDAO expDAO      = new ExperienciaDAO();
    private final ServicioDAO    servicioDAO = new ServicioDAO();
    private final InscripcionDAO inscDAO     = new InscripcionDAO();
    private final PagoDAO        pagoDAO     = new PagoDAO();
    private final NovedadDAO     novedadDAO  = new NovedadDAO();

    // =========================================================
    //  HU1 — REGISTRAR O BUSCAR CLIENTE
    //  Primero busca si el cliente ya existe por documento
    //  Si existe → lo retorna directamente sin insertar
    //  Si no existe → lo crea y retorna el nuevo con ID
    // =========================================================
    public Cliente registrarOBuscarCliente(String tipoDoc, String numDoc,
                                           String nombre, String correo,
                                           String telefono)
            throws SQLException {
        // Buscar en BD por tipo y número de documento
        Cliente c = clienteDAO.buscarPorDocumento(tipoDoc, numDoc);

        if (c == null) {
            // No existe → crear objeto y guardarlo en BD
            c = new Cliente(nombre, tipoDoc, numDoc, correo, telefono);
            int id = clienteDAO.insertar(c); // insertar retorna el ID generado
            c.setIdCliente(id); // setear el ID en el objeto para usarlo después
        }
        return c; // Retorna cliente existente o recién creado
    }

    // =========================================================
    //  HU1 — CALCULAR TOTAL DE LA INSCRIPCIÓN
    //  Fórmula: precioBase + sum(costo * cantidad + impuestos)
    //  También setea impuestoAplicado en cada servicio para que el comprobante pueda mostrarlo desglosado todo
    // =========================================================
    public double calcularTotal(Experiencia exp,
                                List<Servicio> servicios)
            throws SQLException {
        // Empezar con el precio base de la experiencia
        double total = exp.getPrecioBase();

        for (Servicio s : servicios) {
            // Subtotal del servicio = costo * cantidad
            double subtotal = s.getCostoAdicional() * s.getCantidad();

            // calcularImpuesto va a BD y suma todos los % del servicio
            // Ej: Transporte tiene IVA 16% + Turístico 5% = 21%
            // impuesto = 150000 * 21 / 100 = 31500
            double impuesto = servicioDAO.calcularImpuesto(
                    s.getIdServicio(), subtotal);

            // Guardar el impuesto en el objeto para usarlo
            // en el comprobante y en inscxservicio
            s.setImpuestoAplicado(impuesto);

            // Sumar subtotal + impuesto al total general
            total += subtotal + impuesto;
        }
        return total;
    }

    // =========================================================
    //  HU1 — REGISTRAR INSCRIPCIÓN COMPLETA
    //  Usa transacción para garantizar que se guarda bien o nada se guarda — si falla el pago, se hace rollback y la inscripción no queda huérfana en BD
    //
    //  Flujo:
    //  1. Calcular total
    //  2. Insertar inscripcion → dispara TRG_VALIDAR_CUPOS
    //  3. Insertar servicios  → dispara TRG_NOVEDAD_MODIFICACION
    //  4. Insertar pago       → dispara TRG_ACTUALIZAR_SALDO
    // =========================================================
    public Inscripcion registrarInscripcion(Cliente cliente, Experiencia exp, List<Servicio> servicios, String tipoPago) throws SQLException {
        // Obtener la conexión para manejar la transacción manualmente
        Connection con = Conexion.getConexion();

        // setAutoCommit(false) desactiva el commit automático
        // Ahora los cambios solo se guardan cuando llamemos con.commit()
        con.setAutoCommit(false);

        try {
            // PASO 1: Calcular total con impuestos incluidos
            double total = calcularTotal(exp, servicios);

            // PASO 2: Generar número de reserva único
            // System.currentTimeMillis() garantiza unicidad por timestamp
            String numReserva = "RES-" + System.currentTimeMillis();

            // PASO 3: Crear objeto inscripcion
            // saldoPendiente = total porque aún no ha pagado nada
            Inscripcion ins = new Inscripcion(
                    numReserva,
                    new java.sql.Date(System.currentTimeMillis()),
                    total, total, // costoTotal y saldoPendiente iguales al inicio
                    exp.getIdExperiencia(),
                    cliente.getIdCliente()
            );

            // PASO 4: Insertar inscripcion en BD
            // Aquí se dispara TRG_VALIDAR_CUPOS automáticamente
            // Si no hay cupos Oracle lanza ORA-20002 y va al catch
            int idIns = inscDAO.insertar(ins);
            ins.setIdInscripcion(idIns);

            // PASO 5: Insertar cada servicio seleccionado
            // Aquí se dispara TRG_NOVEDAD_MODIFICACION por cada INSERT
            for (Servicio s : servicios) {
                inscDAO.insertarServicio(
                        idIns,
                        s.getIdServicio(),
                        s.getCantidad(),
                        s.getCostoAdicional(),
                        s.getImpuestoAplicado()
                );
            }

            // PASO 6: Registrar el pago
            // Aquí se dispara TRG_ACTUALIZAR_SALDO automáticamente
            // que descuenta el valor del saldo_pendiente
            Pago pago = new Pago(
                    idIns,
                    numReserva + "-PAG", // Referencia única del pago
                    total,
                    new java.sql.Date(System.currentTimeMillis()),
                    tipoPago
            );
            pagoDAO.insertar(pago);

            //  salió bien → confirmar todos los cambios en BD
            con.commit();

            ins.setServicios(servicios);
            return ins;

        } catch (SQLException e) {
            // Algo falló → deshacer los cambios de esta transacción
            // La inscripción, servicios y pago NO quedan en BD
            con.rollback();
            throw e; // Re-lanzar para que el Controller lo muestre en GUI
        } finally {
            // Siempre restaurar autoCommit a true
            // para que otras operaciones funcionen normalmente
            con.setAutoCommit(true);
        }
    }

    // =========================================================
    //  HU2 — BUSCAR INSCRIPCIÓN POR NÚMERO DE RESERVA
    //  Busca la inscripción y además carga sus servicios
    //  en una sola llamada para que la GUI tenga las cosas listas
    // =========================================================
    public Inscripcion buscarPorReserva(String numReserva)
            throws SQLException {
        // Buscar inscripción con datos de cliente y experiencia
        Inscripcion ins = inscDAO.buscarPorReserva(numReserva);

        if (ins != null) {
            // Cargar los servicios asociados a esta inscripción
            // y agregarlos al objeto para mostrarlos en la GUI
            ins.setServicios(
                    servicioDAO.listarPorInscripcion(ins.getIdInscripcion())
            );
        }
        return ins; // null si no existe la reserva
    }

    // =========================================================
    //  HU2 — MODIFICAR SERVICIOS DE UNA INSCRIPCIÓN
    //  También usa transacción para garantizar consistencia
    //
    //  Flujo:
    //  1. Eliminar servicios actuales → dispara TRG_NOVEDAD_MODIFICACION
    //  2. Insertar nuevos servicios   → dispara TRG_NOVEDAD_MODIFICACION
    //  3. Recalcular y actualizar total en inscripcion
    // =========================================================
    public Inscripcion modificarServicios(Inscripcion ins,
                                          List<Servicio> nuevosServicios)
            throws SQLException {
        Connection con = Conexion.getConexion();
        con.setAutoCommit(false);

        try {
            // PASO 1: Obtener servicios actuales para eliminarlos
            List<Servicio> actuales = servicioDAO
                    .listarPorInscripcion(ins.getIdInscripcion());

            // PASO 2: Eliminar cada servicio actual de inscxservicio
            // Cada DELETE dispara TRG_NOVEDAD_MODIFICACION → registro en historial
            for (Servicio s : actuales) {
                inscDAO.eliminarServicio(
                        ins.getIdInscripcion(), s.getIdServicio());
            }

            // PASO 3: Obtener experiencia para recalcular desde precio_base
            Experiencia exp = expDAO.buscarPorId(ins.getIdExperiencia());

            // PASO 4: Calcular nuevo total con los servicios nuevos
            double nuevoTotal = calcularTotal(exp, nuevosServicios);

            // PASO 5: Insertar nuevos servicios en inscxservicio
            // Cada INSERT dispara TRG_NOVEDAD_MODIFICACION → registro en historial
            for (Servicio s : nuevosServicios) {
                inscDAO.insertarServicio(
                        ins.getIdInscripcion(),
                        s.getIdServicio(),
                        s.getCantidad(),
                        s.getCostoAdicional(),
                        s.getImpuestoAplicado()
                );
            }

            // PASO 6: Actualizar costo_total y saldo_pendiente en inscripcion
            inscDAO.actualizarCostoTotal(
                    ins.getIdInscripcion(), nuevoTotal, nuevoTotal);

            // Actualizar el objeto en memoria para retornarlo actualizado
            ins.setCostoTotal(nuevoTotal);
            ins.setSaldoPendiente(nuevoTotal);
            ins.setServicios(nuevosServicios);

            con.commit(); // Confirmar todos los cambios
            return ins;

        } catch (SQLException e) {
            con.rollback(); // Deshacer  si algo falla
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    // =========================================================
    //  HU2 — REGISTRAR NOVEDAD MANUAL
    //  El coordinador registra observaciones especiales
    //  Ej: "Cliente requiere asistencia de movilidad"
    //  Las novedades automáticas las manejan los triggers
    // =========================================================
    public void registrarNovedad(int idInscripcion, String descripcion,
                                 String tipo, String registradoPor)
            throws SQLException {
        Novedad n = new Novedad(
                idInscripcion, descripcion, tipo, registradoPor);
        novedadDAO.insertar(n);
    }

    // =========================================================
    //  HELPERS — Métodos de consulta simples
    //  Delegan directamente al DAO sin lógica adicional
    //  Existen aquí para que el Controller solo hable
    //  con el Service y nunca directamente con el DAO
    // =========================================================

    // Trae historial completo de novedades para HU2
    public List<Novedad> obtenerHistorial(int idIns) throws SQLException {
        return novedadDAO.listarPorInscripcion(idIns);
    }

    // Trae pagos para la ventana de detalle de HU2
    public List<Pago> obtenerPagos(int idIns) throws SQLException {
        return pagoDAO.listarPorInscripcion(idIns);
    }

    // Trae experiencias disponibles para el combo de HU1
    public List<Experiencia> listarExperienciasDisponibles()
            throws SQLException {
        return expDAO.listarDisponibles();
    }

    // Trae servicios de una experiencia para la tabla de HU1
    public List<Servicio> listarServiciosPorExperiencia(int idExp)
            throws SQLException {
        return servicioDAO.listarPorExperiencia(idExp);
    }

    // Trae historial de inscripciones de un cliente para HU2
    public List<Inscripcion> listarInscripcionesPorCliente(int idCliente)
            throws SQLException {
        return inscDAO.listarPorCliente(idCliente);
    }
}