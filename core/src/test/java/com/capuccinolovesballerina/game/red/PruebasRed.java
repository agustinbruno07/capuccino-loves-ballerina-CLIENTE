package com.capuccinolovesballerina.game.red;

import com.capuccinolovesballerina.game.Utilidades.*;
import com.capuccinolovesballerina.game.entidades.*;
import com.capuccinolovesballerina.game.mapa.Nivel;
import com.capuccinolovesballerina.game.objetos.*;
import com.badlogic.gdx.math.Rectangle;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static com.capuccinolovesballerina.game.red.ProtocolosRed.*;

/** Pruebas reproducibles, sin JUnit ni dependencias adicionales. Usan sockets UDP reales. */
public class PruebasRed {
    private static int comprobaciones;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    private static void esperar(BooleanSupplier condicion, long limiteMs, String mensaje) throws Exception {
        long limite = System.nanoTime() + limiteMs * 1_000_000L;
        while (!condicion.getAsBoolean() && System.nanoTime() < limite) Thread.sleep(10);
        comprobar(condicion.getAsBoolean(), mensaje);
    }
    public static void main(String[] args) throws Exception {
        probarReglas();
        probarDestinos();
        probarAsignacionSinOK();
        probarConfirmacion();
        probarDuplicados();
        probarUDP();
        probarClienteSilencioso();
        probarInicioSeparado();
        System.out.println("PRUEBAS RED OK: " + comprobaciones + " comprobaciones.");
    }

    private static void probarInicioSeparado() throws Exception {
        try (Cliente cliente = new Cliente()) {
            cliente.conectar();
            esperar(cliente::termino, BUSQUEDA_MS + 2000, "Cliente sin servidor termina la busqueda");
            comprobar(cliente.sinServidor() && cliente.getMiRol() == -1, "Sin servidor no asigna personaje");
            try (DatagramSocket libre = new DatagramSocket(PUERTO)) {
                comprobar(libre.isBound(), "Cliente no ocupa el puerto del servidor");
            }
        }
        try (DatagramSocket ocupado = new DatagramSocket(PUERTO); ServicioServidor servicio = new ServicioServidor()) {
            esperar(servicio::termino, 3000, "Servidor detecta puerto ocupado");
            comprobar(!servicio.estaIniciado(), "No crea otra instancia");
            comprobar(servicio.getMensaje().contains("otra aplicacion"), "Error de puerto entendible");
        }
        ServicioServidor servicio = new ServicioServidor();
        try {
            esperar(servicio::estaIniciado, 3000, "Aplicacion servidor inicia explicitamente");
        } finally { servicio.close(); }
        esperar(servicio::termino, 3000, "Servicio cierra y termina su bucle");
        try (DatagramSocket libre = new DatagramSocket(PUERTO)) {
            comprobar(libre.isBound(), "Servicio libera el puerto al detenerse");
        }
    }

    private static void probarConfirmacion() throws Exception {
        try (Servidor servidor = new Servidor(); DatagramSocket uno = new DatagramSocket(); DatagramSocket dos = new DatagramSocket()) {
            procesar(servidor, uno, CONEXION);
            comprobar(java.util.Arrays.stream((Object[]) campo(servidor, "clientes")).allMatch(java.util.Objects::isNull),
                "Broadcast no reserva cupos ni duplica jugadores por interfaz");
            procesar(servidor, uno, REGISTRO);
            String primera = sesion(uno);
            procesar(servidor, dos, CONEXION);
            procesar(servidor, dos, REGISTRO);
            String segunda = sesion(dos);
            comprobar(campo(servidor, "fase").equals(ESPERA), "No inicia antes de confirmar personajes");
            procesar(servidor, uno, LISTO + "|" + primera);
            comprobar(campo(servidor, "fase").equals(ESPERA), "Una confirmacion no inicia cuenta");
            procesar(servidor, dos, LISTO + "|" + segunda);
            comprobar(campo(servidor, "fase").equals(CUENTA), "Dos confirmaciones inician cuenta");
            long ronda = (long) campo(servidor, "ronda");
            procesar(servidor, dos, LISTO + "|" + segunda);
            comprobar((long) campo(servidor, "ronda") == ronda, "LISTO duplicado no reinicia cuenta");
            ((Nivel) campo(servidor, "nivel")).dispose();
        }
    }

