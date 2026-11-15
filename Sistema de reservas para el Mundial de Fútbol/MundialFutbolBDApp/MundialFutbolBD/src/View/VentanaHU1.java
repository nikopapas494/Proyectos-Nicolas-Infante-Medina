package View;

import Controller.InscripcionController;
import Model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VentanaHU1 extends JFrame {

    // ── CONTROLLER ────────────────────────────────────────────
    // La Vista SOLO habla con el Controller — nunca con Service ni DAO
    // Esto cumple la arquitectura MVC requerida por el enunciado
    private final InscripcionController controller
            = new InscripcionController();

    // ── DATOS EN MEMORIA DURANTE EL FLUJO ─────────────────────
    // Se llenan progresivamente en los 4 tabs
    // Al confirmar se usan todos juntos para crear la inscripción
    private Cliente            clienteActual;
    private Experiencia        expActual;
    private final List<Servicio> serviciosSeleccionados = new ArrayList<>();

    // ── COMPONENTES TAB 1: CLIENTE ────────────────────────────
    private JTextField txtTipoDoc, txtNumDoc, txtNombre,
            txtCorreo, txtTelefono;
    private JButton    btnBuscarCliente, btnGuardarCliente;
    private JLabel     lblEstadoCliente;

    // ── COMPONENTES TAB 2: EXPERIENCIA ────────────────────────
    private JComboBox<Experiencia> cmbExperiencia;
    private JLabel lblPrecioBase, lblCupos, lblGuia;

    // ── COMPONENTES TAB 3: SERVICIOS ──────────────────────────
    private JTable             tablaServicios;
    private DefaultTableModel  modeloServicios;

    // ── COMPONENTES TAB 4: PAGO ───────────────────────────────
    private JLabel             lblSubtotal, lblImpuestos, lblTotal;
    private JComboBox<String>  cmbTipoPago;
    private JButton            btnCalcular, btnConfirmar;

    public VentanaHU1() {
        setTitle("Nueva Inscripción - HU1");
        setSize(800, 700);
        setLocationRelativeTo(null);
        // DISPOSE_ON_CLOSE solo cierra ESTA ventana
        // la ventana principal sigue abierta
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // JTabbedPane crea el control de pestañas
        // Cada tab es un JPanel independiente
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Cliente",     crearPanelCliente());
        tabs.addTab("2. Experiencia", crearPanelExperiencia());
        tabs.addTab("3. Servicios",   crearPanelServicios());
        tabs.addTab("4. Pago",        crearPanelPago());

        add(tabs, BorderLayout.CENTER);
    }

    // ── TAB 1: PANEL CLIENTE ──────────────────────────────────
    private JPanel crearPanelCliente() {
        // GridBagLayout es el layout más flexible de Swing
        // permite controlar posición y tamaño de cada componente
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8); // Espaciado entre componentes
        g.fill   = GridBagConstraints.HORIZONTAL; // Estirar horizontalmente

        // Inicializar campos de texto
        txtTipoDoc  = new JTextField(10);
        txtNumDoc   = new JTextField(20);
        txtNombre   = new JTextField(30);
        txtCorreo   = new JTextField(30);
        txtTelefono = new JTextField(15);
        btnBuscarCliente  = new JButton("Buscar");
        btnGuardarCliente = new JButton("Registrar cliente");
        lblEstadoCliente  = new JLabel(" ");
        lblEstadoCliente.setFont(new Font("Arial", Font.BOLD, 12));

        // agregarFila es un helper que pone label + campo en una fila
        agregarFila(panel, g, 0, "Tipo documento:", txtTipoDoc);
        agregarFila(panel, g, 1, "Número documento:", txtNumDoc);

        // Botón buscar y label de estado en la misma fila
        g.gridx = 0; g.gridy = 2;
        panel.add(btnBuscarCliente, g);
        g.gridx = 1;
        panel.add(lblEstadoCliente, g);

        agregarFila(panel, g, 3, "Nombre completo:", txtNombre);
        agregarFila(panel, g, 4, "Correo:", txtCorreo);
        agregarFila(panel, g, 5, "Teléfono:", txtTelefono);

        // gridwidth = 2 hace que el botón ocupe 2 columnas
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        panel.add(btnGuardarCliente, g);

        // ── ACCIÓN BUSCAR ──────────────────────────────────────
        btnBuscarCliente.addActionListener(e -> {
            try {
                // Llama al Controller con tipo y número de documento
                Cliente c = controller.registrarOBuscarCliente(
                        txtTipoDoc.getText().trim(),
                        txtNumDoc.getText().trim(),
                        "", "", ""); // Nombre/correo/tel vacíos para solo buscar
                if (c != null && c.getIdCliente() > 0) {
                    // Cliente encontrado → llenar campos automáticamente
                    clienteActual = c;
                    txtNombre.setText(c.getNombreCompleto());
                    txtCorreo.setText(c.getCorreo());
                    txtTelefono.setText(c.getTelefono());
                    lblEstadoCliente.setText(" Cliente encontrado");
                    lblEstadoCliente.setForeground(new Color(0, 150, 0));
                } else {
                    // No existe → pedir que lo registre
                    lblEstadoCliente.setText(
                            "⚠️ Cliente no encontrado, regístralo");
                    lblEstadoCliente.setForeground(Color.ORANGE);
                }
            } catch (Exception ex) {
                lblEstadoCliente.setText(" " + ex.getMessage());
                lblEstadoCliente.setForeground(Color.ORANGE);
            }
        });

        // ── ACCIÓN REGISTRAR ───────────────────────────────────
        btnGuardarCliente.addActionListener(e -> {
            try {
                // Registra o retorna el cliente existente
                clienteActual = controller.registrarOBuscarCliente(
                        txtTipoDoc.getText().trim(),
                        txtNumDoc.getText().trim(),
                        txtNombre.getText().trim(),
                        txtCorreo.getText().trim(),
                        txtTelefono.getText().trim()
                );
                lblEstadoCliente.setText(
                        " Cliente listo: "
                                + clienteActual.getNombreCompleto());
                lblEstadoCliente.setForeground(new Color(0, 150, 0));
                JOptionPane.showMessageDialog(this,
                        "Cliente registrado correctamente.");
            } catch (Exception ex) {
                // showMessageDialog muestra popup de error
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // ── TAB 2: PANEL EXPERIENCIA ──────────────────────────────
    private JPanel crearPanelExperiencia() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // JComboBox<Experiencia> muestra los objetos usando toString()
        // Experiencia.toString() retorna "descripcion - $precio"
        cmbExperiencia = new JComboBox<>();
        lblPrecioBase  = new JLabel("-");
        lblCupos       = new JLabel("-");
        lblGuia        = new JLabel("-");
        JButton btnCargarExp = new JButton("Cargar experiencias");

        agregarFila(panel, g, 0, "Experiencia:",     cmbExperiencia);
        agregarFila(panel, g, 1, "Precio base:",     lblPrecioBase);
        agregarFila(panel, g, 2, "Cupos restantes:", lblCupos);
        agregarFila(panel, g, 3, "Guía principal:",  lblGuia);

        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        panel.add(btnCargarExp, g);

        // ── ACCIÓN CARGAR EXPERIENCIAS ─────────────────────────
        btnCargarExp.addActionListener(e -> {
            try {
                cmbExperiencia.removeAllItems(); // Limpiar combo
                List<Experiencia> lista = controller.listarExperiencias();
                // Agregar cada experiencia al combo
                // El combo llama toString() para mostrar el texto
                for (Experiencia exp : lista)
                    cmbExperiencia.addItem(exp);
                JOptionPane.showMessageDialog(this,
                        lista.size() + " experiencias disponibles.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ── ACCIÓN SELECCIONAR EXPERIENCIA ────────────────────
        // Se dispara cada vez que cambia la selección del combo
        cmbExperiencia.addActionListener(e -> {
            // getSelectedItem retorna Object → cast a Experiencia
            expActual = (Experiencia) cmbExperiencia.getSelectedItem();
            if (expActual != null) {
                // Actualizar labels con datos de la experiencia
                lblPrecioBase.setText("$" + expActual.getPrecioBase());
                lblCupos.setText(
                        String.valueOf(expActual.getCuposDisponibles()));
                lblGuia.setText(expActual.getNombreGuia());
            }
        });

        return panel;
    }

    // ── TAB 3: PANEL SERVICIOS ────────────────────────────────
    private JPanel crearPanelServicios() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // DefaultTableModel permite manipular filas/columnas dinámicamente
        // Se sobreescribe getColumnClass para que la col 0 sea checkbox
        // y isCellEditable para que solo se puedan editar sel. y cantidad
        String[] cols = {"Sel.", "Servicio", "Precio", "Cantidad"};
        modeloServicios = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                // Columna 0 = Boolean → Swing la renderiza como checkbox
                return col == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int r, int c) {
                // Solo se pueden editar: checkbox (0) y cantidad (3)
                return c == 0 || c == 3;
            }
        };
        tablaServicios = new JTable(modeloServicios);
        // Limitar ancho de columnas pequeñas
        tablaServicios.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaServicios.getColumnModel().getColumn(2).setMaxWidth(100);
        tablaServicios.getColumnModel().getColumn(3).setMaxWidth(80);

        JButton btnCargar = new JButton(
                "Cargar servicios de la experiencia");

        // ── ACCIÓN CARGAR SERVICIOS ────────────────────────────
        btnCargar.addActionListener(e -> {
            if (expActual == null) {
                JOptionPane.showMessageDialog(this,
                        "Primero selecciona una experiencia.");
                return;
            }
            try {
                modeloServicios.setRowCount(0); // Limpiar tabla
                List<Servicio> lista = controller
                        .listarServicios(expActual.getIdExperiencia());
                // Agregar cada servicio como fila en la tabla
                for (Servicio s : lista) {
                    modeloServicios.addRow(new Object[]{
                            false,                    // Checkbox desmarcado
                            s.getNombre(),            // Nombre del servicio
                            "$" + s.getCostoAdicional(), // Precio
                            "1"                       // Cantidad default
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(btnCargar, BorderLayout.NORTH);
        // JScrollPane agrega barra de scroll a la tabla
        panel.add(new JScrollPane(tablaServicios), BorderLayout.CENTER);
        return panel;
    }

    // ── TAB 4: PANEL PAGO ─────────────────────────────────────
    private JPanel crearPanelPago() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        lblSubtotal  = new JLabel("$0");
        lblImpuestos = new JLabel("$0");
        lblTotal     = new JLabel("$0");
        // Total en verde y negrita para destacarlo
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotal.setForeground(new Color(0, 100, 0));

        // Opciones de pago — deben coincidir con el CHECK en Oracle
        cmbTipoPago = new JComboBox<>(new String[]{
                "crédito", "débito", "efectivo", "billetera digital"
        });
        btnCalcular  = new JButton("Calcular total");
        btnConfirmar = new JButton(" Confirmar inscripción");
        btnConfirmar.setBackground(new Color(0, 150, 0));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFont(new Font("Arial", Font.BOLD, 14));

        agregarFila(panel, g, 0, "Precio base:",  lblSubtotal);
        agregarFila(panel, g, 1, "Impuestos:",    lblImpuestos);
        agregarFila(panel, g, 2, "TOTAL:",        lblTotal);
        agregarFila(panel, g, 3, "Tipo de pago:", cmbTipoPago);

        g.gridx = 0; g.gridy = 4;
        panel.add(btnCalcular, g);
        g.gridy = 5; g.gridwidth = 2;
        panel.add(btnConfirmar, g);

        // Conectar botones con métodos privados
        btnCalcular.addActionListener(e  -> calcularResumen());
        btnConfirmar.addActionListener(e -> confirmarInscripcion());

        return panel;
    }

    // ── LÓGICA: CALCULAR RESUMEN ──────────────────────────────
    private void calcularResumen() {
        if (clienteActual == null || expActual == null) {
            JOptionPane.showMessageDialog(this,
                    "Completa los pasos 1 y 2 primero.");
            return;
        }
        try {
            serviciosSeleccionados.clear(); // Limpiar lista anterior

            // Obtener todos los servicios disponibles para comparar
            List<Servicio> todos = controller
                    .listarServicios(expActual.getIdExperiencia());

            // Recorrer la tabla y recoger los que tienen checkbox marcado
            for (int i = 0; i < modeloServicios.getRowCount(); i++) {
                Boolean sel = (Boolean) modeloServicios.getValueAt(i, 0);
                if (Boolean.TRUE.equals(sel)) {
                    Servicio s = todos.get(i); // Obtener objeto del índice
                    // Leer cantidad que escribió el usuario en la tabla
                    int cant = Integer.parseInt(
                            modeloServicios.getValueAt(i, 3).toString());
                    s.setCantidad(cant);
                    serviciosSeleccionados.add(s);
                }
            }

            // Calcular total con impuestos incluidos
            double total = controller.calcularTotal(
                    expActual, serviciosSeleccionados);

            // Sumar impuestos de todos los servicios seleccionados
            double impuestos = serviciosSeleccionados.stream()
                    .mapToDouble(Servicio::getImpuestoAplicado).sum();

            // Actualizar labels con los valores calculados
            lblSubtotal.setText("$" + expActual.getPrecioBase());
            lblImpuestos.setText("$" + String.format("%.2f", impuestos));
            lblTotal.setText("$"     + String.format("%.2f", total));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── LÓGICA: CONFIRMAR INSCRIPCIÓN ─────────────────────────
    private void confirmarInscripcion() {
        if (clienteActual == null || expActual == null) {
            JOptionPane.showMessageDialog(this,
                    "Completa todos los pasos antes de confirmar.");
            return;
        }
        try {
            // Llamar al Controller con todos los datos recolectados
            // El Service maneja la transacción completa
            Inscripcion ins = controller.registrarInscripcion(
                    clienteActual,
                    expActual,
                    serviciosSeleccionados,
                    (String) cmbTipoPago.getSelectedItem()
            );

            // Abrir ventana de comprobante con los datos de la inscripción
            new VentanaComprobante(ins, serviciosSeleccionados)
                    .setVisible(true);

            // Cerrar esta ventana después de confirmar
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── HELPER: AGREGAR FILA ──────────────────────────────────
    // Agrega un par label + componente en una fila del GridBagLayout
    // Evita repetir 6 líneas de GridBagConstraints por cada campo
    private void agregarFila(JPanel p, GridBagConstraints g,
                             int fila, String label,
                             JComponent comp) {
        g.gridx = 0; g.gridy = fila; g.gridwidth = 1;
        p.add(new JLabel(label), g); // Columna 0 = etiqueta
        g.gridx = 1;
        p.add(comp, g);              // Columna 1 = campo
    }
}