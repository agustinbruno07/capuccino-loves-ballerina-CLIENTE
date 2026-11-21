package com.capuccinolovesballerina.game.red.hilos;

import com.capuccinolovesballerina.game.red.ProtocolosRed.Controles;
import com.capuccinolovesballerina.game.red.ProtocolosRed.EstadoPartida;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static com.capuccinolovesballerina.game.red.ProtocolosRed.*;

public class HiloCliente extends Thread {

    private volatile boolean fin;
    private volatile boolean sinServidor;

    private volatile String estado = "Buscando servidor...";
    private volatile EstadoPartida foto;
    private volatile Controles controles = Controles.VACIOS;
    private volatile long pedidoReinicio;

    public HiloCliente() {
        super("UDP-cliente");
        setDaemon(true);
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {

            socket.setBroadcast(true);
            socket.setSoTimeout(25);

            List<InetSocketAddress> destinos = destinosDescubrimiento();

            long inicio = System.nanoTime();
            long ultimaRecepcion = inicio;
            long ultimoEstado = inicio;
            long ultimoEnvio = 0;
            long ultimoPing = 0;
            long secuencia = 0;

            long rondaReinicio = -1;
            long reinicioEnviado = 0;

            InetSocketAddress servidor = null;
            String sesion = null;

            while (!fin) {
                long ahora = System.nanoTime();

                if (sesion == null) {

                    if (ahora - inicio > BUSQUEDA_MS * 1_000_000L) {
                        sinServidor = servidor == null;
                        estado = "No se encontro una partida.";
                        break;
                    }

                    if (ahora - ultimoEnvio > 500_000_000L) {
                        List<InetSocketAddress> destinosEnvio =
                            servidor == null ? destinos : List.of(servidor);

                        for (InetSocketAddress destino : destinosEnvio) {
                            enviar(socket, destino, servidor == null ? CONEXION : REGISTRO);
                        }

                        ultimoEnvio = ahora;
                    }

                } else {

                    if (ahora - ultimaRecepcion > TIMEOUT_MS * 1_000_000L) {
                        estado = "Se perdio la conexion con el servidor.";
                        break;
                    }

                    if (ahora - ultimoEstado > TIMEOUT_MS * 1_000_000L) {
                        estado = "No se pudo iniciar la partida. Usen la misma version del juego.";
                        break;
                    }

                    if (ahora - ultimoPing > 1_000_000_000L) {
                        enviar(socket, servidor, PING + "|" + sesion);

                        if (foto == null || foto.fase().equals(ESPERA)) {
                            enviar(socket, servidor, LISTO + "|" + sesion);
                        }

                        ultimoPing = ahora;
                    }

                    if (foto != null && ahora - ultimoEnvio > 33_000_000L) {
                        enviar(socket, servidor,
                            controles.mensaje(sesion, foto.ronda(), ++secuencia));

                        if (pedidoReinicio > reinicioEnviado) {
                            reinicioEnviado = pedidoReinicio;
                            rondaReinicio = foto.ronda();
                        }

                        if (rondaReinicio == foto.ronda()) {
                            enviar(socket, servidor,
                                REINICIO + "|" + sesion + "|" + rondaReinicio + "|" + reinicioEnviado);
                        }

                        ultimoEnvio = ahora;
                    }
                }

                byte[] datos = new byte[MAX_PAQUETE + 1];
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);

                try {
                    socket.receive(paquete);

                    if (paquete.getLength() > MAX_PAQUETE || paquete.getPort() != PUERTO) {
                        continue;
                    }

                    InetSocketAddress origen =
                        (InetSocketAddress) paquete.getSocketAddress();

                    if (servidor != null && !origen.equals(servidor)) {
                        continue;
                    }

                    String[] mensaje = partes(
                        new String(datos, 0, paquete.getLength(), StandardCharsets.UTF_8)
                    );

                    if (sesion == null) {

                        if (mensaje.length == 1 && mensaje[0].equals(OK)) {
                            servidor = origen;
                            estado = "Partida encontrada. Esperando personaje...";
                            enviar(socket, servidor, REGISTRO);

                        } else if (mensaje.length == 3 && mensaje[0].equals(ASIGNADO)) {
                            int rol = Integer.parseInt(mensaje[2]);

                            if ((rol != 0 && rol != 1)
                                || !mensaje[1].matches("[a-f0-9-]{36}")) {
                                continue;
                            }

                            servidor = origen;
                            sesion = mensaje[1];

                            estado = "Conectado como "
                                + (rol == 0 ? "Cappuccino" : "Ballerina")
                                + ". Esperando al otro jugador...";

                            ultimaRecepcion = ahora;
                            ultimoEstado = ahora;

                            enviar(socket, servidor, LISTO + "|" + sesion);

                        } else if (mensaje.length == 1 && mensaje[0].equals(LLENO)) {
                            estado = "El servidor ya tiene dos jugadores.";
                            break;
                        }

                    } else if (mensaje.length >= 2 && mensaje[1].equals(sesion)) {

                        if (mensaje[0].equals(ESTADO)) {
                            EstadoPartida nuevoEstado = EstadoPartida.leer(mensaje);

                            if (foto != null && nuevoEstado.secuencia() <= foto.secuencia()) {
                                continue;
                            }

                            foto = nuevoEstado;

                            if (foto.fase().equals(CUENTA)) {
                                estado = "Inicio en " + foto.cuenta() + "...";
                            } else if (foto.fase().equals(ESPERA)) {
                                estado = "Esperando al otro jugador...";
                            } else {
                                estado = "Partida en red";
                            }

                            ultimaRecepcion = ahora;
                            ultimoEstado = ahora;

                        } else if (mensaje[0].equals(PONG) && mensaje.length == 2) {
                            ultimaRecepcion = ahora;

                        } else if (mensaje[0].equals(DESCONECTADO) && mensaje.length == 3) {
                            estado = mensaje[2];
                            break;
                        }
                    }

                } catch (SocketTimeoutException ignorado) {
                } catch (IllegalArgumentException ignorado) {
                }
            }

            if (sesion != null) {
                for (int i = 0; i < 3; i++) {
                    enviar(socket, servidor, SALIR + "|" + sesion);
                }
            }

        } catch (IOException error) {
            if (!fin) {
                estado = "No se pudo conectar. Comproba la conexion de red.";
                error.printStackTrace();
            }
        } finally {
            fin = true;
        }
    }

    private static List<InetSocketAddress> destinosDescubrimiento() throws SocketException {
        LinkedHashSet<InetSocketAddress> destinos = new LinkedHashSet<>();

        destinos.add(new InetSocketAddress("255.255.255.255", PUERTO));

        var interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface interfaz = interfaces.nextElement();

            if (!interfaz.isUp() || interfaz.isLoopback()) {
                continue;
            }

            for (InterfaceAddress direccion : interfaz.getInterfaceAddresses()) {
                if (direccion.getBroadcast() != null) {
                    destinos.add(new InetSocketAddress(direccion.getBroadcast(), PUERTO));
                }
            }
        }

        destinos.add(new InetSocketAddress("127.0.0.1", PUERTO));

        return new ArrayList<>(destinos);
    }

    private static void enviar(DatagramSocket socket, InetSocketAddress destino, String mensaje)
        throws IOException {

        byte[] datos = bytes(mensaje);

        DatagramPacket paquete = new DatagramPacket(datos, datos.length, destino);

        socket.send(paquete);
    }

    public void controles(Controles nuevos) {
        controles = nuevos;
    }

    public void reiniciar(long numero) {
        pedidoReinicio = numero;
    }

    public EstadoPartida getFoto() {
        return foto;
    }

    public String getEstado() {
        return estado;
    }

    public boolean sinServidor() {
        return sinServidor;
    }

    public boolean terminado() {
        return fin;
    }

    public void detener() {
        fin = true;
        interrupt();
    }

    public void detener(String motivo) {
        estado = motivo;
        detener();
    }
}