    private static void probarDestinos() throws Exception {
        var destinos = com.capuccinolovesballerina.game.red.hilos.HiloCliente.destinosDescubrimiento();
        comprobar(destinos.contains(new InetSocketAddress("255.255.255.255", PUERTO)), "Conserva broadcast de SpeedG2L");
        comprobar(destinos.contains(new InetSocketAddress("127.0.0.1", PUERTO)), "Prueba automatica en una PC");
        comprobar(destinos.stream().allMatch(d -> d.getPort() == PUERTO), "Solo puerto 9992");
        comprobar(new java.util.HashSet<>(destinos).size() == destinos.size(), "Destinos sin duplicados");
    }
    private static void probarAsignacionSinOK() throws Exception {
        try (DatagramSocket servidor = new DatagramSocket(PUERTO); Cliente cliente = new Cliente()) {
            servidor.setSoTimeout(1500);
            cliente.conectar("127.0.0.1");
            DatagramPacket pedido = new DatagramPacket(new byte[1400], 1400);
            servidor.receive(pedido);
            String sesion = "11111111-1111-4111-8111-111111111111";
            // Se omite OK intencionalmente: en UDP ASIGNADO puede llegar primero.
            byte[] datos = bytes(ASIGNADO + "|" + sesion + "|0");
            servidor.send(new DatagramPacket(datos, datos.length, pedido.getSocketAddress()));
            esperar(() -> cliente.getMiRol() == 0, 1500, "ASIGNADO sin OK no bloquea conexion");
            comprobar(cliente.getIpServer().equals("127.0.0.1"), "IP tomada del paquete recibido");
            var jugador = new Jugador(10, 20, true, false, com.capuccinolovesballerina.game.graficos.Estado.IDLE, 0);
            for (int secuencia : new int[]{2, 1}) {
                var foto = new EstadoPartida(secuencia, 0, ESPERA, 0, 0, jugador, jugador, false, false, false, "", 0, 0, 0, 0);
                datos = bytes(foto.mensaje(sesion));
                servidor.send(new DatagramPacket(datos, datos.length, pedido.getSocketAddress()));
            }
            esperar(() -> cliente.getEstadoPartida() != null, 1500, "Estado recibido luego de asignacion");
            comprobar(cliente.getEstadoPartida().secuencia() == 2, "Foto atrasada no reemplaza estado nuevo");
        }
    }

    private static void probarClienteSilencioso() throws Exception {
        Servidor servidor = new Servidor();
        Thread hilo = new Thread(servidor::ejecutar, "Prueba-timeout-servidor");
        hilo.start();
        try (DatagramSocket silencioso = new DatagramSocket(); Cliente otro = new Cliente()) {
            byte[] datos = bytes(CONEXION);
            silencioso.send(new DatagramPacket(datos, datos.length, InetAddress.getLoopbackAddress(), PUERTO));
            datos = bytes(REGISTRO);
            silencioso.send(new DatagramPacket(datos, datos.length, InetAddress.getLoopbackAddress(), PUERTO));
            comprobar(!sesion(silencioso).isEmpty(), "Cliente silencioso registrado");
            otro.conectar("127.0.0.1");
            esperar(() -> otro.getMiRol() == 1, 2000, "Segundo cliente con heartbeat");
            // El primer socket sigue abierto pero no envia PING ni controles: simula un cable desconectado.
            esperar(otro::termino, 6500, "Servidor detecta cliente sin heartbeat");
            comprobar(otro.getEstadoConexion().contains("perdio"), "Aviso por timeout de jugador");
        } finally { servidor.close(); hilo.join(2000); }
    }

