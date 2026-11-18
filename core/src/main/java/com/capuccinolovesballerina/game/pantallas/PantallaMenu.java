package com.capuccinolovesballerina.game.pantallas;

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
import com.badlogic.gdx.Game;
import com.capuccinolovesballerina.game.Utilidades.Audio;
import com.capuccinolovesballerina.game.Utilidades.FabricaViewport;
import com.capuccinolovesballerina.game.Utilidades.Recursos;
import com.capuccinolovesballerina.game.red.Cliente;

public class PantallaMenu implements Screen {

    private Image fondo;

    private Stage stage;
    private Skin skin;
    private TextButton botonJugar;


    private TextButton botonOpciones;
    private TextButton botonControles;
    private TextButton botonSalir;
    private Label estadoRed;
    private final Game juego;

    private Cliente clienteRed;

    private boolean transferido;
    private boolean liberado;

    public PantallaMenu(Game juego) {
        this.juego = juego;
    }

    @Override
    public void show() {
        stage = new Stage(FabricaViewport.crear());
        skin = Recursos.getSkin();
        fondo = new Image(Recursos.getImagen("interfaz/fondo_menu.png"));
        fondo.setFillParent(true);
        stage.addActor(fondo);
        crearBotones();
        agregarBotones();
        escucharBotones();
        Audio.reproducirMusica("sonidos/musica_menu.mp3");
    }

    private void crearBotones() {
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
        Gdx.input.setInputProcessor(stage);
    }

    private void escucharBotones() {

        botonJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!botonJugar.isDisabled()) buscarServidor();
            }
        });


        botonSalir.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        botonControles.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaControles(juego));
            }
        });

        botonOpciones.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaOpciones(juego));
            }
        });
    }

    private void buscarServidor() {
        if (clienteRed != null) clienteRed.cerrar();
        clienteRed = new Cliente();
        clienteRed.conectar();
        botonJugar.setDisabled(true);
    }

    public Cliente getClienteRed() {
        return clienteRed;
    }

    @Override
    public void render(float delta) {
        if (clienteRed != null) {
            estadoRed.setText(clienteRed.termino() && clienteRed.sinServidor()
                ? "NO SE ENCONTRÓ UN SERVIDOR ACTIVO" : clienteRed.getEstadoConexion());
            botonJugar.setDisabled(!clienteRed.termino());
            var foto = clienteRed.getEstadoPartida();
            if (!clienteRed.termino() && foto != null && foto.fase().equals(com.capuccinolovesballerina.game.red.ProtocolosRed.JUGANDO)) {
                transferido = true;
                juego.setScreen(new PantallaJuego(juego, clienteRed));
                return;
            }
        }
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (!transferido && clienteRed != null) clienteRed.cerrar();
        dispose();
    }

    @Override
    public void dispose() {
        if (liberado) return;
        liberado = true;
        if (!transferido && clienteRed != null) clienteRed.cerrar();
        stage.dispose();
    }
}
