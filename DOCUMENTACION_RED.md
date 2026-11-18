# Multijugador de Cappuccino Loves Ballerina

## Objetivo y arquitectura

La versión 1.7.0 tiene dos aplicaciones gráficas: CappuccinoServidor en una PC y CappuccinoLovesBallerina en las dos PC cliente.
La anfitriona simula en un hilo independiente y no ocupa ningún personaje.
El primer cliente recibe Cappuccino y el segundo Ballerina; un tercero recibe LLENO.
El menú ofrece solamente JUGAR, sin selección de modo ni campo de IP.

Una arquitectura **cliente-servidor** tiene una computadora que coordina y otras que le piden acciones.
En este juego el servidor decide las posiciones, colisiones, habilidades, palancas, muerte y victoria.
El cliente envía botones; no puede declarar una posición ni decidir que ganó.

```text
PC 2: Cappuccino  -- controles -->  PC 1: Servidor  <-- controles --  PC 3: Ballerina
                 <-- estado ----  UDP 9992         ---- estado -->
```

Se utiliza UDP con `DatagramSocket` y `DatagramPacket`, sin librerías de red externas.
UDP permite enviar mensajes pequeños y frecuentes sin establecer una conexión TCP.
No garantiza entrega, orden ni ausencia de duplicados: esas situaciones se contemplan en el protocolo.
No elegimos UDP porque nunca pierda paquetes ni porque siempre sea más rápido.

Un **socket** es el extremo del programa que envía y recibe mensajes de red.
Un **datagrama** es uno de esos mensajes, con una dirección y un puerto de destino.
Una **IP** identifica la computadora en la red; un **puerto** identifica el servicio dentro de ella.
El único puerto del servidor es **UDP 9992**. Los clientes usan puertos locales efímeros elegidos por el sistema operativo.

## Conexión, sala y comienzo

1. En el menú, **JUGAR** crea un `Cliente` y arranca su `HiloCliente`.
2. Sin IP escrita, ese mismo hilo envía `CONEXION` a `255.255.255.255:9992`, a los broadcasts de cada interfaz y a loopback para pruebas locales.
3. **Broadcast** significa enviar a los equipos de la red local sin conocer todavía la IP del servidor.
4. El servidor responde `OK`, sin ocupar todavía un cupo de jugador.
5. El cliente obtiene la IP con `paquete.getAddress()` y envía `REGISTRO` directamente a esa dirección. El servidor responde `ASIGNADO|sesion|rol`.
6. El servidor identifica cada conexión por **IP y puerto**. Dos clientes en una misma PC funcionan porque tienen puertos diferentes.
7. Cada conexión también recibe una sesión aleatoria, para no aceptar mensajes atrasados de otra sesión.
8. Cada cliente confirma su personaje con `LISTO|sesion`. Hasta que ambos confirman, el servidor manda `ESPERA`; después manda `CUENTA` durante cinco segundos.
9. Al terminar, cambia a `JUGANDO`. Ambos clientes entran al mismo nivel y muestran la misma ronda.

El inicio corresponde a una misma transición del servidor. La recepción y carga gráfica pueden diferir unos milisegundos;
no se afirma una simultaneidad física exacta entre las pantallas.

No existe un hilo separado de descubrimiento ni se usa el puerto 9993.
Si el colegio bloquea broadcast, escribir la IPv4 Ethernet del servidor en el campo del menú y pulsar **JUGAR**.
Para probar en una sola PC, escribir `127.0.0.1`.
También funciona con el campo vacío mediante el destino loopback incluido en la búsqueda automática.
Descubrir por varias interfaces no consume varios cupos: solo se registra la IP elegida por el cliente.
La búsqueda reintenta cada 500 ms, con un límite de diez segundos. El botón permite volver a intentarlo al terminar.
Para cancelar la búsqueda se puede pasar a otro modo o salir del menú; el cliente cierra su socket.

## Mensajes de texto

