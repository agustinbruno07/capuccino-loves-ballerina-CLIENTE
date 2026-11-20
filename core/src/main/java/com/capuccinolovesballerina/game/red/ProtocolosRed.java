package com.capuccinolovesballerina.game.red;

import com.capuccinolovesballerina.game.graficos.Estado;
import java.nio.charset.StandardCharsets;

public final class ProtocolosRed {
    private ProtocolosRed() {}
    public static final int PUERTO = 9992, MAX_PAQUETE = 1400;
    public static final long TIMEOUT_MS = 5000, BUSQUEDA_MS = 10000;
    public static final String CONEXION = "CONEXION", OK = "OK", ASIGNADO = "ASIGNADO";
    public static final String REGISTRO = "REGISTRO", LISTO = "LISTO", PONG = "PONG";
    public static final String LLENO = "LLENO", CONTROLES = "CONTROLES", ESTADO = "ESTADO";
    public static final String PING = "PING", SALIR = "SALIR", DESCONECTADO = "DESCONECTADO", REINICIO = "REINICIO";
    public static final String ESPERA = "ESPERA", CUENTA = "CUENTA", JUGANDO = "JUGANDO", DERROTA = "DERROTA", VICTORIA = "VICTORIA";
    public static final String MAPA = "mapas/NIVEL 0.tmx";
    public static byte[] bytes(String mensaje) { return mensaje.getBytes(StandardCharsets.UTF_8); }
    public static String[] partes(String mensaje) { return mensaje.split("\\|", -1); }
    public static int bit(boolean valor) { return valor ? 1 : 0; }
    public static boolean booleano(String valor) {
        if (!valor.equals("0") && !valor.equals("1")) throw new IllegalArgumentException("Booleano invalido");
        return valor.equals("1");
    }
    public static long natural(String valor) {
        long numero = Long.parseLong(valor);
        if (numero < 0) throw new IllegalArgumentException("Numero negativo");
        return numero;
    }
    public static float numero(String valor) {
        float numero = Float.parseFloat(valor);
        if (!Float.isFinite(numero) || Math.abs(numero) > 1_000_000f) throw new IllegalArgumentException("Numero invalido");
        return numero;
    }

    public record Controles(boolean izquierda, boolean derecha, long salto, long habilidad, long interactuar) {
        public static final Controles VACIOS = new Controles(false, false, 0, 0, 0);
        public String mensaje(String sesion, long ronda, long secuencia) {
            return CONTROLES + "|" + sesion + "|" + ronda + "|" + secuencia + "|" + bit(izquierda)
                + "|" + bit(derecha) + "|" + salto + "|" + habilidad + "|" + interactuar;
        }
        public static Controles leer(String[] p) {
            if (p.length != 9) throw new IllegalArgumentException("Controles incompletos");
            return new Controles(booleano(p[4]), booleano(p[5]), natural(p[6]), natural(p[7]), natural(p[8]));
        }
    }
    public record Jugador(float x, float y, boolean mirando, boolean suelo, Estado animacion, long habilidad) {
        public String texto() { return x + "," + y + "," + bit(mirando) + "," + bit(suelo) + "," + animacion + "," + habilidad; }
        public static Jugador leer(String texto) {
            String[] p = texto.split(",", -1);
            if (p.length != 6) throw new IllegalArgumentException("Jugador incompleto");
            return new Jugador(numero(p[0]), numero(p[1]), booleano(p[2]), booleano(p[3]), Estado.valueOf(p[4]), natural(p[5]));
        }
    }
    public record EstadoPartida(long secuencia, long ronda, String fase, int cuenta, float tiempo,
        Jugador cappuccino, Jugador ballerina, boolean palancaCap, boolean palancaBal, boolean puerta,
        String cortados, long espumaId, float espumaX, float espumaY, float espumaVida) {
        public String mensaje(String sesion) {
            return ESTADO + "|" + sesion + "|" + secuencia + "|" + ronda + "|" + fase + "|" + cuenta + "|" + tiempo
                + "|" + cappuccino.texto() + "|" + ballerina.texto() + "|" + bit(palancaCap) + "|" + bit(palancaBal)
                + "|" + bit(puerta) + "|" + cortados + "|" + espumaId + "|" + espumaX + "|" + espumaY + "|" + espumaVida;
        }
        public static EstadoPartida leer(String[] p) {
            if (p.length != 17) throw new IllegalArgumentException("Estado incompleto");
            if (!java.util.Set.of(ESPERA, CUENTA, JUGANDO, DERROTA, VICTORIA).contains(p[4])) throw new IllegalArgumentException("Fase invalida");
            int cuenta = Integer.parseInt(p[5]);
            float tiempo = numero(p[6]), vida = numero(p[16]);
            if (cuenta < 0 || cuenta > 5 || tiempo < 0 || vida < 0 || vida > 4f || !p[12].matches("[01]*"))
                throw new IllegalArgumentException("Estado invalido");
            return new EstadoPartida(natural(p[2]), natural(p[3]), p[4], cuenta, tiempo,
                Jugador.leer(p[7]), Jugador.leer(p[8]), booleano(p[9]), booleano(p[10]), booleano(p[11]),
                p[12], natural(p[13]), numero(p[14]), numero(p[15]), vida);
        }
    }
}
