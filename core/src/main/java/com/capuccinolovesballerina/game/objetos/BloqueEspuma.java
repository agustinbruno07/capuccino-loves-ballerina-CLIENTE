package com.capuccinolovesballerina.game.objetos;

import com.badlogic.gdx.math.Rectangle;
import com.capuccinolovesballerina.game.Utilidades.Constantes;

public class BloqueEspuma {

    private final Rectangle zona;
    private final float vidaInicial;
    private float vida;

    public BloqueEspuma(float x, float y, float ancho, float alto, float vida) {
        this.zona = new Rectangle(x, y, ancho, alto);
        this.vidaInicial = vida;
        this.vida = vida;
    }

    public void actualizar(float delta) {
        vida -= delta;
    }

    public boolean estaViva() {
        return vida > 0f;
    }

    public float getVidaRestante() { return vida; }

    public void aplicarVidaRemota(float restante) { vida = Math.max(0f, Math.min(vidaInicial, restante)); }

    public Rectangle getZona() {
        return zona;
    }

    public int getIndiceFrame() {
        float transcurrido = Math.max(0f, vidaInicial - vida);
        if (vida <= Constantes.DURACION_DISOLUCION_ESPUMA) {
            float disolucion = Constantes.DURACION_DISOLUCION_ESPUMA - vida;
            return 6 + Math.min(1, (int) (disolucion / (Constantes.DURACION_DISOLUCION_ESPUMA / 2f)));
        }
        if (transcurrido < Constantes.DURACION_FORMACION_ESPUMA) {
            return Math.min(3, (int) (transcurrido / (Constantes.DURACION_FORMACION_ESPUMA / 4f)));
        }
        float activo = transcurrido - Constantes.DURACION_FORMACION_ESPUMA;
        return 4 + (int) (activo / Constantes.DURACION_FRAME_ACTIVO_ESPUMA) % 2;
    }


    public float getAlpha() {
        float fade = Constantes.DURACION_DISOLUCION_ESPUMA;
        if (vida >= fade) return 1f;
        return Math.max(0f, vida / fade);
    }
}
