package com.capuccinolovesballerina.game.Utilidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.capuccinolovesballerina.game.entidades.BallerinaCapuccina;
import com.capuccinolovesballerina.game.entidades.CapuccinoAssassino;
import com.capuccinolovesballerina.game.entidades.Player;
import com.capuccinolovesballerina.game.graficos.AnimacionesCappuccino;
import com.capuccinolovesballerina.game.graficos.AnimacionesBallerina;
import com.capuccinolovesballerina.game.graficos.Estado;
import com.capuccinolovesballerina.game.mapa.Nivel;
import com.capuccinolovesballerina.game.objetos.BloqueEspuma;
import com.capuccinolovesballerina.game.objetos.ElementosNivel;
import com.capuccinolovesballerina.game.objetos.ObjetoCortable;
import com.capuccinolovesballerina.game.objetos.Palanca;
import com.capuccinolovesballerina.game.objetos.Puerta;
import com.capuccinolovesballerina.game.pantallas.HUD;
import com.capuccinolovesballerina.game.pantallas.MenuGameOver;
import com.capuccinolovesballerina.game.pantallas.MenuPausa;
import com.capuccinolovesballerina.game.pantallas.MenuVictoria;


public class ControladorPartida {

    private final Nivel nivel;
    private final ElementosNivel elementos;

    private final MenuPausa menuPausa;
    private final MenuGameOver menuGameOver;
    private final MenuVictoria menuVictoria;
    private final HUD hud;

    private final ControladorEntradaJugador entrada;
    private final CapuccinoAssassino cappuccino;
    private final BallerinaCapuccina ballerina;
    private final AnimacionesCappuccino animacionesCappuccino;
    private final AnimacionesBallerina animacionesBallerina;
    private static final float ALCANCE_PALANCA = 12f;
    private boolean derrota;
    private boolean victoria;
    private final Estado[] estados = {Estado.IDLE, Estado.IDLE};

    public ControladorPartida(Nivel nivel, ElementosNivel elementos, ControladorEntradaJugador entrada,
        CapuccinoAssassino cappuccino, BallerinaCapuccina ballerina) {
        this(nivel, elementos, null, null, null, null, entrada, cappuccino, ballerina, null, null);
    }

    public boolean estaDerrotada() { return derrota; }
    public boolean hayVictoria() { return victoria; }
    public Estado getEstado(int rol) { return estados[rol]; }

    private void estadoCappuccino(Estado estado) {
        estados[0] = estado;
        if (animacionesCappuccino != null) animacionesCappuccino.setEstado(estado);
    }

    private void estadoBallerina(Estado estado) {
        estados[1] = estado;
        if (animacionesBallerina != null) animacionesBallerina.setEstado(estado);
    }

    public ControladorPartida(
        Nivel nivel,
        ElementosNivel elementos,
        MenuPausa menuPausa,
        MenuGameOver menuGameOver,
        MenuVictoria menuVictoria,
        HUD hud,
        ControladorEntradaJugador entrada,
        CapuccinoAssassino cappuccino,
        BallerinaCapuccina ballerina,
        AnimacionesCappuccino animacionesCappuccino,
        AnimacionesBallerina animacionesBallerina
    ) {
        this.nivel = nivel;
        this.elementos = elementos;
        this.menuPausa = menuPausa;
        this.menuGameOver = menuGameOver;
        this.menuVictoria = menuVictoria;
        this.hud = hud;
        this.entrada = entrada;
        this.cappuccino = cappuccino;
        this.ballerina = ballerina;
        this.animacionesCappuccino = animacionesCappuccino;
        this.animacionesBallerina = animacionesBallerina;
    }

    public void actualizar(float delta) {

        if (menuPausa != null && entrada.isEscapePresionado()) {
            if (menuPausa.estaVisible()) {
                menuPausa.ocultar();
            } else if (!menuGameOver.estaVisible() && !menuVictoria.estaVisible()) {
                menuPausa.mostrar();
                entrada.soltarTodo();
            }
        }

        if (menuPausa != null && menuPausa.estaVisible()) {
            menuPausa.actualizar(delta);
            return;
        }

        if (derrota) {
            if (menuGameOver != null) menuGameOver.actualizar(delta);
            return;
        }

        if (victoria) {
            if (menuVictoria != null) menuVictoria.actualizar(delta);
            return;
        }

        if (menuPausa != null && Gdx.input != null && Gdx.input.getInputProcessor() != entrada) {
            Gdx.input.setInputProcessor(entrada);
        }

        if (hud != null) hud.actualizar(delta);

        ballerina.actualizarHabilidad(delta);

        if (entrada.isHabilidadPresionadaBallerina()) {
            ballerina.crearEspuma();
        }


        Array<Rectangle> solidos = new Array<>(nivel.getColisiones().getSolidos());

        for (ObjetoCortable cortable : elementos.getCortables()) {
            if (cortable.bloquea()) {
                solidos.add(cortable.getZona());
            }
        }

        BloqueEspuma espuma = ballerina.getEspuma();
        if (espuma != null) {
            solidos.add(espuma.getZona());
        }

        cappuccino.actualizar(
            delta,
            entrada.isIzquierda(),
            entrada.isDerecha(),
            entrada.isSaltoPresionado(),
            solidos
        );

        ballerina.actualizar(
            delta,
            entrada.isIzquierdaBallerina(),
            entrada.isDerechaBallerina(),
            entrada.isSaltoPresionadoBallerina(),
            solidos
        );


        if (entrada.isHabilidadPresionada()) {
            estadoCappuccino(Estado.ABILITY);
            cortar();
        } else if (!cappuccino.isEnSuelo()) {
            estadoCappuccino(Estado.JUMP);
        } else if (entrada.isIzquierda() || entrada.isDerecha()) {
            estadoCappuccino(Estado.WALK);
        } else {
            estadoCappuccino(Estado.IDLE);
        }

        if (entrada.isHabilidadPresionadaBallerina()) {
            estadoBallerina(Estado.ABILITY);
        } else if (!ballerina.isEnSuelo()) {
            estadoBallerina(Estado.JUMP);
        } else if (entrada.isIzquierdaBallerina() || entrada.isDerechaBallerina()) {
            estadoBallerina(Estado.WALK);
        } else {
            estadoBallerina(Estado.IDLE);
        }

        if (animacionesCappuccino != null) animacionesCappuccino.actualizar(delta);
        if (animacionesBallerina != null) animacionesBallerina.actualizar(delta);


        if (estaMuerto(cappuccino) || estaMuerto(ballerina)) {
            mostrarGameOver();
            return;
        }


        comprobarVictoria();

        if (victoria) {
            return;
        }
        actualizarPalancas();
        entrada.limpiarEventos();
    }

