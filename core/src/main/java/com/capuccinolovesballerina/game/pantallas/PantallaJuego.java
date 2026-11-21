package com.capuccinolovesballerina.game.pantallas;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.capuccinolovesballerina.game.Utilidades.Audio;
import com.capuccinolovesballerina.game.Utilidades.ControladorEntradaJugador;
import com.capuccinolovesballerina.game.Utilidades.ControladorPartida;
import com.capuccinolovesballerina.game.Utilidades.FabricaViewport;
import com.capuccinolovesballerina.game.Utilidades.Recursos;
import com.capuccinolovesballerina.game.Utilidades.RenderizadorJuego;
import com.capuccinolovesballerina.game.entidades.BallerinaCapuccina;
import com.capuccinolovesballerina.game.entidades.CapuccinoAssassino;
import com.capuccinolovesballerina.game.graficos.AnimacionesBallerina;
import com.capuccinolovesballerina.game.graficos.AnimacionesCappuccino;
import com.capuccinolovesballerina.game.graficos.Estado;
import com.capuccinolovesballerina.game.mapa.Nivel;
import com.capuccinolovesballerina.game.objetos.ElementosNivel;
import com.capuccinolovesballerina.game.objetos.Palanca;
import com.capuccinolovesballerina.game.red.Cliente;
import com.capuccinolovesballerina.game.red.ProtocolosRed;
import com.capuccinolovesballerina.game.red.ProtocolosRed.EstadoPartida;
import com.capuccinolovesballerina.game.red.ProtocolosRed.Jugador;

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
    private long rondaRed = -1;
    private long espumaRed;
    private long habilidadCap;
    private long habilidadBal;

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
        inicializarNivel();
        inicializarMenus();
        inicializarPersonajes();
        inicializarControladores();
        inicializarInterfazRed();

        Audio.reproducirMusica("sonidos/musica_juego.mp3");
    }

    private void inicializarNivel() {
        camera = new OrthographicCamera();

        nivel = new Nivel("mapas/NIVEL 0.tmx");
        viewport = FabricaViewport.crear(camera, nivel.getAnchoPx(), nivel.getAltoPx());
        elementos = new ElementosNivel(nivel);

        camera.position.set(
            nivel.getAnchoPx() / 2f,
            nivel.getAltoPx() / 2f,
            0
        );

        camera.update();
    }

    private void inicializarMenus() {
        menuPausa = new MenuPausa(juego);

        menuGameOver = new MenuGameOver(juego, () -> {
            if (clienteRed == null) {
                controladorPartida.reiniciar();
            } else {
                clienteRed.enviarReinicio();
            }
        });

        menuVictoria = new MenuVictoria(juego);
        hud = new HUD();
    }

    private void inicializarPersonajes() {
        cappuccino = new CapuccinoAssassino(
            elementos.getSpawnX(),
            elementos.getSpawnY()
        );

        ballerina = new BallerinaCapuccina(
            elementos.getSpawnBallerinaX(),
            elementos.getSpawnBallerinaY()
        );

        animacionesCappuccino = new AnimacionesCappuccino();
        animacionesBallerina = new AnimacionesBallerina();
    }

    private void inicializarControladores() {
        controladorEntrada = new ControladorEntradaJugador();

        controladorPartida = new ControladorPartida(
            nivel,
            elementos,
            menuPausa,
            menuGameOver,
            menuVictoria,
            hud,
            controladorEntrada,
            cappuccino,
            ballerina,
            animacionesCappuccino,
            animacionesBallerina
        );

        renderizadorJuego = new RenderizadorJuego(
            camera,
            nivel,
            cappuccino,
            ballerina,
            animacionesCappuccino,
            animacionesBallerina,
            elementos,
            hud,
            menuPausa,
            menuGameOver,
            menuVictoria
        );

        Gdx.input.setInputProcessor(controladorEntrada);
    }

    private void inicializarInterfazRed() {
        if (clienteRed == null) {
            return;
        }

        interfazRed = new Stage(FabricaViewport.crear());

        estadoRed = new Label("", Recursos.getSkin());
        estadoRed.setAlignment(com.badlogic.gdx.utils.Align.center);
        estadoRed.setWrap(true);

        volverRed = new TextButton("VOLVER AL MENU", Recursos.getSkin());
        volverRed.setVisible(false);

        volverRed.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                juego.setScreen(new PantallaMenu(juego));
            }
        });

        tablaRed = new Table();
        tablaRed.setFillParent(true);
        tablaRed.center();

        tablaRed.add(estadoRed).width(600).padBottom(24).row();
        tablaRed.add(volverRed).size(250, 55);

        interfazRed.addActor(tablaRed);
    }

    @Override
    public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f);

        if (clienteRed == null) {
            controladorPartida.actualizar(dt);
        } else {
            actualizarRed(dt);
        }

        renderizadorJuego.dibujar(delta);

        if (interfazRed != null) {
            interfazRed.act(delta);
            interfazRed.draw();
        }
    }

    private void actualizarRed(float delta) {
        EstadoPartida foto = clienteRed.getEstadoPartida();

        if (clienteRed.termino()) {
            mostrarDesconexion();
            return;
        }

        actualizarMensajeRed(foto);

        if (foto != null && foto.secuencia() > ultimaFoto) {
            aplicarEstadoServidor(foto);
        }

        actualizarPausa();
        actualizarControles(foto,delta);
        actualizarAnimaciones(delta);
    }

    private void mostrarDesconexion() {
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
    }

    private void actualizarMensajeRed(EstadoPartida foto) {
        if (foto != null && foto.fase().equals(ProtocolosRed.CUENTA)) {
            estadoRed.setText("Iniciando partida en " + foto.cuenta() + "...");
        } else {
            estadoRed.setText("");
        }
    }

    private void aplicarEstadoServidor(EstadoPartida foto) {
        if (foto.cortados().length() != elementos.getCortables().size) {
            clienteRed.cerrar("El mapa no coincide con el servidor. Usen la misma version.");
            return;
        }

        ultimaFoto = foto.secuencia();

        if (rondaRed != foto.ronda()) {
            reiniciarInterfazRonda(foto.ronda());
        }

        actualizarPersonajes(foto);
        actualizarAnimacionesRemotas(foto);
        actualizarElementos(foto);
        actualizarEspuma(foto);

        hud.setTiempoRemoto(foto.tiempo());
        actualizarResultado(foto);
    }

    private void reiniciarInterfazRonda(long ronda) {
        rondaRed = ronda;

        controladorEntrada.soltarTodo();

        menuGameOver.ocultar();
        menuVictoria.ocultar();
        menuPausa.ocultar();

        animacionesCappuccino.reiniciar();
        animacionesBallerina.reiniciar();
    }

    private void actualizarPersonajes(EstadoPartida foto) {
        Jugador cap = foto.cappuccino();
        Jugador bal = foto.ballerina();

        cappuccino.setPosicionRemota(
            cap.x(), cap.y(), cap.mirando(), cap.suelo()
        );

        ballerina.setPosicionRemota(
            bal.x(), bal.y(), bal.mirando(), bal.suelo()
        );
    }

    private void actualizarAnimacionesRemotas(EstadoPartida foto) {
        Jugador cap = foto.cappuccino();
        Jugador bal = foto.ballerina();

        if (habilidadCap != cap.habilidad()) {
            habilidadCap = cap.habilidad();

            animacionesCappuccino.reiniciar();
            animacionesCappuccino.setEstado(Estado.ABILITY);
        }

        if (habilidadBal != bal.habilidad()) {
            habilidadBal = bal.habilidad();

            animacionesBallerina.reiniciar();
            animacionesBallerina.setEstado(Estado.ABILITY);
        }

        animacionesCappuccino.setEstado(cap.animacion());
        animacionesBallerina.setEstado(bal.animacion());
    }

    private void actualizarElementos(EstadoPartida foto) {
        aplicarPalanca(elementos.getPalancaCappuccino(), foto.palancaCap());
        aplicarPalanca(elementos.getPalancaBallerina(), foto.palancaBal());

        if (elementos.getPuerta() != null) {
            elementos.getPuerta().setAbierta(foto.puerta());
        }

        for (int i = 0; i < elementos.getCortables().size; i++) {
            elementos.getCortables().get(i).setCortado(
                foto.cortados().charAt(i) == '1'
            );
        }
    }

    private void actualizarEspuma(EstadoPartida foto) {
        if (foto.espumaId() == 0 || foto.espumaVida() <= 0f) {
            ballerina.reiniciarHabilidad();
        } else {
            if (espumaRed != foto.espumaId() || ballerina.getEspuma() == null) {
                ballerina.setEspumaRemota(foto.espumaX(), foto.espumaY());
            }

            ballerina.getEspuma().aplicarVidaRemota(foto.espumaVida());
        }

        espumaRed = foto.espumaId();
    }

    private void actualizarResultado(EstadoPartida foto) {
        if (foto.fase().equals(ProtocolosRed.DERROTA)
            && !menuGameOver.estaVisible()) {

            menuPausa.ocultar();
            menuGameOver.mostrar();
        }

        if (foto.fase().equals(ProtocolosRed.VICTORIA)
            && !menuVictoria.estaVisible()) {

            menuPausa.ocultar();
            menuVictoria.mostrar();
        }
    }

    private void actualizarPausa() {
        if (controladorEntrada.isEscapePresionado()
            && !menuGameOver.estaVisible()
            && !menuVictoria.estaVisible()) {

            if (menuPausa.estaVisible()) {
                menuPausa.ocultar();
            } else {
                menuPausa.mostrar();
                controladorEntrada.soltarTodo();
            }
        }
    }

    private void actualizarControles(EstadoPartida foto, float delta) {
        boolean puedeMover = foto != null
            && foto.fase().equals(ProtocolosRed.JUGANDO)
            && !menuPausa.estaVisible();

        if (menuPausa.estaVisible()) {
            menuPausa.actualizar(delta);
        } else if (menuGameOver.estaVisible()) {
            menuGameOver.actualizar(delta);
        } else if (menuVictoria.estaVisible()) {
            menuVictoria.actualizar(delta);
        } else {
            Gdx.input.setInputProcessor(controladorEntrada);
        }

        clienteRed.enviarControles(
            puedeMover && controladorEntrada.isIzquierda(),
            puedeMover && controladorEntrada.isDerecha(),
            puedeMover && controladorEntrada.isSaltoPresionado(),
            puedeMover && controladorEntrada.isHabilidadPresionada(),
            puedeMover && controladorEntrada.isInteractuarPresionado()
        );

        controladorEntrada.limpiarEventos();
    }

    private void actualizarAnimaciones(float delta) {
        animacionesCappuccino.actualizar(delta);
        animacionesBallerina.actualizar(delta);
    }

    private void aplicarPalanca(Palanca palanca, boolean activada) {
        if (palanca != null && palanca.estaActivada() != activada) {
            palanca.alternar();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        menuPausa.resize(width, height);
        menuGameOver.resize(width, height);
        menuVictoria.resize(width, height);
        hud.resize(width, height);

        if (interfazRed != null) {
            interfazRed.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (liberado) {
            return;
        }

        liberado = true;

        if (clienteRed != null) {
            clienteRed.cerrar();
        }

        renderizadorJuego.dispose();
        nivel.dispose();

        menuPausa.dispose();
        menuGameOver.dispose();
        menuVictoria.dispose();
        hud.dispose();

        animacionesCappuccino.dispose();
        animacionesBallerina.dispose();

        if (interfazRed != null) {
            interfazRed.dispose();
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
        Gdx.input.setInputProcessor(null);
        dispose();
    }
}
