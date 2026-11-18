package com.capuccinolovesballerina.game.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.capuccinolovesballerina.game.Utilidades.*;
import com.capuccinolovesballerina.game.red.ServicioServidor;

/** Solo inicia el servicio cuando el usuario pulsa JUGAR. */
public final class PantallaMenuServidor extends ScreenAdapter {
    private final Game juego;
    private Stage stage;
    private TextButton botonJugar;
    private Label estado;
    private ServicioServidor servicio;
    private boolean transferido, liberado;

    public PantallaMenuServidor(Game juego) { this.juego = juego; }
    @Override public void show() {
        stage = new Stage(FabricaViewport.crear());
        var skin = Recursos.getSkin();
        Table tabla = new Table();
        tabla.setFillParent(true);
        tabla.add(new Label("CAPPUCCINO LOVES BALLERINA - SERVIDOR", skin)).pad(20).row();
        estado = new Label("", skin);
        estado.setWrap(true);
        estado.setAlignment(Align.center);
        tabla.add(estado).size(700, 80).pad(10).row();
        botonJugar = new TextButton("JUGAR", skin);
        botonJugar.addListener(new ClickListener() {
            @Override public void clicked(InputEvent evento, float x, float y) {
                if (botonJugar.isDisabled()) return;
                if (servicio != null) servicio.close();
                servicio = new ServicioServidor();
                botonJugar.setDisabled(true);
            }
        });
        TextButton salir = new TextButton("SALIR", skin);
        salir.addListener(new ClickListener() {
            @Override public void clicked(InputEvent evento, float x, float y) { Gdx.app.exit(); }
        });
        tabla.add(botonJugar).size(250, 60).pad(10).row();
        tabla.add(salir).size(250, 60).pad(10);
        stage.addActor(tabla);
        Gdx.input.setInputProcessor(stage);
    }
    @Override public void render(float delta) {
        if (servicio != null) {
            estado.setText(servicio.getMensaje());
            botonJugar.setDisabled(!servicio.termino());
            if (servicio.estaIniciado() && !servicio.termino()) {
                transferido = true;
                juego.setScreen(new PantallaServidor(juego, servicio));
                return;
            }
        }
        ScreenUtils.clear(.12f, .1f, .16f, 1);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int ancho, int alto) { stage.getViewport().update(ancho, alto, true); }
    @Override public void hide() { dispose(); }
    @Override public void dispose() {
        if (liberado) return;
        liberado = true;
        if (!transferido && servicio != null) servicio.close();
        if (stage != null) stage.dispose();
    }
}
