/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package videojuego;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Timer;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 * @author marco
 */
public class PanelJuego extends JPanel{
    private final Jugador jugador = new Jugador(80, 0);
    private final Enemigo enemigo = new Enemigo(430, 0);
    private boolean izquierdaPulsada;
    private boolean derechaPulsada;
    private final Timer timer;
    private double alturaSalto = 0;
    private double velocidadVertical = 0;
    private boolean enSuelo = true;
    private final int monedaX = 620;
    private boolean monedaRecogida = false;
    private int monedas = 0;
    private int vidas = 3;
    private boolean partidaTerminada = false;
    private boolean partidaGanada = false;
    private boolean mirandoIzquierda = false;

    public PanelJuego(){
        setBackground(new Color(135,206,235));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (botonIzquierda().contains(e.getPoint())) {
                    izquierdaPulsada = true;
                } else if (botonDerecha().contains(e.getPoint())) {
                    derechaPulsada = true;
                } else if (botonSaltar().contains(e.getPoint())) {
                    intentarSaltar();
                } else if (botonReiniciar().contains(e.getPoint())) {
                    reiniciarJuego();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                izquierdaPulsada = false;
                derechaPulsada = false;
            }
        });

        timer = new Timer(16, e -> actualizarJuego());
        timer.start();
    }

    private void intentarSaltar() {
        if (enSuelo) {
            velocidadVertical = 12;
            enSuelo = false;
        }
    }

    private Rectangle botonIzquierda() {
        return new Rectangle(14, 34, 54, 54);
    }

    private Rectangle botonDerecha() {
        return new Rectangle(76, 34, 54, 54);
    }

    private Rectangle botonSaltar() {
        return new Rectangle(138, 34, 54, 54);
    }

    private Rectangle botonReiniciar() {
        return new Rectangle(getWidth() - 56, 34, 42, 42);
    }

    private void reiniciarJuego() {
        jugador.setPosicion(80, getHeight() - 90);
        izquierdaPulsada = false;
        derechaPulsada = false;

        alturaSalto = 0;
        velocidadVertical = 0;
        enSuelo = true;

        monedaRecogida = false;
        monedas = 0;
        enemigo.reiniciar(430, getHeight() - 82);
        vidas = 3;
        partidaTerminada = false;
        partidaGanada = false;

        timer.start();
        repaint();
    }

    private void actualizarJuego() {
        if (izquierdaPulsada) {
            jugador.moverIzquierda();
            mirandoIzquierda = true;
        }

        if (derechaPulsada) {
            jugador.moverDerecha(getWidth());
            mirandoIzquierda = false;
        }

        enemigo.moverAutomaticamente(getWidth());
        enemigo.actualizarAnimacion();

        // Si está apoyado en la plataforma elevada, comprueba que siga encima.
        boolean estaSobrePrimera = jugador.getX() + jugador.getAncho() > 250
                && jugador.getX() < 400;
        boolean estaSobreSegunda = jugador.getX() + jugador.getAncho() > 500
                && jugador.getX() < 630;
        boolean sigueSobrePlataforma = estaSobrePrimera || estaSobreSegunda;
        if (enSuelo && alturaSalto > 0 && !sigueSobrePlataforma) {
            enSuelo = false;
        }

        if (!enSuelo) {
            alturaSalto += velocidadVertical;
            velocidadVertical -= 0.55;

            if (alturaSalto <= 0) {
                alturaSalto = 0;
                velocidadVertical = 0;
                enSuelo = true;
            }
        }

        // Al bajar, aterriza si cruza la parte superior de la plataforma.
        if (!enSuelo && velocidadVertical < 0) {
            int jugadorY = getHeight() - 90 - (int) alturaSalto;
            int plataformaY = getHeight() - 160;

            boolean solapaHorizontalmente = estaSobrePrimera || estaSobreSegunda;
            boolean cruzaLaPlataforma = jugadorY < plataformaY && jugadorY + 40 >= plataformaY;

            if (solapaHorizontalmente && cruzaLaPlataforma) {
                alturaSalto = (getHeight() - 50) - plataformaY;
                velocidadVertical = 0;
                enSuelo = true;
            }
        }

        // Actualiza las cajas de colisión después de resolver el salto y el aterrizaje.
        jugador.setPosicion(jugador.getX(), getHeight() - 90 - (int) alturaSalto);
        enemigo.setPosicion(enemigo.getX(), getHeight() - 82);
        
        if(!monedaRecogida){
             Rectangle moneda = new Rectangle(monedaX, getHeight() - 80, 18, 18);
             
             if (jugador.getBounds().intersects(moneda)) {
             monedaRecogida = true;
             monedas++;
    }
        }
        if (vidas > 0 && jugador.colisionaCon(enemigo)) {
            vidas--;
            jugador.setPosicion(80, getHeight() - 90);
            alturaSalto = 0;
            velocidadVertical = 0;
            enSuelo = true;
            izquierdaPulsada = false;
            derechaPulsada = false;
            if (vidas == 0) {
                partidaTerminada = true;
                timer.stop();
            }
        }

        if (!partidaTerminada) {
            Rectangle puertaBounds =
                    new Rectangle(getWidth() - 70, getHeight() - 110, 45, 60);

            if (jugador.getBounds().intersects(puertaBounds)) {
                partidaGanada = true;
                timer.stop();
            }
        }

        jugador.actualizarAnimacion((izquierdaPulsada || derechaPulsada) && enSuelo, !enSuelo);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        
        super.paintComponent(g);

        g.setColor(new Color(80, 180, 70));
        g.fillRect(0, getHeight() - 50, getWidth(), 50);

        int puertaX = getWidth() - 70;
        g.setColor(new Color(125, 70, 30));
        g.fillRect(puertaX, getHeight() - 110, 45, 60);
        g.setColor(Color.YELLOW);
        g.fillOval(puertaX + 32, getHeight() - 82, 6, 6);
        
        g.setColor(Color.BLACK);
        g.drawString("Monedas: " + monedas, 10, 20);
        g.drawString("Vidas: " + vidas, getWidth() - 90, 20);
        if (!monedaRecogida) {
            g.setColor(new Color(255, 200, 0));
            g.fillOval(monedaX, getHeight() - 80, 18, 18);
        }
        
        int jugadorY = getHeight() - 90 - (int) alturaSalto;
        if (jugador.getImagen() != null) {
          if (mirandoIzquierda) {
            g.drawImage(jugador.getImagen(),
            jugador.getX() + jugador.getAncho(), jugadorY,
            jugador.getX(), jugadorY + jugador.getAlto(),
            0, 0,
            jugador.getImagen().getWidth(this),
            jugador.getImagen().getHeight(this), this);
        } else {
            g.drawImage(jugador.getImagen(), jugador.getX(), jugadorY,
            jugador.getAncho(), jugador.getAlto(), this);
          }
        } else {
            g.setColor(new Color(220, 60, 50));
            g.fillRect(jugador.getX(), jugadorY,
                    jugador.getAncho(), jugador.getAlto());
        }
        
        int plataformaY = getHeight() -160;
        g.setColor(new Color(135, 85, 45));
        g.fillRect(250, plataformaY, 150, 15);
        g.setColor(new Color(80, 180, 70));
        g.fillRect(250, plataformaY, 150, 5);
        
        if (enemigo.getImagen() != null) {
            if (!enemigo.estaMirandoIzquierda()) {
                g.drawImage(enemigo.getImagen(),
                        enemigo.getX() + enemigo.getAncho(), enemigo.getY(),
                        enemigo.getX(), enemigo.getY() + enemigo.getAlto(),
                        0, 0, enemigo.getImagen().getWidth(this),
                        enemigo.getImagen().getHeight(this), this);
            } else {
                g.drawImage(enemigo.getImagen(), enemigo.getX(), enemigo.getY(),
                        enemigo.getAncho(), enemigo.getAlto(), this);
            }
        } else {
            g.setColor(new Color(90, 75, 60));
            g.fillRoundRect(enemigo.getX(), getHeight() - 82,
                    enemigo.getAncho(), enemigo.getAlto(), 8, 8);
        }
        
        g.setColor(new Color(135, 85, 45));
        g.fillRect(500, plataformaY, 130, 15);

        g.setColor(new Color(80, 180, 70));
        g.fillRect(500, plataformaY, 130, 5);

        if (partidaTerminada) {
            g.setColor(Color.BLACK);
            g.drawString("FIN DEL JUEGO", getWidth() / 2 - 45, getHeight() / 2);
            g.drawString("Pulsa ↻ para reiniciar", getWidth() / 2 - 78, getHeight() / 2 + 20);
        } else if (partidaGanada) {
            g.setColor(Color.BLACK);
            g.drawString("NIVEL COMPLETADO", getWidth() / 2 - 55, getHeight() / 2);
            g.drawString("Pulsa ↻ para reiniciar", getWidth() / 2 - 78, getHeight() / 2 + 20);
        }

        dibujarBotones(g);
    }

    private void dibujarBotones(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        dibujarBoton(g2, botonIzquierda(), "◀", izquierdaPulsada);
        dibujarBoton(g2, botonDerecha(), "▶", derechaPulsada);
        dibujarBoton(g2, botonSaltar(), "↑", false);
        dibujarBoton(g2, botonReiniciar(), "↻", false);
        g2.dispose();
    }

    private void dibujarBoton(Graphics2D g, Rectangle bounds,
            String texto, boolean pulsado) {
        g.setColor(pulsado ? new Color(35, 93, 145, 225)
                : new Color(25, 45, 65, 205));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
                14, 14);
        g.setColor(new Color(255, 255, 255, 225));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height,
                14, 14);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD,
                bounds.width > 45 ? 25 : 22));
        int textoX = bounds.x + (bounds.width - g.getFontMetrics().stringWidth(texto)) / 2;
        int textoY = bounds.y + (bounds.height + g.getFontMetrics().getAscent()) / 2 - 3;
        g.drawString(texto, textoX, textoY);
    }

}
