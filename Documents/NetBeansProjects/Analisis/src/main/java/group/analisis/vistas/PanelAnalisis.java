package group.analisis.vistas;

import group.analisis.modelo.Deportista;
import group.analisis.servicio.ServicioAnalisis;
import group.analisis.servicio.ServicioAnalisis.EstadisticaDescriptiva;
import group.analisis.servicio.ServicioAnalisis.Regresion;
import group.analisis.servicio.ServicioAnalisis.ResumenSemanal;
import group.analisis.servicio.ServicioAnalisis.ResultadoAnalisis;
import group.analisis.servicio.ServicioDeportivo;
import group.analisis.servicio.ExportadorExcel;
import group.analisis.servicio.GeneradorInformePdf;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Presenta indicadores deportivos calculados por ServicioAnalisis. */
public class PanelAnalisis extends JPanel {

    private final ServicioDeportivo servicioDeportivo;
    private final ServicioAnalisis servicioAnalisis;
    private final ExportadorExcel exportadorExcel = new ExportadorExcel();
    private final GeneradorInformePdf generadorInformePdf = new GeneradorInformePdf();
    private final JComboBox<Deportista> selectorDeportista = new JComboBox<>();
    private final JTextField txtDesde = new JTextField();
    private final JTextField txtHasta = new JTextField();
    private final JTextArea areaInforme = new JTextArea();
    private ResultadoAnalisis ultimoResultado;

    public PanelAnalisis(ServicioDeportivo servicioDeportivo) {
        if (servicioDeportivo == null) {
            throw new IllegalArgumentException("El servicio deportivo es obligatorio.");
        }
        this.servicioDeportivo = servicioDeportivo;
        this.servicioAnalisis = new ServicioAnalisis(servicioDeportivo);
        prepararPanel();
        actualizarDeportistas();
    }

    private void prepararPanel() {
        setLayout(new BorderLayout(8, 8));
        JPanel filtros = new JPanel(new GridLayout(0, 2, 6, 6));
        filtros.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        filtros.add(new JLabel("Deportista:"));
        filtros.add(selectorDeportista);
        filtros.add(new JLabel("Desde (AAAA-MM-DD):"));
        filtros.add(txtDesde);
        filtros.add(new JLabel("Hasta (AAAA-MM-DD):"));
        filtros.add(txtHasta);

        LocalDate hoy = LocalDate.now();
        txtDesde.setText(hoy.minusDays(90).toString());
        txtHasta.setText(hoy.toString());

        JButton botonAnalizar = new JButton("Calcular análisis");
        botonAnalizar.addActionListener(evento -> analizar());
        JButton botonComparar = new JButton("Comparar deportistas");
        botonComparar.addActionListener(evento -> compararDeportistas());
        JButton botonExportar = new JButton("Exportar libro Excel");
        botonExportar.addActionListener(evento -> exportarExcel());
        JButton botonPdf = new JButton("Generar informe PDF");
        botonPdf.addActionListener(evento -> exportarPdf());
        JPanel acciones = new JPanel();
        acciones.add(botonAnalizar);
        acciones.add(botonComparar);
        acciones.add(botonExportar);
        acciones.add(botonPdf);
        JPanel superior = new JPanel(new BorderLayout(6, 6));
        superior.add(filtros, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.SOUTH);

        areaInforme.setEditable(false);
        areaInforme.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        areaInforme.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        areaInforme.setText("Elige un deportista y un periodo para calcular sus indicadores.");

        add(superior, BorderLayout.NORTH);
        add(new JScrollPane(areaInforme), BorderLayout.CENTER);
    }

    /** Actualiza el selector después de modificar la lista de atletas. */
    public void actualizarDeportistas() {
        Integer idSeleccionado = selectorDeportista.getSelectedItem() instanceof Deportista seleccionado
                ? seleccionado.getId() : null;
        selectorDeportista.removeAllItems();
        for (Deportista deportista : servicioDeportivo.getDeportistas()) {
            selectorDeportista.addItem(deportista);
            if (idSeleccionado != null && idSeleccionado == deportista.getId()) {
                selectorDeportista.setSelectedItem(deportista);
            }
        }
    }

