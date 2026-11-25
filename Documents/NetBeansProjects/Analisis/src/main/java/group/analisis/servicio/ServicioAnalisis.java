package group.analisis.servicio;

import group.analisis.modelo.Deportista;
import group.analisis.modelo.Entrenamiento;
import group.analisis.modelo.PruebaRendimiento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calcula los indicadores de la primera parte. No escribe en Excel ni dibuja
 * la interfaz: recibe datos del servicio y devuelve resultados ya calculados.
 */
public class ServicioAnalisis {

    private final ServicioDeportivo servicioDeportivo;

    public ServicioAnalisis(ServicioDeportivo servicioDeportivo) {
        if (servicioDeportivo == null) {
            throw new IllegalArgumentException("El servicio deportivo es obligatorio.");
        }
        this.servicioDeportivo = servicioDeportivo;
    }

    /** Analiza a un atleta dentro de un intervalo de fechas inclusivo. */
    public ResultadoAnalisis analizar(int deportistaId, LocalDate desde, LocalDate hasta) {
        validarIntervalo(desde, hasta);
        Deportista deportista = servicioDeportivo.buscarDeportistaPorId(deportistaId);

        List<Entrenamiento> sesiones = new ArrayList<>();
        for (Entrenamiento sesion : servicioDeportivo.getEntrenamientos()) {
            if (sesion.getDeportistaId() == deportistaId
                    && estaDentro(sesion.getFecha(), desde, hasta)) {
                sesiones.add(sesion);
            }
        }
        sesiones.sort(Comparator.comparing(Entrenamiento::getFecha));

        List<PruebaRendimiento> pruebas = new ArrayList<>();
        for (PruebaRendimiento prueba : servicioDeportivo.getPruebas()) {
            if (prueba.getDeportistaId() == deportistaId
                    && estaDentro(prueba.getFecha(), desde, hasta)) {
                pruebas.add(prueba);
            }
        }
        pruebas.sort(Comparator.comparing(PruebaRendimiento::getFecha));

        List<ResumenSemanal> semanas = construirResumenSemanal(desde, hasta, sesiones);
        List<Double> cargasSemanalesCompletas = new ArrayList<>();
        for (ResumenSemanal semana : semanas) {
            if (semana.isSemanaCompleta()) {
                cargasSemanalesCompletas.add((double) semana.getCargaUA());
            }
        }

        List<Double> distancias = new ArrayList<>();
        double kilometrosTotales = 0;
        long minutosTotales = 0;
        long cargaTotalUA = 0;
        for (Entrenamiento sesion : sesiones) {
            distancias.add(sesion.getDistanciaKm());
            kilometrosTotales += sesion.getDistanciaKm();
            minutosTotales += sesion.getDuracionMinutos();
            cargaTotalUA += sesion.getCargaSesionUA();
        }

        EstadisticaDescriptiva estadisticaCarga = calcularEstadistica(cargasSemanalesCompletas);
        EstadisticaDescriptiva estadisticaDistancia = calcularEstadistica(distancias);
        Regresion regresion = calcularRegresion(pruebas);
        ResultadoCorrelacion correlacion = calcularCorrelacion(pruebas, semanas);
        Integer mejorMarca = pruebas.isEmpty()
                ? null
                : pruebas.stream().mapToInt(PruebaRendimiento::getMarca5kSegundos).min().getAsInt();

        String interpretacion = crearInterpretacion(
                regresion, correlacion.coeficiente(), estadisticaCarga);

        return new ResultadoAnalisis(
                deportista,
                desde,
                hasta,
                List.copyOf(sesiones),
                List.copyOf(pruebas),
                List.copyOf(semanas),
                kilometrosTotales,
                minutosTotales,
                cargaTotalUA,
                mejorMarca,
                estadisticaCarga,
                estadisticaDistancia,
                regresion,
                correlacion.coeficiente(),
                correlacion.pares(),
                interpretacion
        );
    }

