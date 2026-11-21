# Changelog

Todos los cambios importantes del proyecto CLIENTE serán registrados en este archivo.

## [2.0.0] - 2026-11-21

### Agregado
- Comunicación online entre dos jugadores y un servidor independiente mediante UDP.
- Búsqueda automática del servidor por broadcast general, direcciones de difusión de las interfaces de red y loopback.
- Registro de cada jugador, recepción de su personaje asignado y confirmación de que está listo para comenzar.
- Envío periódico de controles y recepción de estados de la partida para representar en pantalla lo que sucede en el servidor.
- Comprobación periódica de conexión y avisos cuando no se encuentra el servidor o se pierde la comunicación.

### Modificado
- Separación de responsabilidades de red: `Cliente` sirve de conexión entre las pantallas y la red, mientras que `HiloCliente` se ocupa de la comunicación UDP.
- Simplificación de `Cliente` y `HiloCliente`, manteniendo el formato de mensajes compartido con el servidor.
- Organización de `PantallaMenu` y `PantallaJuego` para separar la interfaz de usuario de la comunicación y la actualización de la partida online.
- Sincronización visual de personajes, animaciones, habilidades, espuma, palancas, puerta, objetos cortables, cronómetro y resultados de la partida a partir de los estados recibidos.
- El cliente utiliza los mismos controles para manejar al personaje que le asigna el servidor.

### Corregido
- Manejo de asignaciones de personaje aunque el mensaje de confirmación inicial llegue en otro orden.
- Filtrado de datagramas demasiado grandes, de origen incorrecto o correspondientes a estados antiguos.
- Detección de desconexiones y limpieza de recursos al salir de una partida o cambiar de pantalla.
- Error al renderizar el menú después de transferir la conexión a la pantalla de juego.
- El cliente informa cuando no encuentra un servidor, permite volver a intentar y no inicia un servidor local.

### Eliminado
- Conversión automática de la aplicación cliente en servidor.
- Selección de modos local/en red e ingreso manual de IP desde el flujo normal del menú.
- Responsabilidades propias del servidor: administrar jugadores, ejecutar la simulación autoritativa y decidir el resultado de la partida.

## [1.7.0] - 2026-11-16

### Agregado
- Animaciones de Ballerina y spritesheet de espuma con formación, actividad y disolución; conserva duración, cooldown, transparencia y colisiones.
- Animación de lianas con cuatro fotogramas en LOOP_PINGPONG, dibujando la liana completa y separando su tamaño visual de la hitbox.
- Distribución independiente de Windows para el cliente: `CappuccinoLovesBallerina.exe`, con Java incluido y sin consola, generada mediante jpackage.
- Pruebas reproducibles de reglas, comunicación UDP, funcionamiento del cliente sin servidor y ejecución de varias instancias.

### Modificado
- Integración de la comunicación UDP con las pantallas del juego: el cliente envía controles y representa los estados autoritativos recibidos del servidor.
- Descubrimiento del servidor por broadcast general, interfaces de red y loopback; registro separado del descubrimiento y recepción de la asignación de personaje.
- Envío de controles con sesión, ronda, secuencia y contadores de acciones; recepción de estados completos y comprobación periódica de conexión.
- Menú cliente con un único botón JUGAR y búsqueda asíncrona del servidor, sin selector de rol.
- Aviso de desconexión centrado, con fondo oscurecido y opción para volver al menú.
- README y documentación de entrega actualizados.

### Corregido
- Descubrimiento duplicado del mismo servidor a través de distintas interfaces de red.
- Recepción de la asignación del personaje aunque llegue antes que el mensaje OK.
- Descarte de paquetes inválidos, antiguos o duplicados y detección de pérdida de conexión.
- Cliente sin servidor: muestra «NO SE ENCONTRÓ UN SERVIDOR ACTIVO» y permite reintentar sin iniciar un servidor.
- Glifo Ó ausente en la fuente del menú, para mostrar correctamente el aviso de servidor no encontrado.

