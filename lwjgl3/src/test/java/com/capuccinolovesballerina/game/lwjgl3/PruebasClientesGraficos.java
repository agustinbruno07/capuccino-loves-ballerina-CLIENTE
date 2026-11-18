package com.capuccinolovesballerina.game.lwjgl3;

import com.badlogic.gdx.Game;
import com.capuccinolovesballerina.game.CapuccinoLovesBallerinaGame;
import com.capuccinolovesballerina.game.CapuccinoServidorGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import java.net.DatagramSocket;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.capuccinolovesballerina.game.Utilidades.Recursos;
import com.capuccinolovesballerina.game.entidades.*;
import com.capuccinolovesballerina.game.pantallas.*;
import com.capuccinolovesballerina.game.red.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Solo pruebas: inicia el servidor real y dos procesos de juego separados. No hay sockets simulados. */
public class PruebasClientesGraficos {
    private static Object campo(Object objeto, String nombre) throws Exception {
        Field campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        return campo.get(objeto);
    }
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 3) { ejecutarCliente(Path.of(args[0]), Integer.parseInt(args[1]), args[2]); return; }
        Path base = Path.of(args[0]); Files.createDirectories(base);
        Path salida = Files.createTempDirectory(base, "ejecucion-");
        List<Process> procesos = new ArrayList<>();
        try {
            Process sinServidor = iniciar(salida, salida.resolve("sin-servidor.log"), PruebasClientesGraficos.class.getName(), salida.toString(), "-2", "");
            procesos.add(sinServidor);
            comprobar(sinServidor.waitFor(18000, TimeUnit.MILLISECONDS) && sinServidor.exitValue() == 0, "Cliente sin servidor finaliza correctamente");
            comprobar(Files.exists(salida.resolve("sin-servidor-ok")), "Busqueda sin servidor, puerto libre y reintento");
            procesos.clear();
            // Carpeta vacia: comprueba que el servidor puede leer mapas del classpath.
            Path carpetaServidor = Files.createDirectory(salida.resolve("servidor-independiente"));
            Process servidor = iniciar(carpetaServidor, salida.resolve("servidor.log"), PruebasClientesGraficos.class.getName(), salida.toString(), "-1", "");
            procesos.add(servidor);
            esperarArchivo(salida.resolve("anfitrion"), procesos, 35000);
            procesos.add(iniciar(salida, salida.resolve("cliente0.log"), PruebasClientesGraficos.class.getName(), salida.toString(), "0", ""));
            esperarArchivo(salida.resolve("asignado0"), procesos, 15000);
            procesos.add(iniciar(salida, salida.resolve("cliente1.log"), PruebasClientesGraficos.class.getName(), salida.toString(), "1", ""));
            esperarArchivo(salida.resolve("listo0"), procesos, 25000);
            esperarArchivo(salida.resolve("listo1"), procesos, 25000);
            Properties cap = leer(salida.resolve("listo0")), bal = leer(salida.resolve("listo1"));
            comprobar(cap.getProperty("capX").equals(bal.getProperty("capX")), "Posicion Cappuccino coincide en ambas JVM");
            comprobar(cap.getProperty("balX").equals(bal.getProperty("balX")), "Posicion Ballerina coincide en ambas JVM");
            comprobar(cap.getProperty("espuma").equals(bal.getProperty("espuma")), "Misma espuma en ambos clientes");
            Files.writeString(salida.resolve("cerrarServidor"), "cerrar");
            esperarArchivo(salida.resolve("desconexion0"), procesos, 8000);
            esperarArchivo(salida.resolve("desconexion1"), procesos, 8000);
            comprobar(procesos.get(1).waitFor(3000, TimeUnit.MILLISECONDS) && procesos.get(1).exitValue() == 0, "Primer cliente muestra desconexion");
            comprobar(procesos.get(2).waitFor(3000, TimeUnit.MILLISECONDS) && procesos.get(2).exitValue() == 0, "Segundo cliente muestra desconexion");
            comprobar(servidor.waitFor(5000, TimeUnit.MILLISECONDS) && servidor.exitValue() == 0, "Anfitrion cierra correctamente");
            try (DatagramSocket libre = new DatagramSocket(ProtocolosRed.PUERTO)) {
                comprobar(libre.isBound(), "Cerrar anfitrion libera el puerto");
            }
            System.out.println("DOS CLIENTES GRAFICOS OK: tres JVM independientes, aplicaciones separadas, cliente sin servidor no crea anfitrion, anfitrion grafico sin cupos, conexion por broadcast, roles, nivel, posiciones, habilidades, aislamiento de controles y desconexion.");
            System.out.println("Informes y capturas: " + salida);
        } finally {
            for (Process proceso : procesos) {
                proceso.destroy();
                if (!proceso.waitFor(3000, TimeUnit.MILLISECONDS)) proceso.destroyForcibly();
            }
        }
    }
    private static Process iniciar(Path carpeta, Path log, String clase, String... argumentos) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> orden = new ArrayList<>(List.of(java, "--enable-native-access=ALL-UNNAMED", "-cp", System.getProperty("java.class.path"), clase));
        orden.addAll(List.of(argumentos));
        return new ProcessBuilder(orden).directory(carpeta.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }
    private static void esperarArchivo(Path archivo, List<Process> procesos, long limiteMs) throws Exception {
        long fin = System.nanoTime() + limiteMs * 1_000_000L;
        while (!Files.exists(archivo) && System.nanoTime() < fin) {
            for (Process proceso : procesos) {
                if (!proceso.isAlive() && proceso.exitValue() != 0)
                    throw new AssertionError("Un proceso de prueba fallo. Revisar logs en " + archivo.getParent());
            }
            Thread.sleep(25);
        }
        comprobar(Files.exists(archivo), "No llego " + archivo + "; revisar logs");
    }
    private static Properties leer(Path archivo) throws Exception {
        Properties valores = new Properties();
        try (var entrada = Files.newInputStream(archivo)) { valores.load(entrada); }
        return valores;
    }
    private static void ejecutarCliente(Path salida, int rol, String ip) {
        Throwable[] fallo = new Throwable[1];
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false); config.disableAudio(true); config.setWindowedMode(1280, 720);
        config.setTitle("Prueba cliente " + rol);
        Game aplicacion = rol == -1 ? new CapuccinoServidorGame() : new CapuccinoLovesBallerinaGame();
        new Lwjgl3Application(new Game() {
            @Override public com.badlogic.gdx.Screen getScreen() { return aplicacion.getScreen(); }
            @Override public void resize(int ancho, int alto) { aplicacion.resize(ancho, alto); }
            Cliente cliente;
            PantallaMenu menu;
            long inicio = System.nanoTime(), inicioJuego;
            float capInicial, balInicial, capQuieto, balQuieto;
            boolean menuCapturado, asignado, moviendo, soltado, habilidad, otrasTeclas, otrasSoltadas, terminado;
            public void create() {
                try {
                    aplicacion.create();
                    var pantallaMenu = getScreen();
                    if (pantallaMenu instanceof PantallaMenu clienteMenu) menu = clienteMenu;

                    TextButton boton = (TextButton) campo(pantallaMenu, "botonJugar");
                    var escena = (com.badlogic.gdx.scenes.scene2d.Stage) campo(pantallaMenu, "stage");
                    var tablaMenu = (com.badlogic.gdx.scenes.scene2d.ui.Table) escena.getActors().peek();
                    int botonesJugar = 0;
                    for (var actor : tablaMenu.getChildren()) {
                        comprobar(!(actor instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField), "Menu sin IP manual");
                        if (actor instanceof TextButton b && b.getText().toString().startsWith("JUGAR")) botonesJugar++;
                    }
                    comprobar(botonesJugar == 1, "Exactamente un boton JUGAR");
                    comprobar(boton.getText().toString().equals("JUGAR"), "Boton unico JUGAR");
                    for (var listener : boton.getListeners()) if (listener instanceof ClickListener click)
                        click.clicked(new InputEvent(), 0, 0);
                    if (menu != null) cliente = menu.getClienteRed();
                } catch (Throwable error) { fallar(error); }
            }
            public void render() {
                try {
                    aplicacion.render();
                    comprobar(System.nanoTime() - inicio < 65_000_000_000L, "Timeout del cliente " + rol);
                    if (rol != -1 && !menuCapturado && getScreen() instanceof PantallaMenu) {
                        Pixmap captura = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                        PixmapIO.writePNG(Gdx.files.absolute(salida.resolve("menu.png").toString()), captura, -1, true);
                        captura.dispose();
                        menuCapturado = true;
                    }
                    if (rol == -2) {
                        if (cliente.termino()) {
                            comprobar(getScreen() instanceof PantallaMenu, "Cliente conserva su menu");
                            var etiqueta = (com.badlogic.gdx.scenes.scene2d.ui.Label) campo(menu, "estadoRed");
                            comprobar(etiqueta.getText().toString().equals("NO SE ENCONTRÓ UN SERVIDOR ACTIVO"), "Error exacto sin servidor");
                            TextButton jugar = (TextButton) campo(menu, "botonJugar");
                            comprobar(!jugar.isDisabled(), "Permite reintentar");
                            try (DatagramSocket libre = new DatagramSocket(ProtocolosRed.PUERTO)) {
                                comprobar(libre.isBound(), "Cliente no inicia servidor");
                            }
                            Pixmap captura = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                            PixmapIO.writePNG(Gdx.files.absolute(salida.resolve("sin-servidor.png").toString()), captura, -1, true);
                            captura.dispose();
                            Cliente anterior = cliente;
                            for (var listener : jugar.getListeners()) if (listener instanceof ClickListener click)
                                click.clicked(new InputEvent(), 0, 0);
                            comprobar(menu.getClienteRed() != anterior, "Reintento crea un nuevo cliente");
                            Files.writeString(salida.resolve("sin-servidor-ok"), "ok");
                            Gdx.app.exit();
                        }
                        return;
                    }
                    if (rol == -1) {
                        if (getScreen() instanceof PantallaServidor pantalla) {
                            ServicioServidor lan = (ServicioServidor) campo(pantalla, "inicio");
                            var resumen = lan.getServidor().getResumen();
                            if (!asignado) {
                                comprobar(!resumen.cappuccino() && !resumen.ballerina(), "Anfitrion no ocupa cupos");
                                Files.writeString(salida.resolve("anfitrion"), "listo");
                                asignado = true;
                            }
                            if (resumen.fase().equals(ProtocolosRed.JUGANDO) && !terminado) {
                                comprobar(resumen.cappuccino() && resumen.ballerina(), "Ambos clientes en pantalla anfitriona");
                                Pixmap captura = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                                PixmapIO.writePNG(Gdx.files.absolute(salida.resolve("anfitrion.png").toString()), captura, -1, true);
                                captura.dispose();
                                terminado = true;
                            }
                            if (Files.exists(salida.resolve("cerrarServidor"))) {
                                // Mismo listener que utiliza la interfaz; comprueba el retorno al menu.
                                var stage = (com.badlogic.gdx.scenes.scene2d.Stage) campo(pantalla, "stage");
                                var tabla = (com.badlogic.gdx.scenes.scene2d.ui.Table) stage.getActors().first();
                                for (var actor : tabla.getChildren()) if (actor instanceof TextButton detener)
                                    for (var listener : detener.getListeners()) if (listener instanceof ClickListener click)
                                        click.clicked(new InputEvent(), 0, 0);
                                comprobar(getScreen() instanceof PantallaMenuServidor, "Retorno al menu propio del servidor");
                                Gdx.app.exit();
                            }
                        }
                        return;
                    }
                    if (!asignado && cliente.getMiRol() >= 0) {
                        comprobar(cliente.getMiRol() == rol, "Rol asignado " + rol);
                        Files.writeString(salida.resolve("asignado" + rol), cliente.getIpServer());
                        asignado = true;
                    }
                    if (!(getScreen() instanceof PantallaJuego pantalla)) return;
                    if (cliente.termino()) {
                        comprobar(terminado, "Desconexion antes de completar prueba: " + cliente.getEstadoConexion());
                        comprobar(((TextButton) campo(pantalla, "volverRed")).isVisible(), "Boton de retorno");
                        Pixmap captura = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                        PixmapIO.writePNG(Gdx.files.absolute(salida.resolve("desconexion" + rol + ".png").toString()), captura, -1, true);
                        captura.dispose();
                        Files.writeString(salida.resolve("desconexion" + rol), cliente.getEstadoConexion());
                        Gdx.app.exit(); return;
                    }
                    CapuccinoAssassino cap = (CapuccinoAssassino) campo(pantalla, "cappuccino");
                    BallerinaCapuccina bal = (BallerinaCapuccina) campo(pantalla, "ballerina");
                    if (inicioJuego == 0) {
                        inicioJuego = System.nanoTime(); capInicial = cap.getX(); balInicial = bal.getX();
                    }
                    float segundos = (System.nanoTime() - inicioJuego) / 1e9f;
                    if (!moviendo && segundos > .1f) {
                        Gdx.input.getInputProcessor().keyDown(rol == 0 ? Input.Keys.A : Input.Keys.RIGHT); moviendo = true;
                    }
                    if (!soltado && segundos > .5f) {
                        Gdx.input.getInputProcessor().keyUp(rol == 0 ? Input.Keys.A : Input.Keys.RIGHT); soltado = true;
                    }
                    if (!habilidad && segundos > 1f) {
                        Gdx.input.getInputProcessor().keyDown(rol == 0 ? Input.Keys.F : Input.Keys.SHIFT_RIGHT); habilidad = true;
                    }
                    if (!otrasTeclas && segundos > 2f) {
                        capQuieto = cap.getX(); balQuieto = bal.getX();
                        Gdx.input.getInputProcessor().keyDown(rol == 0 ? Input.Keys.LEFT : Input.Keys.A); otrasTeclas = true;
                    }
                    if (!otrasSoltadas && segundos > 2.5f) {
                        Gdx.input.getInputProcessor().keyUp(rol == 0 ? Input.Keys.LEFT : Input.Keys.A);
                        comprobar(cap.getX() == capQuieto && bal.getX() == balQuieto, "No controla al otro personaje");
                        otrasSoltadas = true;
                    }
                    if (!terminado && segundos > 3f) {
                        comprobar(cap.getX() < capInicial - 20 && bal.getX() > balInicial + 20, "Ambos movimientos visibles");
                        var foto = cliente.getEstadoPartida();
                        comprobar(foto.cappuccino().habilidad() > 0 && foto.ballerina().habilidad() > 0, "Dos habilidades recibidas");
                        comprobar(bal.getEspuma() != null && foto.espumaId() > 0, "Espuma recibida y dibujada");
                        Pixmap imagen = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                        PixmapIO.writePNG(Gdx.files.absolute(salida.resolve("cliente" + rol + ".png").toString()), imagen, -1, true);
                        imagen.dispose();
                        String datos = "capX=" + cap.getX() + "\nbalX=" + bal.getX() + "\nespuma=" + foto.espumaId() + "\n";
                        Files.writeString(salida.resolve("listo" + rol), datos);
                        terminado = true;
                    }
                    if (rol == 0 && Files.exists(salida.resolve("cerrar0"))) Gdx.app.exit();
                } catch (Throwable error) { fallar(error); }
            }
            private void fallar(Throwable error) { fallo[0] = error; Gdx.app.exit(); }
            public void dispose() {
                aplicacion.dispose();
                if (cliente != null) cliente.close();

            }
        }, config);
        if (fallo[0] != null) throw new AssertionError("Cliente grafico " + rol, fallo[0]);
    }
}
