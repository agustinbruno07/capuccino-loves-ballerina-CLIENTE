package com.capuccinolovesballerina.game.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.Game;
import com.capuccinolovesballerina.game.Utilidades.*;
import com.capuccinolovesballerina.game.entidades.BallerinaCapuccina;
import com.capuccinolovesballerina.game.entidades.CapuccinoAssassino;
import com.capuccinolovesballerina.game.graficos.AnimacionesCappuccino;
import com.capuccinolovesballerina.game.graficos.AnimacionesBallerina;
import com.capuccinolovesballerina.game.mapa.Nivel;
import com.capuccinolovesballerina.game.objetos.ElementosNivel;
import com.capuccinolovesballerina.game.red.Cliente;
import com.capuccinolovesballerina.game.red.ProtocolosRed.*;
import com.capuccinolovesballerina.game.red.ProtocolosRed;
import com.capuccinolovesballerina.game.objetos.Palanca;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class PantallaJuego implements Screen {

    private final Game juego;

    private OrthographicCamera camera;
    private Viewport viewport;

    private Nivel nivel;
    private ElementosNivel elementos;

    private MenuPausa menuPausa;
    private MenuGameOver menuGameOver;
    private MenuVictoria menuVictoria;
    private HUD hud;

    private ControladorEntradaJugador controladorEntrada;
    private CapuccinoAssassino cappuccino;
    private BallerinaCapuccina ballerina;
    private AnimacionesCappuccino animacionesCappuccino;
    private AnimacionesBallerina animacionesBallerina;

    private ControladorPartida controladorPartida;
    private RenderizadorJuego renderizadorJuego;
    private Cliente clienteRed;
    private long ultimaFoto = -1;
    private long rondaRed = -1, espumaRed, habilidadCap, habilidadBal;
    private Stage interfazRed;
    private Label estadoRed;
    private Table tablaRed;
    private TextButton volverRed;
    private boolean liberado;

    public PantallaJuego(Game juego) {
        this.juego = juego;
    }

    public PantallaJuego(Game juego, Cliente cliente) {
        this(juego);
        clienteRed = cliente;
    }

    @Override
    public void show() {

        camera = new OrthographicCamera();

        nivel = new Nivel("mapas/NIVEL 0.tmx");
        viewport = FabricaViewport.crear(camera, nivel.getAnchoPx(), nivel.getAltoPx());
        elementos = new ElementosNivel(nivel);

        menuPausa = new MenuPausa(juego);
        menuGameOver = new MenuGameOver(juego, () -> {
            if (clienteRed == null) controladorPartida.reiniciar();
            else clienteRed.enviarReinicio();
        });
        menuVictoria = new MenuVictoria(juego);
        hud = new HUD();

        controladorEntrada = new ControladorEntradaJugador();
        animacionesCappuccino = new AnimacionesCappuccino();
        animacionesBallerina = new AnimacionesBallerina();

        cappuccino = new CapuccinoAssassino(elementos.getSpawnX(), elementos.getSpawnY());
        ballerina = new BallerinaCapuccina(elementos.getSpawnBallerinaX(), elementos.getSpawnBallerinaY());

        Gdx.input.setInputProcessor(controladorEntrada);

        camera.position.set(nivel.getAnchoPx() / 2f, nivel.getAltoPx() / 2f, 0);
        camera.update();

        controladorPartida = new ControladorPartida(
            nivel, elementos, menuPausa, menuGameOver, menuVictoria,
            hud, controladorEntrada, cappuccino, ballerina,
            animacionesCappuccino, animacionesBallerina
        );

        renderizadorJuego = new RenderizadorJuego(
            camera, nivel, cappuccino, ballerina,
            animacionesCappuccino, animacionesBallerina,
            elementos, hud, menuPausa, menuGameOver, menuVictoria
        );

        if (clienteRed != null) {
            interfazRed = new Stage(FabricaViewport.crear());
            estadoRed = new Label("", Recursos.getSkin());
            estadoRed.setAlignment(com.badlogic.gdx.utils.Align.center);
            estadoRed.setWrap(true);
            volverRed = new TextButton("VOLVER AL MENU", Recursos.getSkin());
            volverRed.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    juego.setScreen(new PantallaMenu(juego));
                }
            });
            tablaRed = new Table();
            tablaRed.setFillParent(true);
            tablaRed.center();
            tablaRed.add(estadoRed).width(600).padBottom(24).row();
            tablaRed.add(volverRed).size(250, 55);
            interfazRed.addActor(tablaRed);
            volverRed.setVisible(false);
        }

        Audio.reproducirMusica("sonidos/musica_juego.mp3");
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);
        if (clienteRed == null) controladorPartida.actualizar(dt);
        else actualizarRed(dt);
        renderizadorJuego.dibujar(delta);
        if (interfazRed != null) { interfazRed.act(delta); interfazRed.draw(); }
    }

    private void actualizarRed(float delta) {
        EstadoPartida foto = clienteRed.getEstadoPartida();
        if (clienteRed.termino()) {
            if (!volverRed.isVisible()) {
                menuPausa.ocultar();
                menuGameOver.ocultar();
                menuVictoria.ocultar();
                tablaRed.setBackground(Recursos.getSkin().getDrawable("dialogDim"));
                estadoRed.setFontScale(1.25f);
            }
            estadoRed.setText(clienteRed.getEstadoConexion());
            volverRed.setVisible(true);
            controladorEntrada.soltarTodo();
            Gdx.input.setInputProcessor(interfazRed);
            return;
        }
        estadoRed.setText(foto != null && foto.fase().equals(ProtocolosRed.CUENTA)
            ? "Iniciando partida en " + foto.cuenta() + "..." : "");
        if (foto != null && foto.secuencia() > ultimaFoto) {
            if (foto.cortados().length() != elementos.getCortables().size) {
                clienteRed.cerrar("El mapa no coincide con el servidor. Usen la misma version.");
                return;
            }
            ultimaFoto = foto.secuencia();
            if (rondaRed != foto.ronda()) {
                rondaRed = foto.ronda();
                controladorEntrada.soltarTodo();
                menuGameOver.ocultar(); menuVictoria.ocultar(); menuPausa.ocultar();
                animacionesCappuccino.reiniciar(); animacionesBallerina.reiniciar();
            }
            Jugador capRed = foto.cappuccino(), balRed = foto.ballerina();
            cappuccino.setPosicionRemota(capRed.x(), capRed.y(), capRed.mirando(), capRed.suelo());
            ballerina.setPosicionRemota(balRed.x(), balRed.y(), balRed.mirando(), balRed.suelo());
            if (habilidadCap != capRed.habilidad()) {
                habilidadCap = capRed.habilidad();
                animacionesCappuccino.reiniciar();
                animacionesCappuccino.setEstado(com.capuccinolovesballerina.game.graficos.Estado.ABILITY);
            }
            if (habilidadBal != balRed.habilidad()) {
                habilidadBal = balRed.habilidad();
                animacionesBallerina.reiniciar();
                animacionesBallerina.setEstado(com.capuccinolovesballerina.game.graficos.Estado.ABILITY);
            }
            animacionesCappuccino.setEstado(capRed.animacion());
            animacionesBallerina.setEstado(balRed.animacion());
            aplicarPalanca(elementos.getPalancaCappuccino(), foto.palancaCap());
            aplicarPalanca(elementos.getPalancaBallerina(), foto.palancaBal());
            if (elementos.getPuerta() != null) elementos.getPuerta().setAbierta(foto.puerta());
            for (int i = 0; i < elementos.getCortables().size; i++)
                elementos.getCortables().get(i).setCortado(foto.cortados().charAt(i) == '1');
            if (foto.espumaId() == 0 || foto.espumaVida() <= 0f) ballerina.reiniciarHabilidad();
            else {
                if (espumaRed != foto.espumaId() || ballerina.getEspuma() == null)
                    ballerina.setEspumaRemota(foto.espumaX(), foto.espumaY());
                ballerina.getEspuma().aplicarVidaRemota(foto.espumaVida());
            }
            espumaRed = foto.espumaId();
            hud.setTiempoRemoto(foto.tiempo());
            if (foto.fase().equals(ProtocolosRed.DERROTA) && !menuGameOver.estaVisible()) {
                menuPausa.ocultar(); menuGameOver.mostrar();
            }
            if (foto.fase().equals(ProtocolosRed.VICTORIA) && !menuVictoria.estaVisible()) {
                menuPausa.ocultar(); menuVictoria.mostrar();
            }
        }
        if (controladorEntrada.isEscapePresionado() && !menuGameOver.estaVisible() && !menuVictoria.estaVisible()) {
            if (menuPausa.estaVisible()) menuPausa.ocultar();
            else { menuPausa.mostrar(); controladorEntrada.soltarTodo(); }
        }
        boolean puedeMover = foto != null && foto.fase().equals(ProtocolosRed.JUGANDO) && !menuPausa.estaVisible();
        if (menuPausa.estaVisible()) menuPausa.actualizar(delta);
        else if (menuGameOver.estaVisible()) menuGameOver.actualizar(delta);
        else if (menuVictoria.estaVisible()) menuVictoria.actualizar(delta);
        else Gdx.input.setInputProcessor(controladorEntrada);
        boolean cap = clienteRed.getMiRol() == 0;
        clienteRed.enviarControles(puedeMover && (cap ? controladorEntrada.isIzquierda() : controladorEntrada.isIzquierdaBallerina()),
            puedeMover && (cap ? controladorEntrada.isDerecha() : controladorEntrada.isDerechaBallerina()),
            puedeMover && (cap ? controladorEntrada.isSaltoPresionado() : controladorEntrada.isSaltoPresionadoBallerina()),
            puedeMover && (cap ? controladorEntrada.isHabilidadPresionada() : controladorEntrada.isHabilidadPresionadaBallerina()),
            puedeMover && (cap ? controladorEntrada.isInteractuarPresionado() : controladorEntrada.isInteractuarPresionadoBallerina()));
        controladorEntrada.limpiarEventos();
        animacionesCappuccino.actualizar(delta);
        animacionesBallerina.actualizar(delta);
    }

    private void aplicarPalanca(Palanca palanca, boolean activada) {
        if (palanca != null && palanca.estaActivada() != activada) palanca.alternar();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        menuPausa.resize(width, height);
        menuGameOver.resize(width, height);
        menuVictoria.resize(width, height);
        hud.resize(width, height);
        if (interfazRed != null) interfazRed.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (liberado) return;
        liberado = true;
        if (clienteRed != null) clienteRed.cerrar();
        renderizadorJuego.dispose();
        nivel.dispose();
        menuPausa.dispose();
        menuGameOver.dispose();
        menuVictoria.dispose();
        hud.dispose();
        animacionesCappuccino.dispose();
        animacionesBallerina.dispose();
        if (interfazRed != null) interfazRed.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        if (clienteRed != null) clienteRed.cerrar();
        Gdx.input.setInputProcessor(null);
        dispose();
    }
}
