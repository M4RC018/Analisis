package group.analisis.modelo;

import java.time.LocalDate;

/** Una sesión registrada. La carga se expresa en unidades arbitrarias (UA). */
public class Entrenamiento {

    private LocalDate fecha;
    private int deportistaId;
    private String tipoSesion;
    private int duracionMinutos;
    private double distanciaKm;
    private int frecuenciaCardiacaMedia;
    private int rpe;

    public Entrenamiento(LocalDate fecha, int deportistaId, String tipoSesion,
            int duracionMinutos, double distanciaKm,
            int frecuenciaCardiacaMedia, int rpe) {
        setFecha(fecha);
        setDeportistaId(deportistaId);
        setTipoSesion(tipoSesion);
        setDuracionMinutos(duracionMinutos);
        setDistanciaKm(distanciaKm);
        setFrecuenciaCardiacaMedia(frecuenciaCardiacaMedia);
        setRpe(rpe);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del entrenamiento no puede ser futura.");
        }
        this.fecha = fecha;
    }

    public int getDeportistaId() {
        return deportistaId;
    }

    public void setDeportistaId(int deportistaId) {
        validarPositivo(deportistaId, "El identificador del deportista");
        this.deportistaId = deportistaId;
    }

    public String getTipoSesion() {
        return tipoSesion;
    }

    public void setTipoSesion(String tipoSesion) {
        if (tipoSesion == null || tipoSesion.isBlank()) {
            throw new IllegalArgumentException("El tipo de sesión es obligatorio.");
        }
        this.tipoSesion = tipoSesion.trim();
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        validarPositivo(duracionMinutos, "La duración");
        this.duracionMinutos = duracionMinutos;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(double distanciaKm) {
        if (!Double.isFinite(distanciaKm) || distanciaKm <= 0) {
            throw new IllegalArgumentException("La distancia debe ser un número positivo.");
        }
        this.distanciaKm = distanciaKm;
    }

    public int getFrecuenciaCardiacaMedia() {
        return frecuenciaCardiacaMedia;
    }

    public void setFrecuenciaCardiacaMedia(int frecuenciaCardiacaMedia) {
        validarPositivo(frecuenciaCardiacaMedia, "La frecuencia cardiaca media");
        this.frecuenciaCardiacaMedia = frecuenciaCardiacaMedia;
    }

    public int getRpe() {
        return rpe;
    }

    public void setRpe(int rpe) {
        if (rpe < 1 || rpe > 10) {
            throw new IllegalArgumentException("El RPE debe estar entre 1 y 10.");
        }
        this.rpe = rpe;
    }

    public long getCargaSesionUA() {
        return (long) duracionMinutos * rpe;
    }

    /** Calcula el ritmo de la sesión para detectar datos de distancia/duración incoherentes. */
    public double getRitmoMinutosPorKm() {
        return (double) duracionMinutos / distanciaKm;
    }

    private static void validarPositivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo.");
        }
    }
}