Se codifican en UTF-8. Los campos se separan con `|`. Los booleanos son `0` y `1`.
Un paquete tiene como máximo 1400 bytes. Este límite alcanza para el nivel actual;
si se agregan cientos de objetos habrá que revisar el tamaño de las fotos de estado.

| Mensaje | Dirección | Función |
|---|---|---|
| `CONEXION` | Cliente → servidor | Descubrimiento o conexión por IP |
| `OK` | Servidor → cliente | Respuesta inicial |
| `REGISTRO` | Cliente → servidor elegido | Registro directo por IP/puerto, sin duplicar cupos por interfaz |
| `ASIGNADO|sesion|rol` | Servidor → cliente | Rol 0 Cappuccino, rol 1 Ballerina |
| `LISTO|sesion` | Cliente → servidor | Confirmación del personaje; ambos deben confirmarlo antes de la cuenta |
| `LLENO` | Servidor → cliente | Ya hay dos conexiones |
| `CONTROLES|sesion|ronda|secuencia|izq|der|saltos|habilidades|interacciones` | Cliente → servidor | Botones del personaje asignado |
| `PING|sesion` | Cliente → servidor | Aviso periódico de que sigue conectado |
| `PONG|sesion` | Servidor → cliente | Respuesta a PING que confirma actividad |
| `ESTADO|...` | Servidor → clientes | Foto completa de la partida |
| `REINICIO|sesion|ronda|numero` | Cliente → servidor | Solicitud de reinicio después de derrota o victoria |
| `SALIR|sesion` | Cliente → servidor | Salida voluntaria |
| `DESCONECTADO|sesion|motivo` | Servidor → cliente | Cancelación de la sala o partida |

La foto tiene este formato:

```text
ESTADO|sesion|secuencia|ronda|fase|cuenta|tiempo|jugador0|jugador1|palanca0|palanca1|puerta|cortados|espumaId|espumaX|espumaY|espumaVida
```

Cada jugador contiene `x,y,mirandoDerecha,enSuelo,animacion,numeroHabilidad`.
`cortados` tiene un `0` o `1` por objeto cortable, en el orden de `ElementosNivel`.
`espumaId=0` significa que no hay espuma. Una espuma nueva tiene otro identificador.
Las fases son `ESPERA`, `CUENTA`, `JUGANDO`, `DERROTA` y `VICTORIA`.

### Ejemplo completo de intercambio

Los identificadores se abrevian aquí como S0 y S1 para facilitar la lectura;
en los paquetes reales son UUID, por ejemplo `11111111-1111-4111-8111-111111111111`.

