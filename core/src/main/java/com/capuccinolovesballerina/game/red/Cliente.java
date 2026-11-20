package com.capuccinolovesballerina.game.red;

import com.capuccinolovesballerina.game.red.hilos.HiloCliente;
import com.capuccinolovesballerina.game.red.ProtocolosRed.*;

public class Cliente implements AutoCloseable {
    private HiloCliente hilo;
    private long saltos, habilidades, interacciones, reinicios;
    public void conectar() { conectar(""); }
    public void conectar(String ipManual) {
        cerrar();
        saltos = habilidades = interacciones = reinicios = 0;
        hilo = new HiloCliente(ipManual);
        hilo.start();
    }
    public void enviarControles(boolean izquierda, boolean derecha, boolean salto, boolean habilidad, boolean interactuar) {
        if (hilo == null) return;
        if (salto) saltos++;
        if (habilidad) habilidades++;
        if (interactuar) interacciones++;
        hilo.controles(new Controles(izquierda, derecha, saltos, habilidades, interacciones));
    }
    public void enviarReinicio() { if (hilo != null) hilo.reiniciar(++reinicios); }
    public EstadoPartida getEstadoPartida() { return hilo == null ? null : hilo.getFoto(); }
    public int getMiRol() { return hilo == null ? -1 : hilo.getRol(); }
    public String getIpServer() { return hilo == null ? "" : hilo.getIpServidor(); }
    public String getEstadoConexion() { return hilo == null ? "Sin conexion" : hilo.getEstado(); }
    public boolean sinServidor() { return hilo != null && hilo.sinServidor(); }
    public boolean termino() { return hilo == null || hilo.terminado(); }
    public void cerrar() { if (hilo != null) hilo.detener(); }
    public void cerrar(String motivo) { if (hilo != null) hilo.detener(motivo); }
    @Override public void close() { cerrar(); }
}
