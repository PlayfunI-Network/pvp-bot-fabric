# API Integration Instructions

## Quick Integration Guide

The API classes are ready. To complete the integration, add these method calls to the existing code:

### 1. BotManager.java - Add Spawn Events

Find these lines and add the API call after them:

**Line ~334** (delayed spawn):
```java
System.out.println("[PVP_BOT] Added bot to list (delayed): " + name);
// ADD THIS:
org.stepan1411.pvp_bot.api.BotAPIIntegration.fireSpawnEvent(newBot);
```

**Line ~351** (immediate spawn):
```java
System.out.println("[PVP_BOT] Added bot to list (immediate): " + name);
// ADD THIS:
org.stepan1411.pvp_bot.api.BotAPIIntegration.fireSpawnEvent(newBot);
```

### 2. BotManager.java - Add Death Event

Find this line and add the API call BEFORE removing the bot:

**Line ~454** (before removing dead bot):
```java
boolean isDead = !bot.isAlive() || bot.getHealth() <= 0 || bot.isDead();
if (isDead) {
    // ADD THIS FIRST:
    org.stepan1411.pvp_bot.api.BotAPIIntegration.fireDeathEvent(bot);
    
    // Then remove bot:
    bots.remove(name);
    // ... rest of cleanup
}
```

### 3. BotTicker.java - Already Done ✅

Tick events are already integrated!

### 4. BotCombat.java - Add Attack Event

Find where the bot attacks (search for `bot.attack(target)` or similar) and add:

```java
// Before attacking:
boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireAttackEvent(bot, target);
if (cancelled) {
    return; // Don't attack
}

// Then do the attack
bot.attack(target);
```

### 5. Damage Handler - Add Damage Event

In the mixin or damage handler where bot takes damage:

```java
// When bot takes damage:
boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireDamageEvent(bot, attacker, damage);
if (cancelled) {
    return; // Cancel damage
}
```

## Testing

After adding these calls, test with this simple addon:

```java
public class TestAddon implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("[TEST] Addon loaded!");
        
        PvpBotAPI.getEventManager().registerSpawnHandler(bot -> {
            System.out.println("[TEST] ✅ Spawn event works! Bot: " + bot.getName().getString());
        });
        
        PvpBotAPI.getEventManager().registerDeathHandler(bot -> {
            System.out.println("[TEST] ✅ Death event works! Bot: " + bot.getName().getString());
        });
        
        PvpBotAPI.getEventManager().registerTickHandler(bot -> {
            if (bot.age % 100 == 0) { // Every 5 seconds
                System.out.println("[TEST] ✅ Tick event works! Bot: " + bot.getName().getString());
            }
        });
    }
}
```

## Build and Test

```bash
./gradlew build
```

The API will be included in the JAR and available to other mods!

## For Users

Once integrated, users can create addons like:

```java
// Just Enough Guns integration
PvpBotAPI.getEventManager().registerSpawnHandler(bot -> {
    bot.giveItemStack(new ItemStack(ModItems.ASSAULT_RIFLE));
    bot.giveItemStack(new ItemStack(ModItems.RIFLE_AMMO, 192));
});
```

## Documentation

Full API documentation is in `wiki/developer/Home.md`
