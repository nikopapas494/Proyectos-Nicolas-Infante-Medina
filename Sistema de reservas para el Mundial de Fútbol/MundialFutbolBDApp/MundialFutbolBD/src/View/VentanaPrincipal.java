package View;

import javax.swing.*;
import java.awt.*;

public class VentanaPrincipal extends JFrame {

    // JFrame es la ventana principal de Swing
    // Todas las ventanas del proyecto extienden JFrame
    public VentanaPrincipal() {

        // Título que aparece en la barra superior de la ventana
        setTitle("SGEM - Copa Mundial FIFA 2026");

        // Tamaño inicial de la ventana en píxeles (ancho x alto)
        setSize(600, 400);

        // EXIT_ON_CLOSE cierra toda la aplicación al cerrar esta ventana
        // Solo se usa en la ventana PRINCIPAL — las demás usan DISPOSE_ON_CLOSE, de esta manera la principal siempre esta activa
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Centra la ventana en la pantalla
        // null para que sea independiente del monitor que se use
        setLocationRelativeTo(null);

        // BorderLayout divide la ventana en 5 zonas:NORTH, SOUTH, EAST, WEST, CENTER
        setLayout(new BorderLayout());

        // ── TÍTULO ────────────────────────────────────────────
        JLabel titulo = new JLabel(
                "Sistema de Gestión de Experiencias Mundialistas",
                SwingConstants.CENTER); // Texto centrado horizontalmente
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        // createEmptyBorder agrega un espacio  alrededor del label
        // orden: arriba, izquierda, abajo, derecha
        titulo.setBorder(
                BorderFactory.createEmptyBorder(30, 10, 10, 10));
        add(titulo, BorderLayout.NORTH); // Va en la zona superior

        JLabel subtitulo = new JLabel(
                "Copa Mundial FIFA 2026", SwingConstants.CENTER);
        subtitulo.setFont(new Font("Arial", Font.ITALIC, 14));
        subtitulo.setForeground(Color.GRAY);

        // ── BOTONES ───────────────────────────────────────────
        // GridLayout(filas, columnas, espacio horizontal, espacio vertical)
        JPanel panelBotones = new JPanel(new GridLayout(3, 1, 10, 10));
        panelBotones.setBorder(
                BorderFactory.createEmptyBorder(20, 100, 20, 100));

        JButton btnHU1      = new JButton("📋 Nueva Inscripción (HU1)");
        JButton btnHU2      = new JButton("🔍 Gestionar Inscripción (HU2)");
        JButton btnConsultas = new JButton("📊 Consultas");

        btnHU1.setFont(new Font("Arial", Font.PLAIN, 14));
        btnHU2.setFont(new Font("Arial", Font.PLAIN, 14));
        btnConsultas.setFont(new Font("Arial", Font.PLAIN, 14));

        // setPreferredSize sugiere el tamaño — el layout puede ignorarlo
        btnHU1.setPreferredSize(new Dimension(0, 50));
        btnHU2.setPreferredSize(new Dimension(0, 50));
        btnConsultas.setPreferredSize(new Dimension(0, 50));

        panelBotones.add(btnHU1);
        panelBotones.add(btnHU2);
        panelBotones.add(btnConsultas);

        // Panel centro agrupa subtítulo + botones para que el texto no aparezca en otro lado
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(subtitulo,    BorderLayout.NORTH);
        centro.add(panelBotones, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // ── ACCIONES DE BOTONES ───────────────────────────────
        // addActionListener define qué pasa al hacer clic en el boton
        //  e -> hace que sea on action listener, el boton espera a que pasea algo
        btnHU1.addActionListener(e -> {
            // Crea y muestra la ventana HU1
            // setVisible(true) la hace visible
            new VentanaHU1().setVisible(true);
        });

        btnHU2.addActionListener(e -> {
            new VentanaHU2().setVisible(true);
        });

        btnConsultas.addActionListener(e -> {
            new VentanaConsultas().setVisible(true);
        });
    }

    // ── PUNTO DE ENTRADA DE LA APLICACIÓN ─────────────────────
    // main es el metodo que ejecuta Java de primeras
    // SwingUtilities.invokeLater garantiza que la GUI se cree en el hilo de eventos de Swing (EDT - Event Dispatch Thread)
    // Esto evita problemas de concurrencia en la interfaz
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal().setVisible(true));
    }
}