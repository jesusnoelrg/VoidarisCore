# VoidarisCore

Plugin núcleo del servidor de Minecraft **Voidaris** ([web.voidaris.club](https://web.voidaris.club)).
Reúne en un solo plugin las funciones que antes cubrían varios plugins pequeños
(EssentialsX, LPC, VIPJoinMessage, MSG, Announcement, ColoredSigns y VoidSpawn),
con mensajes y estilo uniformes.

## Plataforma

- Paper 26.3
- Java 25
- Gradle (Kotlin DSL)
- Integraciones: LuckPerms, Vault, PlaceholderAPI

## Módulos planeados

| Módulo | Descripción |
|---|---|
| Spawn | `/spawn`, `/setspawn`, `/delspawn` con delay y cooldown, envío al spawn en la primera entrada y al reaparecer (respetando cama/ancla) y retorno al caer al vacío |
| Teletransporte | `/tpa`, `/tpaccept`, `/tpdeny`, `/tpacancel` con delay y cooldown |
| Efectos de teletransporte | Sonido para todos (cada jugador oye solo el suyo) y partículas elegidas como cosmético, por causa configurable |
| Warps | `/warp [warp]` (sin argumento, o con `/warps`, lista los warps clicables), `/setwarp`, `/delwarp`, con delay, cooldown y permiso opcional por warp |
| Kits | `/kit`, `/kits` con cooldown y permiso por kit, kit de primera entrada |
| Homes | `/home [home]` (con una sola home va directo; con varias sugiere el listado), `/sethome [home]`, `/delhome <home>`, `/listhome` (`/homes`) abre un menú (`guis/homes.yml`) con clic para ir; `/home <home> seticon` usa el tipo del objeto en la mano como icono (`voidaris.homes.seticon`); límites por permiso (`voidaris.homes.limit.<grupo>`), delay, cooldown (`voidaris.homes.cooldown.bypass`) y variantes de staff con `<jugador>` (también desconectado) bajo `voidaris.homes.*.others` |
| Utilidades | Estaciones virtuales (`/craft`, `/anvil`, `/grindstone`, `/stonecutter`, `/loom`, `/cartography`, `/smithing`), `/enderchest [jugador]`, `/gm <modo\|0-3>` y atajos (`/gms`, `/gmc`, `/gma`, `/gmsp`), `/heal`, `/feed`, `/fly`, `/god`, `/speed`, `/hat`, `/day`, `/night`, `/sun`, `/rain`; todo bajo `voidaris.utilities.*` (OP por defecto) |
| Bienvenida | Anuncio y kit de primera entrada |
| MOTD | Mensaje de bienvenida multilínea (MiniMessage) en `motd.yml`, enviado justo antes del mensaje de entrada del jugador y con `/motd`; incluye novedades, comandos clicables y redes sociales |
| Chat | Formato por grupo de LuckPerms (`chat-format.yml`), hover en el nombre y clic que sugiere `/msg <jugador>`; colores `&` (`voidaris.chat.color`) y hexadecimales (`voidaris.chat.hex`) |
| Entrada/salida | Mensajes de entrada (según el cosmético equipado) y primera entrada, con PlaceholderAPI |
| Cosméticos | `/cosmetics` abre un menú (definido en `guis/`) para equipar mensajes de entrada y partículas de teletransporte desbloqueados por permiso |
| Mensajes privados | `/message` (`/msg`, `/tell`) y `/reply` (`/r`) con clic para responder; `/ignore` (solo privados) y `/superignore` (chat y privados) como interruptores, `/listignore` paginado con clic para dejar de ignorar; `/spy` (`voidaris.privatemessages.spy`), con `.spy.exempt` para ocultar las conversaciones de un admin y `.spy.bypass` para verlas igualmente |
| Anuncios | Anuncios automáticos con MiniMessage |
| Carteles | Colores en carteles |
| Economía | Proveedor de economía para Vault |

## Compilar

```bash
./gradlew build
```

El jar queda en `build/libs/`. Para levantar un servidor Paper de prueba:

```bash
./gradlew runServer
```