```text
192.168.1.20:51000 -> 255.255.255.255:9992  CONEXION
192.168.1.10:9992  -> 192.168.1.20:51000  OK
192.168.1.20:51000 -> 192.168.1.10:9992   REGISTRO
192.168.1.10:9992  -> 192.168.1.20:51000  ASIGNADO|S0|0
192.168.1.20:51000 -> 192.168.1.10:9992   LISTO|S0
Servidor -> cliente 0: ESTADO|S0|1|0|ESPERA|0|0.0|880.0,224.0,1,0,IDLE,0|48.0,224.0,1,0,IDLE,0|0|0|0|0|0|0.0|0.0|0.0

192.168.1.30:52000 -> 255.255.255.255:9992  CONEXION
Servidor -> cliente 1: OK
Cliente 1 -> servidor: REGISTRO
Servidor -> cliente 1: ASIGNADO|S1|1
Cliente 1 -> servidor: LISTO|S1
Servidor -> ambos: ESTADO con ronda=1, fase=CUENTA y cuenta=5,4,3,2,1
Servidor -> ambos: ESTADO con ronda=1, fase=JUGANDO y cuenta=0

Cliente 0 -> servidor: CONTROLES|S0|1|1|1|0|0|0|0
Servidor calcula un paso de movimiento hacia la izquierda y sus colisiones.
Servidor -> ambos: ESTADO con la nueva posición de Cappuccino.

Cliente 1 -> servidor: CONTROLES|S1|1|1|0|0|0|1|0
Servidor llama crearEspuma(), comprueba cooldown y actualiza su duración.
Servidor -> ambos: ESTADO con espumaId=1 y espumaVida aproximada de 4.0.

Cliente 0 -> servidor: CONTROLES|S0|1|2|0|0|0|1|0
Servidor ejecuta la katana; solo corta si su alcance se superpone al cortable.
Servidor -> ambos: ESTADO con el bit de la liana cortada en 1.

Cliente 0 -> servidor: CONTROLES|S0|1|3|0|0|0|1|1
Cliente 1 -> servidor: CONTROLES|S1|1|2|0|0|0|1|1
El servidor valida cercanía a la palanca exclusiva de cada personaje.
Servidor -> ambos: ESTADO con palanca0=1, palanca1=1 y puerta=1.
Los dos llegan a la puerta: el servidor envía fase=VICTORIA.

Alternativa: si uno muere, el servidor envía fase=DERROTA.
Cliente 0 -> servidor: REINICIO|S0|1|1
Servidor restaura posiciones, liana, palancas, puerta y habilidades.
Servidor -> ambos: ESTADO con ronda=2, fase=CUENTA y cuenta=5.

Cliente 1 -> servidor: SALIR|S1
Servidor -> ambos: DESCONECTADO|sesionDeCadaDestinatario|El otro jugador salio de la partida
El servidor libera ambos cupos y vuelve a esperar conexiones nuevas.
```

## Movimiento y habilidades

El servidor simula a **60 pasos por segundo** y envía estados aproximadamente **20 veces por segundo**.
El cliente envía controles aproximadamente **30 veces por segundo**, aun cuando está quieto.
En red, `PantallaJuego` no ejecuta físicas locales ni decide interacciones: aplica las fotos recibidas.
No hay predicción ni interpolación adicional; es una implementación pequeña pensada para LAN.

`Player` sigue calculando gravedad, movimiento, salto y colisiones. No se reemplazaron sus físicas.
`ControladorPartida` sirve tanto para local como para servidor; en este último caso no necesita menús ni texturas.
El mapa del servidor se lee con el mismo lector TMX de LibGDX y el mismo `GestorColisiones`.
Solo se omite cargar imágenes, que el servidor no dibuja.

Controles de red: Cappuccino usa **A/D, W o Espacio, E, F**.
Ballerina usa **flechas, flecha arriba, Enter y Shift derecho**.
Las teclas del otro personaje no se transmiten.

- La katana usa el mismo rectángulo de alcance de 40 unidades que en local.
- El servidor comprueba la superposición con la liana antes de marcarla como cortada.
- Una liana cortada desaparece en los clientes y ya no se añade a los sólidos del servidor.
- Ballerina conserva espuma de cuatro segundos y cooldown de 2,5 segundos.
- Los clientes reciben el identificador y vida restante de la espuma: no reinician su animación con cada paquete.
- Cada personaje solo interactúa con su propia palanca y debe estar cerca.
- La puerta se abre al activar ambas palancas del nivel actual.
- La victoria requiere que los dos jugadores lleguen a la puerta abierta.
- Muerte y victoria se comprueban en el servidor; los clientes muestran el menú correspondiente.
- Reintentar solicita un reinicio al servidor, que reinicia a ambos y hace una cuenta regresiva nueva.

En red, Escape abre un menú **solo en ese cliente** y su personaje deja de enviar movimiento.
El servidor y el otro jugador siguen jugando. En modo local se conserva la pausa de toda la partida.

## Pérdidas, duplicados y desconexiones

