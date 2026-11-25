package group.analisis.servicio;

import group.analisis.modelo.PruebaRendimiento;
import group.analisis.servicio.ServicioAnalisis.EstadisticaDescriptiva;
import group.analisis.servicio.ServicioAnalisis.ResumenSemanal;
import group.analisis.servicio.ServicioAnalisis.ResultadoAnalisis;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Maqueta el informe personal usando los resultados calculados por ServicioAnalisis. */
public class GeneradorInformePdf {

    private static final float MARGEN = 50;
    private static final float ALTO_FILA = 23;
    private static final Color AZUL = new Color(25, 73, 112);
    private static final Color AZUL_CLARO = new Color(225, 237, 246);
    private static final Color GRIS = new Color(90, 100, 110);
    private static final PDType1Font NORMAL =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font NEGRITA =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final GeneradorGraficosPdf generadorGraficos = new GeneradorGraficosPdf();

    /** Crea el PDF y devuelve true si pudo insertar la fotografía elegida. */
    public boolean generar(ResultadoAnalisis resultado, Path destino, Path rutaFoto)
            throws IOException {
        if (resultado == null || destino == null) {
            throw new IllegalArgumentException("Faltan los datos del análisis o el destino.");
        }
        if (resultado.getEntrenamientos().isEmpty() && resultado.getPruebas().isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay entrenamientos ni pruebas en ese periodo para generar el informe.");
        }
        Path carpeta = destino.toAbsolutePath().getParent();
        if (carpeta == null || !Files.isDirectory(carpeta)) {
            throw new IOException("La carpeta elegida no existe.");
        }

        // Los gráficos son archivos temporales: se insertan en el PDF y luego se borran.
        Path temporales = Files.createTempDirectory("informe-deportivo-");
        Path graficoCarga = temporales.resolve("carga-semanal.png");
        Path graficoMarca = temporales.resolve("evolucion-marca.png");
        Path logotipo = temporales.resolve("logotipo-club.png");
        try {
            generadorGraficos.crearGraficoCarga(resultado, graficoCarga);
            generadorGraficos.crearGraficoRendimiento(resultado, graficoMarca);
            generadorGraficos.crearLogotipo(logotipo);
            try (PDDocument documento = new PDDocument()) {
                PDImageXObject foto = cargarFoto(documento, rutaFoto);
                PDImageXObject imagenLogo = PDImageXObject.createFromFile(
                        logotipo.toString(), documento);
                Lienzo lienzo = new Lienzo(documento);

                escribirPortada(lienzo, resultado, imagenLogo, foto);
                escribirResumen(lienzo, resultado, graficoCarga);
                escribirRendimiento(lienzo, documento, resultado, graficoMarca);
                escribirInterpretacion(lienzo, resultado);
                lienzo.cerrarPagina();

                documento.getDocumentInformation().setTitle(
                        "Informe de rendimiento - " + resultado.getDeportista().getNombre());
                documento.getDocumentInformation().setAuthor("Centro Deportivo Rendimiento");
                documento.save(destino.toFile());
                return foto != null;
            }
        } finally {
            borrarTemporal(graficoCarga);
            borrarTemporal(graficoMarca);
            borrarTemporal(logotipo);
            borrarTemporal(temporales);
        }
    }

    private PDImageXObject cargarFoto(PDDocument documento, Path rutaFoto) {
        if (rutaFoto == null || !Files.isRegularFile(rutaFoto)) {
            return null;
        }
        try {
            // PDFBox lee la imagen del archivo y conserva su proporción al dibujarla.
            return PDImageXObject.createFromFile(rutaFoto.toString(), documento);
        } catch (IOException | IllegalArgumentException error) {
            return null;
        }
    }

