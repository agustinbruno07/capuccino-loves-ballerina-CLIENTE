# Cappuccino Loves Ballerina — Cliente

## Integrantes del grupo

- Guillermo Bordoli
- Agustín Bruno
- Santino Fierro

## Descripción del videojuego

Cappuccino Loves Ballerina es un videojuego cooperativo de plataformas y puzles en 2D para PC, inspirado en las mecánicas principales de Fireboy and Watergirl.

Cada jugador controla desde una computadora diferente a Cappuccino Assassino o Ballerina Capuccina. Los dos personajes deben colaborar para superar obstáculos, activar mecanismos, resolver puzles y escapar de una estructura misteriosa mediante el uso coordinado de sus habilidades especiales.

## ¿Qué hace este repositorio? — CLIENTE

Este repositorio contiene la aplicación que utilizan los jugadores en sus computadoras.

- Cada jugador abre el juego en su propia PC.
- El cliente busca el servidor de la partida y solicita conectarse.
- Envía al servidor los controles del jugador, como moverse, saltar, interactuar o usar una habilidad.
- Recibe del servidor la información de la partida y la muestra en pantalla: posiciones de los personajes, animaciones, tiempo, mecanismos y resultado de la ronda.
- Informa si no encuentra el servidor o si se pierde la conexión, y permite volver a intentar.

**El cliente no crea ni administra la partida:** se encarga de recibir las acciones del jugador y mostrar lo que está ocurriendo. La partida real se procesa en el servidor.

## Tecnologías principales

- Java JDK 17
- LibGDX 1.14.1
- Gradle
- IntelliJ IDEA
- Git y GitHub
- Plataforma objetivo: escritorio mediante LWJGL3
- Comunicación entre computadoras: UDP

## Wiki del proyecto

La propuesta formal y detallada del videojuego se encuentra en la Wiki:

[Ver la propuesta del proyecto](https://github.com/agustinbruno07/capuccino-loves-ballerina/wiki)

## Cómo jugar online — versión 2.0

Para jugar una partida online dentro de la misma red local se necesitan **una PC que ejecute el servidor y dos PC que ejecuten el cliente**.

1. En la PC que hará de anfitriona, abrir **CappuccinoServidor.exe** y pulsar **JUGAR**.
2. En cada una de las dos PC de los jugadores, abrir **CappuccinoLovesBallerina.exe** y pulsar **JUGAR**.
3. Los clientes buscan automáticamente el servidor disponible en la red local y se conectan.
4. Cuando los dos jugadores estén conectados y listos, el servidor inicia una cuenta regresiva y comienza la partida.
5. Cada jugador controla a su personaje desde su propia computadora y ambos ven el mismo desarrollo de la partida.

Los tres equipos deben estar conectados a la misma red local. La comunicación utiliza UDP por el puerto **9992**; el firewall de la PC servidor debe permitir esa comunicación. Esta configuración está pensada para jugar en una red local; jugar desde redes distintas puede requerir una configuración adicional de red.

Si no hay un servidor disponible, el cliente muestra un mensaje y permite volver a intentar. **El cliente nunca inicia un servidor por su cuenta.**

## Distribución para Windows

En la PC de cada jugador se utiliza **CappuccinoLovesBallerina.exe**. El servidor debe iniciarse por separado desde **CappuccinoServidor.exe**.

Para generar el paquete del cliente desde la raíz del proyecto, ejecutar:

```bash
gradlew.bat :lwjgl3:zipClienteWindows
```

El archivo ZIP se genera en `lwjgl3/build/distributions`. Extraer el contenido completo del ZIP: incluye Java y los archivos necesarios para ejecutar el juego.

El nombre y número de versión del ZIP se obtienen de la propiedad `projectVersion` configurada en `gradle.properties`.

## Compilación y ejecución (desarrollo)

### Requisitos

- Java JDK 17.
- Git.
- IntelliJ IDEA o cualquier entorno compatible con proyectos Gradle.

### Ejecutar en Windows

Desde la carpeta raíz del proyecto:

```bash
gradlew.bat lwjgl3:run
```

Si se utiliza Git Bash, también se puede ejecutar:

```bash
./gradlew.bat lwjgl3:run
```

### Ejecutar en Linux o macOS

Desde la carpeta raíz del proyecto:

```bash
./gradlew lwjgl3:run
```

### Ejecutar desde IntelliJ IDEA

1. Abrir IntelliJ IDEA y seleccionar **Open**.
2. Elegir la carpeta raíz del proyecto CLIENTE.
3. Esperar a que Gradle importe el proyecto y descargue las dependencias.
4. Abrir el módulo `lwjgl3`.
5. Buscar `Lwjgl3Launcher.java` y ejecutar su método `main`.

## Video de demostración (segunda pre-entrega)

[▶ Ver video de demostración](https://drive.google.com/file/d/1RYrjh4mWFUnnSkVwSDYIL4kscPV1n7NP/view?usp=drive_link)

## Estado actual del proyecto — versión 2.0

Prototipo cooperativo para escritorio con:

- Movimiento de los personajes, salto, gravedad y colisiones contra los mapas diseñados en Tiled.
- Animaciones de Cappuccino Assassino y Ballerina Capuccina.
- Mecánicas cooperativas de palancas y puertas, corte de lianas con katana, peligros y caída al vacío.
- Condiciones de derrota y victoria, con posibilidad de reiniciar la ronda.
- Pantallas de menú principal, opciones, controles, juego, pausa, derrota y victoria.
- HUD con cronómetro y música de fondo con volumen configurable.
- **Modo multijugador online por red local:** dos jugadores pueden participar desde computadoras diferentes.
- **Conexión automática con el servidor:** el cliente busca la partida disponible y solicita un lugar.
- **Controles enviados por UDP:** las acciones de cada jugador se transmiten al servidor para que las procese.
- **Partida sincronizada:** el cliente recibe periódicamente la información actualizada de los dos personajes y del escenario para mostrar la misma partida en ambas computadoras.
- **Seguimiento de conexión:** se comprueba periódicamente que el servidor siga respondiendo. Si se pierde la conexión, se informa al jugador.
- **Reinicio coordinado:** las solicitudes de reinicio se envían al servidor para que ambos jugadores continúen en la misma ronda.

En pocas palabras: **cada cliente es la computadora de un jugador; el servidor coordina la partida y les devuelve a ambos lo que está pasando en el juego.**
