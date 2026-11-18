package com.capuccinolovesballerina.game;

import com.badlogic.gdx.Game;
import com.capuccinolovesballerina.game.Utilidades.Audio;
import com.capuccinolovesballerina.game.Utilidades.Recursos;
import com.capuccinolovesballerina.game.pantallas.PantallaMenuServidor;

/** Aplicacion grafica exclusiva del servidor; nunca entra a una partida como jugador. */
public class CapuccinoServidorGame extends Game {
    @Override public void create() { setScreen(new PantallaMenuServidor(this)); }
    @Override public void dispose() {
        super.dispose();
        Recursos.dispose();
        Audio.dispose();
    }
}
