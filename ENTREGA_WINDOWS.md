# Entrega Windows — Cappuccino Loves Ballerina 1.7.0

## Dos aplicaciones

| Computadora | ZIP que debe extraerse | Ejecutable |
|---|---|---|
| Servidor | CappuccinoServidor-1.7.0-Windows.zip | CappuccinoServidor/CappuccinoServidor.exe |
| Cada uno de los dos clientes | CappuccinoLovesBallerina-1.7.0-Windows.zip | CappuccinoLovesBallerina/CappuccinoLovesBallerina.exe |

Copiar y extraer la carpeta completa: **app** y **runtime** deben permanecer junto al EXE.
Ambas distribuciones incluyen Java y dependencias; no requieren IntelliJ, Gradle instalado
ni BAT en los equipos de destino. Son aplicaciones Windows x64.
El código sigue compilando para Java 17; el runtime empaquetado es el JDK utilizado por jpackage.

## Iniciar y detener una partida

1. Abrir **CappuccinoServidor.exe** y pulsar **JUGAR**. Aparece SERVIDOR INICIADO y la sala de espera.
2. Abrir **CappuccinoLovesBallerina.exe** en cada PC cliente y pulsar **JUGAR**.
3. El primero recibe Cappuccino y el segundo Ballerina. Esperar la cuenta de cinco segundos.
4. La PC servidor supervisa y no controla personajes.
5. DETENER SERVIDOR vuelve al menú del servidor, cierra el socket y libera el mapa.
   Cerrar su ventana también lo detiene; los clientes detectan la pérdida de conexión.

Si no hay servidor, después de **BUSQUEDA_MS = 10 segundos** aparece
**NO SE ENCONTRÓ UN SERVIDOR ACTIVO** en el cliente. Se puede pulsar JUGAR para reintentar.
**El cliente nunca se convierte en servidor.** Tampoco hay selección de rol ni campo de IP.

Para probar en una PC: abrir una aplicación servidor y dos clientes. Cambiar de ventana
para dar el foco al personaje que se desea controlar.
Cappuccino: A/D, W o Espacio, F, E. Ballerina: flechas, arriba, Shift derecho, Enter.

El protocolo sigue siendo **UDP, puerto 9992**, con descubrimiento por broadcast de las
interfaces activas y loopback para pruebas locales. Las tres PC deben compartir la LAN.
Comprobar permisos del firewall en la red privada, especialmente en la PC servidor.

## Compilar y generar los ejecutables

Desde la raíz del proyecto, usando un JDK para Windows con jpackage (se probó con JDK 21):
```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat :lwjgl3:zipClienteWindows :lwjgl3:zipServidorWindows --console=plain
```

Ambos ZIP se generan en **lwjgl3/build/distributions**.

Ejecutables:
- **lwjgl3/build/windows/cliente/CappuccinoLovesBallerina/CappuccinoLovesBallerina.exe**
- **lwjgl3/build/windows/servidor/CappuccinoServidor/CappuccinoServidor.exe**

Las tareas prepararClienteWindows / prepararServidorWindows usan directorios de entrada
separados. distribucionClienteWindows / distribucionServidorWindows reutilizan jpackage
con entradas y salidas independientes; cada imagen tiene su runtime. zipWindows sigue
disponible como tarea conjunta para generar ambos ZIP. No genera una aplicación mixta.
La versión de EXE, JAR y ZIP procede de **projectVersion=1.7.0** en gradle.properties.

Desarrollo: **:lwjgl3:run** inicia el cliente; **:lwjgl3:runServidor** inicia el servidor gráfico.
**:core:jarServidor** y **:lwjgl3:servidor** quedan exclusivamente como herramientas opcionales
de consola; no forman parte de los ZIP de entrega y assemble ya no genera jarServidor.

## Clases y cambios de esta separación

Archivos creados:
- core/src/main/java/com/capuccinolovesballerina/game/CapuccinoServidorGame.java
- core/src/main/java/com/capuccinolovesballerina/game/pantallas/PantallaMenuServidor.java
- core/src/main/java/com/capuccinolovesballerina/game/red/ServicioServidor.java
- lwjgl3/src/main/java/com/capuccinolovesballerina/game/lwjgl3/Lwjgl3ServidorLauncher.java

