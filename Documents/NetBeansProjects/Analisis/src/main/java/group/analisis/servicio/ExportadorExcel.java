package group.analisis.servicio;

import group.analisis.modelo.Deportista;
import group.analisis.modelo.Entrenamiento;
import group.analisis.modelo.PruebaRendimiento;
import group.analisis.servicio.ServicioAnalisis.ComparacionDeportista;
import group.analisis.servicio.ServicioAnalisis.EstadisticaDescriptiva;
import group.analisis.servicio.ServicioAnalisis.Regresion;
import group.analisis.servicio.ServicioAnalisis.ResumenSemanal;
import group.analisis.servicio.ServicioAnalisis.ResultadoAnalisis;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Crea el libro Excel solicitado a partir de los datos y cálculos de Java. */
public class ExportadorExcel {

    /** Exporta cinco hojas y cierra correctamente el libro y el archivo. */
    public void exportar(Path destino, ServicioDeportivo servicio,
            ServicioAnalisis analisis, int atletaId, LocalDate desde, LocalDate hasta)
            throws IOException {
        if (destino == null || servicio == null || analisis == null
                || desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("Revisa el destino, los servicios y las fechas.");
        }

        List<Deportista> deportistas = servicio.getDeportistas();
        List<ComparacionDeportista> comparaciones = analisis.compararDeportistas(desde, hasta);
        List<Entrenamiento> entrenamientos = servicio.getEntrenamientos();
        ResultadoAnalisis seleccionado = analisis.analizar(atletaId, desde, hasta);

        try (XSSFWorkbook libro = new XSSFWorkbook();
                OutputStream salida = Files.newOutputStream(destino)) {
            Estilos estilos = crearEstilos(libro);
            XSSFSheet resumen = libro.createSheet("Resumen");
            XSSFSheet hojaDeportistas = libro.createSheet("Deportistas");
            XSSFSheet hojaEntrenamientos = libro.createSheet("Entrenamientos");
            XSSFSheet hojaAnalisis = libro.createSheet("Análisis");
            XSSFSheet hojaRendimiento = libro.createSheet("Rendimiento");

            escribirDeportistas(hojaDeportistas, deportistas, estilos);
            escribirEntrenamientos(hojaEntrenamientos, entrenamientos, servicio, estilos);
            escribirAnalisis(hojaAnalisis, seleccionado, estilos);
            escribirRendimiento(hojaRendimiento, servicio.getPruebas(), servicio,
                    seleccionado, estilos);
            escribirResumen(resumen, comparaciones, desde, hasta, estilos);

            // Las fórmulas de apoyo se recalculan al abrir el libro en Excel.
            libro.setForceFormulaRecalculation(true);
            libro.write(salida);
        }
    }

    private void escribirDeportistas(XSSFSheet hoja, List<Deportista> deportistas,
            Estilos estilos) {
        String[] cabeceras = {"ID", "Nombre", "Edad", "Categoría", "Objetivo", "Marca inicial 5 km (s)"};
        cabecera(hoja, 0, 0, cabeceras, estilos.cabecera);
        int fila = 1;
        for (Deportista deportista : deportistas) {
            Row row = hoja.createRow(fila++);
            numero(row, 0, deportista.getId(), estilos.entero);
            texto(row, 1, deportista.getNombre(), estilos.texto);
            numero(row, 2, deportista.getEdad(), estilos.entero);
            texto(row, 3, deportista.getCategoria(), estilos.texto);
            texto(row, 4, deportista.getObjetivoDeCompeticion(), estilos.texto);
            numero(row, 5, deportista.getMarcaInicial5kSegundos(), estilos.segundos);
        }
        terminarTabla(hoja, 0, fila - 1, cabeceras.length);
    }

