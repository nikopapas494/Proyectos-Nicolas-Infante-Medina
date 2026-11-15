package View;

import Controller.ConsultaController;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class VentanaConsultas extends JFrame {

    private final ConsultaController controller
            = new ConsultaController();

    // Tabla genérica que muestra los resultados de cualquier consulta
    // Se reutiliza para las 7 consultas — las columnas son dinámicas
    private JTable            tabla;
    private DefaultTableModel modelo;

    public VentanaConsultas() {
        setTitle("Consultas SGEM - Copa Mundial 2026");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(crearPanelBotones(), BorderLayout.NORTH);
        add(crearPanelTabla(),   BorderLayout.CENTER);
    }

    // ── PANEL BOTONES ─────────────────────────────────────────
    private JPanel crearPanelBotones() {
        // GridLayout(2, 4) = 2 filas, 4 columnas de botones
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        panel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Un botón por cada consulta mínima requerida
        JButton[] botones = {
                new JButton("1. Experiencias por filtro"),
                new JButton("2. Clientes top"),
                new JButton("3. Ingresos por guía"),
                new JButton("4. Más demandadas"),
                new JButton("5. Impuestos por país"),
                new JButton("6. Pagos pendientes"),
                new JButton("7. Cupos disponibles")
        };

        for (JButton b : botones) {
            b.setFont(new Font("Arial", Font.PLAIN, 11));
            panel.add(b);
        }

        // Consulta 1 pide parámetros al usuario via diálogo
        botones[0].addActionListener(e -> ejecutarConsulta1());

        // Las demás consultas no tienen parámetros
        // ejecutar() recibe el resultado y lo muestra en la tabla
        botones[1].addActionListener(e -> ejecutar(
                controller.clientesConMasInscripciones(),
                "Clientes con más inscripciones"));
        botones[2].addActionListener(e -> ejecutar(
                controller.ingresosPorGuia(),
                "Ingresos por guía"));
        botones[3].addActionListener(e -> ejecutar(
                controller.experienciasMasDemandadas(),
                "Experiencias más demandadas"));
        botones[4].addActionListener(e -> ejecutar(
                controller.impuestosPorPais(),
                "Impuestos por país sede"));
        botones[5].addActionListener(e -> ejecutar(
                controller.clientesConPagosPendientes(),
                "Clientes con pagos pendientes"));
        botones[6].addActionListener(e -> ejecutar(
                controller.cuposDisponibles(),
                "Cupos disponibles por experiencia"));

        return panel;
    }

    // ── PANEL TABLA ───────────────────────────────────────────
    private JPanel crearPanelTabla() {
        // DefaultTableModel sin columnas iniciales
        // Las columnas se agregan dinámicamente al mostrar resultados
        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) {
                return false; // Tabla de solo lectura
            }
        };
        tabla = new JTable(modelo);
        // AUTO_RESIZE_ALL_COLUMNS distribuye el ancho entre todas
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.getTableHeader().setFont(
                new Font("Arial", Font.BOLD, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panel.add(new JScrollPane(tabla));
        return panel;
    }

    // ── CONSULTA 1 — CON PARÁMETROS ───────────────────────────
    private void ejecutarConsulta1() {
        // showInputDialog muestra un combo para elegir el tipo de filtro
        String[] opciones = {"Ciudad", "Estadio", "Partido (equipo)"};
        String tipo = (String) JOptionPane.showInputDialog(
                this, "Buscar por:", "Consulta 1",
                JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[0]);
        if (tipo == null) return; // Usuario canceló

        // Pedir el valor según el tipo elegido
        String valor = JOptionPane.showInputDialog(
                this, "Ingresa el valor para " + tipo + ":");
        if (valor == null || valor.isBlank()) return;

        // Switch expression llama el metodo correcto del controller
        List<Map<String, Object>> resultado = switch (tipo) {
            case "Ciudad"  -> controller.experienciasPorCiudad(valor);
            case "Estadio" -> controller.experienciasPorEstadio(valor);
            default        -> controller.experienciasPorPartido(valor);
        };
        mostrarResultados(resultado,
                "Experiencias por " + tipo + ": " + valor);
    }

    // ── HELPER: EJECUTAR CONSULTA SIN PARÁMETROS ──────────────
    private void ejecutar(List<Map<String, Object>> datos,
                          String titulo) {
        mostrarResultados(datos, titulo);
    }

    // ── HELPER: MOSTRAR RESULTADOS EN TABLA ───────────────────
    // Recibe List<Map> y puebla la tabla dinámicamente
    // Las columnas se crean desde las keys del primer mapa
    // Los valores llenan las filas
    private void mostrarResultados(List<Map<String, Object>> datos,
                                   String titulo) {
        // Limpiar tabla antes de mostrar nuevos resultados
        modelo.setRowCount(0);
        modelo.setColumnCount(0);

        if (datos == null || datos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No se encontraron resultados.");
            return;
        }

        // Columnas dinámicas: keys del primer mapa = nombres de columnas
        // Ej: "NOMBRE_COMPLETO", "TOTAL_INSCRIPCIONES", "GASTO_TOTAL"
        datos.get(0).keySet().forEach(modelo::addColumn);

        // Filas: values de cada mapa = celdas de cada fila
        for (Map<String, Object> fila : datos) {
            modelo.addRow(fila.values().toArray());
        }

        // Actualizar título con el nombre de la consulta y total filas
        setTitle("Consultas SGEM — " + titulo
                + " (" + datos.size() + " registros)");
    }
}