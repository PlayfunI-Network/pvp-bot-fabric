package org.stepan1411.pvp_bot.bot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class HerobotMovement {

    public static boolean isHerobotAvailable() {
        return true;
    }

    public static boolean walkTowards(ServerPlayer bot, Vec3 targetPos) {
        Vec3 botPos = new Vec3(bot.getX(), bot.getY(), bot.getZ());
        double dx = targetPos.x - botPos.x;
        double dz = targetPos.z - botPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.5) {
            stopMovement(bot);
            return true;
        }

        dx /= dist;
        dz /= dist;

        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.setXRot(0);

        bot.setSprinting(dist > 3.0);
        bot.zza = 1.0f;
        bot.xxa = 0;

        double moveForce = bot.onGround() ? 0.1 : 0.02;
        bot.push(dx * moveForce, 0, dz * moveForce);

        double dy = targetPos.y - botPos.y;
        if (dy > 0.5 && bot.onGround()) {
            doJump(bot);
        }
        return true;
    }

    public static void stopMovement(ServerPlayer bot) {
        bot.zza = 0;
        bot.xxa = 0;
        bot.setSprinting(false);
    }

    public static void jump(ServerPlayer bot) {
        if (bot.onGround()) {
            doJump(bot);
        }
    }

    public static void sprint(ServerPlayer bot, boolean enable) {
        bot.setSprinting(enable);
    }

    public static void lookAt(ServerPlayer bot, Vec3 targetPos) {
        Vec3 botPos = bot.getEyePosition();
        double dx = targetPos.x - botPos.x;
        double dy = targetPos.y - botPos.y;
        double dz = targetPos.z - botPos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
        bot.setXRot(pitch);
    }

    public static void executeCommand(net.minecraft.server.MinecraftServer server, String command) {
        // No-op: use direct NMS instead of commands
    }

    private static void doJump(ServerPlayer bot) {
        float jumpPower = 0.42f;
        Vec3 vel = bot.getDeltaMovement();
        bot.setDeltaMovement(vel.x, jumpPower, vel.z);
        if (bot.isSprinting()) {
            float f = bot.getYRot() * ((float) Math.PI / 180.0f);
            bot.setDeltaMovement(
                bot.getDeltaMovement().x - Mth.sin(f) * 0.2,
                bot.getDeltaMovement().y,
                bot.getDeltaMovement().z + Mth.cos(f) * 0.2
            );
        }
        bot.hasImpulse = true;
    }
}