    private void escribirEntrenamientos(XSSFSheet hoja, List<Entrenamiento> entrenamientos,
            ServicioDeportivo servicio, Estilos estilos) {
        String[] cabeceras = {"Fecha", "ID deportista", "Deportista", "Tipo de sesión",
            "Duración (min)", "Distancia (km)", "FC media (ppm)", "RPE", "Carga (UA)"};
        cabecera(hoja, 0, 0, cabeceras, estilos.cabecera);
        entrenamientos = new ArrayList<>(entrenamientos);
        entrenamientos.sort(Comparator.comparing(Entrenamiento::getFecha)
                .thenComparingInt(Entrenamiento::getDeportistaId));

        int fila = 1;
        for (Entrenamiento entrenamiento : entrenamientos) {
            Row row = hoja.createRow(fila);
            fecha(row, 0, entrenamiento.getFecha(), estilos.fecha);
            numero(row, 1, entrenamiento.getDeportistaId(), estilos.entero);
            texto(row, 2, servicio.buscarDeportistaPorId(
                    entrenamiento.getDeportistaId()).getNombre(), estilos.texto);
            texto(row, 3, entrenamiento.getTipoSesion(), estilos.texto);
            numero(row, 4, entrenamiento.getDuracionMinutos(), estilos.entero);
            numero(row, 5, entrenamiento.getDistanciaKm(), estilos.decimal);
            numero(row, 6, entrenamiento.getFrecuenciaCardiacaMedia(), estilos.entero);
            numero(row, 7, entrenamiento.getRpe(), estilos.entero);

            // La fórmula solo presenta la carga; Java realiza los cálculos del análisis.
            Cell carga = row.createCell(8);
            carga.setCellFormula("E" + (fila + 1) + "*H" + (fila + 1));
            carga.setCellStyle(estilos.decimal);
            fila++;
        }

        terminarTabla(hoja, 0, fila - 1, cabeceras.length);
        if (fila > 1) {
            // Resalta sesiones con RPE alto como ayuda visual.
            SheetConditionalFormatting formato = hoja.getSheetConditionalFormatting();
            ConditionalFormattingRule regla = formato.createConditionalFormattingRule(
                    ComparisonOperator.GT, "7");
            PatternFormatting relleno = regla.createPatternFormatting();
            relleno.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            relleno.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
            formato.addConditionalFormatting(
                    new CellRangeAddress[]{new CellRangeAddress(1, fila - 1, 7, 7)}, regla);
        }
    }

