package videojuego;

import java.awt.Image;
import java.net.URL;
import java.util.Locale;
import javax.swing.ImageIcon;

public class Enemigo extends Personaje {

    private static final int NUMERO_FOTOGRAMAS = 10;
    private static final int TICKS_POR_FOTOGRAMA = 8;

    private final Image[] fotogramas;
    private int dx = 2;
    private int fotogramaActual;
    private int ticksAnimacion;

    public Enemigo(int x, int y) {
        super(x, y, 32, 32, 2,
                "/imagenes/enemigos/verde/verde_fotograma_01.png");

        fotogramas = new Image[NUMERO_FOTOGRAMAS];
        fotogramas[0] = super.getImagen();
        for (int i = 1; i < fotogramas.length; i++) {
            fotogramas[i] = cargarFotograma(i + 1);
        }
    }

    private Image cargarFotograma(int numero) {
        String ruta = String.format(Locale.ROOT,
                "/imagenes/enemigos/verde/verde_fotograma_%02d.png", numero);
        URL recurso = Enemigo.class.getResource(ruta);
        if (recurso == null) {
            throw new IllegalStateException("No se encontró el recurso: " + ruta);
        }

        return new ImageIcon(recurso).getImage().getScaledInstance(
                getAncho(), getAlto(), Image.SCALE_SMOOTH);
    }

    @Override
    public Image getImagen() {
        return fotogramas[fotogramaActual];
    }

    public void actualizarAnimacion() {
        ticksAnimacion++;
        if (ticksAnimacion >= TICKS_POR_FOTOGRAMA) {
            ticksAnimacion = 0;
            fotogramaActual = (fotogramaActual + 1) % fotogramas.length;
        }
    }

    public void moverAutomaticamente(int anchoPanel) {
        int nuevaX = getX() + dx;

        if (nuevaX <= 420) {
            nuevaX = 420;
            dx = 2;
        } else if (nuevaX + getAncho() >= anchoPanel) {
            nuevaX = anchoPanel - getAncho();
            dx = -2;
        }

        mover(nuevaX - getX(), 0);
    }

    public boolean estaMirandoIzquierda() {
        return dx < 0;
    }

    public void reiniciar(int x, int y) {
        setPosicion(x, y);
        dx = 2;
        fotogramaActual = 0;
        ticksAnimacion = 0;
    }
}