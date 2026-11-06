package videojuego;

import java.awt.Image;
import javax.swing.ImageIcon;

public class Jugador extends Personaje {

    private final Image caminar1;
    private final Image caminar2;
    private final Image salto;

    private Image imagenActual;
    private int contadorAnimacion;
    private boolean alternarFrame = true;

    public Jugador(int x, int y) {
        super(x, y, 32, 40, 4, "/imagenes/jugador.png");

        caminar1 = cargarImagen("/imagenes/jugador_walk1.png");
        caminar2 = cargarImagen("/imagenes/jugador_walk2.png");
        salto = cargarImagen("/imagenes/jugador_jump.png");

        imagenActual = super.getImagen();
    }

    private Image cargarImagen(String ruta) {
        var recurso = Jugador.class.getResource(ruta);

        if (recurso == null) {
            return null;
        }

        return new ImageIcon(recurso).getImage()
                .getScaledInstance(getAncho(), getAlto(), Image.SCALE_SMOOTH);
    }

    @Override
    public Image getImagen() {
        return imagenActual;
    }

    public void actualizarAnimacion(boolean caminando, boolean saltando) {
        if (saltando && salto != null) {
            imagenActual = salto;
            contadorAnimacion = 0;
            return;
        }

        if (caminando && caminar1 != null && caminar2 != null) {
            contadorAnimacion++;

            if (contadorAnimacion >= 6) {
                alternarFrame = !alternarFrame;
                contadorAnimacion = 0;
            }

            imagenActual = alternarFrame ? caminar1 : caminar2;
        } else {
            contadorAnimacion = 0;
            imagenActual = super.getImagen();
        }
    }

    public void moverIzquierda() {
        int nuevaX = Math.max(0, getX() - velocidad);
        mover(nuevaX - getX(), 0);
    }

    public void moverDerecha(int anchoPanel) {
        int nuevaX = Math.min(getX() + velocidad, anchoPanel - getAncho());
        mover(nuevaX - getX(), 0);
    }
}