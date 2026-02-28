# PVP Bot API Test Addon

Simple test addon to verify the PVP Bot API is working correctly.

## What it does

This addon registers handlers for all API events and logs when they fire:

- ✅ **Spawn Event** - Logs when a bot spawns
- ✅ **Death Event** - Logs when a bot dies
- ✅ **Attack Event** - Logs attacks and cancels attacks on villagers
- ✅ **Damage Event** - Logs damage and cancels fall damage
- ✅ **Tick Event** - Logs every 5 seconds

## How to use

### 1. Create a new Fabric mod

Use the [Fabric Template](https://github.com/FabricMC/fabric-example-mod) as a base.

### 2. Add PVP Bot dependency

In `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    modImplementation "com.github.Stepan1411:pvp-bot-fabric:VERSION"
}
```

### 3. Copy the test addon

Copy `TestAddon.java` to your mod's source folder.

### 4. Update fabric.mod.json

```json
{
  "entrypoints": {
    "main": [
      "com.example.testaddon.TestAddon"
    ]
  },
  "depends": {
    "pvp-bot-fabric": "*"
  }
}
```

### 5. Build and test

```bash
./gradlew build
```

Copy the JAR to your `mods` folder and launch Minecraft.

## Testing

1. Start Minecraft with both PVP Bot and your test addon
2. Check logs for: "PVP Bot API Test Addon Loaded!"
3. Spawn a bot: `/pvpbot spawn TestBot`
4. Watch the logs for event messages:
   - ✅ Spawn event should fire immediately
   - ✅ Tick event should fire every 5 seconds
   - ✅ Attack event fires when bot attacks
   - ✅ Death event fires when bot dies

## Expected Output

```
═══════════════════════════════════════
  PVP Bot API Test Addon Loaded!
  API Version: 1.0.0
═══════════════════════════════════════
  All event handlers registered!
  Spawn a bot to test: /pvpbot spawn TestBot
═══════════════════════════════════════

✅ [TEST] Spawn event works!
   Bot: TestBot
   Position: Vec3d(100.5, 64.0, 200.5)
   Health: 20.0

✅ [TEST] Tick event works!
   Bot: TestBot
   Age: 100 ticks
   Health: 20.0/20.0

✅ [TEST] Attack event works!
   TestBot attacks Zombie
   
✅ [TEST] Damage event works!
   TestBot took 5.0 damage
   From: Zombie
```

## Troubleshooting

### "Cannot find symbol: PvpBotAPI"

Make sure you added the dependency in `build.gradle` and ran `./gradlew build`.

### No events firing

Check that:
1. PVP Bot mod is installed
2. Your addon is loaded (check logs)
3. API integration is complete in PVP Bot

### Events fire but with errors

Check the PVP Bot version matches your dependency version.

## Next Steps

Once you verify the API works, you can:

- Create custom combat strategies
- Add mod integrations (Just Enough Guns, etc.)
- Build advanced bot behaviors
- Create bot management tools

See the [full API documentation](../wiki/developer/Home.md) for more examples!