Cada control y foto lleva una secuencia creciente. Los mensajes atrasados o repetidos se descartan.
Los saltos, habilidades e interacciones usan contadores acumulados: si se pierde el primer paquete,
el siguiente sigue llevando el contador aumentado. Si llega dos veces, el evento se aplica una sola vez.
Varias pulsaciones antes de un mismo paso de simulación se consolidan en una acción de ese paso.
La ronda evita que un paquete viejo afecte una partida reiniciada.
El reinicio se retransmite hasta recibir la ronda nueva, sin repetir el reinicio por duplicados.

Cada foto contiene todo el estado compartido. Perder un aviso de liana cortada, espuma o victoria no lo pierde para siempre:
la foto siguiente vuelve a incluirlo. No se promete recuperar un datagrama particular.

El cliente manda `PING` cada segundo y el servidor responde `PONG`, para comprobar actividad.
El servidor usa los controles válidos y los PING para renovar la actividad.
Si faltan controles nuevos durante 350 ms, deja de mover a ese personaje para evitar que una tecla quede pegada.
Si pasan cinco segundos sin comunicación válida, termina la conexión.
El cliente también espera como máximo cinco segundos sin fotos válidas del servidor.
Un PONG sin fotos no mantiene indefinidamente una pantalla congelada: se avisa que no llegan estados compatibles.

La salida voluntaria envía `SALIR` y cierra el socket. El aviso de cancelación se envía tres veces;
si se pierden todas las copias, igualmente funciona el timeout.
Al desconectarse cualquiera, se cancela esa sala y se liberan ambos cupos.
Para reconectar, volver al menú y elegir **JUGAR**. Se comienza una sala nueva;
no se intenta continuar a mitad de una partida desconectada.

Se comprueban origen, sesión, número de campos, booleanos, números finitos, fase y secuencia.
Un mensaje desconocido o incompleto se ignora. Los clientes no aceptan estados desde otra IP/puerto.
La sesión identifica la partida; no es un sistema de cuentas ni de cifrado para jugar por Internet.

## Clases y concurrencia

| Clase | Responsabilidad |
|---|---|
| `ProtocolosRed` | Constantes, formatos y lectura estricta; records inmutables de controles y estado |
| `Cliente` | Interfaz pequeña para el menú y la pantalla, sin esperas bloqueantes |
| `HiloCliente` | Un único socket: broadcast, recepción, controles, PING, reintentos y timeout |
| `Servidor` | Registro IP/puerto, personajes, cuenta regresiva, simulación, reinicio y desconexiones |
| `HiloServidor` | Recibe datagramas y los deja en una cola limitada; envía las respuestas del servidor |
| `ControladorEntradaJugador` | Teclado local o controles del personaje recibidos por el servidor |
| `ControladorPartida` | Reglas originales reutilizadas por el servidor |
| `CargadorMapaServidor` | Lectura del TMX sin texturas en el hilo del servidor |
| `Nivel`, `ElementosNivel`, `Player` | Mapa, objetos y físicas compartidos con el modo local |
| `PantallaMenu` | Botón JUGAR, búsqueda automática, sala e ingreso al juego |
| `ServicioServidor` | Ciclo de vida y hilo exclusivo de la aplicación servidor |
| `PantallaMenuServidor` | JUGAR inicia el servicio; SALIR cierra la aplicación servidor |
| `Lwjgl3ServidorLauncher`, `CapuccinoServidorGame` | Entrada y aplicación gráfica exclusiva del servidor |
| `PantallaServidor` | Resumen inmutable de conexiones y botón para detener la sesión |
| `PantallaJuego` | En local simula; en red envía su teclado y muestra el estado autorizado |
| `PruebasRed` | Pruebas reproducibles sin dependencias adicionales |

`HiloServidor` no toca jugadores: entrega mensajes por `ArrayBlockingQueue`.
El bucle de `Servidor` es el único que modifica su partida.
`HiloCliente` publica una foto inmutable mediante una referencia `volatile`.
El hilo gráfico la lee y recién ahí modifica posiciones, animaciones, menús y etiquetas.
No se llama a Scene2D desde los hilos UDP y no se espera una respuesta de red durante `render()`.