### Eliminado
- `InicioLan` y la conversión automática del cliente en servidor.
- Selección manual de IP, botones JUGAR LOCAL/JUGAR EN RED e IP visibles durante la partida.
- Descubrimiento separado mediante `HiloDescubrimiento`, reemplazado por el mecanismo de búsqueda integrado en la comunicación del cliente.
- Lanzadores BAT de la entrega; la herramienta de consola queda opcional para desarrollo.

## [1.6.1] - 2026-11-08

### Agregado
- Conexión UDP con descubrimiento automático de direcciones IP mediante `HiloDescubrimiento`.

## [1.6.0] - 2026-11-07

### Agregado
- Infraestructura inicial de comunicación UDP entre cliente y servidor.
- Clases `Cliente` y `HiloCliente` para establecer la conexión de forma asíncrona y recibir mensajes de red.
- Métodos iniciales para enviar información de controles y acciones del jugador.
- Soporte de configuración de la IP del servidor mediante argumentos de línea de comandos para pruebas.

### Corregido
- Serialización de los datos booleanos de los personajes para mantener un formato consistente entre los mensajes enviados y recibidos.

### Notas técnicas
- Se realizaron pruebas de conexión con varias instancias locales y de límite de jugadores.
- La integración completa de la red con la partida y las pantallas se incorporó en una iteración posterior.

## [1.5.2] - 2026-10-30

### Agregado
- Integración completa de Ballerina Capuccina como segundo personaje.
- Habilidad exclusiva de Ballerina: creación de `BloqueEspuma`, una plataforma sólida temporal.
- Sistema de cooldown para limitar el uso de la espuma.
- Spawn personalizado de Ballerina leído desde el mapa de Tiled.
- Soporte para el control de ambos personajes y sus mecánicas cooperativas.
- Condiciones cooperativas de victoria y derrota.
- Interacción compartida con palancas y puerta.
- Visualización de Ballerina y de la espuma durante la partida.

### Cambiado
- Actualización de `Constantes.java` con dimensiones, duración y cooldown de la habilidad de Ballerina.

## [1.5.1] - 2026-10-29

### Refactorizado
- Reemplazo de la dependencia directa de `CapuccinoLovesBallerinaGame` por `Game` en las pantallas y los menús, reduciendo el acoplamiento con la clase principal.

## [1.5] - 2026-10-27

### Agregado
- Selector de resolución de ventana en `PantallaOpciones`, con opciones de 1280x720, 1024x768 y 800x600.
- Alternancia entre modo ventana y pantalla completa desde `PantallaOpciones`.
- Comunicación UDP inicial entre cliente y servidor mediante `Cliente` y `HiloCliente`.
- Lectura de mensajes por consola para realizar pruebas de comunicación.

### Cambiado
- Resolución inicial de la ventana configurada en 1280x720 en `Lwjgl3Launcher`.
- El selector de resolución queda deshabilitado mientras el juego está en pantalla completa.
- Al volver al modo ventana, se recupera la última resolución seleccionada.

## [1.4.6] - 2026-10-10

### Agregado
- Controles de volumen y silencio en `PantallaOpciones`.

### Cambiado
- Se quitó la maximización automática de la ventana en `Lwjgl3Launcher`.
- Se limpiaron comentarios en `Lwjgl3Launcher`.

### Refactorizado
- Se separaron responsabilidades de `PantallaJuego` en `ControladorPartida`, `RenderizadorJuego` y `ElementosNivel` para mejorar la organización del código.

## [1.4.5] - 2026-09-22

### Corregido
- Manejo de la tecla ESC mediante `ControladorEntradaJugador` para alternar el menú de pausa.
- `soltarTodo()` restablece todos los estados de entrada, incluyendo `saltoW` y `saltoEspacio`.
- Liberación de recursos compartidos desde `MenuPausa`.
- `Recursos` se libera únicamente desde `CapuccinoLovesBallerinaGame`.
- Se evitó liberar el `Skin` compartido al cerrar el menú de pausa.

