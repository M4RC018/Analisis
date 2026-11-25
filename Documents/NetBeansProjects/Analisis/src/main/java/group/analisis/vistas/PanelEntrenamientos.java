package group.analisis.vistas;

import group.analisis.modelo.Deportista;
import group.analisis.modelo.Entrenamiento;
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

/** Formulario Swing para registrar sesiones vinculadas a un deportista. */
public class PanelEntrenamientos extends JPanel {

    private final ServicioDeportivo servicio;
    private final JComboBox<Deportista> selectorDeportista = new JComboBox<>();
    private final JTextField txtFecha = new JTextField();
    private final JComboBox<String> selectorTipo = new JComboBox<>(new String[]{
        "Rodaje", "Series", "Tirada larga", "Fuerza", "Recuperación", "Otra"
    });
    private final JTextField txtDuracion = new JTextField();
    private final JTextField txtDistancia = new JTextField();
    private final JTextField txtPulso = new JTextField();
    private final JTextField txtRpe = new JTextField();
    private final JTextArea areaSesiones = new JTextArea();

    public PanelEntrenamientos(ServicioDeportivo servicio) {
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
        formulario.add(new JLabel("Tipo de sesión:"));
        formulario.add(selectorTipo);
        formulario.add(new JLabel("Duración (minutos):"));
        formulario.add(txtDuracion);
        formulario.add(new JLabel("Distancia (km; admite coma o punto):"));
        formulario.add(txtDistancia);
        formulario.add(new JLabel("Frecuencia cardiaca media (ppm):"));
        formulario.add(txtPulso);
        formulario.add(new JLabel("Esfuerzo percibido (RPE 1–10):"));
        formulario.add(txtRpe);

        JButton botonAgregar = new JButton("Registrar entrenamiento");
        botonAgregar.addActionListener(evento -> agregarEntrenamiento());
        JPanel zonaFormulario = new JPanel(new BorderLayout(6, 6));
        zonaFormulario.add(formulario, BorderLayout.CENTER);
        zonaFormulario.add(botonAgregar, BorderLayout.SOUTH);

        areaSesiones.setEditable(false);
        add(zonaFormulario, BorderLayout.NORTH);
        add(new JScrollPane(areaSesiones), BorderLayout.CENTER);
    }

    /** Actualiza el selector cuando se añade o modifica un deportista. */
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

    private void agregarEntrenamiento() {
        try {
            Deportista deportista = (Deportista) selectorDeportista.getSelectedItem();
            if (deportista == null) {
                throw new IllegalArgumentException("Primero registra o selecciona un deportista.");
            }

            LocalDate fecha = LocalDate.parse(txtFecha.getText().trim());
            int duracion = Integer.parseInt(txtDuracion.getText().trim());
            double distancia = Double.parseDouble(
                    txtDistancia.getText().trim().replace(',', '.'));
            int pulso = Integer.parseInt(txtPulso.getText().trim());
            int rpe = Integer.parseInt(txtRpe.getText().trim());

            Entrenamiento entrenamiento = new Entrenamiento(
                    fecha,
                    deportista.getId(),
                    (String) selectorTipo.getSelectedItem(),
                    duracion,
                    distancia,
                    pulso,
                    rpe
            );
            servicio.agregarEntrenamiento(entrenamiento);
            actualizarLista();
            limpiarCampos();
            JOptionPane.showMessageDialog(this, "Entrenamiento registrado.");
        } catch (NumberFormatException error) {
            mostrarError("Duración, distancia, pulso y RPE deben tener formato numérico.");
        } catch (DateTimeParseException error) {
            mostrarError("Escribe la fecha como AAAA-MM-DD, por ejemplo 2026-03-14.");
        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void actualizarLista() {
        StringBuilder texto = new StringBuilder();
        for (Entrenamiento entrenamiento : servicio.getEntrenamientos()) {
            Deportista deportista = servicio.buscarDeportistaPorId(entrenamiento.getDeportistaId());
            texto.append(entrenamiento.getFecha())
                    .append(" | ").append(deportista.getNombre())
                    .append(" | ").append(entrenamiento.getTipoSesion())
                    .append(" | ").append(entrenamiento.getDistanciaKm()).append(" km")
                    .append(" | ").append(entrenamiento.getDuracionMinutos()).append(" min")
                    .append(" | RPE ").append(entrenamiento.getRpe())
                    .append(" | Carga ").append(entrenamiento.getCargaSesionUA()).append(" UA")
                    .append(System.lineSeparator());
        }
        areaSesiones.setText(texto.isEmpty() ? "Todavía no hay entrenamientos." : texto.toString());
    }

    private void limpiarCampos() {
        txtFecha.setText("");
        txtDuracion.setText("");
        txtDistancia.setText("");
        txtPulso.setText("");
        txtRpe.setText("");
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo registrar", JOptionPane.ERROR_MESSAGE);
    }
}
