package View;

import Controller.InscripcionController;
import Model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VentanaHU2 extends JFrame {

    private final InscripcionController controller
            = new InscripcionController();

    // Inscripción cargada — se llena al buscar por número de reserva
    // Se usa en todos los tabs como referencia
    private Inscripcion inscripcionActual;

    // ── COMPONENTES BÚSQUEDA ───────────────────────────────────
    private JTextField txtNumReserva;

    // ── COMPONENTES TAB RESUMEN ───────────────────────────────
    private JLabel lblCliente, lblExperiencia,
            lblFecha, lblTotal, lblSaldo;

    // ── COMPONENTES TAB SERVICIOS ─────────────────────────────
    // tablaServicios → servicios actuales de la inscripción
    // tablaServiciosNuevos → servicios disponibles para cambiar
    private JTable            tablaServicios, tablaServiciosNuevos;
    private DefaultTableModel modeloServicios, modeloServiciosNuevos;

    // ── COMPONENTES TAB HISTORIAL ─────────────────────────────
    private JTable            tablaHistorial;
    private DefaultTableModel modeloHistorial;

    // ── COMPONENTES TAB NOVEDADES ─────────────────────────────
    private JTextArea         txtNovedad;
    private JComboBox<String> cmbTipoNovedad;

    public VentanaHU2() {
        setTitle("Gestionar Inscripción - HU2");
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel de búsqueda siempre visible en la parte superior
        add(crearPanelBusqueda(), BorderLayout.NORTH);

        // Tabs con la información de la inscripción encontrada
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(" Resumen",             crearPanelResumen());
        tabs.addTab(" Modificar servicios",  crearPanelModificarServicios());
        tabs.addTab(" Novedades",            crearPanelNovedades());
        tabs.addTab(" Pagos e impuestos",    crearPanelPagos());
        add(tabs, BorderLayout.CENTER);
    }

    // ── PANEL BÚSQUEDA ────────────────────────────────────────
    private JPanel crearPanelBusqueda() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        // createTitledBorder agrega un borde con título al panel
        panel.setBorder(BorderFactory.createTitledBorder(
                "Buscar inscripción por número de reserva"));

        txtNumReserva = new JTextField(20);
        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.setFont(new Font("Arial", Font.BOLD, 13));

        panel.add(new JLabel("N° Reserva:"));
        panel.add(txtNumReserva);
        panel.add(btnBuscar);

        // Al buscar se llena toda la ventana con los datos
        btnBuscar.addActionListener(e -> buscarInscripcion());
        return panel;
    }

    // ── TAB RESUMEN ───────────────────────────────────────────
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Labels que se llenan cuando se carga la inscripción
        lblCliente     = new JLabel("-");
        lblExperiencia = new JLabel("-");
        lblFecha       = new JLabel("-");
        lblTotal       = new JLabel("-");
        lblSaldo       = new JLabel("-");
        lblSaldo.setFont(new Font("Arial", Font.BOLD, 14));

        agregarFila(panel, g, 0, "Cliente:",         lblCliente);
        agregarFila(panel, g, 1, "Experiencia:",     lblExperiencia);
        agregarFila(panel, g, 2, "Fecha:",           lblFecha);
        agregarFila(panel, g, 3, "Costo total:",     lblTotal);
        agregarFila(panel, g, 4, "Saldo pendiente:", lblSaldo);

        // Tabla historial de inscripciones del cliente
        String[] cols = {"N° Reserva", "Experiencia",
                "Fecha", "Total", "Saldo"};
        modeloHistorial = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false; // Tabla de solo lectura
            }
        };
        tablaHistorial = new JTable(modeloHistorial);

        JPanel panelHist = new JPanel(new BorderLayout());
        panelHist.setBorder(BorderFactory.createTitledBorder(
                "Historial de inscripciones del cliente"));
        panelHist.add(new JScrollPane(tablaHistorial));

        // weighty = 1.0 hace que la tabla use todo el espacio sobrante
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        g.weighty = 1.0;
        g.fill = GridBagConstraints.BOTH;
        panel.add(panelHist, g);

        return panel;
    }

    // ── TAB MODIFICAR SERVICIOS ───────────────────────────────
    private JPanel crearPanelModificarServicios() {
        // GridLayout(2,1) divide el panel en 2 mitades verticales
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Mitad superior: servicios ACTUALES (solo lectura)
        String[] colsAct = {"Servicio", "Cantidad",
                "Precio Unit.", "Impuesto"};
        modeloServicios = new DefaultTableModel(colsAct, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaServicios = new JTable(modeloServicios);
        JPanel panelAct = new JPanel(new BorderLayout());
        panelAct.setBorder(BorderFactory.createTitledBorder(
                "Servicios actuales"));
        panelAct.add(new JScrollPane(tablaServicios));

        // Mitad inferior: servicios DISPONIBLES para seleccionar
        String[] colsNuev = {"Sel.", "Servicio", "Precio", "Cantidad"};
        modeloServiciosNuevos = new DefaultTableModel(colsNuev, 0) {
            @Override public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) {
                return c == 0 || c == 3; // Solo checkbox y cantidad
            }
        };
        tablaServiciosNuevos = new JTable(modeloServiciosNuevos);
        tablaServiciosNuevos.getColumnModel().getColumn(0).setMaxWidth(50);

        JButton btnGuardar = new JButton("Guardar cambios");
        btnGuardar.setBackground(new Color(0, 100, 180));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 13));
        btnGuardar.addActionListener(e -> guardarCambiosServicios());

        JPanel panelNuev = new JPanel(new BorderLayout());
        panelNuev.setBorder(BorderFactory.createTitledBorder(
                "Seleccionar nuevos servicios"));
        panelNuev.add(new JScrollPane(tablaServiciosNuevos),
                BorderLayout.CENTER);
        panelNuev.add(btnGuardar, BorderLayout.SOUTH);

        panel.add(panelAct);
        panel.add(panelNuev);
        return panel;
    }

    // ── TAB NOVEDADES ─────────────────────────────────────────
    private JPanel crearPanelNovedades() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tabla historial de novedades — incluye automáticas y manuales
        String[] cols = {"Fecha", "Tipo",
                "Descripción", "Registrado por"};
        DefaultTableModel modeloNov = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tablaNov = new JTable(modeloNov);
        // Dar más espacio a la columna descripción
        tablaNov.getColumnModel().getColumn(2).setPreferredWidth(300);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder(
                "Historial de novedades"));
        panelTabla.add(new JScrollPane(tablaNov));

        // Formulario para registrar novedad manual
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createTitledBorder(
                "Registrar nueva novedad"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill   = GridBagConstraints.HORIZONTAL;

        txtNovedad     = new JTextArea(3, 30);
        // Solo OBSERVACION y OTRO son manuales
        // PAGO y MODIFICACION_SERVICIO los insertan los triggers
        cmbTipoNovedad = new JComboBox<>(new String[]{
                "OBSERVACION", "OTRO"
        });
        JButton btnRegistrar = new JButton(" Registrar novedad");
        btnRegistrar.setBackground(new Color(180, 100, 0));
        btnRegistrar.setForeground(Color.WHITE);

        g.gridx = 0; g.gridy = 0;
        panelForm.add(new JLabel("Tipo:"), g);
        g.gridx = 1;
        panelForm.add(cmbTipoNovedad, g);
        g.gridx = 0; g.gridy = 1;
        panelForm.add(new JLabel("Descripción:"), g);
        g.gridx = 1;
        panelForm.add(new JScrollPane(txtNovedad), g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        panelForm.add(btnRegistrar, g);

        btnRegistrar.addActionListener(e -> {
            if (inscripcionActual == null) {
                JOptionPane.showMessageDialog(this,
                        "Primero busca una inscripción.");
                return;
            }
            try {
                // Registrar novedad manual via Controller
                controller.registrarNovedad(
                        inscripcionActual.getIdInscripcion(),
                        txtNovedad.getText().trim(),
                        (String) cmbTipoNovedad.getSelectedItem(),
                        "COORDINADOR" // Quien registra la novedad
                );
                txtNovedad.setText(""); // Limpiar campo
                cargarHistorialNovedades(modeloNov); // Refrescar tabla
                JOptionPane.showMessageDialog(this,
                        "Novedad registrada correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnRecargar = new JButton(" Actualizar historial");
        // Al hacer clic recarga las novedades desde BD
        btnRecargar.addActionListener(e ->
                cargarHistorialNovedades(modeloNov));

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.add(panelTabla,  BorderLayout.CENTER);
        panelNorte.add(btnRecargar, BorderLayout.SOUTH);

        panel.add(panelNorte, BorderLayout.CENTER);
        panel.add(panelForm,  BorderLayout.SOUTH);
        return panel;
    }

    // ── TAB PAGOS E IMPUESTOS ─────────────────────────────────
    private JPanel crearPanelPagos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tabla de pagos registrados
        String[] colsPago = {"ID", "Referencia", "Valor",
                "Fecha", "Tipo"};
        DefaultTableModel modeloPagos = new DefaultTableModel(
                colsPago, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tablaPagos = new JTable(modeloPagos);
        JPanel panelPagos = new JPanel(new BorderLayout());
        panelPagos.setBorder(BorderFactory.createTitledBorder(
                "Pagos registrados"));
        panelPagos.add(new JScrollPane(tablaPagos));

        // Tabla de impuestos por servicio
        String[] colsImp = {"Servicio", "Subtotal",
                "Impuesto", "Total servicio"};
        DefaultTableModel modeloImp = new DefaultTableModel(
                colsImp, 0) {
            @Override public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tablaImp = new JTable(modeloImp);
        JPanel panelImp = new JPanel(new BorderLayout());
        panelImp.setBorder(BorderFactory.createTitledBorder(
                "Detalle de impuestos por servicio"));
        panelImp.add(new JScrollPane(tablaImp));

        JButton btnCargar = new JButton(" Cargar detalle de pagos");
        btnCargar.addActionListener(e -> {
            if (inscripcionActual == null) {
                JOptionPane.showMessageDialog(this,
                        "Primero busca una inscripción.");
                return;
            }
            try {
                // Cargar pagos desde BD
                modeloPagos.setRowCount(0);
                controller.obtenerPagos(
                                inscripcionActual.getIdInscripcion())
                        .forEach(p -> modeloPagos.addRow(new Object[]{
                                p.getIdPago(),
                                p.getReferencia(),
                                "$" + p.getValorPagado(),
                                p.getFechaPago(),
                                p.getTipoPago()
                        }));

                // Cargar desglose de impuestos por servicio
                modeloImp.setRowCount(0);
                if (inscripcionActual.getServicios() != null) {
                    for (Servicio s : inscripcionActual.getServicios()) {
                        double sub = s.getCostoAdicional()
                                * s.getCantidad();
                        modeloImp.addRow(new Object[]{
                                s.getNombre(),
                                "$" + String.format("%.2f", sub),
                                "$" + String.format("%.2f",
                                        s.getImpuestoAplicado()),
                                "$" + String.format("%.2f",
                                        sub + s.getImpuestoAplicado())
                        });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // JSplitPane divide el área en dos paneles redimensionables
        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, panelPagos, panelImp);
        split.setDividerLocation(250); // Posición inicial del divisor

        panel.add(btnCargar, BorderLayout.NORTH);
        panel.add(split,     BorderLayout.CENTER);
        return panel;
    }

    // ── LÓGICA: BUSCAR INSCRIPCIÓN ────────────────────────────
    private void buscarInscripcion() {
        String numRes = txtNumReserva.getText().trim();
        if (numRes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingresa un número de reserva.");
            return;
        }
        try {
            // Buscar inscripción con todos sus datos
            inscripcionActual = controller.buscarPorReserva(numRes);

            // Llenar labels del tab Resumen
            lblCliente.setText(inscripcionActual.getNombreCliente());
            lblExperiencia.setText(
                    inscripcionActual.getDescripcionExperiencia());
            lblFecha.setText(
                    inscripcionActual.getFechaInscripcion().toString());
            lblTotal.setText("$" + inscripcionActual.getCostoTotal());

            // Saldo en rojo si debe algo, verde si está al día
            double saldo = inscripcionActual.getSaldoPendiente();
            lblSaldo.setText("$" + saldo);
            lblSaldo.setForeground(
                    saldo > 0 ? Color.RED : new Color(0, 150, 0));

            // Llenar historial de inscripciones del cliente
            modeloHistorial.setRowCount(0);
            controller.listarInscripcionesCliente(
                            inscripcionActual.getIdCliente())
                    .forEach(i -> modeloHistorial.addRow(new Object[]{
                            i.getNumReserva(),
                            i.getDescripcionExperiencia(),
                            i.getFechaInscripcion(),
                            "$" + i.getCostoTotal(),
                            "$" + i.getSaldoPendiente()
                    }));

            // Llenar tabla servicios actuales
            modeloServicios.setRowCount(0);
            if (inscripcionActual.getServicios() != null) {
                inscripcionActual.getServicios()
                        .forEach(s -> modeloServicios.addRow(new Object[]{
                                s.getNombre(),
                                s.getCantidad(),
                                "$" + s.getCostoAdicional(),
                                "$" + String.format("%.2f",
                                        s.getImpuestoAplicado())
                        }));
            }

            // Llenar tabla servicios disponibles para modificar
            modeloServiciosNuevos.setRowCount(0);
            controller.listarServicios(
                            inscripcionActual.getIdExperiencia())
                    .forEach(s -> modeloServiciosNuevos.addRow(new Object[]{
                            false,
                            s.getNombre(),
                            "$" + s.getCostoAdicional(),
                            "1"
                    }));

            JOptionPane.showMessageDialog(this,
                    "Inscripción cargada: "
                            + inscripcionActual.getNumReserva());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── LÓGICA: GUARDAR CAMBIOS SERVICIOS ─────────────────────
    private void guardarCambiosServicios() {
        if (inscripcionActual == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero busca una inscripción.");
            return;
        }
        try {
            // Obtener todos los servicios disponibles para mapear
            List<Servicio> todos = controller
                    .listarServicios(inscripcionActual.getIdExperiencia());
            List<Servicio> nuevos = new ArrayList<>();

            // Recoger servicios marcados con checkbox
            for (int i = 0; i < modeloServiciosNuevos.getRowCount(); i++) {
                Boolean sel = (Boolean)
                        modeloServiciosNuevos.getValueAt(i, 0);
                if (Boolean.TRUE.equals(sel)) {
                    Servicio s = todos.get(i);
                    int cant = Integer.parseInt(
                            modeloServiciosNuevos.getValueAt(i, 3)
                                    .toString());
                    s.setCantidad(cant);
                    nuevos.add(s);
                }
            }

            // Llamar al Controller — maneja transacción completa
            inscripcionActual = controller
                    .modificarServicios(inscripcionActual, nuevos);

            // Refrescar tabla de servicios actuales con los nuevos
            modeloServicios.setRowCount(0);
            inscripcionActual.getServicios()
                    .forEach(s -> modeloServicios.addRow(new Object[]{
                            s.getNombre(), s.getCantidad(),
                            "$" + s.getCostoAdicional(),
                            "$" + String.format("%.2f",
                                    s.getImpuestoAplicado())
                    }));

            // Actualizar total y saldo en los labels
            lblTotal.setText("$" + inscripcionActual.getCostoTotal());
            lblSaldo.setText("$" + inscripcionActual.getSaldoPendiente());

            JOptionPane.showMessageDialog(this,
                    "Servicios actualizados.\nNuevo total: $"
                            + inscripcionActual.getCostoTotal());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── HELPER: CARGAR HISTORIAL NOVEDADES ────────────────────
    // Recarga las novedades desde BD y actualiza la tabla
    // Se llama al registrar novedad y al hacer clic en actualizar
    private void cargarHistorialNovedades(DefaultTableModel modelo) {
        if (inscripcionActual == null) return;
        try {
            modelo.setRowCount(0); // Limpiar tabla
            controller.obtenerHistorial(
                            inscripcionActual.getIdInscripcion())
                    .forEach(n -> modelo.addRow(new Object[]{
                            n.getFechaNovedad(),
                            n.getTipo(),
                            n.getDescripcion(),
                            n.getRegistradoPor()
                    }));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── HELPER: AGREGAR FILA ──────────────────────────────────
    private void agregarFila(JPanel p, GridBagConstraints g,
                             int fila, String label,
                             JComponent comp) {
        g.gridx = 0; g.gridy = fila; g.gridwidth = 1;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        p.add(comp, g);
    }
}