## [1.4.0] - 2026-09-04

### Agregado
- Sistema de audio (`Utilidades/Audio`): música de fondo por pantalla, con cambio automático al navegar entre pantallas y liberación del recurso al salir del juego.
- Volumen general configurable en `Audio`, acotado entre 0 y 1, con valor bajo por defecto.

### Cambiado
- `PantallaOpciones` incorpora una pantalla informativa con botón para volver al menú.

## [1.3.0] - 2026-08-27

### Agregado
- HUD con cronómetro de la partida en formato MM:SS sobre un marco decorativo.

### Cambiado
- Imagen del menú principal.
- Imagen de la pantalla de victoria y cambio del nombre de su botón a «Siguiente nivel».

## [1.2.0] - 2026-08-24

### Agregado
- Zonas de peligro que muestran GAME OVER al tocarlas, además de la muerte por caída al vacío.
- Corte de objetos como cuerdas y barriles mediante la katana de Cappuccino Assassino.
- Mecánica cooperativa de palanca y puerta.
- Condición de victoria al interactuar con la puerta abierta.
- Movimiento de Cappuccino Assassino: desplazamiento horizontal, salto, gravedad y colisiones contra los sólidos del mapa.
- Spawn del personaje leído desde la capa de objetos de Tiled.
- Nivel 0 diseñado en Tiled Map Editor, con tileset, fondo, objetos y colisiones.
- Paquete `mapa` con `Nivel` y `GestorColisiones`.
- Carga y dibujo del mapa de Tiled en `PantallaJuego`.
- Spritesheet de Cappuccino Assassino con animaciones de idle, caminar, saltar y atacar.
- Paquete `graficos` con la clase de animaciones de Cappuccino.
- Integración de animaciones en `PantallaJuego` según el estado del personaje.
- Arte de objetos interactivos: puerta, palanca, pinchos y liana.

### Cambiado
- Rediseño visual y funcional de la puerta.
- `reintentar()` restablece también la puerta y la palanca.
- Ajuste de `ANCHO_MUNDO` para que coincida con el mapa.
- `PantallaJuego` delega la carga del mapa en `Nivel` y el dibujo en el renderizador.

### Corregido
- Teclas que quedaban presionadas al abrir un menú o reintentar; se agregó `soltarTodo()` a `ControladorEntradaJugador`.

## [1.1.0] - 2026-08-23

### Agregado
- Menú de pausa como overlay con botones Reanudar, Volver al menú y Salir.
- Alternancia de la pausa con la tecla ESC.
- Cámara fija con `FitViewport` para adaptar el contenido a distintos tamaños de ventana sin deformarlo.
- Entorno de prueba con plataformas.
- Menú principal con viewport propio y fondo adaptable.
- Paquete `Utilidades` con `Constantes`, `FabricaViewport` y `Recursos`.
- Clase `BallerinaCapuccina` como base del segundo personaje.

### Cambiado
- `PantallaJuego` recibe la instancia del juego en el constructor.
- Pantallas y menús utilizan `FabricaViewport` y `Constantes`.
- Resolución virtual y ventana de escritorio configuradas en 1366x768.

### Corregido
- Botones del menú principal al maximizar la ventana: `resize()` actualiza el viewport del `Stage`.

## [1.0.1] - 2026-08-04

### Agregado
- Actualización del README en el apartado de ejecución y compilación.
- Renombre de la clase principal a `CapuccinoLovesBallerinaGame`.
- Creación del menú principal, su fondo y los botones Salir, Opciones y Jugar.
- Funcionalidades de los botones del menú.

## [1.0.0] - 2026-07-16

### Agregado
- Creación inicial del proyecto con LibGDX.
- Configuración del módulo Core y de la plataforma de escritorio LWJGL3.
- Incorporación del archivo `.gitignore`.
- Creación del repositorio en GitHub.
- Creación del README inicial y de la Wiki con la propuesta del proyecto.
