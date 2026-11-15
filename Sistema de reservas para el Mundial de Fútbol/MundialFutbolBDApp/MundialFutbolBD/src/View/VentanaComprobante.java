package View;

import Model.Inscripcion;
import Model.Servicio;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class VentanaComprobante extends JFrame {

    // Recibe la inscripción creada y los servicios seleccionados
    // para armar el texto del comprobante dinámicamente
    public VentanaComprobante(Inscripcion ins,
                              List<Servicio> servicios) {
        setTitle("Comprobante de Inscripción");
        setSize(500, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // JTextArea permite texto multilínea no editable
        // Fuente monoespaciada para alinear columnas como una factura
        JTextArea area = new JTextArea();
        area.setEditable(false); // Solo lectura — es un comprobante
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setMargin(new Insets(15, 15, 15, 15));

        // StringBuilder construye el texto del comprobante
        // más eficiente que concatenar Strings con +
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════\n");
        sb.append("   COMPROBANTE DE INSCRIPCIÓN - SGEM  \n");
        sb.append("   Copa Mundial FIFA 2026             \n");
        sb.append("══════════════════════════════════════\n\n");

        // Datos generales de la inscripción
        sb.append("N° Reserva:    ")
                .append(ins.getNumReserva()).append("\n");
        sb.append("Cliente:       ")
                .append(ins.getNombreCliente()).append("\n");
        sb.append("Experiencia:   ")
                .append(ins.getDescripcionExperiencia()).append("\n");
        sb.append("Fecha:         ")
                .append(ins.getFechaInscripcion()).append("\n\n");

        sb.append("── Servicios seleccionados ────────────\n");

        // Recorrer servicios y mostrar desglose de cada uno
        for (Servicio s : servicios) {
            // String.format formatea el texto con anchura fija
            // %-25s = texto alineado a la izquierda con 25 caracteres
            sb.append(String.format("  %-25s x%d  $%.2f\n",
                    s.getNombre(),
                    s.getCantidad(),
                    s.getCostoAdicional() * s.getCantidad()));
            // Mostrar impuesto aplicado por servicio
            sb.append(String.format(
                    "    Impuesto:                    $%.2f\n",
                    s.getImpuestoAplicado()));
        }

        sb.append("\n══════════════════════════════════════\n");
        // Total final de la inscripción
        sb.append(String.format("TOTAL:                        $%.2f\n",
                ins.getCostoTotal()));
        sb.append("══════════════════════════════════════\n");

        area.setText(sb.toString());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());

        // JScrollPane por si el comprobante es muy largo
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(btnCerrar,             BorderLayout.SOUTH);
    }
}