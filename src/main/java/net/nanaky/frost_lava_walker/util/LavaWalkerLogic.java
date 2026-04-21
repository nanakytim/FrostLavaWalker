package net.nanaky.frost_lava_walker.util;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.nanaky.frost_lava_walker.config.ServerConfigManager;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket.EventType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import java.util.Map;

public class LavaWalkerLogic {

    private static int GILDED_INITIAL_TICKS()  { return ServerConfigManager.INSTANCE.gildedInitialTicks; }
    private static int BLACKSTONE_MAIN_TICKS() { return ServerConfigManager.INSTANCE.blackstoneMainTicks; }
    private static int GILDED_WARNING_TICKS()  { return ServerConfigManager.INSTANCE.gildedWarningTicks; }
    private static int MAGMA_SHORT_TICKS()     { return ServerConfigManager.INSTANCE.magmaShortTicks; }

    private static int TOTAL_LIFECYCLE_TICKS() {
        return GILDED_INITIAL_TICKS()
            + BLACKSTONE_MAIN_TICKS()
            + GILDED_WARNING_TICKS()
            + MAGMA_SHORT_TICKS();
    }

    private static int CONVERSION_COOLDOWN_TICKS() {
        return TOTAL_LIFECYCLE_TICKS() + ServerConfigManager.INSTANCE.cooldownExtraTicks;
    }

    static final java.util.Map<BlockPos, Long> CONVERSION_COOLDOWN = new java.util.HashMap<>();
    static final java.util.Map<BlockPos, Long> CONVERSION_START    = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, BlockPos> LAST_POS = new java.util.HashMap<>();

    private static BlockState getLavaBlockState(BlockPos pos, ServerLevel level) {
        return Blocks.LAVA.defaultBlockState();
    }

    public static void pruneConversionCooldown(long now) {
        CONVERSION_COOLDOWN.entrySet().removeIf(e -> now - e.getValue() >= CONVERSION_COOLDOWN_TICKS());
        CONVERSION_START.entrySet().removeIf(e -> now - e.getValue() >= TOTAL_LIFECYCLE_TICKS() + 20);
    }

