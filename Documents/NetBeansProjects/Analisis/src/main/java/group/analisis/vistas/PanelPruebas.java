package group.analisis.vistas;

import group.analisis.modelo.Deportista;
import group.analisis.modelo.PruebaRendimiento;
import group.analisis.servicio.ServicioDeportivo;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Formulario Swing para registrar pruebas de rendimiento de 5 km. */
public class PanelPruebas extends JPanel {

    private final ServicioDeportivo servicio;
    private final JComboBox<Deportista> selectorDeportista = new JComboBox<>();
    private final JTextField txtFecha = new JTextField();
    private final JTextField txtMarca = new JTextField();
    private final JTextArea areaPruebas = new JTextArea();

    public PanelPruebas(ServicioDeportivo servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio deportivo es obligatorio.");
        }
        this.servicio = servicio;
        prepararPanel();
        actualizarDeportistas();
        actualizarLista();
    }

    private void prepararPanel() {
        setLayout(new BorderLayout(8, 8));
        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        formulario.add(new JLabel("Deportista:"));
        formulario.add(selectorDeportista);
        formulario.add(new JLabel("Fecha (AAAA-MM-DD):"));
        formulario.add(txtFecha);
        formulario.add(new JLabel("Marca de 5 km (segundos):"));
        formulario.add(txtMarca);

        JButton botonAgregar = new JButton("Registrar prueba");
        botonAgregar.addActionListener(evento -> agregarPrueba());
        JPanel zonaFormulario = new JPanel(new BorderLayout(6, 6));
        zonaFormulario.add(formulario, BorderLayout.CENTER);
        zonaFormulario.add(botonAgregar, BorderLayout.SOUTH);

        areaPruebas.setEditable(false);
        add(zonaFormulario, BorderLayout.NORTH);
        add(new JScrollPane(areaPruebas), BorderLayout.CENTER);
    }

    /** Mantiene el selector sincronizado con las altas o modificaciones. */
    public void actualizarDeportistas() {
        Integer idSeleccionado = selectorDeportista.getSelectedItem() instanceof Deportista seleccionado
                ? seleccionado.getId() : null;
        selectorDeportista.removeAllItems();
        for (Deportista deportista : servicio.getDeportistas()) {
            selectorDeportista.addItem(deportista);
            if (idSeleccionado != null && idSeleccionado == deportista.getId()) {
                selectorDeportista.setSelectedItem(deportista);
            }
        }
    }

    private void agregarPrueba() {
        try {
            Deportista deportista = (Deportista) selectorDeportista.getSelectedItem();
            if (deportista == null) {
                throw new IllegalArgumentException("Primero registra o selecciona un deportista.");
            }

            LocalDate fecha = LocalDate.parse(txtFecha.getText().trim());
            int marca = Integer.parseInt(txtMarca.getText().trim());
            PruebaRendimiento prueba = new PruebaRendimiento(fecha, deportista.getId(), marca);
            servicio.agregarPruebaRendimiento(prueba);
            actualizarLista();
            txtFecha.setText("");
            txtMarca.setText("");
            JOptionPane.showMessageDialog(this, "Prueba registrada.");
        } catch (NumberFormatException error) {
            mostrarError("La marca debe indicarse en segundos, como un número entero.");
        } catch (DateTimeParseException error) {
            mostrarError("Escribe la fecha como AAAA-MM-DD, por ejemplo 2026-03-14.");
        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void actualizarLista() {
        StringBuilder texto = new StringBuilder();
        for (PruebaRendimiento prueba : servicio.getPruebas()) {
            Deportista deportista = servicio.buscarDeportistaPorId(prueba.getDeportistaId());
            texto.append(prueba.getFecha())
                    .append(" | ").append(deportista.getNombre())
                    .append(" | ").append(prueba.getMarca5kSegundos()).append(" s")
                    .append(System.lineSeparator());
        }
        areaPruebas.setText(texto.isEmpty() ? "Todavía no hay pruebas." : texto.toString());
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo registrar", JOptionPane.ERROR_MESSAGE);
    }
}
