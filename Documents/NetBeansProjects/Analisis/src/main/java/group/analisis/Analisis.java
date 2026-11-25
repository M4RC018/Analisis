
package group.analisis;

import group.analisis.servicio.ServicioDeportivo;
import group.analisis.vistas.VentanaDeportistas;
import javax.swing.SwingUtilities;

public class Analisis {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServicioDeportivo servicio = new ServicioDeportivo();
            VentanaDeportistas ventana = new VentanaDeportistas(servicio);
            ventana.setVisible(true);
        });
    }
}