Archivos modificados:
- core/src/main/java/com/capuccinolovesballerina/game/Utilidades/Recursos.java
- core/src/main/java/com/capuccinolovesballerina/game/pantallas/PantallaMenu.java
- core/src/main/java/com/capuccinolovesballerina/game/pantallas/PantallaServidor.java
- lwjgl3/src/main/java/com/capuccinolovesballerina/game/lwjgl3/Lwjgl3Launcher.java
- core/src/test/java/com/capuccinolovesballerina/game/red/PruebasRed.java
- lwjgl3/src/test/java/com/capuccinolovesballerina/game/lwjgl3/PruebasClientesGraficos.java
- core/build.gradle, lwjgl3/build.gradle, gradle.properties
- CHANGELOG.md, README.md, DOCUMENTACION_RED.md, ENTREGA_WINDOWS.md

Eliminado: core/src/main/java/com/capuccinolovesballerina/game/red/InicioLan.java.

ServicioServidor solo administra construcción, hilo y cierre de Servidor. PantallaServidor
sigue leyendo Servidor.getResumen(). PantallaMenu crea únicamente Cliente y usa su búsqueda
asíncrona. Se conservaron Servidor, HiloServidor, Cliente, HiloCliente, ProtocolosRed,
ControladorPartida, Nivel y CargadorMapaServidor sin reescribir sus reglas en esta separación.
PantallaJuego conserva el aviso de desconexión centrado y el fondo oscurecido.
No se modificaron mapas, sprites, controles ni dependencias para separar las aplicaciones.
La fuente ASCII original no tenía Ó: Recursos agrega ese glifo en memoria para mostrar el mensaje exacto, y Skin administra su textura.

## Limpieza y licencias

Los ZIP contienen únicamente la imagen de aplicación: EXE, JAR y runtime.
No incluyen referencias, informes, código de pruebas, fuentes de consulta, BAT de desarrollo
ni temporales. El archivo histórico docs/desarrollo queda en el repositorio, conservando
contenidos y atribuciones. Los avisos de terceros y las licencias del runtime se conservan.
La fecha 2026-11-16 de la sección 1.7.0 del changelog corresponde a la fecha de entrega
solicitada; las comprobaciones de esta separación se realizan el 18 de noviembre de 2026.

## Pruebas

Para reproducir (cerrar partidas anteriores que ocupen 9992):
```powershell
.\gradlew.bat :core:pruebasRed --console=plain
.\gradlew.bat :lwjgl3:pruebasClientesGraficos --console=plain
```

Las pruebas gráficas crean procesos independientes usando las clases Game reales:
primero un cliente solo, después el servidor gráfico y dos clientes. Comprueban error
sin servidor y reintento, puerto libre, roles, cuenta, movimiento, habilidades, espuma,
aislamiento de controles y desconexión de ambos clientes al detener el servidor en partida.
Las capturas y logs quedan en lwjgl3/build/reports/red-grafica/ejecucion-... .
El log de compilación y empaquetado de esta entrega queda en build/verificacion-1.7.0.log.

Resultados comprobados el 18/11/2026:

- Gradle build: BUILD SUCCESSFUL. PruebasRed: **72 comprobaciones aprobadas**.
- Prueba gráfica: cliente sin servidor con texto exacto, reintento y puerto libre; servidor con menú propio y sin cupos; dos clientes con roles, cuenta, movimientos y habilidades; cierre del servidor en partida, aviso en ambos clientes y puerto disponible.
- Capturas inspeccionadas: error de servidor no encontrado y desconexión centrada sobre fondo oscuro. Se corrigió también el glifo Ó que faltaba en la fuente.
- Ambos EXE tienen versión de producto **1.7.0**, mainclass independiente y subsistema PE gráfico (2), sin consola. Sus configuraciones apuntan al JAR 1.7.0 y el runtime está incluido.
- Los **41 archivos de assets** se compararon por SHA256 dentro de ambos JAR: todos coinciden. No se empaquetan InicioLan, pruebas, referencias, logs ni BAT.
- Copias de ambos ejecutables arrancaron fuera del proyecto: se comprobó el bucle gráfico de LibGDX y el launcher correspondiente. La herramienta de captura de ventanas falló por timeout; **no se da por verificada una sesión manual completa por doble clic con los EXE**. Las pruebas interactivas automatizadas corresponden a los procesos gráficos de prueba con las clases Game reales.
- Se retiraron de build la imagen y el ZIP anteriores de la aplicación mixta. No se hizo commit ni push.

La revisión visual y empaquetado posterior están en build/verificacion-interfaz-1.7.0.log.

Pendiente en el colegio: ejecutar ambos ZIP en las tres PC físicas, validar firewall,
broadcast, controladores gráficos/audio y completar el nivel cooperativo. Las pruebas
en una sola PC no sustituyen la validación de la LAN entre equipos.
