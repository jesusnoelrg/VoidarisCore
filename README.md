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
| Spawn | `/spawn`, `/setspawn`, primer spawn, respawn y retorno al caer al vacío |
| Teletransporte | `/tpa`, `/tpaccept`, `/tpdeny`, `/tpacancel` con delay y cooldown |
| Warps | `/warp`, `/setwarp`, `/delwarp`, `/warps` con permiso opcional por warp |
| Kits | `/kit`, `/kits` con cooldown y permiso por kit, kit de primera entrada |
| Homes | `/home`, `/sethome`, `/delhome`, `/homes` con límites por permiso |
| Utilidades | Mesas de trabajo virtuales, `/enderchest`, `/hat`, `/gamemode` |
| Bienvenida | Anuncio y kit de primera entrada, MOTD |
| Chat | Formato por grupo de LuckPerms, colores `&` y hexadecimales |
| Entrada/salida | Mensajes de entrada, salida y primera entrada por grupo de LuckPerms, con PlaceholderAPI |
| Mensajes privados | `/msg`, `/r`, ignorar, desactivar y modo espía |
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
