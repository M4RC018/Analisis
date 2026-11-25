/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package group.analisis.vistas;

import group.analisis.modelo.Deportista;
import group.analisis.servicio.ServicioDeportivo;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Ventana principal con las pestañas de gestión y análisis del equipo deportivo. */
public class VentanaDeportistas extends javax.swing.JFrame {

    private final ServicioDeportivo servicio;
    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtEdad = new JTextField();
    private final JTextField txtCategoria = new JTextField();
    private final JTextField txtObjetivo = new JTextField();
    private final JTextField txtMarca5k = new JTextField();
    private final JTextArea areaDeportistas = new JTextArea();
    private PanelEntrenamientos panelEntrenamientos;
    private PanelPruebas panelPruebas;
    private PanelAnalisis panelAnalisis;

    /** Construye la ventana con un servicio en memoria nuevo. */
    public VentanaDeportistas() {
        this(new ServicioDeportivo());
    }

    public VentanaDeportistas(ServicioDeportivo servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio es obligatorio.");
        }
        this.servicio = servicio;
        initComponents();
        prepararInterfaz();
    }

    private void prepararInterfaz() {
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout(8, 8));

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        formulario.add(new JLabel("Identificador:"));
        formulario.add(txtId);
        formulario.add(new JLabel("Nombre:"));
        formulario.add(txtNombre);
        formulario.add(new JLabel("Edad:"));
        formulario.add(txtEdad);
        formulario.add(new JLabel("Categoría:"));
        formulario.add(txtCategoria);
        formulario.add(new JLabel("Objetivo de competición:"));
        formulario.add(txtObjetivo);
        formulario.add(new JLabel("Marca inicial de 5 km (segundos):"));
        formulario.add(txtMarca5k);

        JPanel botones = new JPanel();

        JButton botonAgregar = new JButton("Añadir");
        botonAgregar.addActionListener(evento -> agregarDeportista());

        JButton botonModificar = new JButton("Modificar");
        botonModificar.addActionListener(evento -> modificarDeportista());

        JButton botonEliminar = new JButton("Eliminar");
        botonEliminar.addActionListener(evento -> eliminarDeportista());

        botones.add(botonAgregar);
        botones.add(botonModificar);
        botones.add(botonEliminar);

        JPanel parteSuperior = new JPanel(new BorderLayout(6, 6));
        parteSuperior.add(formulario, BorderLayout.CENTER);
        parteSuperior.add(botones, BorderLayout.SOUTH);

        areaDeportistas.setEditable(false);
        areaDeportistas.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel panelDeportistas = new JPanel(new BorderLayout(8, 8));
        panelDeportistas.add(parteSuperior, BorderLayout.NORTH);
        panelDeportistas.add(new JScrollPane(areaDeportistas), BorderLayout.CENTER);

        panelEntrenamientos = new PanelEntrenamientos(servicio);
        panelPruebas = new PanelPruebas(servicio);
        panelAnalisis = new PanelAnalisis(servicio);
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Deportistas", panelDeportistas);
        pestanas.addTab("Entrenamientos", panelEntrenamientos);
        pestanas.addTab("Pruebas", panelPruebas);
        pestanas.addTab("Análisis", panelAnalisis);
        add(pestanas, BorderLayout.CENTER);

        setTitle("Gestión de deportistas");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        actualizarLista();
    }

    private void agregarDeportista() {
        try {
            int id = Integer.parseInt(txtId.getText().trim());
            int edad = Integer.parseInt(txtEdad.getText().trim());
            int marca5k = Integer.parseInt(txtMarca5k.getText().trim());

            Deportista deportista = new Deportista(
                    id,
                    edad,
                    txtNombre.getText(),
                    txtCategoria.getText(),
                    txtObjetivo.getText(),
                    marca5k
            );

            servicio.agregarDeportista(deportista);
            actualizarLista();
            actualizarSelectoresDeportista();
            limpiarCampos();
            JOptionPane.showMessageDialog(
                    this,
                    "Deportista añadido correctamente.",
                    "Alta completada",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (NumberFormatException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "El identificador, la edad y la marca deben ser números enteros.",
                    "Dato incorrecto",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(
                    this,
                    error.getMessage(),
                    "No se pudo añadir",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void modificarDeportista() {
        try {
            int id = Integer.parseInt(txtId.getText().trim());
            int edad = Integer.parseInt(txtEdad.getText().trim());
            int marca5k = Integer.parseInt(txtMarca5k.getText().trim());

            Deportista actualizado = new Deportista(
                    id,
                    edad,
                    txtNombre.getText(),
                    txtCategoria.getText(),
                    txtObjetivo.getText(),
                    marca5k
            );

            servicio.modificarDeportista(actualizado);
            actualizarLista();
            actualizarSelectoresDeportista();
            limpiarCampos();

            JOptionPane.showMessageDialog(this, "Deportista modificado.");
        } catch (NumberFormatException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "El identificador, la edad y la marca deben ser números enteros.",
                    "Dato incorrecto",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(
                    this,
                    error.getMessage(),
                    "No se pudo modificar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void eliminarDeportista() {
        try {
            int id = Integer.parseInt(txtId.getText().trim());
            int confirmacion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Quieres eliminar al deportista con ID " + id + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            servicio.eliminarDeportista(id);
            actualizarLista();
            actualizarSelectoresDeportista();
            limpiarCampos();

            JOptionPane.showMessageDialog(this, "Deportista eliminado.");
        } catch (NumberFormatException error) {
            JOptionPane.showMessageDialog(
                    this,
                    "El identificador debe ser un número entero.",
                    "Dato incorrecto",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException | IllegalStateException error) {
            JOptionPane.showMessageDialog(
                    this,
                    error.getMessage(),
                    "No se pudo eliminar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void actualizarLista() {
        StringBuilder texto = new StringBuilder();
        java.util.List<Deportista> lista = servicio.getDeportistas();

        if (lista.isEmpty()) {
            areaDeportistas.setText("Todavía no hay deportistas registrados.");
            return;
        }

        for (Deportista deportista : lista) {
            texto.append("ID: ").append(deportista.getId())
                    .append(" | ").append(deportista.getNombre())
                    .append(" | ").append(deportista.getEdad()).append(" años")
                    .append(" | ").append(deportista.getCategoria())
                    .append(" | Objetivo: ").append(deportista.getObjetivoDeCompeticion())
                    .append(" | Marca: ").append(deportista.getMarcaInicial5kSegundos())
                    .append(" s")
                    .append(System.lineSeparator());
        }

        areaDeportistas.setText(texto.toString());
    }

    private void limpiarCampos() {
        txtId.setText("");
        txtNombre.setText("");
        txtEdad.setText("");
        txtCategoria.setText("");
        txtObjetivo.setText("");
        txtMarca5k.setText("");
        txtId.requestFocusInWindow();
    }

    /** Refresca los combos de otras pestañas tras cambios en las fichas. */
    private void actualizarSelectoresDeportista() {
        panelEntrenamientos.actualizarDeportistas();
        panelPruebas.actualizarDeportistas();
        panelAnalisis.actualizarDeportistas();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VentanaDeportistas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VentanaDeportistas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VentanaDeportistas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaDeportistas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VentanaDeportistas().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
