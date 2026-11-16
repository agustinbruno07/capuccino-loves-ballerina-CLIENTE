package com.capuccinolovesballerina.game.graficos;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AnimacionesBallerina {

    private static final int FRAME = 128;
    private static final int FRAMES_IDLE = 4;
    private static final int FRAMES_WALK = 8;
    private static final int FRAMES_JUMP = 3;
    private static final int FRAMES_ABILITY = 8;

    private final Texture textura;
    private final Animation<TextureRegion> idle;
    private final Animation<TextureRegion> walk;
    private final Animation<TextureRegion> jump;
    private final Animation<TextureRegion> ability;

    private Estado estado = Estado.IDLE;
    private float tiempo = 0f;

    public AnimacionesBallerina() {
        textura = new Texture("sprites/ballerina.png");

        TextureRegion[][] grilla = TextureRegion.split(textura, FRAME, FRAME);

        idle = new Animation<>(0.2f, primeros(grilla[0], FRAMES_IDLE));
        idle.setPlayMode(Animation.PlayMode.LOOP);

        walk = new Animation<>(0.1f, primeros(grilla[1], FRAMES_WALK));
        walk.setPlayMode(Animation.PlayMode.LOOP);

        jump = new Animation<>(0.15f, primeros(grilla[2], FRAMES_JUMP));
        jump.setPlayMode(Animation.PlayMode.NORMAL);

        ability = new Animation<>(0.08f, primeros(grilla[3], FRAMES_ABILITY));
        ability.setPlayMode(Animation.PlayMode.NORMAL);
    }

    private TextureRegion[] primeros(TextureRegion[] fila, int n) {
        TextureRegion[] frames = new TextureRegion[n];
        for (int i = 0; i < n; i++) {
            frames[i] = fila[i];
        }
        return frames;
    }

    public void setEstado(Estado nuevo) {
        if (estado == Estado.ABILITY && !ability.isAnimationFinished(tiempo)) return;

        if (nuevo != estado) {
            estado = nuevo;
            tiempo = 0f;
        }
    }

    public void actualizar(float delta) {
        tiempo += delta;
    }

    public void reiniciar() {
        estado = Estado.IDLE;
        tiempo = 0f;
    }

    public TextureRegion getFrameActual() {
        switch (estado) {
            case WALK:
                return walk.getKeyFrame(tiempo);
            case JUMP:
                return jump.getKeyFrame(tiempo);
            case ABILITY:
                return ability.getKeyFrame(tiempo);
            default:
                return idle.getKeyFrame(tiempo);
        }
    }

    public void dispose() {
        textura.dispose();
    }
}
