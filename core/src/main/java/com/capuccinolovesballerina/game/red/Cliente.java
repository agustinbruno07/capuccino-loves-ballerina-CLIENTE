
package com.capuccinolovesballerina.game.red;

import com.capuccinolovesballerina.game.red.hilos.HiloCliente;
import com.capuccinolovesballerina.game.red.ProtocolosRed.Controles;
import com.capuccinolovesballerina.game.red.ProtocolosRed.EstadoPartida;

public class Cliente {

    private HiloCliente hiloRed;

    private long saltos;
    private long habilidades;
    private long interacciones;
    private long reinicios;

    public void conectar() {

        cerrar();

        saltos = 0;
        habilidades = 0;
        interacciones = 0;
        reinicios = 0;

        hiloRed = new HiloCliente();
        hiloRed.start();
    }

    public void enviarControles(
        boolean izquierda,
        boolean derecha,
        boolean salto,
        boolean habilidad,
        boolean interactuar
    ) {

        if (hiloRed == null) return;

        if (salto) saltos++;
        if (habilidad) habilidades++;
        if (interactuar) interacciones++;

        Controles controles = new Controles(
            izquierda,
            derecha,
            saltos,
            habilidades,
            interacciones
        );

        hiloRed.controles(controles);
    }

    public void enviarReinicio() {

        if (hiloRed == null) return;

        reinicios++;
        hiloRed.reiniciar(reinicios);
    }

    public EstadoPartida getEstadoPartida() {

        if (hiloRed == null) return null;

        return hiloRed.getFoto();
    }

    public String getEstadoConexion() {

        if (hiloRed == null) return "Sin conexion";

        return hiloRed.getEstado();
    }

    public boolean sinServidor() {

        return hiloRed != null && hiloRed.sinServidor();
    }

    public boolean termino() {

        return hiloRed == null || hiloRed.terminado();
    }

    public void cerrar() {

        if (hiloRed != null) {
            hiloRed.detener();
        }
    }

    public void cerrar(String motivo) {

        if (hiloRed != null) {
            hiloRed.detener(motivo);
        }
    }
}