    private void escribirPortada(Lienzo lienzo, ResultadoAnalisis resultado,
            PDImageXObject logo, PDImageXObject foto) throws IOException {
        lienzo.nuevaPagina("Portada", true);
        lienzo.imagenProporcional(logo, 55, 690, 68, 68);
        lienzo.texto("CENTRO DEPORTIVO RENDIMIENTO", 142, 736, NEGRITA, 11, AZUL);
        lienzo.texto("INFORME DE RENDIMIENTO", 55, 625, NEGRITA, 27, AZUL);
        lienzo.texto(resultado.getDeportista().getNombre(), 55, 575, NEGRITA, 22, Color.BLACK);
        lienzo.texto("Categoría: " + resultado.getDeportista().getCategoria(),
                55, 535, NORMAL, 12, GRIS);
        lienzo.texto("Objetivo: " + resultado.getDeportista().getObjetivoDeCompeticion(),
                55, 510, NORMAL, 12, GRIS);
        lienzo.texto("Periodo: " + resultado.getDesde() + " a " + resultado.getHasta(),
                55, 475, NORMAL, 12, GRIS);
        lienzo.texto("Generado: " + LocalDate.now(), 55, 450, NORMAL, 11, GRIS);

        float xFoto = 370;
        float yFoto = 245;
        float anchoFoto = 175;
        float altoFoto = 210;
        if (foto == null) {
            dibujarImagenGenerica(lienzo.contenido, xFoto, yFoto, anchoFoto, altoFoto);
            lienzo.texto("Imagen deportiva genérica", xFoto, yFoto - 18,
                    NORMAL, 9, GRIS);
        } else {
            lienzo.imagenProporcional(foto, xFoto, yFoto, anchoFoto, altoFoto);
            lienzo.texto("Fotografía del deportista", xFoto, yFoto - 18,
                    NORMAL, 9, GRIS);
        }
        lienzo.texto("Informe individual de entrenamiento y pruebas de 5 km",
                55, 120, NORMAL, 12, AZUL);
    }

    private void escribirResumen(Lienzo lienzo, ResultadoAnalisis resultado,
            Path graficoCarga) throws IOException {
        lienzo.nuevaPagina("Resumen y entrenamiento", false);
        lienzo.titulo("Resumen del periodo");
        String mejorMarca = resultado.getMejorMarca5kSegundos() == null
                ? "Sin pruebas de 5 km en el periodo"
                : formatearMarca(resultado.getMejorMarca5kSegundos());
        String resumen = "Durante el periodo se registraron "
                + resultado.getEntrenamientos().size() + " sesiones, "
                + formato(resultado.getKilometrosTotales(), 2) + " km y "
                + resultado.getMinutosTotales() + " minutos de entrenamiento. "
                + "La carga acumulada fue de " + resultado.getCargaTotalUA()
                + " unidades (UA). La mejor marca de 5 km fue " + mejorMarca + ".";
        lienzo.parrafo(resumen, MARGEN, 500, 11, NORMAL, Color.BLACK);

        lienzo.subtitulo("Resumen semanal");
        String[] cabeceras = {"Semana", "Km", "Min", "Carga UA", "Sesiones", "MM 3 sem."};
        float[] anchos = {105, 55, 55, 75, 75, 125};
        List<String[]> filas = new ArrayList<>();
        for (ResumenSemanal semana : resultado.getSemanas()) {
            filas.add(new String[]{
                semana.getLunes().toString(),
                formato(semana.getKilometros(), 2),
                Long.toString(semana.getMinutos()),
                Long.toString(semana.getCargaUA()),
                Integer.toString(semana.getSesiones()),
                semana.getMediaMovilCarga() == null
                        ? "Sin dato" : formato(semana.getMediaMovilCarga(), 2)
            });
        }
        lienzo.tabla(cabeceras, filas, anchos);

        lienzo.espacio(12);
        if (lienzo.y < 360) {
            lienzo.nuevaPagina("Carga semanal", false);
        }
        lienzo.subtitulo("Gráfico de carga semanal");
        PDImageXObject imagen = PDImageXObject.createFromFile(
                graficoCarga.toString(), lienzo.documento);
        lienzo.imagenProporcional(imagen, MARGEN, lienzo.y - 250, 500, 250);
        lienzo.y -= 265;
        lienzo.parrafo("La media móvil aparece cuando hay tres semanas completas consecutivas.",
                MARGEN, 500, 9, NORMAL, GRIS);
    }