    private void analizar() {
        // Evita exportar un resultado antiguo si la nueva selección no es válida.
        ultimoResultado = null;
        try {
            Deportista deportista = (Deportista) selectorDeportista.getSelectedItem();
            if (deportista == null) {
                throw new IllegalArgumentException("Primero registra un deportista.");
            }
            LocalDate desde = LocalDate.parse(txtDesde.getText().trim());
            LocalDate hasta = LocalDate.parse(txtHasta.getText().trim());
            ultimoResultado = servicioAnalisis.analizar(deportista.getId(), desde, hasta);
            areaInforme.setText(formatearResultado(ultimoResultado));
            areaInforme.setCaretPosition(0);
        } catch (DateTimeParseException error) {
            mostrarError("Escribe las fechas como AAAA-MM-DD, por ejemplo 2026-03-14.");
        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void compararDeportistas() {
        try {
            LocalDate desde = LocalDate.parse(txtDesde.getText().trim());
            LocalDate hasta = LocalDate.parse(txtHasta.getText().trim());
            var comparaciones = servicioAnalisis.compararDeportistas(desde, hasta);
            if (comparaciones.isEmpty()) {
                areaInforme.setText("No hay deportistas registrados para comparar.");
                return;
            }

            StringBuilder texto = new StringBuilder("COMPARACIÓN DE DEPORTISTAS\n")
                    .append("Periodo: ").append(desde).append(" a ").append(hasta).append("\n\n")
                    .append("Mejor marca y evolución se expresan en segundos; km y carga mantienen sus unidades.\n");
            for (var comparacion : comparaciones) {
                texto.append("\n").append(comparacion.getDeportista().getNombre()).append("\n")
                        .append("  Mejor 5 km: ")
                        .append(comparacion.getMejorMarca5kSegundos() == null
                                ? "sin pruebas" : formatearMarca(comparacion.getMejorMarca5kSegundos()))
                        .append("\n  Cambio desde primera prueba: ")
                        .append(comparacion.getCambioSegundosRespectoPrimeraPrueba() == null
                                ? "datos insuficientes"
                                : comparacion.getCambioSegundosRespectoPrimeraPrueba() + " s")
                        .append("\n  Km medios/semana: ")
                        .append(comparacion.getKilometrosMediosSemana() == null
                                ? "datos insuficientes" : formato(comparacion.getKilometrosMediosSemana(), 2))
                        .append("\n  Carga media/semana: ")
                        .append(comparacion.getCargaMediaSemanaUA() == null
                                ? "datos insuficientes" : formato(comparacion.getCargaMediaSemanaUA(), 1) + " UA")
                        .append('\n');
            }
            texto.append("\nLa comparación no mezcla unidades ni atribuye causalidad.");
            areaInforme.setText(texto.toString());
            areaInforme.setCaretPosition(0);
        } catch (DateTimeParseException error) {
            mostrarError("Escribe las fechas como AAAA-MM-DD, por ejemplo 2026-03-14.");
        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void exportarExcel() {
        // Recalcula con los filtros que aparecen ahora en pantalla.
        analizar();
        if (ultimoResultado == null) {
            return;
        }

        JFileChooser selectorArchivo = new JFileChooser();
        selectorArchivo.setDialogTitle("Guardar informe Excel");
        selectorArchivo.setFileFilter(
                new FileNameExtensionFilter("Libro Excel (*.xlsx)", "xlsx"));
        selectorArchivo.setSelectedFile(new java.io.File("InformeRendimiento.xlsx"));
        if (selectorArchivo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destino = selectorArchivo.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            destino = destino.resolveSibling(destino.getFileName() + ".xlsx");
        }
        if (Files.exists(destino)) {
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Quieres reemplazarlo?",
                    "Confirmar reemplazo", JOptionPane.YES_NO_OPTION);
            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            exportadorExcel.exportar(destino, servicioDeportivo, servicioAnalisis,
                    ultimoResultado.getDeportista().getId(),
                    ultimoResultado.getDesde(), ultimoResultado.getHasta());
            JOptionPane.showMessageDialog(this,
                    "Libro Excel creado en:\n" + destino.toAbsolutePath(),
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException error) {
            mostrarError("No se pudo crear el archivo: " + error.getMessage());
        }
    }

    /** Genera el informe PDF usando el mismo resultado mostrado por el análisis. */
    private void exportarPdf() {
        analizar();
        if (ultimoResultado == null) {
            return;
        }

        JFileChooser selectorArchivo = new JFileChooser();
        selectorArchivo.setDialogTitle("Guardar informe PDF");
        selectorArchivo.setFileFilter(
                new FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf"));
        String nombre = ultimoResultado.getDeportista().getNombre()
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        selectorArchivo.setSelectedFile(new java.io.File("Informe_" + nombre + ".pdf"));
        if (selectorArchivo.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destino = selectorArchivo.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            destino = destino.resolveSibling(destino.getFileName() + ".pdf");
        }
        if (Files.exists(destino)) {
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Quieres reemplazarlo?",
                    "Confirmar reemplazo", JOptionPane.YES_NO_OPTION);
            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }
        }

        Path foto = elegirFotoOpcional();
        try {
            boolean fotoIncluida = generadorInformePdf.generar(ultimoResultado, destino, foto);
            String avisoFoto = fotoIncluida
                    ? ""
                    : "\nSe usó una imagen genérica porque no se añadió una foto válida.";
            JOptionPane.showMessageDialog(this,
                    "Informe PDF creado en:\n" + destino.toAbsolutePath() + avisoFoto,
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo crear el PDF: " + error.getMessage(),
                    "Error al generar el informe", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** La foto es opcional; si se omite, el PDF dibuja una ilustración genérica. */
    private Path elegirFotoOpcional() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Quieres añadir una fotografía del deportista? Si eliges No, "
                + "el informe incluirá una imagen deportiva genérica.",
                "Fotografía opcional", JOptionPane.YES_NO_OPTION);
        if (respuesta != JOptionPane.YES_OPTION) {
            return null;
        }

        JFileChooser selectorImagen = new JFileChooser();
        selectorImagen.setDialogTitle("Seleccionar fotografía");
        selectorImagen.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        return selectorImagen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? selectorImagen.getSelectedFile().toPath() : null;
    }

    private String formatearResultado(ResultadoAnalisis resultado) {
        StringBuilder texto = new StringBuilder();
        texto.append("ANÁLISIS DE RENDIMIENTO\n")
                .append(resultado.getDeportista().getNombre())
                .append(" | periodo: ").append(resultado.getDesde())
                .append(" a ").append(resultado.getHasta()).append("\n\n")
                .append("Sesiones: ").append(resultado.getEntrenamientos().size()).append('\n')
                .append("Kilómetros: ").append(formato(resultado.getKilometrosTotales(), 2)).append(" km\n")
                .append("Tiempo entrenado: ").append(resultado.getMinutosTotales()).append(" min\n")
                .append("Carga acumulada: ").append(resultado.getCargaTotalUA()).append(" UA\n")
                .append("Mejor marca de 5 km: ")
                .append(resultado.getMejorMarca5kSegundos() == null
                        ? "sin pruebas en el periodo"
                        : formatearMarca(resultado.getMejorMarca5kSegundos()))
                .append("\n\nRESUMEN SEMANAL\n")
                .append("Semana desde | Km | Min | Carga UA | Sesiones | MM 3 semanas | Monotonía | Tensión\n");

        for (ResumenSemanal semana : resultado.getSemanas()) {
            texto.append(semana.getLunes()).append(" | ")
                    .append(formato(semana.getKilometros(), 2)).append(" | ")
                    .append(semana.getMinutos()).append(" | ")
                    .append(semana.getCargaUA()).append(" | ")
                    .append(semana.getSesiones()).append(" | ")
                    .append(semana.getMediaMovilCarga() == null
                            ? "datos insuficientes" : formato(semana.getMediaMovilCarga(), 1))
                    .append(" | ")
                    .append(semana.getMonotonia() == null
                            ? "no definida" : formato(semana.getMonotonia(), 2))
                    .append(" | ")
                    .append(semana.getTension() == null
                            ? "no definida" : formato(semana.getTension(), 1))
                    .append(semana.isSemanaCompleta() ? "" : " (semana parcial)")
                    .append('\n');
        }

        texto.append("\nESTADÍSTICA DESCRIPTIVA\n");
        anexarEstadistica(texto, "Carga semanal (solo semanas completas)",
                resultado.getEstadisticaCargaSemanal());
        anexarEstadistica(texto, "Distancia por sesión", resultado.getEstadisticaDistanciaSesion());

        texto.append("\nRENDIMIENTO\n");
        Regresion regresion = resultado.getRegresion();
        texto.append("Pruebas del periodo: ").append(regresion.getPruebas()).append('\n');
        if (regresion.getPendiente() == null) {
            texto.append("Regresión: datos insuficientes o fechas no distintas.\n");
        } else {
            texto.append("Pendiente: ").append(formato(regresion.getPendiente(), 2)).append(" s/semana\n")
                    .append("Intercepto: ").append(formato(regresion.getIntercepto(), 2)).append(" s\n")
                    .append("R²: ").append(regresion.getR2() == null
                            ? "no definido" : formato(regresion.getR2(), 3)).append('\n')
                    .append("Estimación para la semana siguiente (extrapolación): ")
                    .append(regresion.getEstimacionSiguienteSemana() == null
                            ? "no definida" : formatearMarca(regresion.getEstimacionSiguienteSemana().intValue()))
                    .append('\n');
        }
        texto.append("Correlación carga-marca: ")
                .append(resultado.getCorrelacionCargaMarca() == null
                        ? "no definida" : formato(resultado.getCorrelacionCargaMarca(), 3))
                .append(" (pares: ").append(resultado.getParesCorrelacion()).append(")\n\n")
                .append("INTERPRETACIÓN PRUDENTE\n")
                .append(resultado.getInterpretacion()).append('\n')
                .append("La estimación es una extrapolación, no una garantía de rendimiento.\n");
        return texto.toString();
    }

    private void anexarEstadistica(StringBuilder texto, String nombre, EstadisticaDescriptiva estadistica) {
        texto.append(nombre).append(": ");
        if (estadistica.getCantidad() == 0) {
            texto.append("datos insuficientes\n");
            return;
        }
        texto.append("n=").append(estadistica.getCantidad())
                .append(", media=").append(formato(estadistica.getMedia(), 2))
                .append(", mediana=").append(formato(estadistica.getMediana(), 2))
                .append(", mínimo=").append(formato(estadistica.getMinimo(), 2))
                .append(", máximo=").append(formato(estadistica.getMaximo(), 2))
                .append(", desv. típica=").append(formato(estadistica.getDesviacionTipica(), 2))
                .append(", CV=").append(estadistica.getCoeficienteVariacion() == null
                        ? "no definido" : formato(estadistica.getCoeficienteVariacion(), 2) + "%")
                .append('\n');
    }

    private static String formato(double valor, int decimales) {
        return String.format(Locale.forLanguageTag("es-ES"), "%." + decimales + "f", valor);
    }

    private static String formatearMarca(int segundos) {
        return formatearMarca((double) segundos);
    }

    private static String formatearMarca(double segundos) {
        if (!Double.isFinite(segundos) || segundos <= 0) {
            return "estimación no plausible";
        }
        int totalSegundos = (int) Math.round(segundos);
        return String.format(Locale.ROOT, "%d:%02d", totalSegundos / 60, totalSegundos % 60);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo analizar", JOptionPane.ERROR_MESSAGE);
    }
}
