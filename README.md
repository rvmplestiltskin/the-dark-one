# The Dark One

Fabric mod for **Minecraft 26.2**.

Only one player can be The Dark One at a time. The power is given by command or transferred by killing the current Dark One while holding the unique dagger.

## Features

- Passive powers while Dark One: Strength, Speed, Regeneration, Resistance, Night Vision, Fire Resistance, Water Breathing
- Teleport ability (default key `V`)
- Unique dagger (only one in the world)
- `/darkone set <player>` / `clear` / `who` (OP level 2)

## Build (GitHub Actions)

Every push runs a build for 26.2. Download the JAR from the Actions tab → Artifacts.

## Local build

Requires **Java 25**.

```bash
./gradlew build
```

Output: `build/libs/the-dark-one-1.1.0.jar`

## Important (26.2)

Minecraft 26.1+ is unobfuscated. This project uses the official Fabric Loom 1.17 template.
The current Java sources still need a full pass to official Mojang names before the first successful compile.
Once that port is done, GitHub Actions will produce a working JAR automatically.
