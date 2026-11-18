package com.capuccinolovesballerina.game.red.hilos;

import com.capuccinolovesballerina.game.red.ProtocolosRed.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import static com.capuccinolovesballerina.game.red.ProtocolosRed.*;

/** Un solo hilo/socket descubre, recibe y envia. Nunca modifica elementos de LibGDX. */
public class HiloCliente extends Thread {
    private final String ipManual;
    private volatile DatagramSocket socket;
    private volatile boolean fin, sinServidor;
    private volatile int rol = -1;
    private volatile String estado = "Buscando servidor...", ipServidor = "";
    private volatile EstadoPartida foto;
    private volatile Controles controles = Controles.VACIOS;
    private volatile long pedidoReinicio;
    private InetSocketAddress servidor;
    private String sesion;

    public HiloCliente(String ipManual) {
        super("UDP-cliente");
        this.ipManual = ipManual == null ? "" : ipManual.trim();
        setDaemon(true);
    }
    @Override public void run() {
        try (DatagramSocket abierto = new DatagramSocket()) {
            socket = abierto;
            socket.setBroadcast(true);
            socket.setSoTimeout(25);
            InetAddress manual = ipManual.isEmpty() ? null : InetAddress.getByName(ipManual);
            List<InetSocketAddress> busqueda = manual == null ? destinosDescubrimiento()
                : List.of(new InetSocketAddress(manual, PUERTO));
            System.out.println("[RED CLIENTE] Socket local UDP " + socket.getLocalPort() + ". Destinos: " + busqueda);
            long inicio = System.nanoTime(), ultimaRecepcion = inicio, ultimoEnvio = 0, ultimoPing = 0, secuencia = 0;
            long ultimoEstado = inicio;
            int intentos = 0;
            long rondaReinicio = -1, reinicioEnviado = 0;
            while (!fin) {
                long ahora = System.nanoTime();
                if (sesion == null) {
                    if (ahora - inicio > BUSQUEDA_MS * 1_000_000L) {
                        sinServidor = servidor == null;
                        estado = "No se encontro una partida.";
                        break;
                    }
                    if (ahora - ultimoEnvio > 500_000_000L) {
                        // Como SpeedG2L: despues de OK se reintenta directamente contra la IP recibida.
                        List<InetSocketAddress> destinos = servidor == null ? busqueda : List.of(servidor);
                        System.out.println("[RED CLIENTE] CONEXION, intento " + (++intentos) + ": " + destinos);
                        for (InetSocketAddress destino : destinos) {
                            try { enviar(destino, servidor == null ? CONEXION : REGISTRO); }
                            catch (IOException error) {
                                // Una interfaz virtual sin ruta no debe cancelar las demas interfaces.
                                System.err.println("[RED CLIENTE] No se pudo enviar a " + destino + ": " + error.getMessage());
                            }
                        }
                        ultimoEnvio = ahora;
                    }
                } else {
                    if (ahora - ultimaRecepcion > TIMEOUT_MS * 1_000_000L) { estado = "Se perdio la conexion con el servidor."; break; }
                    if (ahora - ultimoEstado > TIMEOUT_MS * 1_000_000L) {
                        estado = "No se pudo iniciar la partida. Usen la misma version del juego.";
                        break;
                    }
                    if (ahora - ultimoPing > 1_000_000_000L) {
                        enviar(servidor, PING + "|" + sesion);
                        if (foto == null || foto.fase().equals(ESPERA)) enviar(servidor, LISTO + "|" + sesion);
                        ultimoPing = ahora;
                    }
                    if (foto != null && ahora - ultimoEnvio > 33_000_000L) {
                        enviar(servidor, controles.mensaje(sesion, foto.ronda(), ++secuencia));
                        if (pedidoReinicio > reinicioEnviado) {
                            reinicioEnviado = pedidoReinicio;
                            rondaReinicio = foto.ronda();
                        }
                        if (rondaReinicio == foto.ronda())
                            enviar(servidor, REINICIO + "|" + sesion + "|" + rondaReinicio + "|" + reinicioEnviado);
                        ultimoEnvio = ahora;
                    }
                }
                byte[] datos = new byte[MAX_PAQUETE + 1];
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);
                try {
                    socket.receive(paquete);
                    if (paquete.getLength() > MAX_PAQUETE || paquete.getPort() != PUERTO) continue;
                    InetSocketAddress origen = (InetSocketAddress) paquete.getSocketAddress();
                    if (servidor != null && !origen.equals(servidor)) continue;
                    if (manual != null && !paquete.getAddress().equals(manual)) continue;
                    String[] p = partes(new String(datos, 0, paquete.getLength(), StandardCharsets.UTF_8));
                    if (sesion == null && p.length == 1 && p[0].equals(OK)) {
                        servidor = origen;
                        ipServidor = origen.getAddress().getHostAddress();
                        estado = "Partida encontrada. Esperando personaje...";
                        System.out.println("[RED CLIENTE] OK recibido desde " + servidor);
                        enviar(servidor, REGISTRO);
                    } else if (sesion == null && p.length == 3 && p[0].equals(ASIGNADO)) {
                        int asignado = Integer.parseInt(p[2]);
                        if ((asignado != 0 && asignado != 1) || !p[1].matches("[a-f0-9-]{36}")) continue;
                        // ASIGNADO tambien confirma el servidor si OK se perdio o llego despues.
                        servidor = origen;
                        ipServidor = origen.getAddress().getHostAddress();
                        sesion = p[1]; rol = asignado;
                        estado = "Conectado como " + (rol == 0 ? "Cappuccino" : "Ballerina") + ". Esperando al otro jugador...";
                        ultimaRecepcion = ahora;
                        ultimoEstado = ahora;
                        enviar(servidor, LISTO + "|" + sesion);
                        System.out.println("[RED CLIENTE] Personaje " + rol + " asignado en " + servidor + "; LISTO enviado.");
                    } else if (sesion == null && p.length == 1 && p[0].equals(LLENO)) {
                        estado = "El servidor ya tiene dos jugadores."; break;
                    } else if (sesion != null && p.length >= 2 && p[1].equals(sesion)) {
                        if (p[0].equals(ESTADO)) {
                            EstadoPartida nueva = EstadoPartida.leer(p);
                            if (foto != null && nueva.secuencia() <= foto.secuencia()) continue;
                            foto = nueva;
                            estado = nueva.fase().equals(CUENTA) ? "Inicio en " + nueva.cuenta() + "..."
                                : nueva.fase().equals(ESPERA) ? "Esperando al otro jugador..." : "Partida en red";
                            ultimaRecepcion = ahora;
                            ultimoEstado = ahora;
                        } else if (p[0].equals(PONG) && p.length == 2) {
                            ultimaRecepcion = ahora;
                        } else if (p[0].equals(DESCONECTADO) && p.length == 3) {
                            estado = p[2]; break;
                        }
                    }
                } catch (SocketTimeoutException ignorado) {
                    // Despierta para reintentar, enviar controles o comprobar el timeout.
                } catch (IllegalArgumentException ignorado) {
                    // Un datagrama incorrecto nunca llega a la pantalla.
                }
            }
            if (sesion != null && !socket.isClosed())
                for (int i = 0; i < 3; i++) enviar(servidor, SALIR + "|" + sesion);
        } catch (IOException error) {
            if (!fin) { estado = "No se pudo conectar. Comproba la conexion de red."; error.printStackTrace(); }
        } finally {
            fin = true;
            System.out.println("[RED CLIENTE] Conexion cerrada: " + estado);
        }
    }
    /** Broadcast general + broadcast de cada interfaz; loopback permite probar dos clientes en una PC. */
    public static List<InetSocketAddress> destinosDescubrimiento() throws SocketException, UnknownHostException {
        LinkedHashSet<InetSocketAddress> destinos = new LinkedHashSet<>();
        destinos.add(new InetSocketAddress(InetAddress.getByName("255.255.255.255"), PUERTO));
        var interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface interfaz = interfaces.nextElement();
            if (!interfaz.isUp() || interfaz.isLoopback()) continue;
            for (InterfaceAddress direccion : interfaz.getInterfaceAddresses()) {
                if (direccion.getBroadcast() != null)
                    destinos.add(new InetSocketAddress(direccion.getBroadcast(), PUERTO));
            }
        }
        destinos.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PUERTO));
        return new ArrayList<>(destinos);
    }
    private void enviar(InetSocketAddress destino, String mensaje) throws IOException {
        byte[] datos = bytes(mensaje);
        socket.send(new DatagramPacket(datos, datos.length, destino));
    }
    public void controles(Controles nuevos) { controles = nuevos; }
    public void reiniciar(long numero) { pedidoReinicio = numero; }
    public EstadoPartida getFoto() { return foto; }
    public int getRol() { return rol; }
    public String getEstado() { return estado; }
    public String getIpServidor() { return ipServidor; }
    public boolean sinServidor() { return sinServidor; }
    public boolean terminado() { return fin; }
    public void detener() { fin = true; interrupt(); }
    public void detener(String motivo) { estado = motivo; detener(); }
}
