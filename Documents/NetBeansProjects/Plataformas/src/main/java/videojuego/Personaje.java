package videojuego;

import java.awt.Rectangle;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 * Datos y operaciones comunes de los personajes del juego.
 * PanelJuego conserva el dibujo de los personajes.
 */
public abstract class Personaje {

    private int x;
    private int y;
    private final int ancho;
    private final int alto;
    protected final int velocidad;
    private final Image imagen;

    protected Personaje(int x, int y, int ancho, int alto,
            int velocidad, String rutaImagen) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.velocidad = velocidad;

        URL recurso = Personaje.class.getResource(rutaImagen);
        if (recurso == null) {
            imagen = null;
        } else {
            ImageIcon icono = new ImageIcon(recurso);
            imagen = icono.getImage().getScaledInstance(
                    ancho, alto, Image.SCALE_SMOOTH);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public Image getImagen() {
        return imagen;
    }

    public void setPosicion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void mover(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, ancho, alto);
    }

    public boolean colisionaCon(Personaje otro) {
        return getBounds().intersects(otro.getBounds());
    }
}
