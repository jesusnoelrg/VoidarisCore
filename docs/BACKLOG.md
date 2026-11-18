- **Redis cache:** add a Lettuce-backed `Cache` implementation if Voidaris becomes a multi-server network.
- **Cosmetics navigation:** the cosmetic menus only have slot 36 for a menu-specific section, so `guis/kill-messages.yml` is not linked from the join message or teleport particle menus (and vice versa for particles). Consider a cosmetics hub menu.
- **Auras visibility:** add a player setting (Ajustes menu) to hide other players' auras, for players who find the trails distracting.

- **cosmetics:** `AurasMenu`, `TeleportParticlesMenu` and `JoinEffectsMenu` are near-identical; extract a shared paginated cosmetic menu.
- **cosmetics:** add a preview of join effects from `/cosmetics joineffect` (e.g. right-click on an icon plays it for the viewer only).
- **cosmetics:** no menu links to `/cosmetics joineffect`, `/cosmetics aura` or `/cosmetics killmessage` yet; a cosmetics hub menu is missing.
- **Idea (cosmetics/chat):** chat formats can use `<nickname>` but it has no hover/click; `ChatListener#name` could build the clickable name from `CosmeticsModule#nickname` so `<player>` shows the nick color in chat. Existing servers keep their old join message formats (user-owned `join-messages.cosmetics`), so `<nickname>` must be added there by hand.
- **Idea (cosmetics/badges):** custom badges (`/badges custom`) have no word filter; staff may need a blocked-words list or a command to reset a player's custom badge.
- **cosmetics:** `CosmeticsCommand#selectTeleportParticle`, `#selectAura` and `#selectJoinEffect` are near-identical; they could take the same `CosmeticMenu.Type` the menus now use.
