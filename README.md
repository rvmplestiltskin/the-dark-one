# The Dark One

A Fabric mod that allows **one player** to become The Dark One.

Only one Dark One can exist at a time. The power can be claimed by an operator or transferred by killing the current Dark One while holding the unique dagger.

## Features

### Being the Dark One grants:
- Increased Strength
- Increased Speed
- Strong Regeneration
- Damage Resistance
- Permanent Night Vision
- Fire Resistance
- Water Breathing

### Active Ability
- Short-range teleport (default key: `V`) with dark particles. Only usable by the current Dark One.

### The Dark One's Dagger
- There is **only one** dagger in the entire world.
- The dagger is never duplicated by commands.
- When an operator sets a new Dark One, the existing dagger (if any) is moved to that player.
- Killing the current Dark One while holding the dagger transfers both the power and the dagger to the killer.

### Commands (requires permission level 2)
```
/darkone set <player>   - Makes the player the Dark One and moves the dagger to them
/darkone clear          - Removes the Dark One status
/darkone who            - Shows who the current Dark One is
```

## Installation

1. Install Fabric Loader for Minecraft 26.2
2. Install Fabric API for 26.2
3. Place this mod in the `mods` folder

## Notes

- Primary target: **Minecraft 26.2**
- The mod is intentionally generic and original. No external franchise content is included.
