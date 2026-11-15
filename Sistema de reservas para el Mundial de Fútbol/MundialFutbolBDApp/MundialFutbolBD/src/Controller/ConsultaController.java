package Controller;

import Service.ConsultaService;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ConsultaController {

    // ── INSTANCIA DEL SERVICE ──────────────────────────────────
    // Igual que InscripcionController — solo habla con el Service
    // La vista llama al Controller, el Controller al Service,
    // el Service al DAO — nunca se salta ninguna capa
    private final ConsultaService service = new ConsultaService();

    // ── CONSULTA 1A — POR CIUDAD ───────────────────────────────
    // Recibe el nombre de la ciudad como parámetro
    // Ej: "Ciudad de México", "Nueva York", "Vancouver"
    public List<Map<String, Object>> experienciasPorCiudad(String ciudad) {
        try {
            // Pasa "ciudad" como tipo de filtro al Service
            return service.experienciasPorFiltro("ciudad", ciudad);
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 1: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 1B — POR ESTADIO ──────────────────────────────
    // Recibe el nombre del estadio
    // Ej: "Estadio Azteca", "MetLife Stadium"
    public List<Map<String, Object>> experienciasPorEstadio(String estadio) {
        try {
            return service.experienciasPorFiltro("estadio", estadio);
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 1: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 1C — POR PARTIDO ──────────────────────────────
    // Recibe el nombre de un equipo — busca en local Y visitante
    // Ej: "Colombia" trae todos los partidos donde juega Colombia
    public List<Map<String, Object>> experienciasPorPartido(String equipo) {
        try {
            return service.experienciasPorFiltro("partido", equipo);
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 1: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 2 — CLIENTES TOP ──────────────────────────────
    // Sin parámetros — trae ranking completo de clientes
    // ordenados por número de inscripciones descendente
    public List<Map<String, Object>> clientesConMasInscripciones() {
        try {
            return service.clientesConMasInscripciones();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 2: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 3 — INGRESOS POR GUÍA ────────────────────────
    // Sin parámetros — ranking de guías por ingresos generados
    // Útil para saber qué guías son más productivos
    public List<Map<String, Object>> ingresosPorGuia() {
        try {
            return service.ingresosPorGuia();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 3: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 4 — MÁS DEMANDADAS ───────────────────────────
    // Sin parámetros — ranking de experiencias por inscripciones
    // Incluye experiencias con 0 inscripciones (LEFT JOIN)
    public List<Map<String, Object>> experienciasMasDemandadas() {
        try {
            return service.experienciasMasDemandadas();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 4: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 5 — IMPUESTOS POR PAÍS ───────────────────────
    // Sin parámetros — agrupa ingresos por México, EEUU, Canadá
    // Muestra cuánto generó cada país sede del mundial
    public List<Map<String, Object>> impuestosPorPais() {
        try {
            return service.impuestosPorPais();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 5: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 6 — PAGOS PENDIENTES ─────────────────────────
    // Sin parámetros — clientes que tienen saldo_pendiente > 0
    // Ordenados por saldo descendente — el que más debe primero
    public List<Map<String, Object>> clientesConPagosPendientes() {
        try {
            return service.clientesConPagosPendientes();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 6: "
                    + e.getMessage());
        }
    }

    // ── CONSULTA 7 — CUPOS DISPONIBLES ────────────────────────
    // Sin parámetros — estado actual de cupos en todas las exp
    // Los cupos son dinámicos: se decrementan con TRG_VALIDAR_CUPOS
    // cada vez que alguien se inscribe
    public List<Map<String, Object>> cuposDisponibles() {
        try {
            return service.cuposDisponibles();
        } catch (SQLException e) {
            throw new RuntimeException("Error consulta 7: "
                    + e.getMessage());
        }
    }
}