package group.analisis.modelo;

/** Datos básicos del atleta. La marca de 5 km se conserva en segundos. */
public class Deportista {

    private final int id;
    private int edad;
    private String nombre;
    private String categoria;
    private String objetivoDeCompeticion;
    private int marcaInicial5kSegundos;

    /** Crea una ficha de atleta y valida sus valores obligatorios. */
    public Deportista(int id, int edad, String nombre, String categoria,
            String objetivoDeCompeticion, int marcaInicial5kSegundos) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        validarPositivo(edad, "La edad");
        validarPositivo(marcaInicial5kSegundos, "La marca inicial de 5 km");

        this.id = id;
        this.edad = edad;
        this.nombre = textoObligatorio(nombre, "El nombre");
        this.categoria = textoObligatorio(categoria, "La categoría");
        this.objetivoDeCompeticion = textoObligatorio(
                objetivoDeCompeticion, "El objetivo de competición");
        this.marcaInicial5kSegundos = marcaInicial5kSegundos;
    }

    public int getId() {
        return id;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        validarPositivo(edad, "La edad");
        this.edad = edad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = textoObligatorio(nombre, "El nombre");
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = textoObligatorio(categoria, "La categoría");
    }

    public String getObjetivoDeCompeticion() {
        return objetivoDeCompeticion;
    }

    public void setObjetivoDeCompeticion(String objetivoDeCompeticion) {
        this.objetivoDeCompeticion = textoObligatorio(
                objetivoDeCompeticion, "El objetivo de competición");
    }

    public int getMarcaInicial5kSegundos() {
        return marcaInicial5kSegundos;
    }

    public void setMarcaInicial5kSegundos(int marcaInicial5kSegundos) {
        validarPositivo(marcaInicial5kSegundos, "La marca inicial de 5 km");
        this.marcaInicial5kSegundos = marcaInicial5kSegundos;
    }

    /** Texto breve que se muestra en los selectores de deportista de Swing. */
    @Override
    public String toString() {
        return id + " - " + nombre;
    }

    private static void validarPositivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo.");
        }
    }

    private static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio.");
        }
        return valor.trim();
    }
}
