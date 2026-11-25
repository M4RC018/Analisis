package group.analisis.modelo;

import java.time.LocalDate;

/** Resultado fechado de un test de 5 km, con la marca almacenada en segundos. */
public class PruebaRendimiento {

    private LocalDate fecha;
    private int deportistaId;
    private int marca5kSegundos;

    public PruebaRendimiento(LocalDate fecha, int deportistaId, int marca5kSegundos) {
        setFecha(fecha);
        setDeportistaId(deportistaId);
        setMarca5kSegundos(marca5kSegundos);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la prueba es obligatoria.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la prueba no puede ser futura.");
        }
        this.fecha = fecha;
    }

    public int getDeportistaId() {
        return deportistaId;
    }

    public void setDeportistaId(int deportistaId) {
        if (deportistaId <= 0) {
            throw new IllegalArgumentException("El identificador del deportista debe ser positivo.");
        }
        this.deportistaId = deportistaId;
    }

    public int getMarca5kSegundos() {
        return marca5kSegundos;
    }

    public void setMarca5kSegundos(int marca5kSegundos) {
        if (marca5kSegundos <= 0) {
            throw new IllegalArgumentException("La marca de 5 km debe ser positiva y estar en segundos.");
        }
        this.marca5kSegundos = marca5kSegundos;
    }
}