    /**
     * Compara a todos los atletas por separado en las mismas unidades que
     * solicita el enunciado. No mezcla segundos, kilómetros y unidades de carga.
     */
    public List<ComparacionDeportista> compararDeportistas(LocalDate desde, LocalDate hasta) {
        validarIntervalo(desde, hasta);
        List<ComparacionDeportista> comparaciones = new ArrayList<>();

        for (Deportista deportista : servicioDeportivo.getDeportistas()) {
            ResultadoAnalisis resultado = analizar(deportista.getId(), desde, hasta);
            Integer cambioSegundos = null;
            if (resultado.getPruebas().size() >= 2) {
                int primera = resultado.getPruebas().get(0).getMarca5kSegundos();
                int ultima = resultado.getPruebas()
                        .get(resultado.getPruebas().size() - 1).getMarca5kSegundos();
                cambioSegundos = ultima - primera;
            }

            List<ResumenSemanal> semanasCompletas = resultado.getSemanas().stream()
                    .filter(ResumenSemanal::isSemanaCompleta)
                    .toList();
            Double mediaKmSemana = semanasCompletas.isEmpty()
                    ? null
                    : semanasCompletas.stream().mapToDouble(ResumenSemanal::getKilometros).average().orElse(0);
            Double mediaCargaSemana = semanasCompletas.isEmpty()
                    ? null
                    : semanasCompletas.stream().mapToDouble(ResumenSemanal::getCargaUA).average().orElse(0);

            comparaciones.add(new ComparacionDeportista(
                    deportista,
                    resultado.getMejorMarca5kSegundos(),
                    cambioSegundos,
                    mediaKmSemana,
                    mediaCargaSemana
            ));
        }
        return List.copyOf(comparaciones);
    }

    private List<ResumenSemanal> construirResumenSemanal(
            LocalDate desde, LocalDate hasta, List<Entrenamiento> sesiones) {
        Map<LocalDate, AcumuladorSemana> acumuladores = new TreeMap<>();
        LocalDate primeraSemana = inicioDeSemana(desde);
        LocalDate ultimaSemana = inicioDeSemana(hasta);

        // Se crean también las semanas sin sesiones, que deben contar como carga cero.
        for (LocalDate lunes = primeraSemana; !lunes.isAfter(ultimaSemana); lunes = lunes.plusWeeks(1)) {
            boolean completa = !lunes.isBefore(desde) && !lunes.plusDays(6).isAfter(hasta);
            acumuladores.put(lunes, new AcumuladorSemana(lunes, completa));
        }

        for (Entrenamiento sesion : sesiones) {
            LocalDate lunes = inicioDeSemana(sesion.getFecha());
            AcumuladorSemana acumulador = acumuladores.get(lunes);
            if (acumulador != null) {
                acumulador.kilometros += sesion.getDistanciaKm();
                acumulador.minutos += sesion.getDuracionMinutos();
                acumulador.cargaUA += sesion.getCargaSesionUA();
                acumulador.sesiones++;
                acumulador.cargaDiaria.merge(
                        sesion.getFecha(), sesion.getCargaSesionUA(), Long::sum);
            }
        }

        List<ResumenSemanal> resultado = new ArrayList<>();
        for (AcumuladorSemana semana : acumuladores.values()) {
            Double monotonia = null;
            Double tension = null;

            // La monotonía usa siete días. En semanas parciales no se inventa carga cero.
            if (semana.completa) {
                double mediaDiaria = semana.cargaUA / 7.0;
                double sumaCuadrados = 0;
                for (int dia = 0; dia < 7; dia++) {
                    LocalDate fecha = semana.lunes.plusDays(dia);
                    double cargaDia = semana.cargaDiaria.getOrDefault(fecha, 0L);
                    sumaCuadrados += Math.pow(cargaDia - mediaDiaria, 2);
                }
                double desviacionDiaria = Math.sqrt(sumaCuadrados / 7.0);
                if (desviacionDiaria > 0) {
                    monotonia = mediaDiaria / desviacionDiaria;
                    tension = semana.cargaUA * monotonia;
                }
            }

            resultado.add(new ResumenSemanal(
                    semana.lunes,
                    semana.kilometros,
                    semana.minutos,
                    semana.cargaUA,
                    semana.sesiones,
                    null,
                    monotonia,
                    tension,
                    semana.completa
            ));
        }

        // La media móvil solo aparece con tres semanas completas consecutivas.
        for (int i = 2; i < resultado.size(); i++) {
            ResumenSemanal actual = resultado.get(i);
            ResumenSemanal anterior1 = resultado.get(i - 1);
            ResumenSemanal anterior2 = resultado.get(i - 2);
            if (actual.isSemanaCompleta()
                    && anterior1.isSemanaCompleta()
                    && anterior2.isSemanaCompleta()) {
                double mediaMovil = (actual.getCargaUA()
                        + anterior1.getCargaUA()
                        + anterior2.getCargaUA()) / 3.0;
                resultado.set(i, actual.conMediaMovil(mediaMovil));
            }
        }
        return resultado;
    }

