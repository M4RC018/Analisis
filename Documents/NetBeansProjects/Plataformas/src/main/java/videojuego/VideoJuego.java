/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package videojuego;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import java.awt.Color;

/**
 *
 * @author marco
 */
public class VideoJuego {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame ("Plataformas");
            ventana.setSize(800,450);
            
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
            
            JPanel panel = new JPanel();
            panel.setBackground(new Color(135, 206, 235));
            ventana.setContentPane(new PanelJuego());;
        });
    }
}