    private void actualizarPalancas() {
        Palanca palCap = elementos.getPalancaCappuccino();
        Palanca palBal = elementos.getPalancaBallerina();

        if (entrada.isInteractuarPresionado()
            && estaCerca(cappuccino, palCap)) {
            intentarAlternar(palCap);
        }

        if (entrada.isInteractuarPresionadoBallerina()
            && estaCerca(ballerina, palBal)) {
            intentarAlternar(palBal);
        }
    }
    private boolean estaCerca(Player personaje, Palanca palanca) {
        if (palanca == null) return false;
        Rectangle c = personaje.getCaja();
        Rectangle alcance = new Rectangle(
            c.x - ALCANCE_PALANCA, c.y - ALCANCE_PALANCA,
            c.width + 2 * ALCANCE_PALANCA, c.height + 2 * ALCANCE_PALANCA);
        return alcance.overlaps(palanca.getZona());
    }
    private void intentarAlternar(Palanca palanca) {
        Puerta puerta = elementos.getPuerta();

        boolean seCerraria = palanca.estaActivada() && puerta != null && puerta.estaAbierta();
        if (seCerraria && alguienEnLaPuerta()) return;

        palanca.alternar();
        actualizarPuerta();
    }


    private void actualizarPuerta() {
        Puerta puerta = elementos.getPuerta();
        Palanca palCap = elementos.getPalancaCappuccino();
        Palanca palBal = elementos.getPalancaBallerina();
        if (puerta == null || (palCap == null && palBal == null)) return;

        boolean capOk = palCap == null || palCap.estaActivada();
        boolean balOk = palBal == null || palBal.estaActivada();
        puerta.setAbierta(capOk && balOk);
    }
    private boolean alguienEnLaPuerta() {
        Puerta puerta = elementos.getPuerta();
        return puerta != null
            && (cappuccino.getCaja().overlaps(puerta.getZona())
            || ballerina.getCaja().overlaps(puerta.getZona()));
    }


    private void comprobarVictoria() {
        Puerta puerta = elementos.getPuerta();

        if (puerta == null || !puerta.estaAbierta()) {
            return;
        }

        Rectangle zona = puerta.getZona();

        if (cappuccino.getCaja().overlaps(zona) && ballerina.getCaja().overlaps(zona)) {
            entrada.soltarTodo();
            victoria = true;
            if (menuVictoria != null) menuVictoria.mostrar();
        }
    }

    private boolean estaMuerto(Player personaje) {
        if (personaje.getY() < -personaje.getAlto()) {
            return true;
        }

        for (Rectangle peligro : elementos.getHitboxPeligros()) {
            if (personaje.getCaja().overlaps(peligro)) {
                return true;
            }
        }

        return false;
    }

    private void mostrarGameOver() {
        entrada.soltarTodo();
        derrota = true;
        if (menuGameOver != null) menuGameOver.mostrar();
    }

    private void cortar() {
        Rectangle caja = cappuccino.getCaja();
        float alcance = 40f;
        Rectangle zonaCorte;

        if (cappuccino.isMirandoDerecha()) {
            zonaCorte = new Rectangle(caja.x + caja.width, caja.y, alcance, caja.height);
        } else {
            zonaCorte = new Rectangle(caja.x - alcance, caja.y, alcance, caja.height);
        }

        for (ObjetoCortable cortable : elementos.getCortables()) {
            if (!cortable.estaCortado() && zonaCorte.overlaps(cortable.getZona())) {
                cortable.setCortado(true);
            }
        }
    }

    public void reiniciar() {
        derrota = false;
        victoria = false;
        estados[0] = estados[1] = Estado.IDLE;
        cappuccino.reaparecer(elementos.getSpawnX(), elementos.getSpawnY());
        ballerina.reaparecer(elementos.getSpawnBallerinaX(), elementos.getSpawnBallerinaY());

        ballerina.reiniciarHabilidad();

        Puerta puerta = elementos.getPuerta();

        if (puerta != null) {
            puerta.setAbierta(false);
        }

        Palanca palCap = elementos.getPalancaCappuccino();
        Palanca palBal = elementos.getPalancaBallerina();
        if (palCap != null && palCap.estaActivada()) palCap.alternar();
        if (palBal != null && palBal.estaActivada()) palBal.alternar();

        for (ObjetoCortable cortable : elementos.getCortables()) {
            cortable.setCortado(false);
        }

        entrada.soltarTodo();
        if (animacionesCappuccino != null) animacionesCappuccino.reiniciar();
        if (animacionesBallerina != null) animacionesBallerina.reiniciar();
        if (hud != null) hud.reiniciar();
    }
}
