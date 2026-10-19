package com.endruqq.imperium.event;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.ImperiumConfig;
import com.endruqq.imperium.command.ImperiumCommand;
import com.endruqq.imperium.entity.Allegiance;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitRegistry;
import com.endruqq.imperium.game.BotAI;
import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.game.DiplomacyManager;
import com.endruqq.imperium.game.Economy;
import com.endruqq.imperium.game.Game;
import com.endruqq.imperium.game.GameData;
import com.endruqq.imperium.game.GameManager;
import com.endruqq.imperium.game.GamePhase;
import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Province;
import com.endruqq.imperium.game.Sync;
import com.endruqq.imperium.game.TerritoryEffects;
import com.endruqq.imperium.game.WarManager;
import com.endruqq.imperium.game.building.Buildings;
import com.endruqq.imperium.game.tech.Tech;
import com.endruqq.imperium.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Imperium.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        GameData d = GameData.get(server);
        ServerLevel level = server.overworld();
        d.gameTick++;
        if (d.gameTick % 200 == 0) d.setDirty();
        try {
            if (d.phase == GamePhase.RUNNING) {
                BuildingManager.tick(level, d);
                if (d.gameTick % 20 == 0) {
                    WarManager.regenerate(d);
                    DiplomacyManager.tick(server, d);
                    com.endruqq.imperium.game.WarReasons.tick(server, d);
                    com.endruqq.imperium.game.BorderManager.tick(server, d);
                    com.endruqq.imperium.game.PeopleManager.tick(level, d);
                    com.endruqq.imperium.game.PrisonerManager.tick(level, d);
                    d.stateDirty = true;
                }
                if (d.gameTick % 40 == 0) TerritoryEffects.tick(server, d);
                long cycle = ImperiumConfig.ECONOMY_CYCLE_SECONDS.get() * 20L;
                if (d.gameTick % cycle == 0) {
                    Economy.cycle(level, d);
                    BotAI.cycle(level, d);
                }
            }
            if (d.gameTick % 100 == 0) com.endruqq.imperium.game.ChunkLoading.tick(level, d);
            Sync.tick(server, d);
        } catch (Exception e) {
            Imperium.LOGGER.error("Imperium tick failed", e);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        GameData d = GameData.get(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n != null) {
            n.memberNames.remove(p.getGameProfile().getName());
            n.memberNames.add(p.getGameProfile().getName());
            d.markChanged();
        }
        GameManager.sendFullState(p, d);
        p.sendSystemMessage(Component.translatable(n == null ? "imperium.msg.welcome_no_nation" : "imperium.msg.welcome", n == null ? "" : n.name)
                .withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Sync.setRtsMode(p, false);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ImperiumCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        UnitRegistry.clear();
        Sync.clear();
        WarManager.clear();
        BuildingManager.clearCaches();
        com.endruqq.imperium.game.BorderManager.clear();
        com.endruqq.imperium.game.ChunkLoading.clear();
    }

    // ------------------------------------------------------------------ combat rules

    private static boolean isNationEntity(Entity e) {
        return e instanceof Player || e instanceof SoldierEntity || e instanceof CitizenEntity;
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        Entity src = event.getSource().getEntity();
        if (src == null || src == victim || !isNationEntity(src) || !isNationEntity(victim)) return;
        if (src instanceof Player p && p.isCreative()) return;
        GameData d = Game.data(victim.level());
        if (d == null) return;
        int a = Allegiance.nationOf(src, d), v = Allegiance.nationOf(victim, d);
        // intruders on closed land and the owners may fight without a war
        if (d.phase == GamePhase.RUNNING && com.endruqq.imperium.game.BorderManager.borderConflict(d, src, victim)) return;
        if (a < 0 || v < 0) return;
        if (d.phase != GamePhase.RUNNING || a == v || d.allied(a, v) || !d.atWar(a, v)) {
            event.setCanceled(true);
            if (src instanceof ServerPlayer sp && a != v && !d.allied(a, v)) {
                Nation other = d.nation(v);
                sp.displayClientMessage(Component.translatable("imperium.msg.not_at_war_with", other == null ? "?" : other.name)
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity src = event.getSource().getEntity();
        GameData d = Game.data(event.getEntity().level());
        if (d == null) return;
        if (src instanceof SoldierEntity s) {
            Nation n = d.nation(s.getNationId());
            if (n != null) {
                double mult = 0.75 + n.morale() / 200.0;
                if (n.has(Tech.TACTICS)) mult *= 1.2;
                event.setAmount((float) (event.getAmount() * mult));
            }
        }
        if (event.getEntity() instanceof SoldierEntity s && s.getNationId() >= 0
                && d.nationIdAt(s.blockPosition()) == s.getNationId() && d.hasActive(s.getNationId(), Buildings.FORTRESS.id)) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }

    // ------------------------------------------------------------------ territory protection

    private static boolean mayBuildAt(GameData d, Player p, BlockPos pos) {
        if (d.phase != GamePhase.RUNNING || !ImperiumConfig.PROTECT_TERRITORY.get()) return true;
        if (p.isCreative() && p.hasPermissions(2)) return true;
        int owner = d.nationIdAt(pos);
        if (owner < 0) return true;
        int pn = d.nationIdOf(p.getUUID());
        if (pn < 0) return false;
        return d.friendly(pn, owner) || d.atWar(pn, owner);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        Player p = event.getPlayer();
        if (event.getState().is(ModBlocks.PROVINCE_CORE.get()) && !(p.isCreative() && p.isShiftKeyDown() && p.hasPermissions(2))) {
            event.setCanceled(true);
            return;
        }
        GameData d = Game.data(level);
        if (d != null && !mayBuildAt(d, p, event.getPos())) {
            event.setCanceled(true);
            Nation owner = d.nationAt(event.getPos());
            p.displayClientMessage(Component.translatable("imperium.msg.protected", owner == null ? "?" : owner.name)
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player p) || !(event.getLevel() instanceof Level level) || level.isClientSide) return;
        GameData d = Game.data(level);
        if (d != null && !mayBuildAt(d, p, event.getPos())) {
            event.setCanceled(true);
            Nation owner = d.nationAt(event.getPos());
            p.displayClientMessage(Component.translatable("imperium.msg.protected", owner == null ? "?" : owner.name)
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ building effects

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        GameData d = GameData.get(level.getServer());
        if (d.phase != GamePhase.RUNNING) return;
        int owner = d.nationIdAt(event.getPos());
        if (owner < 0) return;
        int farms = d.countActive(owner, Buildings.FARM.id);
        if (farms <= 0) return;
        double chance = Math.min(0.5, 0.15 * farms);
        if (level.random.nextDouble() < chance) event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        GameData d = GameData.get(level.getServer());
        LivingEntity cause = event.getExplosion().getIndirectSourceEntity();
        event.getAffectedBlocks().removeIf(pos -> {
            if (!level.getBlockState(pos).is(ModBlocks.PROVINCE_CORE.get())) return false;
            Province p = WarManager.provinceWithCore(d, pos);
            int attacker = Allegiance.nationOf(cause, d);
            if (p != null && WarManager.canAttackCore(d, attacker, p)) {
                WarManager.damageCore(level, p, attacker, 40, cause);
            }
            return true;
        });
    }
}
