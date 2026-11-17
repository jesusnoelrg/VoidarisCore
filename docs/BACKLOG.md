- **Redis cache:** add a Lettuce-backed `Cache` implementation if Voidaris becomes a multi-server network.
- **Cosmetics navigation:** the cosmetic menus only have slot 36 for a menu-specific section, so `guis/kill-messages.yml` is not linked from the join message or teleport particle menus (and vice versa for particles). Consider a cosmetics hub menu.
- **Auras visibility:** add a player setting (Ajustes menu) to hide other players' auras, for players who find the trails distracting.

- **cosmetics:** `AurasMenu`, `TeleportParticlesMenu` and `JoinEffectsMenu` are near-identical; extract a shared paginated cosmetic menu.
- **cosmetics:** add a preview of join effects from `/cosmetics joineffect` (e.g. right-click on an icon plays it for the viewer only).
- **cosmetics:** no menu links to `/cosmetics joineffect`, `/cosmetics aura` or `/cosmetics killmessage` yet; a cosmetics hub menu is missing.
