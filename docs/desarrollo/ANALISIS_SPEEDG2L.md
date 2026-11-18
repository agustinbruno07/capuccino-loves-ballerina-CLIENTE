# Corrección de red basada en SpeedG2L

## Referencia leída antes de editar

Se revisaron completas las clases de red de los dos archivos de `referencias_red`:

- Servidor: `HiloServer`, `Cliente`, `DireccionRed`, `PosicionFinal` y `ProtocolosRed`.
- Cliente: `HiloCliente`, `PosicionFinal` y `ProtocolosRed`.
- También se leyó la integración de `PantallaJuego` de ambos proyectos, el envío de controles
  de `Jugador` y el mantenimiento de la conexión/revancha de `PantallaFin` del cliente.

La idea central de SpeedG2L es sencilla: **el cliente comunica teclas; el servidor mueve al jugador asignado;
el servidor comunica las posiciones a todos**. Se conserva esa idea sin trasladar sus físicas, entidades ni pantallas.

## Comparación directa

| Tema | SpeedG2L | Cappuccino Loves Ballerina corregido |
|---|---|---|
| Transporte | `DatagramSocket` / `DatagramPacket`, puerto 6412 | Las mismas clases Java, puerto 9992 |
| Encontrar servidor | `Conexion` a 255.255.255.255; `OK` permite leer `dp.getAddress()` | `CONEXION` por broadcast; `OK` y lectura de la IP del paquete |
| Varias interfaces | Un único destino broadcast general | General y broadcasts de cada interfaz; loopback para pruebas en una PC |
| Registro | IP + puerto, dos espacios en un arreglo | IP + puerto y dos espacios; registro directo después de elegir la IP |
| Personajes | Jugadores 1 y 2, asociados a cada conexión | Rol 0 Cappuccino y rol 1 Ballerina |
| Controles | `Aprete_Izquierda`, `NoAprete_Derecha`, `Aprete_Arriba` cuando cambia la tecla | Izquierda/derecha periódicos y contadores de salto, habilidad e interacción |
| Simulación | El servidor ejecuta las físicas en su `PantallaJuego.render()` | El servidor de consola ejecuta los mismos `Player` y `ControladorPartida` del modo local |
| Posiciones | `Movimiento_x_y_numeroJugador` a ambos clientes | Foto `ESTADO` con posiciones de ambos, dirección, animación y objetos compartidos |
| Cuenta regresiva | Cinco segundos, `CuentaRegresiva` y luego `Empezar` | Cinco segundos; fase `CUENTA`, luego `JUGANDO`, repetidas en las fotos |
| Comprobación | `estasAhi` / `estoyAca`, timeout de cinco segundos | `PING` / `PONG`, fotos y timeout de cinco segundos |
| Fin | Llegadas individuales y `TODOS_TERMINARON` | Muerte compartida o victoria cooperativa con ambos en la puerta abierta |
| Reinicio | Ambos solicitan revancha; `NUEVA_PARTIDA` | Solicitud `REINICIO` y una ronda nueva con cuenta para ambos |
| Hilos | El hilo de red modifica banderas y controles de jugadores | El receptor deja mensajes en una cola; solo el bucle servidor cambia su partida |

No se copió el movimiento de SpeedG2L: nuestra gravedad, salto, alcance de katana, espuma y condiciones
de victoria siguen en las clases originales de Cappuccino Loves Ballerina.
Las fotos completas y los contadores se conservaron para que una pérdida UDP no deje una palanca o una tecla desincronizada.

## Qué estaba fallando o incompleto

1. **Fallo reproducido de arranque independiente.** Al ejecutar el servidor desde una carpeta distinta del repositorio,
   fallaba con `FileNotFoundException: assets/mapas/NIVEL 0.tmx`. El cliente podía abrir su mapa empaquetado,
   pero el servidor no llegaba a escuchar. Ahora el cargador busca primero en disco y después dentro del JAR.
2. **Descubrimiento insuficiente para varias interfaces.** El cliente anterior solo enviaba a 255.255.255.255.
   Este equipo tiene Ethernet y Hamachi. Eso no demuestra por sí solo que Hamachi sea la causa del problema del usuario,
   pero hacía depender la búsqueda de la ruta elegida por Windows. Ahora se prueban los broadcasts de cada interfaz.
3. **Diagnóstico insuficiente.** No se veían los destinos de búsqueda ni las etapas recibidas. Ahora las consolas
   indican puerto local, destinos, intentos, `OK`, `REGISTRO`, personaje y `LISTO`. El servidor muestra sus IPv4.
4. **Orden de los datagramas.** Antes se descartaba `ASIGNADO` si todavía no había llegado `OK`.
   Ahora una asignación válida también confirma el origen. Se agregó una prueba que omite OK intencionalmente.