## Cómo ejecutar

Hay dos aplicaciones distintas de la versión **1.7.0**:

- PC servidor: **CappuccinoServidor.exe**. Abre su menú propio; pulsar JUGAR
  inicia UDP 9992 en un hilo independiente y muestra la sala de espera.
- Dos PC cliente: **CappuccinoLovesBallerina.exe**. Pulsar JUGAR busca al servidor
  mediante el broadcast existente; el primer cliente recibe Cappuccino y el segundo Ballerina.

El servidor no ocupa ningún personaje. Con ambos clientes confirmados comienza la cuenta
de cinco segundos. Si se abre primero un cliente y no hay servidor, la búsqueda termina
tras BUSQUEDA_MS (10 segundos) y aparece **NO SE ENCONTRÓ UN SERVIDOR ACTIVO**.
El botón JUGAR se habilita para reintentar en el mismo menú. El cliente nunca crea un servidor.

DETENER SERVIDOR cierra el socket, finaliza el bucle y libera el mapa, y vuelve al menú
propio del servidor. Cerrar la ventana también lo detiene. Los clientes muestran pérdida
de conexión dentro del timeout de cinco segundos. Su aviso está centrado sobre un fondo
oscurecido, con VOLVER AL MENÚ.

En una PC: abrir una instancia de CappuccinoServidor y dos de CappuccinoLovesBallerina.
No se escriben direcciones ni se eligen roles. Consultar [ENTREGA_WINDOWS.md](ENTREGA_WINDOWS.md).

Comandos de desarrollo:
```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat :lwjgl3:pruebasClientesGraficos --console=plain
.\gradlew.bat :lwjgl3:zipClienteWindows :lwjgl3:zipServidorWindows --console=plain
```

Las PC deben estar en la misma LAN sin aislamiento entre clientes. El firewall debe permitir
UDP 9992 en el servidor. Las interfaces del juego no muestran esos datos técnicos.

## Diez preguntas posibles de la defensa

1. **¿Quién decide dónde está cada jugador?** El servidor. El cliente manda izquierda, derecha o salto; el servidor calcula la posición.
2. **¿Por qué UDP?** Porque enviamos datos pequeños y frecuentes y queremos estudiar datagramas; controlamos pérdidas y duplicados nosotros.
3. **¿UDP garantiza que llegue un mensaje?** No. Repetimos controles/eventos y mandamos fotos completas para recuperar el estado.
4. **¿Cómo encuentran al servidor sin saber su IP?** Envían `CONEXION` por broadcast y leen la IP del paquete `OK` recibido.
5. **¿Por qué el servidor guarda IP y puerto?** Porque puede haber dos clientes en la misma computadora, con la misma IP y distinto puerto.
6. **¿Qué ocurre si llega dos veces una interacción?** Su secuencia y contador permiten saber que ya se procesó; no alterna la palanca otra vez.
7. **¿Cómo evitamos que Ballerina controle a Cappuccino?** El servidor asigna un rol a cada dirección/sesión y aplica sus botones solo a ese rol.
8. **¿Cómo detectamos que alguien sacó el cable?** Dejan de llegar controles y PING; después de cinco segundos se cancela la conexión.
9. **¿Por qué no modifican los sprites desde el hilo UDP?** Porque LibGDX los maneja en el hilo gráfico. La red publica datos inmutables que ese hilo aplica.
10. **¿Por qué las físicas siguen siendo las mismas?** El servidor usa `Player`, `ControladorPartida`, `ElementosNivel` y `GestorColisiones` del propio juego.

## Pruebas realizadas y pendientes

Ver [ENTREGA_WINDOWS.md](ENTREGA_WINDOWS.md): diferencia pruebas UDP, tres JVM gráficas en una PC, ejecutable empaquetado y pendientes en los tres equipos físicos.
El análisis histórico se conserva en docs/desarrollo, fuera de la distribución.