    public static void onEntityStep(ServerLevel level, LivingEntity entity) {
        int enchantLevel;
        try {
            enchantLevel = EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FROST_WALKER),
                entity
            );
        } catch (Exception e) {
            System.err.println("[LavaWalker] enchant lookup failed: " + e);
            return;
        }

        BlockPos center = entity.blockPosition();
        BlockPos last   = LAST_POS.put(entity.getUUID(), center);
        boolean movedBlock  = last != null && !last.equals(center);
        boolean justLanded  = entity.onGround() && entity.fallDistance > 0;

        if (!ServerConfigManager.INSTANCE.lavaWalkerEnabled) return;
        if (enchantLevel <= 0) return;
        if (!movedBlock && !justLanded) return;
        if (entity.isInLava()) return;
        if (!entity.onGround()) return;

        BlockPos floor = entity.getOnPos();
        int radius = ServerConfigManager.INSTANCE.baseRadius - 1 + enchantLevel;
        long now   = level.getGameTime();

        for (BlockPos candidate : BlockPos.betweenClosed(
                new BlockPos(center.getX() - radius, floor.getY(), center.getZ() - radius),
                new BlockPos(center.getX() + radius, floor.getY(), center.getZ() + radius))) {

            if (center.distSqr(candidate) > (radius + 0.5) * (radius + 0.5)) continue;

            int absDx = Math.abs(center.getX() - candidate.getX());
            int absDz = Math.abs(center.getZ() - candidate.getZ());
            if (absDx == radius && absDz == radius) continue;

            if (!level.getFluidState(candidate).is(Fluids.LAVA)) continue;
            if (!level.getFluidState(candidate).isSource()) continue;

            BlockPos immutable = candidate.immutable();

            Long lastConverted = CONVERSION_COOLDOWN.get(immutable);
            if (lastConverted != null && now - lastConverted < CONVERSION_COOLDOWN_TICKS()) continue;
            CONVERSION_COOLDOWN.put(immutable, now);
            CONVERSION_START.putIfAbsent(immutable, now);
            LavaWalkerPersistentState.get(level).startTimes.putIfAbsent(immutable, level.getGameTime());
            LavaWalkerPersistentState.get(level).setDirty();

            level.setBlock(immutable, Blocks.GILDED_BLACKSTONE.defaultBlockState(), 3);
            sendVFX(level, immutable, EventType.LAVA_CONVERSION);

            BlockConversionScheduler.schedule(
                immutable,
                Blocks.BLACKSTONE.defaultBlockState(),
                GILDED_INITIAL_TICKS(),
                level.getGameTime(),
                entity,
                level
            );
        }
    }

    public static void revertBlock(ServerLevel level, BlockPos pos, BlockState revertTo, LivingEntity entity) {
        BlockState current = level.getBlockState(pos);
        long now   = level.getGameTime();
        long START = CONVERSION_START.getOrDefault(pos, now);

        if (current.is(Blocks.GILDED_BLACKSTONE) && revertTo.is(Blocks.BLACKSTONE)) {
            level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 3);
            sendVFX(level, pos, EventType.LAVA_SETTLE);
            long warnAt  = START + GILDED_INITIAL_TICKS() + BLACKSTONE_MAIN_TICKS();
            long delayTo = Math.max(1, warnAt - now);
            BlockConversionScheduler.schedule(pos, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                (int) delayTo, now, entity, level);
            return;
        }

        if (current.is(Blocks.BLACKSTONE)) {
            level.setBlock(pos, Blocks.GILDED_BLACKSTONE.defaultBlockState(), 3);
            sendVFX(level, pos, EventType.LAVA_WARN);
            long magmaAt = START + GILDED_INITIAL_TICKS() + BLACKSTONE_MAIN_TICKS() + GILDED_WARNING_TICKS();
            long delayTo = Math.max(1, magmaAt - now);
            BlockConversionScheduler.schedule(pos, Blocks.MAGMA_BLOCK.defaultBlockState(),
                (int) delayTo, now, entity, level);
            return;
        }

        if (current.is(Blocks.GILDED_BLACKSTONE) && revertTo.is(Blocks.MAGMA_BLOCK)) {
            level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
            sendVFX(level, pos, EventType.LAVA_REVERT);
            long lavaAt  = START + GILDED_INITIAL_TICKS() + BLACKSTONE_MAIN_TICKS() + GILDED_WARNING_TICKS() + MAGMA_SHORT_TICKS();
            long delayTo = Math.max(1, lavaAt - now);
            BlockConversionScheduler.schedule(pos, getLavaBlockState(pos, level),
                (int) delayTo, now, entity, level);
            return;
        }

        if (current.is(Blocks.MAGMA_BLOCK)) {
            level.setBlock(pos, revertTo, 3);
            CONVERSION_START.remove(pos);
            LavaWalkerPersistentState.get(level).startTimes.remove(pos);
            LavaWalkerPersistentState.get(level).setDirty();
            return;
        }

        if (current.isAir()) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            killDroppedItems(level, pos);
            CONVERSION_START.remove(pos);
            LavaWalkerPersistentState.get(level).startTimes.remove(pos);
            LavaWalkerPersistentState.get(level).setDirty();
        }
    }

    private static void sendVFX(ServerLevel level, BlockPos pos, EventType event) {
        Vec3 center = Vec3.atCenterOf(pos);
        SpawnParticlePacket packet = new SpawnParticlePacket(center.x, center.y, center.z, event);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) < 64 * 64) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    public static void killDroppedItems(ServerLevel level, BlockPos pos) {
        net.minecraft.world.phys.AABB searchBox =
            new net.minecraft.world.phys.AABB(pos).inflate(1.0);
        level.getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            searchBox,
            item -> item.getItem().is(net.minecraft.world.item.Items.GILDED_BLACKSTONE)
                 || item.getItem().is(net.minecraft.world.item.Items.BLACKSTONE)
                 || item.getItem().is(net.minecraft.world.item.Items.MAGMA_BLOCK)
        ).forEach(net.minecraft.world.entity.Entity::discard);
    }

    public static void recoverFromCrash(ServerLevel level) {
        LavaWalkerPersistentState state = LavaWalkerPersistentState.get(level);
        if (state.startTimes.isEmpty()) return;

        long now = level.getGameTime();

        for (Map.Entry<BlockPos, Long> entry : new java.util.HashSet<>(state.startTimes.entrySet())) {
            BlockPos pos = entry.getKey();
            long startTick = entry.getValue();
            long elapsed = now - startTick;
            BlockState current = level.getBlockState(pos);

            if (!current.is(Blocks.GILDED_BLACKSTONE)
                    && !current.is(Blocks.BLACKSTONE)
                    && !current.is(Blocks.MAGMA_BLOCK)) {
                state.startTimes.remove(pos);
                CONVERSION_START.remove(pos);
                CONVERSION_COOLDOWN.remove(pos);
                continue;
            }
            CONVERSION_START.put(pos, startTick);
            CONVERSION_COOLDOWN.put(pos, now);

            long phase0End = GILDED_INITIAL_TICKS();
            long phase1End = phase0End + BLACKSTONE_MAIN_TICKS();
            long phase2End = phase1End + GILDED_WARNING_TICKS();
            long phase3End = phase2End + MAGMA_SHORT_TICKS();
            BlockState nextBlock;
            long remaining;

            if (current.is(Blocks.MAGMA_BLOCK)) {
                nextBlock = Blocks.LAVA.defaultBlockState();
                remaining = Math.max(1, phase3End - elapsed);
            } else if (current.is(Blocks.BLACKSTONE)) {
                nextBlock = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                remaining = Math.max(1, phase1End - elapsed);
            } else {
                // GILDED_BLACKSTONE — could be phase 0 or phase 2 (warning)
                if (elapsed < phase0End) {
                    nextBlock = Blocks.BLACKSTONE.defaultBlockState();
                    remaining = Math.max(1, phase0End - elapsed);
                } else {
                    nextBlock = Blocks.MAGMA_BLOCK.defaultBlockState();
                    remaining = Math.max(1, phase2End - elapsed);
                }
            }

            BlockConversionScheduler.schedule(pos, nextBlock, (int) remaining, now, null, level);
        }

        state.setDirty();
        System.out.println("[LavaWalker] Recovered " + state.startTimes.size() + " pending block(s).");
    }
}