5. **Inicio sin confirmación.** Antes se iniciaba la cuenta al registrar dos direcciones, aunque un cliente aún no hubiera
   recibido su personaje. Ahora ambos deben confirmar `LISTO`; la confirmación se repite mientras esperan.
6. **Arranque con Java incorrecto.** En este equipo `java` por defecto es Java 8 y el juego requiere Java 17 o superior.
   No se afirma que haya sido la causa de todos los intentos fallidos. Los scripts nuevos usan el wrapper y su JDK configurado.
7. **Prueba gráfica anterior insuficiente para la entrega.** La validación previa usó una aplicación gráfica y otro cliente
   de datos en el mismo proceso. Ahora hay una prueba reproducible con **tres JVM separadas**: servidor real y dos juegos gráficos completos.

No se atribuyó el problema al firewall sin comprobarlo y no se cambiaron reglas de Windows.
Si los paquetes no llegan entre PC, sigue siendo necesario permitir el servidor Java/UDP 9992 en la red utilizada.

## Por qué se separan descubrimiento y registro

En una computadora con varias interfaces, un único socket puede enviar por Ethernet, una interfaz virtual y loopback.
Si cada broadcast reservara un cupo inmediatamente, el mismo cliente podría aparecer con distintas IP de origen.
Para no ocupar los dos personajes con una sola instancia, la secuencia es:

```text
Cliente -> broadcast general y de interfaces: CONEXION
Servidor -> origen: OK                         (todavía no reserva personaje)
Cliente elige la IP recibida -> servidor: REGISTRO
Servidor registra IP/puerto -> cliente: ASIGNADO|sesion|rol
Cliente -> servidor elegido: LISTO|sesion
Ambos confirmados -> CUENTA 5,4,3,2,1 -> JUGANDO
```

`REGISTRO` y `LISTO` son dos mensajes pequeños dentro del mismo hilo y puerto UDP;
no se agregaron un servicio de descubrimiento, un framework ni otro socket servidor.

## Qué se reutilizó y qué se modificó

Se conservaron `Cliente`, `Servidor`, `HiloCliente`, `HiloServidor`, `ProtocolosRed` y la integración de las pantallas.
No se crearon clases de red duplicadas. `HiloDescubrimiento` ya estaba eliminado y no se reintrodujo.
Se reutilizaron las reglas compartidas y las pruebas de katana, lianas, espuma, palancas, puerta, muerte y victoria.

Cambios de esta corrección:

- `HiloCliente`: destinos por interfaz, selección de servidor, registro directo, tolerancia a OK perdido, LISTO y PONG, trazas.
- `Servidor`: descubrimiento sin ocupar cupos, registro directo, confirmaciones, respuesta PONG, lista de IP y error claro si 9992 está ocupado.
- `ProtocolosRed`: constantes de `REGISTRO`, `LISTO` y `PONG`.
- `CargadorMapaServidor`: recursos empaquetados como respaldo, sin necesidad de ventana ni PNG.
- `core/build.gradle`: JAR independiente del servidor con sus TMX/TSX.
- `PruebasRed`: regresiones de descubrimiento, confirmación y datagramas desordenados.
- `PruebasClientesGraficos` y tarea Gradle: servidor y dos juegos separados, con logs, capturas y comparación de estados.
- `ejecutar_servidor.bat` y `ejecutar_cliente.bat`: arranque desde la carpeta correcta mediante Gradle.
- `DOCUMENTACION_RED.md`: ejecución y protocolo actualizados.

No se modificaron sprites, TMX, físicas ni reglas del modo local en esta corrección.
Las modificaciones previas y ajenas a redes se conservaron. No se hizo commit ni push.

## Alcance de las pruebas

La prueba gráfica de tres procesos usa entrada programada por el `InputProcessor` del propio juego;
no simula respuestas de red ni utiliza un servidor falso. Cada proceso tiene su socket y memoria propios.
Compara las posiciones que ven ambos clientes, comprueba las dos habilidades y la espuma, y verifica el aviso al cerrar un cliente.
Los resultados quedan en `lwjgl3/build/reports/red-grafica/ejecucion-...`.

En la primera ejecución, la búsqueda automática llegó al servidor por **127.0.0.1**.
En la validación final, recibió `OK` desde la dirección Ethernet local **192.168.0.37**.
El servidor recibió CONEXION por Ethernet y loopback, pero registró un solo personaje para esa instancia.
Eso demuestra el arranque y la conexión automática local por la interfaz, pero **no prueba el broadcast entre tres computadoras**.
La prueba por Ethernet y el firewall de la red del colegio siguen pendientes.
La katana sobre una liana, ambas palancas y la victoria completa se verifican en las pruebas de reglas;
queda pendiente recorrer manualmente todo el nivel con dos personas en las PC finales.
