- **Redis cache:** add a Lettuce-backed `Cache` implementation if Voidaris becomes a multi-server network.

- **utilities:** pending Essentials-style commands not included in the first batch: `/invsee`, `/repair`, `/top`, `/ping`, `/clear`, `/time <value>`, `/weather thunder`. `/enderchest <player>` uses the vanilla title because the view is opened on the live inventory.

- private-messages: `/msgtoggle` (keys `private-messages.disabled-self`, `enabled-self`, `target-disabled` already exist but are unused) and sending `/msg` from the console.