    /** Estadística descriptiva con desviación típica poblacional. */
    private EstadisticaDescriptiva calcularEstadistica(List<Double> valores) {
        if (valores.isEmpty()) {
            return new EstadisticaDescriptiva(0, null, null, null, null, null, null);
        }

        List<Double> ordenados = new ArrayList<>(valores);
        ordenados.sort(Double::compareTo);
        double media = valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int n = ordenados.size();
        double mediana = n % 2 == 1
                ? ordenados.get(n / 2)
                : (ordenados.get(n / 2 - 1) + ordenados.get(n / 2)) / 2.0;
        double minimo = ordenados.get(0);
        double maximo = ordenados.get(n - 1);

        double sumaCuadrados = 0;
        for (double valor : valores) {
            sumaCuadrados += Math.pow(valor - media, 2);
        }
        double desviacion = Math.sqrt(sumaCuadrados / n);
        Double coeficienteVariacion = media == 0 ? null : desviacion / media * 100.0;

        return new EstadisticaDescriptiva(
                n, media, mediana, minimo, maximo, desviacion, coeficienteVariacion);
    }

    /** Ajuste de mínimos cuadrados; X se mide en semanas desde la primera prueba. */
    private Regresion calcularRegresion(List<PruebaRendimiento> pruebas) {
        int n = pruebas.size();
        if (n < 2) {
            return new Regresion(n, null, null, null, null);
        }

        LocalDate fechaInicial = pruebas.get(0).getFecha();
        double[] x = new double[n];
        double[] y = new double[n];
        double mediaX = 0;
        double mediaY = 0;
        for (int i = 0; i < n; i++) {
            x[i] = ChronoUnit.DAYS.between(fechaInicial, pruebas.get(i).getFecha()) / 7.0;
            y[i] = pruebas.get(i).getMarca5kSegundos();
            mediaX += x[i];
            mediaY += y[i];
        }
        mediaX /= n;
        mediaY /= n;

        double sxx = 0;
        double sxy = 0;
        double syy = 0;
        for (int i = 0; i < n; i++) {
            sxx += (x[i] - mediaX) * (x[i] - mediaX);
            sxy += (x[i] - mediaX) * (y[i] - mediaY);
            syy += (y[i] - mediaY) * (y[i] - mediaY);
        }
        if (sxx == 0) {
            return new Regresion(n, null, null, null, null);
        }

        double pendiente = sxy / sxx;
        double intercepto = mediaY - pendiente * mediaX;
        Double r2 = null;
        if (syy > 0) {
            double errorCuadratico = 0;
            for (int i = 0; i < n; i++) {
                double estimado = intercepto + pendiente * x[i];
                errorCuadratico += Math.pow(y[i] - estimado, 2);
            }
            r2 = Math.max(0, Math.min(1, 1 - errorCuadratico / syy));
        }

        double ultimaX = x[n - 1];
        double estimacionSiguienteSemana = intercepto + pendiente * (ultimaX + 1);
        return new Regresion(n, pendiente, intercepto, r2, estimacionSiguienteSemana);
    }

