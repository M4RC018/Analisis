package group.analisis.servicio;

import group.analisis.modelo.PruebaRendimiento;
import group.analisis.servicio.ServicioAnalisis.Regresion;
import group.analisis.servicio.ServicioAnalisis.ResumenSemanal;
import group.analisis.servicio.ServicioAnalisis.ResultadoAnalisis;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/** Prepara como PNG los dos gráficos que después se insertan en el informe. */
public class GeneradorGraficosPdf {

    private static final DateTimeFormatter FECHA_GRAFICO = DateTimeFormatter.ofPattern("dd/MM");

    /** Genera el logotipo sencillo del club como imagen PNG para la portada. */
    public void crearLogotipo(Path destino) throws IOException {
        BufferedImage imagen = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D lapiz = imagen.createGraphics();
        try {
            lapiz.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            lapiz.setColor(new Color(25, 73, 112));
            lapiz.fillRoundRect(8, 8, 240, 240, 48, 48);
            lapiz.setColor(Color.WHITE);
            lapiz.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            lapiz.drawLine(60, 186, 96, 120);
            lapiz.drawLine(96, 120, 139, 170);
            lapiz.drawLine(96, 120, 143, 93);
            lapiz.drawOval(127, 55, 24, 24);
            lapiz.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
            lapiz.drawString("CDR", 67, 225);
        } finally {
            lapiz.dispose();
        }
        ImageIO.write(imagen, "png", destino.toFile());
    }

    /** Crea el gráfico semanal de carga con los resultados ya calculados en Java. */
    public void crearGraficoCarga(ResultadoAnalisis resultado, Path destino) throws IOException {
        DefaultCategoryDataset datos = new DefaultCategoryDataset();
        for (ResumenSemanal semana : resultado.getSemanas()) {
            datos.addValue(semana.getCargaUA(), "Carga semanal",
                    semana.getLunes().format(FECHA_GRAFICO));
        }

        JFreeChart grafico = ChartFactory.createBarChart(
                "Carga semanal", "Semana", "Carga (UA)",
                datos, PlotOrientation.VERTICAL, true, false, false);
        grafico.setBackgroundPaint(Color.WHITE);
        CategoryPlot zona = grafico.getCategoryPlot();
        zona.setBackgroundPaint(new Color(248, 250, 252));
        zona.setRangeGridlinePaint(new Color(210, 220, 228));
        ((BarRenderer) zona.getRenderer()).setSeriesPaint(0, new Color(39, 117, 173));
        ChartUtils.saveChartAsPNG(destino.toFile(), grafico, 1000, 560);
    }

    /** Crea el gráfico de marcas y añade la recta de ServicioAnalisis si existe. */
    public void crearGraficoRendimiento(ResultadoAnalisis resultado, Path destino)
            throws IOException {
        XYSeries marcas = new XYSeries("Marca real");
        List<PruebaRendimiento> pruebas = resultado.getPruebas();

        if (!pruebas.isEmpty()) {
            LocalDate primeraFecha = pruebas.get(0).getFecha();
            for (PruebaRendimiento prueba : pruebas) {
                double semanas = ChronoUnit.DAYS.between(
                        primeraFecha, prueba.getFecha()) / 7.0;
                marcas.add(semanas, prueba.getMarca5kSegundos());
            }
        }

        XYSeriesCollection datos = new XYSeriesCollection();
        datos.addSeries(marcas);

        // Se dibuja la misma regresión calculada por ServicioAnalisis, sin recalcularla.
        Regresion regresion = resultado.getRegresion();
        if (regresion.getIntercepto() != null && regresion.getPendiente() != null
                && pruebas.size() >= 2) {
            LocalDate primeraFecha = pruebas.get(0).getFecha();
            double ultimaSemana = ChronoUnit.DAYS.between(
                    primeraFecha, pruebas.get(pruebas.size() - 1).getFecha()) / 7.0;
            XYSeries tendencia = new XYSeries("Recta de regresión");
            tendencia.add(0, regresion.getIntercepto());
            tendencia.add(ultimaSemana,
                    regresion.getIntercepto() + regresion.getPendiente() * ultimaSemana);
            datos.addSeries(tendencia);
        }

        JFreeChart grafico = ChartFactory.createXYLineChart(
                "Evolución de la marca de 5 km", "Semanas desde la primera prueba",
                "Tiempo (segundos)", datos, PlotOrientation.VERTICAL,
                true, false, false);
        grafico.setBackgroundPaint(Color.WHITE);
        XYPlot zona = grafico.getXYPlot();
        zona.setBackgroundPaint(new Color(248, 250, 252));
        zona.setRangeGridlinePaint(new Color(210, 220, 228));
        NumberAxis ejeVertical = (NumberAxis) zona.getRangeAxis();
        ejeVertical.setAutoRangeIncludesZero(false);
        XYLineAndShapeRenderer lineas = (XYLineAndShapeRenderer) zona.getRenderer();
        lineas.setSeriesPaint(0, new Color(39, 117, 173));
        lineas.setSeriesPaint(1, new Color(230, 126, 34));
        lineas.setSeriesShapesVisible(0, true);
        lineas.setSeriesShapesVisible(1, false);
        ChartUtils.saveChartAsPNG(destino.toFile(), grafico, 1000, 560);
    }
}
