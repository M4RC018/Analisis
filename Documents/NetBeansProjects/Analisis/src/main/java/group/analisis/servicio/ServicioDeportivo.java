package group.analisis.servicio;

import group.analisis.modelo.Deportista;
import group.analisis.modelo.Entrenamiento;
import group.analisis.modelo.PruebaRendimiento;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene en memoria los deportistas, las sesiones y las pruebas de la aplicación.
 * Los datos duran mientras el programa está abierto.
 */
public class ServicioDeportivo {

    // Márgenes amplios usados solo para detectar errores de captura del ritmo.
    private static final double RITMO_MINIMO_MIN_KM = 1.0;
    private static final double RITMO_MAXIMO_MIN_KM = 30.0;

    private final List<Deportista> deportistas = new ArrayList<>();
    private final List<Entrenamiento> entrenamientos = new ArrayList<>();
    private final List<PruebaRendimiento> pruebas = new ArrayList<>();

    /** Añade un deportista y evita que se repita su identificador. */
    public void agregarDeportista(Deportista deportista) {
        if (deportista == null) {
            throw new IllegalArgumentException("El deportista es obligatorio.");
        }
        if (buscarDeportistaOpcional(deportista.getId()) != null) {
            throw new IllegalArgumentException("Ya existe un deportista con ese identificador.");
        }
        deportistas.add(deportista);
    }

    /** Devuelve una copia de la lista para que otras clases no la modifiquen directamente. */
    public List<Deportista> getDeportistas() {
        return List.copyOf(deportistas);
    }

    /** Busca un deportista; informa claramente si el identificador no existe. */
    public Deportista buscarDeportistaPorId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }

        Deportista deportista = buscarDeportistaOpcional(id);
        if (deportista == null) {
            throw new IllegalArgumentException("No existe un deportista con ese identificador.");
        }
        return deportista;
    }

    /** Reemplaza los datos editables sin cambiar el identificador del deportista. */
    public void modificarDeportista(Deportista actualizado) {
        if (actualizado == null) {
            throw new IllegalArgumentException("El deportista actualizado es obligatorio.");
        }

        Deportista existente = buscarDeportistaPorId(actualizado.getId());
        existente.setEdad(actualizado.getEdad());
        existente.setNombre(actualizado.getNombre());
        existente.setCategoria(actualizado.getCategoria());
        existente.setObjetivoDeCompeticion(actualizado.getObjetivoDeCompeticion());
        existente.setMarcaInicial5kSegundos(actualizado.getMarcaInicial5kSegundos());
    }

    /** No permite borrar un atleta que aún tenga sesiones o pruebas vinculadas. */
    public void eliminarDeportista(int id) {
        Deportista deportista = buscarDeportistaPorId(id);

        for (Entrenamiento entrenamiento : entrenamientos) {
            if (entrenamiento.getDeportistaId() == id) {
                throw new IllegalStateException(
                        "No se puede eliminar: el deportista tiene entrenamientos registrados.");
            }
        }
        for (PruebaRendimiento prueba : pruebas) {
            if (prueba.getDeportistaId() == id) {
                throw new IllegalStateException(
                        "No se puede eliminar: el deportista tiene pruebas registradas.");
            }
        }

        deportistas.remove(deportista);
    }

    /** Guarda una sesión solo si su deportista existe y el ritmo parece una entrada plausible. */
    public void agregarEntrenamiento(Entrenamiento entrenamiento) {
        if (entrenamiento == null) {
            throw new IllegalArgumentException("El entrenamiento es obligatorio.");
        }
        buscarDeportistaPorId(entrenamiento.getDeportistaId());

        double ritmo = entrenamiento.getRitmoMinutosPorKm();
        if (ritmo < RITMO_MINIMO_MIN_KM || ritmo > RITMO_MAXIMO_MIN_KM) {
            throw new IllegalArgumentException(
                    "La duración y la distancia producen un ritmo fuera del margen de comprobación "
                    + RITMO_MINIMO_MIN_KM + "–" + RITMO_MAXIMO_MIN_KM + " min/km."
            );
        }
        entrenamientos.add(entrenamiento);
    }

    /** Devuelve una copia de las sesiones registradas. */
    public List<Entrenamiento> getEntrenamientos() {
        return List.copyOf(entrenamientos);
    }

    /** Guarda una prueba si el deportista existe y no tiene otra prueba el mismo día. */
    public void agregarPruebaRendimiento(PruebaRendimiento prueba) {
        if (prueba == null) {
            throw new IllegalArgumentException("La prueba es obligatoria.");
        }
        buscarDeportistaPorId(prueba.getDeportistaId());

        for (PruebaRendimiento existente : pruebas) {
            if (existente.getDeportistaId() == prueba.getDeportistaId()
                    && existente.getFecha().equals(prueba.getFecha())) {
                throw new IllegalArgumentException(
                        "Ese deportista ya tiene una prueba registrada en esa fecha.");
            }
        }
        pruebas.add(prueba);
    }

    /** Devuelve una copia de las pruebas registradas. */
    public List<PruebaRendimiento> getPruebas() {
        return List.copyOf(pruebas);
    }

    private Deportista buscarDeportistaOpcional(int id) {
        for (Deportista deportista : deportistas) {
            if (deportista.getId() == id) {
                return deportista;
            }
        }
        return null;
    }
}