    /** Correlaciona carga semanal y marca solo en semanas completas con datos. */
    private ResultadoCorrelacion calcularCorrelacion(
            List<PruebaRendimiento> pruebas, List<ResumenSemanal> semanas) {
        Map<LocalDate, ResumenSemanal> porSemana = new TreeMap<>();
        for (ResumenSemanal semana : semanas) {
            if (semana.isSemanaCompleta()) {
                porSemana.put(semana.getLunes(), semana);
            }
        }

        // Si hay varios tests en una misma semana, se empareja la carga con el último.
        // Así una semana no pesa más que otra por tener pruebas repetidas.
        Map<LocalDate, PruebaRendimiento> ultimaPruebaPorSemana = new TreeMap<>();
        for (PruebaRendimiento prueba : pruebas) {
            ultimaPruebaPorSemana.put(inicioDeSemana(prueba.getFecha()), prueba);
        }

        List<Double> cargas = new ArrayList<>();
        List<Double> marcas = new ArrayList<>();
        for (Map.Entry<LocalDate, PruebaRendimiento> entrada : ultimaPruebaPorSemana.entrySet()) {
            ResumenSemanal semana = porSemana.get(entrada.getKey());
            if (semana != null) {
                cargas.add((double) semana.getCargaUA());
                marcas.add((double) entrada.getValue().getMarca5kSegundos());
            }
        }

        int n = cargas.size();
        if (n < 2) {
            return new ResultadoCorrelacion(null, n);
        }
        double mediaCarga = cargas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double mediaMarca = marcas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumaProducto = 0;
        double sumaCarga2 = 0;
        double sumaMarca2 = 0;
        for (int i = 0; i < n; i++) {
            double deltaCarga = cargas.get(i) - mediaCarga;
            double deltaMarca = marcas.get(i) - mediaMarca;
            sumaProducto += deltaCarga * deltaMarca;
            sumaCarga2 += deltaCarga * deltaCarga;
            sumaMarca2 += deltaMarca * deltaMarca;
        }
        double denominador = Math.sqrt(sumaCarga2 * sumaMarca2);
        return denominador == 0
                ? new ResultadoCorrelacion(null, n)
                : new ResultadoCorrelacion(sumaProducto / denominador, n);
    }

    private String crearInterpretacion(
            Regresion regresion,
            Double correlacion,
            EstadisticaDescriptiva estadisticaCarga) {
        StringBuilder texto = new StringBuilder();
        if (regresion.getPendiente() == null) {
            texto.append("Hay menos de dos pruebas distintas para describir una tendencia.");
        } else if (regresion.getPendiente() < 0) {
            texto.append("La recta describe una tendencia a reducir el tiempo de 5 km.");
        } else if (regresion.getPendiente() > 0) {
            texto.append("La recta describe una tendencia a aumentar el tiempo de 5 km.");
        } else {
            texto.append("La recta describe un tiempo de 5 km estable en las pruebas observadas.");
        }

        if (estadisticaCarga.getCoeficienteVariacion() != null) {
            texto.append(String.format(Locale.forLanguageTag("es-ES"),
                    " CV de carga semanal: %.1f%%.",
                    estadisticaCarga.getCoeficienteVariacion()));
        } else {
            texto.append(" No hay semanas completas suficientes para resumir la variabilidad de carga.");
        }

        if (correlacion == null) {
            texto.append(" La correlación carga-marca no está definida con estos datos.");
        } else if (correlacion > 0) {
            texto.append(String.format(Locale.forLanguageTag("es-ES"),
                    " La asociación lineal carga-marca es positiva (r = %.2f).", correlacion));
        } else if (correlacion < 0) {
            texto.append(String.format(Locale.forLanguageTag("es-ES"),
                    " La asociación lineal carga-marca es negativa (r = %.2f).", correlacion));
        } else {
            texto.append(" La asociación lineal carga-marca es nula (r = 0).");
        }
        texto.append(" Correlación no implica causalidad; son indicadores didácticos, no diagnósticos.");
        return texto.toString();
    }