    private void escribirRendimiento(Lienzo lienzo, PDDocument documento,
            ResultadoAnalisis resultado, Path graficoMarca) throws IOException {
        lienzo.nuevaPagina("Pruebas de rendimiento", false);
        lienzo.titulo("Histórico de pruebas de 5 km");
        String[] cabeceras = {"Fecha", "Marca (min:seg)", "Cambio vs. primera (s)"};
        float[] anchos = {140, 150, 200};
        List<String[]> filas = new ArrayList<>();
        List<PruebaRendimiento> pruebas = resultado.getPruebas();
        Integer primeraMarca = pruebas.isEmpty()
                ? null : pruebas.get(0).getMarca5kSegundos();
        for (PruebaRendimiento prueba : pruebas) {
            String cambio = primeraMarca == null ? "Sin dato"
                    : (prueba.getMarca5kSegundos() - primeraMarca) + " s";
            filas.add(new String[]{prueba.getFecha().toString(),
                formatearMarca(prueba.getMarca5kSegundos()), cambio});
        }
        lienzo.tabla(cabeceras, filas, anchos);

        if (pruebas.isEmpty()) {
            lienzo.parrafo("No hay pruebas de 5 km en este periodo; el gráfico se incluye sin puntos.",
                    MARGEN, 500, 10, NORMAL, GRIS);
        }
        lienzo.espacio(12);
        if (lienzo.y < 360) {
            lienzo.nuevaPagina("Gráfico de rendimiento", false);
        }
        lienzo.subtitulo("Evolución de la marca y tendencia");
        PDImageXObject imagen = PDImageXObject.createFromFile(
                graficoMarca.toString(), documento);
        lienzo.imagenProporcional(imagen, MARGEN, lienzo.y - 250, 500, 250);
        lienzo.y -= 265;
        lienzo.parrafo("La recta solo se muestra cuando el análisis dispone de datos para calcularla.",
                MARGEN, 500, 9, NORMAL, GRIS);
    }

    private void escribirInterpretacion(Lienzo lienzo, ResultadoAnalisis resultado)
            throws IOException {
        lienzo.nuevaPagina("Interpretación y limitaciones", false);
        lienzo.titulo("Interpretación");
        lienzo.parrafo(resultado.getInterpretacion(), MARGEN, 500, 12, NORMAL, Color.BLACK);

        lienzo.subtitulo("Indicadores del periodo");
        anadirEstadistica(lienzo, "Carga semanal", resultado.getEstadisticaCargaSemanal());
        anadirEstadistica(lienzo, "Distancia por sesión",
                resultado.getEstadisticaDistanciaSesion());

        lienzo.subtitulo("Regresión y asociación");
        var regresion = resultado.getRegresion();
        if (regresion.getPendiente() == null) {
            lienzo.parrafo("No hay dos pruebas con fechas distintas para estimar una tendencia.",
                    MARGEN, 500, 11, NORMAL, Color.BLACK);
        } else {
            lienzo.parrafo("Pendiente: " + formato(regresion.getPendiente(), 2)
                    + " s por semana. R²: "
                    + (regresion.getR2() == null ? "no definido" : formato(regresion.getR2(), 3))
                    + ". Estimación de la semana siguiente: "
                    + (regresion.getEstimacionSiguienteSemana() == null
                            ? "no disponible"
                            : formatearMarca(regresion.getEstimacionSiguienteSemana())) + ".",
                    MARGEN, 500, 11, NORMAL, Color.BLACK);
        }
        lienzo.parrafo("Correlación carga-marca: "
                + (resultado.getCorrelacionCargaMarca() == null
                        ? "no definida" : formato(resultado.getCorrelacionCargaMarca(), 3))
                + " (" + resultado.getParesCorrelacion() + " pares).",
                MARGEN, 500, 11, NORMAL, Color.BLACK);

        lienzo.subtitulo("Limitaciones");
        lienzo.parrafo("La regresión es una extrapolación y no garantiza el rendimiento futuro. "
                + "Una correlación no demuestra que una variable cause la otra. "
                + "Los indicadores son herramientas didácticas; no son diagnósticos médicos "
                + "ni recomendaciones individualizadas de entrenamiento.",
                MARGEN, 500, 11, NORMAL, Color.BLACK);
    }

    private void anadirEstadistica(Lienzo lienzo, String nombre,
            EstadisticaDescriptiva estadistica) throws IOException {
        if (estadistica.getCantidad() == 0) {
            lienzo.parrafo(nombre + ": datos insuficientes.",
                    MARGEN, 500, 10, NORMAL, Color.BLACK);
            return;
        }
        String texto = nombre + " (n=" + estadistica.getCantidad() + "): media "
                + formato(estadistica.getMedia(), 2) + ", mediana "
                + formato(estadistica.getMediana(), 2) + ", mínimo "
                + formato(estadistica.getMinimo(), 2) + ", máximo "
                + formato(estadistica.getMaximo(), 2) + ", desviación típica "
                + formato(estadistica.getDesviacionTipica(), 2) + ", CV "
                + (estadistica.getCoeficienteVariacion() == null
                        ? "no definido" : formato(estadistica.getCoeficienteVariacion(), 2) + "%")
                + ".";
        lienzo.parrafo(texto, MARGEN, 500, 10, NORMAL, Color.BLACK);
    }

