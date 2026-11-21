package com.capuccinolovesballerina.game.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;

import com.capuccinolovesballerina.game.Utilidades.Audio;
import com.capuccinolovesballerina.game.Utilidades.FabricaViewport;
import com.capuccinolovesballerina.game.Utilidades.Recursos;
import com.capuccinolovesballerina.game.red.Cliente;
import com.capuccinolovesballerina.game.red.ProtocolosRed;

public class PantallaMenu implements Screen {

    private final Game juego;

    private Stage stage;

    private TextButton botonJugar;
    private TextButton botonOpciones;
    private TextButton botonControles;
    private TextButton botonSalir;

    private Label estadoRed;

    private Cliente clienteRed;

    private boolean transferido;
    private boolean liberado;

    public PantallaMenu(Game juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        stage = new Stage(FabricaViewport.crear());

        Skin skin = Recursos.getSkin();

        Image fondo = new Image(Recursos.getImagen("interfaz/fondo_menu.png"));
        fondo.setFillParent(true);
        stage.addActor(fondo);

        crearBotones(skin);
        agregarBotones();
        escucharBotones();

        Gdx.input.setInputProcessor(stage);

        Audio.reproducirMusica("sonidos/musica_menu.mp3");
    }

    private void crearBotones(Skin skin) {
        botonJugar = new TextButton("JUGAR", skin);
        botonOpciones = new TextButton("OPCIONES", skin);
        botonControles = new TextButton("CONTROLES", skin);
        botonSalir = new TextButton("SALIR", skin);

        estadoRed = new Label("", skin);
        estadoRed.setWrap(true);
        estadoRed.setAlignment(com.badlogic.gdx.utils.Align.center);
    }

    private void agregarBotones() {
        Table tabla = new Table();

        tabla.setFillParent(true);
        tabla.bottom();

        tabla.add(estadoRed).size(750, 60).pad(5).row();
        tabla.add(botonJugar).size(200, 60).pad(10).row();
        tabla.add(botonOpciones).size(200, 60).pad(10).row();
        tabla.add(botonControles).size(200, 60).pad(10).row();
        tabla.add(botonSalir).size(200, 60).pad(10);

        stage.addActor(tabla);
    }

    private void escucharBotones() {
        botonJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!botonJugar.isDisabled()) {
                    buscarServidor();
                }
            }
        });

        botonOpciones.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaOpciones(juego));
            }
        });

        botonControles.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaControles(juego));
            }
        });

        botonSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    private void buscarServidor() {
        if (clienteRed != null) {
            clienteRed.cerrar();
        }

        clienteRed = new Cliente();
        clienteRed.conectar();

        botonJugar.setDisabled(true);
    }

    @Override
    public void render(float delta) {
        if (clienteRed != null) {
            actualizarConexion();

            if (transferido) {
                return;
            }
        }

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void actualizarConexion() {
        if (clienteRed.termino() && clienteRed.sinServidor()) {
            estadoRed.setText("NO SE ENCONTRÓ UN SERVIDOR ACTIVO");
        } else {
            estadoRed.setText(clienteRed.getEstadoConexion());
        }

        botonJugar.setDisabled(!clienteRed.termino());

        var foto = clienteRed.getEstadoPartida();

        if (!clienteRed.termino()
            && foto != null
            && foto.fase().equals(ProtocolosRed.JUGANDO)) {

            transferido = true;
            juego.setScreen(new PantallaJuego(juego, clienteRed));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (liberado) {
            return;
        }

        liberado = true;

        if (!transferido && clienteRed != null) {
            clienteRed.cerrar();
        }

        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
}