    private void escribirAnalisis(XSSFSheet hoja, ResultadoAnalisis resultado, Estilos estilos) {
        Row titulo = hoja.createRow(0);
        texto(titulo, 0, "Análisis semanal - " + resultado.getDeportista().getNombre(), estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        Row periodo = hoja.createRow(1);
        texto(periodo, 0, "Periodo: " + resultado.getDesde() + " a " + resultado.getHasta(), estilos.texto);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

        String[] cabeceras = {"Semana desde (lunes)", "Kilómetros (km)", "Minutos (min)", "Carga (UA)",
            "Sesiones", "Media móvil 3 sem. (UA)", "Monotonía", "Tensión"};
        cabecera(hoja, 3, 0, cabeceras, estilos.cabecera);
        int fila = 4;
        for (ResumenSemanal semana : resultado.getSemanas()) {
            Row row = hoja.createRow(fila++);
            texto(row, 0, semana.getLunes().toString(), estilos.texto);
            numero(row, 1, semana.getKilometros(), estilos.decimal);
            numero(row, 2, semana.getMinutos(), estilos.entero);
            numero(row, 3, semana.getCargaUA(), estilos.decimal);
            numero(row, 4, semana.getSesiones(), estilos.entero);
            numero(row, 5, semana.getMediaMovilCarga(), estilos.decimal);
            numero(row, 6, semana.getMonotonia(), estilos.decimal);
            numero(row, 7, semana.getTension(), estilos.decimal);
        }
        terminarTabla(hoja, 3, fila - 1, cabeceras.length);

        int filaMetricas = fila + 1;
        cabecera(hoja, filaMetricas++, 0, new String[]{"Indicador", "Resultado"}, estilos.cabecera);
        filaMetricas = escribirEstadisticas(hoja, filaMetricas, "Carga semanal (UA)",
                resultado.getEstadisticaCargaSemanal(), estilos.decimal, estilos);
        filaMetricas = escribirEstadisticas(hoja, filaMetricas, "Distancia por sesión (km)",
                resultado.getEstadisticaDistanciaSesion(), estilos.decimal, estilos);

        Regresion regresion = resultado.getRegresion();
        filaMetricas = filaMetrica(hoja, filaMetricas, "Pendiente (s/semana)",
                regresion.getPendiente(), estilos.decimal, estilos);
        filaMetricas = filaMetrica(hoja, filaMetricas, "Intersección (s)",
                regresion.getIntercepto(), estilos.segundos, estilos);
        filaMetricas = filaMetrica(hoja, filaMetricas, "R²",
                regresion.getR2(), estilos.decimal, estilos);
        filaMetricas = filaMetrica(hoja, filaMetricas, "Estimación siguiente semana",
                regresion.getEstimacionSiguienteSemana(), estilos.segundos, estilos);
        filaMetricas = filaMetrica(hoja, filaMetricas, "Correlación carga-marca",
                resultado.getCorrelacionCargaMarca(), estilos.decimal, estilos);

        if (!resultado.getSemanas().isEmpty()) {
            crearGraficoCarga(hoja, 4, fila - 1, 0, 3,
                    10, 3, 23, 22, "Carga semanal - " + resultado.getDeportista().getNombre(),
                    "Carga (UA)");
        }
        ajustarAnchos(hoja, 8);
    }

    private int escribirEstadisticas(XSSFSheet hoja, int fila, String nombre,
            EstadisticaDescriptiva estadistica, CellStyle estiloUnidad, Estilos estilos) {
        fila = filaMetrica(hoja, fila, nombre + " - media", estadistica.getMedia(), estiloUnidad, estilos);
        fila = filaMetrica(hoja, fila, nombre + " - mediana", estadistica.getMediana(), estiloUnidad, estilos);
        fila = filaMetrica(hoja, fila, nombre + " - mínimo", estadistica.getMinimo(), estiloUnidad, estilos);
        fila = filaMetrica(hoja, fila, nombre + " - máximo", estadistica.getMaximo(), estiloUnidad, estilos);
        fila = filaMetrica(hoja, fila, nombre + " - desviación típica",
                estadistica.getDesviacionTipica(), estiloUnidad, estilos);
        return filaMetrica(hoja, fila, nombre + " - CV (%)",
                estadistica.getCoeficienteVariacion(), estilos.porcentaje, estilos);
    }

    private int filaMetrica(XSSFSheet hoja, int fila, String nombre,
            Double valor, CellStyle estiloValor, Estilos estilos) {
        Row row = hoja.createRow(fila);
        texto(row, 0, nombre, estilos.texto);
        if (valor == null || !Double.isFinite(valor)) {
            texto(row, 1, "Datos insuficientes", estilos.texto);
        } else {
            numero(row, 1, valor, estiloValor);
        }
        return fila + 1;
    }

    private void escribirRendimiento(XSSFSheet hoja, List<PruebaRendimiento> pruebas,
            ServicioDeportivo servicio, ResultadoAnalisis seleccionado, Estilos estilos) {
        String[] cabeceras = {"ID deportista", "Deportista", "Fecha", "Marca 5 km (s)"};
        cabecera(hoja, 0, 0, cabeceras, estilos.cabecera);
        pruebas = new ArrayList<>(pruebas);
        pruebas.sort(Comparator.comparing(PruebaRendimiento::getFecha)
                .thenComparingInt(PruebaRendimiento::getDeportistaId));
        int fila = 1;
        for (PruebaRendimiento prueba : pruebas) {
            Row row = hoja.createRow(fila++);
            Deportista deportista = servicio.buscarDeportistaPorId(prueba.getDeportistaId());
            numero(row, 0, deportista.getId(), estilos.entero);
            texto(row, 1, deportista.getNombre(), estilos.texto);
            fecha(row, 2, prueba.getFecha(), estilos.fecha);
            numero(row, 3, prueba.getMarca5kSegundos(), estilos.segundos);
        }
        terminarTabla(hoja, 0, fila - 1, cabeceras.length);

        // Datos auxiliares del gráfico: valores reales y valores de la recta de Java.
        if (seleccionado == null || seleccionado.getPruebas().isEmpty()) {
            return;
        }
        int colFecha = 6;
        int colReal = 7;
        int colRecta = 8;
        cabecera(hoja, 0, colFecha,
                new String[]{"Fecha", "Marca real (s)", "Recta ajustada (s)"}, estilos.cabecera);
        List<PruebaRendimiento> pruebasAtleta = seleccionado.getPruebas();
        LocalDate primeraFecha = pruebasAtleta.get(0).getFecha();
        Regresion regresion = seleccionado.getRegresion();
        boolean hayRecta = regresion.getIntercepto() != null && regresion.getPendiente() != null;
        for (int i = 0; i < pruebasAtleta.size(); i++) {
            PruebaRendimiento prueba = pruebasAtleta.get(i);
            Row row = hoja.getRow(i + 1);
            if (row == null) {
                row = hoja.createRow(i + 1);
            }
            texto(row, colFecha, prueba.getFecha().toString(), estilos.texto);
            numero(row, colReal, prueba.getMarca5kSegundos(), estilos.segundos);
            if (hayRecta) {
                double semanas = ChronoUnit.DAYS.between(primeraFecha, prueba.getFecha()) / 7.0;
                double valorRecta = regresion.getIntercepto() + regresion.getPendiente() * semanas;
                numero(row, colRecta, valorRecta, estilos.segundos);
            }
        }
        crearGraficoRendimiento(hoja, 1, pruebasAtleta.size(), colFecha,
                colReal, colRecta, hayRecta, seleccionado.getDeportista().getNombre(), 10, 1);
        ajustarAnchos(hoja, 9);
    }

    private void escribirResumen(XSSFSheet hoja, List<ComparacionDeportista> comparaciones,
            LocalDate desde, LocalDate hasta, Estilos estilos) {
        Row titulo = hoja.createRow(0);
        texto(titulo, 0, "Resumen de rendimiento deportivo", estilos.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        Row periodo = hoja.createRow(1);
        texto(periodo, 0, "Periodo analizado: " + desde + " a " + hasta, estilos.texto);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        String[] cabeceras = {"ID", "Deportista", "Mejor 5 km (s)", "Cambio (s)",
            "Km medios/semana", "Carga media/semana (UA)"};
        cabecera(hoja, 3, 0, cabeceras, estilos.cabecera);
        int fila = 4;
        for (ComparacionDeportista comparacion : comparaciones) {
            Row row = hoja.createRow(fila++);
            numero(row, 0, comparacion.getDeportista().getId(), estilos.entero);
            texto(row, 1, comparacion.getDeportista().getNombre(), estilos.texto);
            numero(row, 2, comparacion.getMejorMarca5kSegundos(), estilos.segundos);
            numero(row, 3, comparacion.getCambioSegundosRespectoPrimeraPrueba(), estilos.segundos);
            numero(row, 4, comparacion.getKilometrosMediosSemana(), estilos.decimal);
            numero(row, 5, comparacion.getCargaMediaSemanaUA(), estilos.decimal);
        }
        terminarTabla(hoja, 3, fila - 1, cabeceras.length);

        // Gráfico comparativo por deportista; las unidades se mantienen en columnas separadas.
        if (!comparaciones.isEmpty()) {
            crearGraficoCarga(hoja, 4, fila - 1, 1, 4, 8, 2, 18, 20,
                    "Kilómetros medios por semana", "Km/semana");
        }
    }

    /** Gráfico de columnas verticales usando las categorías y valores de la tabla. */
    private void crearGraficoCarga(XSSFSheet hoja, int primeraFila, int ultimaFila,
            int columnaCategorias, int columnaValores, int x1, int y1, int x2, int y2,
            String titulo, String tituloEje) {
        XSSFDrawing dibujo = hoja.createDrawingPatriarch();
        XSSFClientAnchor ancla = dibujo.createAnchor(0, 0, 0, 0, x1, y1, x2, y2);
        XSSFChart grafico = dibujo.createChart(ancla);
        grafico.setTitleText(titulo);
        grafico.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);
        XDDFCategoryAxis ejeCategorias = grafico.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis ejeValores = grafico.createValueAxis(AxisPosition.LEFT);
        ejeValores.setTitle(tituloEje);
        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(hoja,
                new CellRangeAddress(primeraFila, ultimaFila, columnaCategorias, columnaCategorias));
        XDDFNumericalDataSource<Double> valores = XDDFDataSourcesFactory.fromNumericCellRange(hoja,
                new CellRangeAddress(primeraFila, ultimaFila, columnaValores, columnaValores));
        XDDFBarChartData datos = (XDDFBarChartData) grafico.createData(
                ChartTypes.BAR, ejeCategorias, ejeValores);
        XDDFChartData.Series serie = datos.addSeries(categorias, valores);
        serie.setTitle(titulo, null);
        datos.setBarDirection(BarDirection.COL);
        grafico.plot(datos);
    }

    private void crearGraficoRendimiento(XSSFSheet hoja, int primeraFila, int ultimaFila,
            int columnaFechas, int columnaReal, int columnaRecta, boolean hayRecta, String nombre,
            int x1, int y1) {
        XSSFDrawing dibujo = hoja.createDrawingPatriarch();
        XSSFClientAnchor ancla = dibujo.createAnchor(0, 0, 0, 0, x1, y1, x1 + 13, y1 + 19);
        XSSFChart grafico = dibujo.createChart(ancla);
        grafico.setTitleText("Marcas reales y recta ajustada - " + nombre);
        grafico.getOrAddLegend().setPosition(LegendPosition.TOP_RIGHT);
        XDDFCategoryAxis ejeFechas = grafico.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis ejeMarcas = grafico.createValueAxis(AxisPosition.LEFT);
        ejeMarcas.setTitle("Tiempo (segundos)");
        XDDFDataSource<String> categorias = XDDFDataSourcesFactory.fromStringCellRange(hoja,
                new CellRangeAddress(primeraFila, ultimaFila, columnaFechas, columnaFechas));
        XDDFNumericalDataSource<Double> marcas = XDDFDataSourcesFactory.fromNumericCellRange(hoja,
                new CellRangeAddress(primeraFila, ultimaFila, columnaReal, columnaReal));
        XDDFChartData datos = grafico.createData(ChartTypes.LINE, ejeFechas, ejeMarcas);
        datos.addSeries(categorias, marcas).setTitle("Marca real", null);
        if (hayRecta) {
            XDDFNumericalDataSource<Double> recta = XDDFDataSourcesFactory.fromNumericCellRange(hoja,
                    new CellRangeAddress(primeraFila, ultimaFila, columnaRecta, columnaRecta));
            datos.addSeries(categorias, recta).setTitle("Recta ajustada", null);
        }
        grafico.plot(datos);
    }

    private Estilos crearEstilos(XSSFWorkbook libro) {
        CreationHelper ayuda = libro.getCreationHelper();
        Estilos estilos = new Estilos();
        estilos.titulo = estiloConBordes(libro);
        Font fuenteTitulo = libro.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
        estilos.titulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilos.titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilos.titulo.setFont(fuenteTitulo);
        estilos.titulo.setAlignment(HorizontalAlignment.CENTER);

        estilos.cabecera = estiloConBordes(libro);
        Font fuenteCabecera = libro.createFont();
        fuenteCabecera.setBold(true);
        fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
        estilos.cabecera.setFont(fuenteCabecera);
        estilos.cabecera.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        estilos.cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilos.cabecera.setWrapText(true);
        estilos.cabecera.setAlignment(HorizontalAlignment.CENTER);

        estilos.texto = estiloConBordes(libro);
        estilos.fecha = formato(libro, ayuda, "dd/mm/yyyy");
        estilos.entero = formato(libro, ayuda, "#,##0");
        estilos.segundos = formato(libro, ayuda, "0 \"s\"");
        estilos.decimal = formato(libro, ayuda, "0.00");
        estilos.porcentaje = formato(libro, ayuda, "0.00\"%\"");
        return estilos;
    }

    private CellStyle formato(XSSFWorkbook libro, CreationHelper ayuda, String formato) {
        CellStyle estilo = estiloConBordes(libro);
        estilo.setDataFormat(ayuda.createDataFormat().getFormat(formato));
        return estilo;
    }

    private CellStyle estiloConBordes(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private void cabecera(XSSFSheet hoja, int fila, int columna,
            String[] titulos, CellStyle estilo) {
        Row row = hoja.getRow(fila);
        if (row == null) {
            row = hoja.createRow(fila);
        }
        for (int i = 0; i < titulos.length; i++) {
            texto(row, columna + i, titulos[i], estilo);
        }
    }

    private void terminarTabla(XSSFSheet hoja, int filaCabecera, int ultimaFila, int columnas) {
        hoja.createFreezePane(0, filaCabecera + 1);
        if (ultimaFila >= filaCabecera) {
            hoja.setAutoFilter(new CellRangeAddress(filaCabecera, ultimaFila, 0, columnas - 1));
        }
        ajustarAnchos(hoja, columnas);
    }

    private void ajustarAnchos(XSSFSheet hoja, int columnas) {
        for (int col = 0; col < columnas; col++) {
            hoja.autoSizeColumn(col);
            if (hoja.getColumnWidth(col) > 56 * 256) {
                hoja.setColumnWidth(col, 56 * 256);
            }
        }
    }

    private void texto(Row row, int columna, String valor, CellStyle estilo) {
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private void fecha(Row row, int columna, LocalDate valor, CellStyle estilo) {
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private void numero(Row row, int columna, Number valor, CellStyle estilo) {
        if (valor == null) {
            return;
        }
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private static final class Estilos {
        private CellStyle titulo;
        private CellStyle cabecera;
        private CellStyle texto;
        private CellStyle fecha;
        private CellStyle entero;
        private CellStyle segundos;
        private CellStyle decimal;
        private CellStyle porcentaje;
    }
}
