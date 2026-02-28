# PVP Bot API Integration Guide

## Current Status

The API classes have been created in `src/main/java/org/stepan1411/pvp_bot/api/` but need to be integrated with the existing mod code.

## What's Already Done

✅ API classes created:
- `PvpBotAPI` - Main API class
- `BotEventManager` - Event system
- `CombatStrategyRegistry` - Combat strategy registry
- Event handlers (Spawn, Death, Attack, Damage, Tick)
- Combat strategy interface

✅ Documentation created (English):
- Quick Start guide
- API Reference
- Events documentation
- Combat Strategies guide
- Examples
- FAQ

## What Needs to Be Done

### 1. Integrate Events into BotManager.java

Add event firing in these locations:

**Bot Spawn Event** (around line 330-340):
```java
// After: bots.add(name);
// Add:
try {
    org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager().fireSpawnEvent(newBot);
} catch (Exception e) {
    System.err.println("[PVP_BOT_API] Error firing spawn event: " + e.getMessage());
}
```

**Bot Death Event** (around line 455-465):
```java
// Before: bots.remove(name);
// Add:
try {
    org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager().fireDeathEvent(bot);
} catch (Exception e) {
    System.err.println("[PVP_BOT_API] Error firing death event: " + e.getMessage());
}
```

### 2. Integrate Events into BotTicker.java

**Bot Tick Event** (around line 30):
```java
for (String botName : BotManager.getAllBots()) {
    ServerPlayerEntity bot = BotManager.getBot(server, botName);
    if (bot != null && bot.isAlive()) {
        // Add:
        try {
            org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager().fireTickEvent(bot);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing tick event: " + e.getMessage());
        }
        
        // ... rest of the code
    }
}
```

### 3. Integrate Events into BotCombat.java

**Bot Attack Event** (find where bot attacks, around line 800-900):
```java
// Before attacking:
boolean cancelled = org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager()
    .fireAttackEvent(bot, target);
if (cancelled) {
    return; // Don't attack
}
```

**Bot Damage Event** (in BotDamageHandler or mixin):
```java
// When bot takes damage:
boolean cancelled = org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager()
    .fireDamageEvent(bot, attacker, damage);
if (cancelled) {
    return; // Cancel damage
}
```

### 4. Integrate Combat Strategies

In `BotCombat.java`, add strategy execution:

```java
// After selecting weapon mode, before executing combat:
List<org.stepan1411.pvp_bot.api.combat.CombatStrategy> strategies = 
    org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance().getStrategies();

for (var strategy : strategies) {
    if (strategy.canUse(bot, target, settings)) {
        boolean executed = strategy.execute(bot, target, settings, server);
        if (executed) {
            return; // Strategy handled combat
        }
    }
}

// Continue with default combat logic
```

### 5. Update Pvp_bot.java

Already done ✅ - API version is logged on startup.

## Testing the API

After integration, test with this simple addon:

```java
public class TestAddon implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Test Addon loaded!");
        System.out.println("API Version: " + PvpBotAPI.getApiVersion());
        
        // Test spawn event
        PvpBotAPI.getEventManager().registerSpawnHandler(bot -> {
            System.out.println("[TEST] Bot spawned: " + bot.getName().getString());
        });
        
        // Test death event
        PvpBotAPI.getEventManager().registerDeathHandler(bot -> {
            System.out.println("[TEST] Bot died: " + bot.getName().getString());
        });
        
        // Test attack event
        PvpBotAPI.getEventManager().registerAttackHandler((bot, target) -> {
            System.out.println("[TEST] Bot attacks: " + target.getName().getString());
            return false; // Don't cancel
        });
    }
}
```

## Building the Mod

After integration:

```bash
./gradlew build
```

The API will be included in the JAR and available to other mods.

## For Addon Developers

Once integrated, addon developers can use:

```gradle
dependencies {
    modImplementation "com.github.Stepan1411:pvp-bot-fabric:VERSION"
}
```

And access the API:

```java
import org.stepan1411.pvp_bot.api.PvpBotAPI;
import org.stepan1411.pvp_bot.api.event.*;
import org.stepan1411.pvp_bot.api.combat.*;
```

## Documentation

Full documentation is available in:
- `wiki/developer/Home.md` - Main page
- `wiki/developer/QuickStart.md` - Getting started
- `wiki/developer/APIReference.md` - Complete API reference
- `wiki/developer/Events.md` - Event system
- `wiki/developer/CombatStrategies.md` - Combat strategies
- `wiki/developer/Examples.md` - Code examples

## Next Steps

1. Integrate events as described above
2. Test with the test addon
3. Build and publish new version
4. Update wiki with integration status
5. Announce API availability to community

## Questions?

- Check `wiki/developer/FAQ.md`
- Open an issue on GitHub
- See examples in `wiki/developer/Examples.md`