    private static Object campo(Object objeto, String nombre) throws Exception {
        var campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(objeto);
    }
    private static void procesar(Servidor servidor, DatagramSocket socket, String mensaje) throws Exception {
        var metodo = Servidor.class.getDeclaredMethod("procesar", com.capuccinolovesballerina.game.red.hilos.HiloServidor.Recibido.class);
        metodo.setAccessible(true);
        metodo.invoke(servidor, new com.capuccinolovesballerina.game.red.hilos.HiloServidor.Recibido(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), socket.getLocalPort()), mensaje));
    }
    private static String sesion(DatagramSocket socket) throws Exception {
        socket.setSoTimeout(1000);
        while (true) {
            DatagramPacket paquete = new DatagramPacket(new byte[1400], 1400);
            socket.receive(paquete);
            String[] p = partes(new String(paquete.getData(), 0, paquete.getLength(), java.nio.charset.StandardCharsets.UTF_8));
            if (p[0].equals(ASIGNADO)) return p[1];
        }
    }
    private static void probarDuplicados() throws Exception {
        try (Servidor servidor = new Servidor(); DatagramSocket uno = new DatagramSocket(); DatagramSocket dos = new DatagramSocket()) {
            procesar(servidor, uno, CONEXION);
            procesar(servidor, uno, REGISTRO);
            String capSesion = sesion(uno);
            procesar(servidor, dos, CONEXION);
            procesar(servidor, dos, REGISTRO);
            String balSesion = sesion(dos);
            procesar(servidor, uno, LISTO + "|" + capSesion);
            procesar(servidor, dos, LISTO + "|" + balSesion);
            var fase = Servidor.class.getDeclaredField("fase");
            fase.setAccessible(true);
            fase.set(servidor, JUGANDO);
            CapuccinoAssassino cap = (CapuccinoAssassino) campo(servidor, "cappuccino");
            BallerinaCapuccina bal = (BallerinaCapuccina) campo(servidor, "ballerina");
            ElementosNivel objetos = (ElementosNivel) campo(servidor, "elementos");
            ControladorPartida partida = (ControladorPartida) campo(servidor, "partida");
            ControladorEntradaJugador entrada = (ControladorEntradaJugador) campo(servidor, "entrada");
            Rectangle palanca = objetos.getPalancaCappuccino().getZona();
            cap.setPosicionRemota(palanca.x, palanca.y, true, true);
            String interaccion = new Controles(false, false, 0, 0, 1).mensaje(capSesion, 1, 1);
            procesar(servidor, uno, interaccion);
            partida.actualizar(0);
            comprobar(objetos.getPalancaCappuccino().estaActivada(), "Interaccion por protocolo");
            procesar(servidor, uno, interaccion);
            procesar(servidor, uno, new Controles(false, false, 0, 0, 1).mensaje(capSesion, 1, 2));
            partida.actualizar(0);
            comprobar(objetos.getPalancaCappuccino().estaActivada(), "Duplicado no vuelve a alternar palanca");
            procesar(servidor, uno, new Controles(true, false, 0, 0, 1).mensaje(capSesion, 1, 4));
            procesar(servidor, uno, new Controles(false, true, 0, 0, 1).mensaje(capSesion, 1, 3));
            comprobar(entrada.isIzquierda() && !entrada.isDerecha(), "Control atrasado descartado");
            procesar(servidor, uno, "CONTROLES|" + capSesion + "|1|5|NaN|0|0|0|1");
            comprobar(entrada.isIzquierda(), "Control mal formado no cambia estado");
            bal.setPosicionRemota(500, 300, true, true);
            procesar(servidor, dos, new Controles(false, false, 0, 1, 0).mensaje(balSesion, 1, 1));
            partida.actualizar(0);
            BloqueEspuma espuma = bal.getEspuma();
            procesar(servidor, dos, new Controles(false, false, 0, 1, 0).mensaje(balSesion, 1, 2));
            partida.actualizar(0);
            comprobar(espuma != null && bal.getEspuma() == espuma, "Habilidad repetida no recrea espuma");
            cap.setY(-100); partida.actualizar(0); fase.set(servidor, DERROTA);
            procesar(servidor, uno, REINICIO + "|" + capSesion + "|0|1");
            comprobar(partida.estaDerrotada(), "Reinicio de ronda vieja descartado");
            procesar(servidor, uno, REINICIO + "|" + capSesion + "|1|1");
            comprobar(!partida.estaDerrotada() && fase.get(servidor).equals(CUENTA), "Reinicio coordinado con cuenta regresiva");
            procesar(servidor, uno, new Controles(true, true, 0, 0, 2).mensaje(capSesion, 1, 100));
            comprobar(!entrada.isIzquierda() && !entrada.isDerecha(), "Control de ronda anterior descartado");
            ((Nivel) campo(servidor, "nivel")).dispose();
        }
    }
    private static void probarReglas() {
        Nivel nivel = new Nivel(MAPA, true);
        ElementosNivel objetos = new ElementosNivel(nivel);
        ControladorEntradaJugador entrada = new ControladorEntradaJugador();
        CapuccinoAssassino cap = new CapuccinoAssassino(objetos.getSpawnX(), objetos.getSpawnY());
        BallerinaCapuccina bal = new BallerinaCapuccina(objetos.getSpawnBallerinaX(), objetos.getSpawnBallerinaY());
        ControladorPartida partida = new ControladorPartida(nivel, objetos, entrada, cap, bal);
        ObjetoCortable liana = null;
        for (ObjetoCortable objeto : objetos.getCortables()) if (objeto.getTipo().equals("liana")) liana = objeto;
        comprobar(liana != null, "Nivel con liana");
        Rectangle zona = new Rectangle(liana.getZona());
        cap.setPosicionRemota(zona.x - cap.getAncho() - 10, 300, true, true);
        entrada.aplicarControlesRed(0, false, false, false, true, false);
        partida.actualizar(0);
        comprobar(liana.estaCortado() && !liana.bloquea(), "Katana corta y libera paso");
        comprobar(zona.equals(liana.getZona()), "Corte no cambia hitbox");
        partida.reiniciar();
        comprobar(liana.bloquea(), "Reinicio restaura liana");
        bal.setPosicionRemota(500, 300, true, true);
        comprobar(bal.crearEspuma(), "Crear espuma");
        BloqueEspuma espuma = bal.getEspuma();
        comprobar(!bal.crearEspuma(), "Cooldown");
        bal.actualizarHabilidad(2.5f);
        comprobar(bal.crearEspuma() && bal.getEspuma() != espuma && bal.getEspuma().getIndiceFrame() == 0, "Nueva espuma y animacion");
        bal.actualizarHabilidad(4);
        comprobar(bal.getEspuma() == null, "Espuma vence a cuatro segundos");
        cap.setPosicionRemota(300, 300, true, true);
        bal.setPosicionRemota(500, 300, true, true);
        Rectangle palCap = objetos.getPalancaCappuccino().getZona(), palBal = objetos.getPalancaBallerina().getZona();
        cap.setPosicionRemota(palBal.x, palBal.y, true, true);
        entrada.aplicarControlesRed(0, false, false, false, false, true);
        partida.actualizar(0);
        comprobar(!objetos.getPalancaBallerina().estaActivada(), "Cappuccino no activa palanca ajena");
        cap.setPosicionRemota(palCap.x, palCap.y, true, true);
        entrada.aplicarControlesRed(0, false, false, false, false, true);
        partida.actualizar(0);
        comprobar(objetos.getPalancaCappuccino().estaActivada() && !objetos.getPuerta().estaAbierta(), "Una palanca no abre puerta");
        bal.setPosicionRemota(palBal.x, palBal.y, true, true);
        entrada.aplicarControlesRed(1, false, false, false, false, true);
        partida.actualizar(0);
        comprobar(objetos.getPuerta().estaAbierta(), "Ambas palancas abren puerta");
        Rectangle puerta = objetos.getPuerta().getZona();
        cap.setPosicionRemota(puerta.x, puerta.y, true, true);
        partida.actualizar(0);
        comprobar(!partida.hayVictoria(), "No gana un jugador solo");
        bal.setPosicionRemota(puerta.x, puerta.y, true, true);
        partida.actualizar(0);
        comprobar(partida.hayVictoria(), "Victoria cooperativa");
        partida.reiniciar();
        cap.setY(-100);
        partida.actualizar(0);
        comprobar(partida.estaDerrotada(), "Muerte autoritativa");
        partida.reiniciar();
        comprobar(!partida.estaDerrotada() && !partida.hayVictoria() && !objetos.getPuerta().estaAbierta(), "Reinicio completo");
        nivel.dispose();
    }

    private static void probarUDP() throws Exception {
        Servidor servidor = new Servidor();
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Thread hilo = new Thread(() -> { try { servidor.ejecutar(); } catch (Throwable e) { fallo.set(e); } }, "Prueba-servidor");
        hilo.start();
        Cliente cap = new Cliente(), bal = new Cliente(), tercero = new Cliente();
        try {
            cap.conectar("127.0.0.1");
            esperar(() -> cap.getMiRol() == 0, 3000, "Primer cliente Cappuccino");
            esperar(() -> cap.getEstadoPartida() != null && cap.getEstadoPartida().fase().equals(ESPERA), 2000, "Sala espera");
            bal.conectar("127.0.0.1");
            esperar(() -> bal.getMiRol() == 1, 3000, "Segundo cliente Ballerina");
            esperar(() -> cap.getEstadoPartida().fase().equals(CUENTA), 2000, "Cuenta regresiva");
            comprobar(cap.getEstadoPartida().cuenta() >= 4, "Cuenta de cinco segundos");
            tercero.conectar("127.0.0.1");
            esperar(tercero::termino, 2000, "Tercer cliente rechazado");
            comprobar(tercero.getEstadoConexion().contains("dos jugadores"), "Mensaje servidor lleno");
            esperar(() -> cap.getEstadoPartida().fase().equals(JUGANDO), 7000, "Inicio");
            esperar(() -> bal.getEstadoPartida() != null && bal.getEstadoPartida().fase().equals(JUGANDO), 2000, "Inicio segundo cliente");
            comprobar(cap.getEstadoPartida().ronda() == bal.getEstadoPartida().ronda(), "Misma ronda");
            float x = cap.getEstadoPartida().cappuccino().x();
            cap.enviarControles(true, false, false, false, false);
            esperar(() -> cap.getEstadoPartida().cappuccino().x() < x - 20, 1500, "Movimiento autoritativo");
            cap.enviarControles(false, false, false, false, false);
            esperar(() -> bal.getEstadoPartida().cappuccino().x() < x - 20, 1500, "Otro cliente observa movimiento");
            bal.enviarControles(false, false, false, true, false);
            esperar(() -> cap.getEstadoPartida().espumaId() > 0, 1500, "Habilidad de Ballerina replicada");
            comprobar(cap.getEstadoPartida().espumaVida() <= 4f, "Vida de espuma autoritativa");
            EstadoPartida foto = cap.getEstadoPartida();
            comprobar(EstadoPartida.leer(partes(foto.mensaje("prueba"))).equals(foto), "Protocolo ida y vuelta");
            for (String basura : new String[]{"ESTADO", "ESTADO|x|NaN", "CONTROLES|x", "POS|NaN|0|true|true"}) {
                try (DatagramSocket atacante = new DatagramSocket()) {
                    byte[] datos = bytes(basura);
                    atacante.send(new DatagramPacket(datos, datos.length, InetAddress.getLoopbackAddress(), PUERTO));
                }
            }
            long secuencia = foto.secuencia();
            esperar(() -> cap.getEstadoPartida().secuencia() > secuencia, 1000, "Paquetes incorrectos no detienen servidor");
            bal.cerrar();
            esperar(cap::termino, 2000, "Salida avisada al otro jugador");
            comprobar(cap.getEstadoConexion().contains("jugador"), "Mensaje de desconexion");
            cap.conectar("127.0.0.1"); bal.conectar("127.0.0.1");
            esperar(() -> cap.getMiRol() >= 0 && bal.getMiRol() >= 0, 3000, "Reconexion libera los cupos");
            servidor.close();
            esperar(() -> cap.termino() && bal.termino(), 6500, "Timeout al cerrar servidor");
            comprobar(fallo.get() == null, "Servidor sin excepciones");
        } finally {
            cap.close(); bal.close(); tercero.close(); servidor.close(); hilo.join(2000);
        }
        comprobar(!hilo.isAlive(), "Hilo servidor cerrado");
        try (DatagramSocket libre = new DatagramSocket(PUERTO)) { comprobar(libre.isBound(), "Puerto liberado"); }
    }
}