    /** Dibuja una figura deportiva si el usuario no tiene una foto para añadir. */
    private void dibujarImagenGenerica(PDPageContentStream contenido,
            float x, float y, float ancho, float alto) throws IOException {
        contenido.setNonStrokingColor(new Color(242, 246, 249));
        contenido.addRect(x, y, ancho, alto);
        contenido.fill();
        contenido.setStrokingColor(AZUL);
        contenido.setLineWidth(5);
        float cx = x + ancho / 2;
        float cy = y + alto / 2;
        dibujarCirculo(contenido, cx + 10, cy + 60, 13);
        contenido.moveTo(cx, cy + 45);
        contenido.lineTo(cx - 12, cy + 3);
        contenido.lineTo(cx + 18, cy - 12);
        contenido.moveTo(cx - 6, cy + 27);
        contenido.lineTo(cx - 42, cy + 48);
        contenido.moveTo(cx - 2, cy + 25);
        contenido.lineTo(cx + 34, cy + 45);
        contenido.moveTo(cx + 18, cy - 12);
        contenido.lineTo(cx + 43, cy - 42);
        contenido.moveTo(cx + 18, cy - 12);
        contenido.lineTo(cx - 25, cy - 38);
        contenido.stroke();
        contenido.setLineWidth(1);
        contenido.setStrokingColor(new Color(180, 196, 208));
        contenido.addRect(x, y, ancho, alto);
        contenido.stroke();
    }

    private void dibujarCirculo(PDPageContentStream contenido,
            float cx, float cy, float radio) throws IOException {
        float curva = radio * 0.5523f;
        contenido.moveTo(cx + radio, cy);
        contenido.curveTo(cx + radio, cy + curva, cx + curva, cy + radio, cx, cy + radio);
        contenido.curveTo(cx - curva, cy + radio, cx - radio, cy + curva, cx - radio, cy);
        contenido.curveTo(cx - radio, cy - curva, cx - curva, cy - radio, cx, cy - radio);
        contenido.curveTo(cx + curva, cy - radio, cx + radio, cy - curva, cx + radio, cy);
        contenido.stroke();
    }

    private static String formato(double valor, int decimales) {
        return String.format(Locale.forLanguageTag("es-ES"), "%." + decimales + "f", valor);
    }

    private static String formatearMarca(int segundos) {
        return formatearMarca((double) segundos);
    }

    private static String formatearMarca(double segundos) {
        int total = (int) Math.round(segundos);
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }

