package com.capuccinolovesballerina.game.red;

import com.capuccinolovesballerina.game.Utilidades.*;
import com.capuccinolovesballerina.game.entidades.*;
import com.capuccinolovesballerina.game.mapa.Nivel;
import com.capuccinolovesballerina.game.objetos.*;
import com.capuccinolovesballerina.game.red.hilos.HiloServidor;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import static com.capuccinolovesballerina.game.red.ProtocolosRed.*;

/** Autoridad sobre la partida: recibe controles y ejecuta las reglas originales. */
public class Servidor implements AutoCloseable {
    private static class Conexion {
        final InetSocketAddress destino;
        final String sesion = UUID.randomUUID().toString();
        long ultimoMensaje = System.nanoTime(), ultimoControl, ultimaSecuencia = -1, reinicio = -1;
        Controles controles = Controles.VACIOS;
        boolean listo;
        Conexion(InetSocketAddress destino) { this.destino = destino; }
    }
    private final HiloServidor red;
    private final Conexion[] clientes = new Conexion[2];
    private final Nivel nivel;
    private final ElementosNivel elementos;
    private final ControladorEntradaJugador entrada = new ControladorEntradaJugador();
    private final CapuccinoAssassino cappuccino;
    private final BallerinaCapuccina ballerina;
    private final ControladorPartida partida;
    public record Resumen(boolean cappuccino, boolean ballerina, String fase) {}
    private volatile Resumen resumen = new Resumen(false, false, ESPERA);
    public Resumen getResumen() { return resumen; }
    private volatile boolean cerrado;
    private String fase = ESPERA;
    private long finCuenta, ronda, secuencia, espumaId;
    private BloqueEspuma ultimaEspuma;
    private float tiempo;