    private static boolean estaDentro(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private static LocalDate inicioDeSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static void validarIntervalo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las dos fechas del análisis son obligatorias.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
        }
    }

    private static final class AcumuladorSemana {
        private final LocalDate lunes;
        private final boolean completa;
        private final Map<LocalDate, Long> cargaDiaria = new TreeMap<>();
        private double kilometros;
        private long minutos;
        private long cargaUA;
        private int sesiones;

        private AcumuladorSemana(LocalDate lunes, boolean completa) {
            this.lunes = lunes;
            this.completa = completa;
        }
    }

    private record ResultadoCorrelacion(Double coeficiente, int pares) {
    }

    /** Resultado agregado de una semana de calendario (lunes a domingo). */
    public static final class ResumenSemanal {
        private final LocalDate lunes;
        private final double kilometros;
        private final long minutos;
        private final long cargaUA;
        private final int sesiones;
        private final Double mediaMovilCarga;
        private final Double monotonia;
        private final Double tension;
        private final boolean semanaCompleta;

        private ResumenSemanal(LocalDate lunes, double kilometros, long minutos,
                long cargaUA, int sesiones, Double mediaMovilCarga,
                Double monotonia, Double tension, boolean semanaCompleta) {
            this.lunes = lunes;
            this.kilometros = kilometros;
            this.minutos = minutos;
            this.cargaUA = cargaUA;
            this.sesiones = sesiones;
            this.mediaMovilCarga = mediaMovilCarga;
            this.monotonia = monotonia;
            this.tension = tension;
            this.semanaCompleta = semanaCompleta;
        }

        private ResumenSemanal conMediaMovil(double valor) {
            return new ResumenSemanal(lunes, kilometros, minutos, cargaUA,
                    sesiones, valor, monotonia, tension, semanaCompleta);
        }

        public LocalDate getLunes() { return lunes; }
        public double getKilometros() { return kilometros; }
        public long getMinutos() { return minutos; }
        public long getCargaUA() { return cargaUA; }
        public int getSesiones() { return sesiones; }
        public Double getMediaMovilCarga() { return mediaMovilCarga; }
        public Double getMonotonia() { return monotonia; }
        public Double getTension() { return tension; }
        public boolean isSemanaCompleta() { return semanaCompleta; }
    }

    /** Media, mediana, extremos, desviación típica poblacional y CV. */
    public static final class EstadisticaDescriptiva {
        private final int cantidad;
        private final Double media;
        private final Double mediana;
        private final Double minimo;
        private final Double maximo;
        private final Double desviacionTipica;
        private final Double coeficienteVariacion;

        private EstadisticaDescriptiva(int cantidad, Double media, Double mediana,
                Double minimo, Double maximo, Double desviacionTipica,
                Double coeficienteVariacion) {
            this.cantidad = cantidad;
            this.media = media;
            this.mediana = mediana;
            this.minimo = minimo;
            this.maximo = maximo;
            this.desviacionTipica = desviacionTipica;
            this.coeficienteVariacion = coeficienteVariacion;
        }

        public int getCantidad() { return cantidad; }
        public Double getMedia() { return media; }
        public Double getMediana() { return mediana; }
        public Double getMinimo() { return minimo; }
        public Double getMaximo() { return maximo; }
        public Double getDesviacionTipica() { return desviacionTipica; }
        public Double getCoeficienteVariacion() { return coeficienteVariacion; }
    }

    /** Parámetros de la regresión y estimación de la semana siguiente. */
    public static final class Regresion {
        private final int pruebas;
        private final Double pendiente;
        private final Double intercepto;
        private final Double r2;
        private final Double estimacionSiguienteSemana;

        private Regresion(int pruebas, Double pendiente, Double intercepto,
                Double r2, Double estimacionSiguienteSemana) {
            this.pruebas = pruebas;
            this.pendiente = pendiente;
            this.intercepto = intercepto;
            this.r2 = r2;
            this.estimacionSiguienteSemana = estimacionSiguienteSemana;
        }

        public int getPruebas() { return pruebas; }
        public Double getPendiente() { return pendiente; }
        public Double getIntercepto() { return intercepto; }
        public Double getR2() { return r2; }
        public Double getEstimacionSiguienteSemana() { return estimacionSiguienteSemana; }
    }

    /** Todos los resultados necesarios para mostrar o exportar un análisis. */
    public static final class ResultadoAnalisis {
        private final Deportista deportista;
        private final LocalDate desde;
        private final LocalDate hasta;
        private final List<Entrenamiento> entrenamientos;
        private final List<PruebaRendimiento> pruebas;
        private final List<ResumenSemanal> semanas;
        private final double kilometrosTotales;
        private final long minutosTotales;
        private final long cargaTotalUA;
        private final Integer mejorMarca5kSegundos;
        private final EstadisticaDescriptiva estadisticaCargaSemanal;
        private final EstadisticaDescriptiva estadisticaDistanciaSesion;
        private final Regresion regresion;
        private final Double correlacionCargaMarca;
        private final int paresCorrelacion;
        private final String interpretacion;

        private ResultadoAnalisis(Deportista deportista, LocalDate desde, LocalDate hasta,
                List<Entrenamiento> entrenamientos, List<PruebaRendimiento> pruebas,
                List<ResumenSemanal> semanas, double kilometrosTotales,
                long minutosTotales, long cargaTotalUA, Integer mejorMarca5kSegundos,
                EstadisticaDescriptiva estadisticaCargaSemanal,
                EstadisticaDescriptiva estadisticaDistanciaSesion, Regresion regresion,
                Double correlacionCargaMarca, int paresCorrelacion, String interpretacion) {
            this.deportista = deportista;
            this.desde = desde;
            this.hasta = hasta;
            this.entrenamientos = entrenamientos;
            this.pruebas = pruebas;
            this.semanas = semanas;
            this.kilometrosTotales = kilometrosTotales;
            this.minutosTotales = minutosTotales;
            this.cargaTotalUA = cargaTotalUA;
            this.mejorMarca5kSegundos = mejorMarca5kSegundos;
            this.estadisticaCargaSemanal = estadisticaCargaSemanal;
            this.estadisticaDistanciaSesion = estadisticaDistanciaSesion;
            this.regresion = regresion;
            this.correlacionCargaMarca = correlacionCargaMarca;
            this.paresCorrelacion = paresCorrelacion;
            this.interpretacion = interpretacion;
        }

        public Deportista getDeportista() { return deportista; }
        public LocalDate getDesde() { return desde; }
        public LocalDate getHasta() { return hasta; }
        public List<Entrenamiento> getEntrenamientos() { return entrenamientos; }
        public List<PruebaRendimiento> getPruebas() { return pruebas; }
        public List<ResumenSemanal> getSemanas() { return semanas; }
        public double getKilometrosTotales() { return kilometrosTotales; }
        public long getMinutosTotales() { return minutosTotales; }
        public long getCargaTotalUA() { return cargaTotalUA; }
        public Integer getMejorMarca5kSegundos() { return mejorMarca5kSegundos; }
        public EstadisticaDescriptiva getEstadisticaCargaSemanal() { return estadisticaCargaSemanal; }
        public EstadisticaDescriptiva getEstadisticaDistanciaSesion() { return estadisticaDistanciaSesion; }
        public Regresion getRegresion() { return regresion; }
        public Double getCorrelacionCargaMarca() { return correlacionCargaMarca; }
        public int getParesCorrelacion() { return paresCorrelacion; }
        public String getInterpretacion() { return interpretacion; }
    }

    /** Fila comparativa que mantiene separadas las unidades de cada indicador. */
    public static final class ComparacionDeportista {
        private final Deportista deportista;
        private final Integer mejorMarca5kSegundos;
        private final Integer cambioSegundosRespectoPrimeraPrueba;
        private final Double kilometrosMediosSemana;
        private final Double cargaMediaSemanaUA;

        private ComparacionDeportista(Deportista deportista, Integer mejorMarca5kSegundos,
                Integer cambioSegundosRespectoPrimeraPrueba,
                Double kilometrosMediosSemana, Double cargaMediaSemanaUA) {
            this.deportista = deportista;
            this.mejorMarca5kSegundos = mejorMarca5kSegundos;
            this.cambioSegundosRespectoPrimeraPrueba = cambioSegundosRespectoPrimeraPrueba;
            this.kilometrosMediosSemana = kilometrosMediosSemana;
            this.cargaMediaSemanaUA = cargaMediaSemanaUA;
        }

        public Deportista getDeportista() { return deportista; }
        public Integer getMejorMarca5kSegundos() { return mejorMarca5kSegundos; }
        public Integer getCambioSegundosRespectoPrimeraPrueba() { return cambioSegundosRespectoPrimeraPrueba; }
        public Double getKilometrosMediosSemana() { return kilometrosMediosSemana; }
        public Double getCargaMediaSemanaUA() { return cargaMediaSemanaUA; }
    }
}