    private void borrarTemporal(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException error) {
            // La limpieza temporal no debe impedir que el informe se guarde.
        }
    }

    private static void escribirTexto(PDPageContentStream contenido, String texto,
            float x, float y, PDType1Font fuente, float tamano, Color color) throws IOException {
        contenido.beginText();
        contenido.setNonStrokingColor(color);
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(limpiarTexto(texto));
        contenido.endText();
    }

    private static String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace('\n', ' ').replace('\r', ' ')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .trim();
    }

    /** Lleva el estado de la página y centraliza tablas, texto, márgenes e imágenes. */
    private static final class Lienzo {

        private final PDDocument documento;
        private PDPage pagina;
        private PDPageContentStream contenido;
        private float y;
        private int numeroPagina;
        private String seccion;

        private Lienzo(PDDocument documento) {
            this.documento = documento;
        }

        private void nuevaPagina(String titulo, boolean portada) throws IOException {
            cerrarPagina();
            seccion = titulo;
            pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            numeroPagina++;
            float anchoPagina = pagina.getMediaBox().getWidth();
            float altoPagina = pagina.getMediaBox().getHeight();

            if (portada) {
                escribirTexto(contenido, "CENTRO DEPORTIVO RENDIMIENTO",
                        MARGEN, altoPagina - 28, NEGRITA, 8, GRIS);
                y = altoPagina - 65;
            } else {
                escribirTexto(contenido, "INFORME DE RENDIMIENTO",
                        MARGEN, altoPagina - 27, NEGRITA, 8, GRIS);
                escribirTexto(contenido, titulo, anchoPagina - 205,
                        altoPagina - 27, NORMAL, 8, GRIS);
                linea(MARGEN, altoPagina - 36, anchoPagina - MARGEN, altoPagina - 36,
                        new Color(190, 202, 212));
                y = altoPagina - 60;
            }
            linea(MARGEN, 42, anchoPagina - MARGEN, 42, new Color(190, 202, 212));
            escribirTexto(contenido, "Centro Deportivo Rendimiento | Informe educativo",
                    MARGEN, 27, NORMAL, 8, GRIS);
            escribirTexto(contenido, "Página " + numeroPagina,
                    anchoPagina - 88, 27, NORMAL, 8, GRIS);
        }

        private void titulo(String texto) throws IOException {
            escribirTexto(contenido, texto, MARGEN, y, NEGRITA, 18, AZUL);
            y -= 30;
        }

        private void texto(String texto, float x, float y, PDType1Font fuente,
                float tamano, Color color) throws IOException {
            escribirTexto(contenido, texto, x, y, fuente, tamano, color);
        }

        private void subtitulo(String texto) throws IOException {
            if (y < 75) {
                nuevaPagina(seccion + " (continuación)", false);
            }
            escribirTexto(contenido, texto, MARGEN, y, NEGRITA, 13, AZUL);
            y -= 21;
        }

        private void parrafo(String texto, float x, float ancho, float tamano,
                PDType1Font fuente, Color color) throws IOException {
            for (String linea : ajustarTexto(texto, fuente, tamano, ancho)) {
                if (y < 67) {
                    nuevaPagina(seccion + " (continuación)", false);
                }
                escribirTexto(contenido, linea, x, y, fuente, tamano, color);
                y -= tamano + 5;
            }
            y -= 8;
        }

        private List<String> ajustarTexto(String texto, PDType1Font fuente,
                float tamano, float ancho) throws IOException {
            List<String> lineas = new ArrayList<>();
            StringBuilder linea = new StringBuilder();
            for (String palabra : limpiarTexto(texto).split("\\s+")) {
                String nueva = linea.isEmpty() ? palabra : linea + " " + palabra;
                if (!linea.isEmpty() && fuente.getStringWidth(nueva) * tamano / 1000 > ancho) {
                    lineas.add(linea.toString());
                    linea = new StringBuilder(palabra);
                } else {
                    linea = new StringBuilder(nueva);
                }
            }
            if (!linea.isEmpty()) {
                lineas.add(linea.toString());
            }
            return lineas;
        }

        private void tabla(String[] cabeceras, List<String[]> filas, float[] anchos)
                throws IOException {
            dibujarFila(cabeceras, anchos, true);
            for (String[] fila : filas) {
                if (y - ALTO_FILA < 58) {
                    nuevaPagina(seccion + " (continuación)", false);
                    dibujarFila(cabeceras, anchos, true);
                }
                dibujarFila(fila, anchos, false);
            }
            y -= 10;
        }

        private void dibujarFila(String[] celdas, float[] anchos, boolean cabecera)
                throws IOException {
            float x = MARGEN;
            float base = y - ALTO_FILA;
            for (int i = 0; i < celdas.length; i++) {
                contenido.setNonStrokingColor(cabecera ? AZUL : Color.WHITE);
                contenido.addRect(x, base, anchos[i], ALTO_FILA);
                contenido.fill();
                contenido.setStrokingColor(new Color(180, 190, 200));
                contenido.addRect(x, base, anchos[i], ALTO_FILA);
                contenido.stroke();

                String texto = celdas[i] == null ? "Sin dato" : celdas[i];
                PDType1Font fuente = cabecera ? NEGRITA : NORMAL;
                Color color = cabecera ? Color.WHITE : Color.BLACK;
                float anchoTexto = fuente.getStringWidth(limpiarTexto(texto))
                        * (cabecera ? 8 : 9) / 1000;
                float textoX = x + (cabecera ? 4 : Math.max(4, anchos[i] - anchoTexto - 6));
                escribirTexto(contenido, texto, textoX, base + 8,
                        fuente, cabecera ? 8 : 9, color);
                x += anchos[i];
            }
            y = base;
        }

        private void imagenProporcional(PDImageXObject imagen, float x, float yBase,
                float anchoMaximo, float altoMaximo) throws IOException {
            float escala = Math.min(anchoMaximo / imagen.getWidth(),
                    altoMaximo / imagen.getHeight());
            float ancho = imagen.getWidth() * escala;
            float alto = imagen.getHeight() * escala;
            contenido.drawImage(imagen, x + (anchoMaximo - ancho) / 2,
                    yBase + (altoMaximo - alto) / 2, ancho, alto);
        }

        private void espacio(float puntos) {
            y -= puntos;
        }

        private void linea(float x1, float y1, float x2, float y2, Color color)
                throws IOException {
            contenido.setStrokingColor(color);
            contenido.moveTo(x1, y1);
            contenido.lineTo(x2, y2);
            contenido.stroke();
        }

        private void cerrarPagina() throws IOException {
            if (contenido != null) {
                contenido.close();
                contenido = null;
            }
        }
    }
}
