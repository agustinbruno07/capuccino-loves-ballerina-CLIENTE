package com.capuccinolovesballerina.game.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.capuccinolovesballerina.game.Utilidades.*;
import com.capuccinolovesballerina.game.red.*;

/** Supervision grafica. El anfitrion no tiene Cliente ni ocupa un personaje. */
public final class PantallaServidor extends ScreenAdapter {
    private final Game juego;
    private final ServicioServidor inicio;
    private Stage stage;
    private Label estado, jugador1, jugador2;
    private boolean liberado;

    public PantallaServidor(Game juego, ServicioServidor inicio) { this.juego = juego; this.inicio = inicio; }
    @Override public void show() {
        stage = new Stage(FabricaViewport.crear());
        var skin = Recursos.getSkin();
        Table tabla = new Table();
        tabla.setFillParent(true);
        tabla.add(new Label("SERVIDOR INICIADO", skin)).pad(20).row();
        estado = new Label("Esperando jugadores...", skin);
        jugador1 = new Label("", skin);
        jugador2 = new Label("", skin);
        tabla.add(estado).pad(20).row();
        tabla.add(jugador1).pad(10).row();
        tabla.add(jugador2).pad(10).row();
        TextButton volver = new TextButton("DETENER SERVIDOR", skin);
        volver.addListener(new ClickListener() {
            @Override public void clicked(InputEvent evento, float x, float y) {
                juego.setScreen(new PantallaMenuServidor(juego));
            }
        });
        tabla.add(volver).size(300, 60).pad(30);
        stage.addActor(tabla);
        Gdx.input.setInputProcessor(stage);
    }
    @Override public void render(float delta) {
        var resumen = inicio.getServidor().getResumen();
        jugador1.setText("Jugador 1: " + (resumen.cappuccino() ? "Cappuccino conectado." : "Esperando..."));
        jugador2.setText("Jugador 2: " + (resumen.ballerina() ? "Ballerina conectado." : "Esperando..."));
        estado.setText(inicio.termino() ? "Se detuvo la partida."
            : resumen.fase().equals(ProtocolosRed.ESPERA) ? "Esperando jugadores..."
            : resumen.fase().equals(ProtocolosRed.CUENTA) ? "Iniciando partida..." : "Partida iniciada");
        ScreenUtils.clear(.12f, .1f, .16f, 1);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int ancho, int alto) { stage.getViewport().update(ancho, alto, true); }
    @Override public void hide() { dispose(); }
    @Override public void dispose() {
        if (liberado) return;
        liberado = true;
        inicio.close();
        if (stage != null) stage.dispose();
    }
}
