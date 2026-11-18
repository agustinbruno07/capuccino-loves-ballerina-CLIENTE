package com.capuccinolovesballerina.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.capuccinolovesballerina.game.CapuccinoServidorGame;

/** Punto de entrada exclusivo de CappuccinoServidor.exe. */
public final class Lwjgl3ServidorLauncher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        var configuracion = Lwjgl3Launcher.getDefaultConfiguration();
        configuracion.setTitle("Cappuccino Loves Ballerina - Servidor");
        new Lwjgl3Application(new CapuccinoServidorGame(), configuracion);
    }
}