    public Servidor() throws SocketException {
        nivel = new Nivel(MAPA, true);
        elementos = new ElementosNivel(nivel);
        cappuccino = new CapuccinoAssassino(elementos.getSpawnX(), elementos.getSpawnY());
        ballerina = new BallerinaCapuccina(elementos.getSpawnBallerinaX(), elementos.getSpawnBallerinaY());
        partida = new ControladorPartida(nivel, elementos, entrada, cappuccino, ballerina);
        try { red = new HiloServidor(); }
        catch (SocketException error) { nivel.dispose(); throw error; }
    }
    public static void main(String[] args) throws Exception {
        try {
            Servidor servidor = new Servidor();
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::close));
            try (servidor) { servidor.ejecutar(); }
        } catch (BindException error) {
            System.err.println("[RED] No se pudo abrir UDP " + PUERTO + ": ya hay otro servidor o prueba usando el puerto.");
            System.exit(1);
        }
    }
    public void ejecutar() {
        try {
            if (cerrado) return;
            red.start();
            System.out.println("[RED] Servidor UDP en 0.0.0.0:" + PUERTO + ". Esperando dos jugadores.");
            System.out.println("[RED] Usar la IP Ethernet de esta PC para conexion manual. Ctrl+C para cerrar.");
            mostrarDirecciones();
            long siguienteTick = System.nanoTime(), ultimoEstado = 0;
            int ultimaCuenta = -1;
            while (!cerrado) {
                HiloServidor.Recibido recibido;
                int leidos = 0;
                while (leidos++ < 512 && (recibido = red.siguiente()) != null) procesar(recibido);
                long ahora = System.nanoTime();
                for (Conexion cliente : clientes) {
                    if (cliente != null && ahora - cliente.ultimoMensaje > TIMEOUT_MS * 1_000_000L) {
                        desconectar("Un jugador perdio la conexion");
                        break;
                    }
                }
                if (fase.equals(CUENTA)) {
                    int cuenta = cuenta(ahora);
                    if (cuenta != ultimaCuenta) { System.out.println("[RED] Inicio en " + cuenta); ultimaCuenta = cuenta; }
                    if (ahora >= finCuenta) { fase = JUGANDO; System.out.println("[RED] INICIO de ronda " + ronda); }
                }
                if (ahora >= siguienteTick) {
                    if (fase.equals(JUGANDO)) {
                        for (int rol = 0; rol < 2; rol++) {
                            Conexion c = clientes[rol];
                            if (c != null && ahora - c.ultimoControl > 350_000_000L)
                                entrada.aplicarControlesRed(rol, false, false, false, false, false);
                        }
                        // Mismo Player y ControladorPartida, con un paso fijo de 60 Hz.
                        partida.actualizar(1f / 60f);
                        tiempo += 1f / 60f;
                        if (partida.estaDerrotada()) { fase = DERROTA; System.out.println("[PARTIDA] Derrota"); }
                        if (partida.hayVictoria()) { fase = VICTORIA; System.out.println("[PARTIDA] Victoria cooperativa"); }
                    }
                    siguienteTick += 1_000_000_000L / 60;
                    if (ahora - siguienteTick > 250_000_000L) siguienteTick = ahora;
                }
                if (ahora - ultimoEstado >= 50_000_000L) { enviarEstado(ahora); ultimoEstado = ahora; }
                resumen = new Resumen(clientes[0] != null, clientes[1] != null, fase);
                LockSupport.parkNanos(1_000_000L);
            }
        } finally { red.detener(); nivel.dispose(); }
    }
    private void procesar(HiloServidor.Recibido recibido) {
        String[] p = partes(recibido.mensaje());
        InetSocketAddress origen = recibido.origen();
        try {
            if (p.length == 1 && p[0].equals(CONEXION)) {
                // Descubrir no ocupa un cupo: el mismo equipo puede emitir por varias interfaces.
                red.enviar(origen, OK);
                System.out.println("[RED] CONEXION desde " + origen + ": OK enviado, esperando REGISTRO directo.");
                return;
            }
            if (p.length == 1 && p[0].equals(REGISTRO)) { conectar(origen); return; }
            int rol = buscar(origen);
            if (rol < 0 || p.length < 2 || !clientes[rol].sesion.equals(p[1])) return;
            Conexion c = clientes[rol];
            if (p[0].equals(LISTO) && p.length == 2) {
                if (!c.listo) System.out.println("[RED] " + origen + " confirmo su personaje (LISTO).");
                c.listo = true;
                if (fase.equals(ESPERA) && clientes[0] != null && clientes[1] != null
                    && clientes[0].listo && clientes[1].listo) comenzarCuenta();
            } else if (p[0].equals(CONTROLES)) {
                Controles controles = Controles.leer(p);
                long rondaRecibida = natural(p[2]), numero = natural(p[3]);
                if (numero <= c.ultimaSecuencia || rondaRecibida != ronda) return;
                if (controles.salto() < c.controles.salto() || controles.habilidad() < c.controles.habilidad()
                    || controles.interactuar() < c.controles.interactuar()) return;
                if (fase.equals(JUGANDO)) {
                    entrada.aplicarControlesRed(rol, controles.izquierda(), controles.derecha(),
                        controles.salto() > c.controles.salto(), controles.habilidad() > c.controles.habilidad(),
                        controles.interactuar() > c.controles.interactuar());
                }
                c.controles = controles;
                c.ultimaSecuencia = numero;
                c.ultimoControl = System.nanoTime();
            } else if (p[0].equals(REINICIO) && p.length == 4) {
                long rondaRecibida = natural(p[2]), numero = natural(p[3]);
                if (rondaRecibida != ronda || numero <= c.reinicio) return;
                c.reinicio = numero;
                if (fase.equals(DERROTA) || fase.equals(VICTORIA)) comenzarCuenta();
            } else if (p[0].equals(SALIR) && p.length == 2) {
                desconectar("El otro jugador salio de la partida"); return;
            } else if (p[0].equals(PING) && p.length == 2) {
                // Equivale a ESTAS_AHI / ESTOY_ACA en SpeedG2L.
                red.enviar(origen, PONG + "|" + c.sesion);
            } else return;
            c.ultimoMensaje = System.nanoTime();
        } catch (IllegalArgumentException error) {
            // Los paquetes incompletos o incorrectos se descartan, sin tocar el hilo grafico.
        }
    }
    private int buscar(InetSocketAddress origen) {
        for (int i = 0; i < 2; i++) if (clientes[i] != null && clientes[i].destino.equals(origen)) return i;
        return -1;
    }
    private void conectar(InetSocketAddress origen) {
        int rol = buscar(origen);
        if (rol < 0) {
            rol = clientes[0] == null ? 0 : clientes[1] == null ? 1 : -1;
            if (rol < 0) { red.enviar(origen, LLENO); return; }
            clientes[rol] = new Conexion(origen);
            System.out.println("[RED] " + origen + " registrado como " + (rol == 0 ? "Cappuccino" : "Ballerina"));
        }
        Conexion cliente = clientes[rol];
        cliente.ultimoMensaje = System.nanoTime();
        red.enviar(origen, ASIGNADO + "|" + cliente.sesion + "|" + rol);
        System.out.println("[RED] REGISTRO desde " + origen + ": ASIGNADO enviado.");
    }
    private void comenzarCuenta() {
        partida.reiniciar(); tiempo = 0; ronda++;
        fase = CUENTA;
        finCuenta = System.nanoTime() + 5_000_000_000L;
        System.out.println("[RED] Dos jugadores listos. Cuenta regresiva de cinco segundos, ronda " + ronda);
    }
    private int cuenta(long ahora) { return fase.equals(CUENTA) ? Math.max(0, (int) Math.ceil((finCuenta - ahora) / 1e9)) : 0; }
    private void enviarEstado(long ahora) {
        BloqueEspuma espuma = ballerina.getEspuma();
        if (espuma != null && espuma != ultimaEspuma) espumaId++;
        ultimaEspuma = espuma;
        StringBuilder cortados = new StringBuilder();
        for (ObjetoCortable objeto : elementos.getCortables()) cortados.append(bit(objeto.estaCortado()));
        EstadoPartida estado = new EstadoPartida(++secuencia, ronda, fase, cuenta(ahora), tiempo,
            jugador(cappuccino, 0), jugador(ballerina, 1), activada(elementos.getPalancaCappuccino()),
            activada(elementos.getPalancaBallerina()), elementos.getPuerta() != null && elementos.getPuerta().estaAbierta(),
            cortados.toString(), espuma == null ? 0 : espumaId,
            espuma == null ? 0 : espuma.getZona().x, espuma == null ? 0 : espuma.getZona().y,
            espuma == null ? 0 : espuma.getVidaRestante());
        for (Conexion c : clientes) if (c != null) red.enviar(c.destino, estado.mensaje(c.sesion));
    }
    private Jugador jugador(Player jugador, int rol) {
        return new Jugador(jugador.getX(), jugador.getY(), jugador.isMirandoDerecha(), jugador.isEnSuelo(),
            partida.getEstado(rol), clientes[rol] == null ? 0 : clientes[rol].controles.habilidad());
    }
    private boolean activada(Palanca palanca) { return palanca != null && palanca.estaActivada(); }
    private void mostrarDirecciones() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface interfaz = interfaces.nextElement();
                if (!interfaz.isUp() || interfaz.isLoopback()) continue;
                for (InterfaceAddress direccion : interfaz.getInterfaceAddresses())
                    if (direccion.getAddress() instanceof Inet4Address)
                        System.out.println("[RED] IP manual en " + interfaz.getDisplayName() + ": " + direccion.getAddress().getHostAddress());
            }
        } catch (SocketException error) { System.err.println("[RED] No se pudieron listar interfaces: " + error.getMessage()); }
    }
    private void desconectar(String motivo) {
        System.out.println("[RED] " + motivo + ". Se libera la sala.");
        for (Conexion c : clientes) if (c != null)
            for (int i = 0; i < 3; i++) red.enviar(c.destino, DESCONECTADO + "|" + c.sesion + "|" + motivo);
        clientes[0] = clientes[1] = null; fase = ESPERA; partida.reiniciar(); tiempo = 0;
    }
    @Override public void close() { cerrado = true; red.detener(); }
}
