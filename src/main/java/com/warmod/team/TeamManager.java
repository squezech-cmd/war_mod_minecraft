package com.warmod.team;

import com.warmod.Warmod;
import com.warmod.config.WarConfig;
import com.warmod.entity.SoldierEntity;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunRegistry;
import com.warmod.network.NetworkHandler;
import com.warmod.network.TeamSyncPacket;
import com.warmod.world.DimensionRegistration;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamManager {
    private TeamManager() {}

    public static TeamSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(TeamSavedData::load, TeamSavedData::new, TeamSavedData.DATA_NAME);
    }

    public static Team getTeam(MinecraftServer server, UUID uuid) {
        return server == null ? null : data(server).getByPlayer(uuid);
    }

    public static Team getTeam(Player player) {
        if (player.getServer() == null) {
            return null;
        }
        return getTeam(player.getServer(), player.getUUID());
    }

    public static BlockPos defaultSpawn(Faction faction) {
        return faction == Faction.BRAVO
                ? DimensionRegistration.BRAVO_SPAWN
                : DimensionRegistration.ALPHA_SPAWN;
    }

    public static Team createTeam(ServerPlayer player, String name, Faction faction) {
        TeamSavedData data = data(player.getServer());
        if (data.get(name) != null) {
            message(player, "warmod.msg.team_exists", name);
            return null;
        }
        if (data.getByPlayer(player.getUUID()) != null) {
            message(player, "warmod.msg.already_in_team");
            return null;
        }
        Team team = data.create(name, faction, player.getUUID(), defaultSpawn(faction));
        message(player, "warmod.msg.team_created", team.getName(), Component.translatable(faction.translationKey()));
        sync(player);
        return team;
    }

    public static boolean invite(ServerPlayer inviter, ServerPlayer target) {
        Team team = getTeam(inviter);
        if (team == null) {
            message(inviter, "warmod.msg.no_team");
            return false;
        }
        if (data(inviter.getServer()).getByPlayer(target.getUUID()) != null) {
            message(inviter, "warmod.msg.target_in_team");
            return false;
        }
        data(inviter.getServer()).getInvites().put(target.getUUID(), team.getName());
        data(inviter.getServer()).setDirty();
        message(inviter, "warmod.msg.invited", target.getName().getString());
        message(target, "warmod.msg.invite_received", team.getName(), inviter.getName().getString());
        return true;
    }

    public static boolean accept(ServerPlayer player, String teamName) {
        TeamSavedData data = data(player.getServer());
        String invited = data.getInvites().get(player.getUUID());
        Team team = data.get(teamName);
        if (invited == null || team == null || !invited.equalsIgnoreCase(team.getName())) {
            message(player, "warmod.msg.no_invite");
            return false;
        }
        Team current = data.getByPlayer(player.getUUID());
        if (current != null) {
            current.removeMember(player.getUUID());
        }
        team.addMember(player.getUUID());
        data.getInvites().remove(player.getUUID());
        data.setDirty();
        message(player, "warmod.msg.joined", team.getName());
        sync(player);
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        TeamSavedData data = data(player.getServer());
        Team team = data.getByPlayer(player.getUUID());
        if (team == null) {
            message(player, "warmod.msg.no_team");
            return false;
        }
        team.removeMember(player.getUUID());
        if (team.getMembers().isEmpty()) {
            data.remove(team.getName());
        } else if (player.getUUID().equals(team.getLeader())) {
            team.setLeader(team.getMembers().iterator().next());
        }
        data.setDirty();
        message(player, "warmod.msg.left_team");
        sync(player);
        return true;
    }

    public static BlockPos spawnFor(ServerPlayer player) {
        Team team = getTeam(player);
        if (team != null && team.getSpawn() != null) {
            return team.getSpawn();
        }
        return DimensionRegistration.ALPHA_SPAWN;
    }

    // ------------------------------------------------------------------ friend / foe

    public static String teamNameOf(Entity entity) {
        if (entity instanceof Player player) {
            if (player.getServer() == null) {
                return null;
            }
            Team team = getTeam(player.getServer(), player.getUUID());
            return team == null ? null : team.getName();
        }
        if (entity instanceof SoldierEntity soldier) {
            return soldier.getTeamName().isEmpty() ? null : soldier.getTeamName();
        }
        return null;
    }

    public static boolean isFriendly(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        String teamA = teamNameOf(a);
        String teamB = teamNameOf(b);
        return teamA != null && teamA.equalsIgnoreCase(teamB);
    }

    public static boolean areEnemies(Entity a, Entity b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        String teamA = teamNameOf(a);
        String teamB = teamNameOf(b);
        if (teamA != null && teamB != null) {
            return !teamA.equalsIgnoreCase(teamB);
        }
        boolean soldierVsPlayer = (a instanceof SoldierEntity && b instanceof Player)
                || (a instanceof Player && b instanceof SoldierEntity);
        if (soldierVsPlayer) {
            return true;
        }
        if (WarConfig.friendlyFire()) {
            return a instanceof LivingEntity && b instanceof LivingEntity;
        }
        return false;
    }

    // ------------------------------------------------------------------ bots

    public static void spawnBots(ServerPlayer player, Team team, int count) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int alive = countBots(level, team.getName());
        int limit = WarConfig.maxBotsPerTeam();
        int toSpawn = Math.max(0, Math.min(count, limit - alive));
        if (toSpawn <= 0) {
            message(player, "warmod.msg.bot_limit", limit);
            return;
        }
        List<GunDefinition> pool = GunRegistry.firearmPool();
        for (int i = 0; i < toSpawn; i++) {
            SoldierEntity soldier = new SoldierEntity(com.warmod.registry.EntityRegistry.SOLDIER.get(), level);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 3.0D + level.random.nextDouble() * 3.0D;
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;
            soldier.moveTo(x, player.getY(), z, level.random.nextFloat() * 360.0F, 0.0F);
            soldier.setTeamName(team.getName());
            soldier.setFaction(team.getFaction());
            soldier.equip(GunRegistry.firearmPool().get(level.random.nextInt(pool.size())).id());
            soldier.setOrder(SoldierEntity.Order.FOLLOW, player.blockPosition());
            level.addFreshEntity(soldier);
        }
        message(player, "warmod.msg.bots_spawned", toSpawn);
        sync(player);
    }

    public static int countBots(ServerLevel level, String teamName) {
        int count = 0;
        for (SoldierEntity soldier : level.getEntitiesOfClass(SoldierEntity.class,
                AABB.ofSize(net.minecraft.world.phys.Vec3.ZERO, 100000, 100000, 100000))) {
            if (soldier.getTeamName().equalsIgnoreCase(teamName)) {
                count++;
            }
        }
        return count;
    }

    public static List<SoldierEntity> botsOf(ServerLevel level, String teamName) {
        List<SoldierEntity> out = new ArrayList<>();
        for (SoldierEntity soldier : level.getEntitiesOfClass(SoldierEntity.class,
                AABB.ofSize(net.minecraft.world.phys.Vec3.ZERO, 100000, 100000, 100000))) {
            if (soldier.getTeamName().equalsIgnoreCase(teamName)) {
                out.add(soldier);
            }
        }
        return out;
    }

    public static void issueOrder(ServerPlayer player, Team team, SoldierEntity.Order order) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<SoldierEntity> bots = botsOf(level, team.getName());
        for (SoldierEntity soldier : bots) {
            soldier.setOrder(order, player.blockPosition());
        }
        message(player, "warmod.msg.order_given", Component.translatable(order.translationKey()), bots.size());
    }

    public static void equipBots(ServerPlayer player, Team team, int gunIndex) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<GunDefinition> pool = GunRegistry.firearmPool();
        if (gunIndex < 0 || gunIndex >= pool.size()) {
            return;
        }
        GunDefinition def = pool.get(gunIndex);
        List<SoldierEntity> bots = botsOf(level, team.getName());
        for (SoldierEntity soldier : bots) {
            soldier.equip(def.id());
        }
        message(player, "warmod.msg.bots_equipped", bots.size(), Component.translatable(def.nameKey()));
    }

    public static void dismissBots(ServerPlayer player, Team team) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<SoldierEntity> bots = botsOf(level, team.getName());
        for (SoldierEntity soldier : bots) {
            soldier.discard();
        }
        message(player, "warmod.msg.bots_dismissed", bots.size());
    }

    // ------------------------------------------------------------------ stats

    public static void onPlayerKilled(ServerPlayer victim, Entity killer) {
        TeamSavedData data = data(victim.getServer());
        data.addPlayerDeath(victim.getUUID());
        Team victimTeam = data.getByPlayer(victim.getUUID());
        if (victimTeam != null) {
            victimTeam.addDeath();
        }
        Team killerTeam = null;
        if (killer instanceof Player killerPlayer) {
            data.addPlayerKill(killerPlayer.getUUID());
            killerTeam = data.getByPlayer(killerPlayer.getUUID());
        } else if (killer instanceof SoldierEntity soldier) {
            killerTeam = data.get(soldier.getTeamName());
        }
        if (killerTeam != null && (victimTeam == null || killerTeam != victimTeam)) {
            killerTeam.addKill();
        }
        data.setDirty();
        sync(victim);
    }

    public static void onSoldierKilled(SoldierEntity soldier) {
        if (soldier.getServer() == null) {
            return;
        }
        TeamSavedData data = data(soldier.getServer());
        Team team = data.get(soldier.getTeamName());
        if (team != null) {
            team.addDeath();
            data.setDirty();
        }
    }

    public static void sync(ServerPlayer player) {
        Team team = getTeam(player);
        TeamSavedData data = data(player.getServer());
        int bots = player.level() instanceof ServerLevel level && team != null
                ? countBots(level, team.getName()) : 0;
        NetworkHandler.sendToPlayer(player, new TeamSyncPacket(
                team != null,
                team == null ? "" : team.getName(),
                team == null ? 0 : team.getFaction().ordinal(),
                team == null ? 0 : team.getKills(),
                team == null ? 0 : team.getDeaths(),
                team == null ? 0 : team.getMembers().size(),
                bots,
                data.getPlayerKills(player.getUUID()),
                data.getPlayerDeaths(player.getUUID())));
    }

    public static void message(ServerPlayer player, String key, Object... args) {
        Component message = Component.translatable(key, args).withStyle(ChatFormatting.GOLD);
        player.displayClientMessage(message, false);
    }

    public static String formatFaction(Team team) {
        return team == null ? "-" : team.getFaction().key().toUpperCase(Locale.ROOT);
    }

    /** Called from the server ticker: waves of enemy bots. */
    public static void spawnWave(MinecraftServer server) {
        ServerLevel level = server.getLevel(DimensionRegistration.BATTLEFIELD);
        if (level == null) {
            return;
        }
        List<Team> teams = new ArrayList<>(data(server).getTeams().values());
        if (teams.isEmpty()) {
            return;
        }
        Faction faction = level.random.nextBoolean() ? Faction.ALPHA : Faction.BRAVO;
        int spawned = 0;
        int budget = WarConfig.botWaveSize();
        for (Team team : teams) {
            if (team.getFaction() != faction) {
                continue;
            }
            int room = WarConfig.maxBotsPerTeam() - countBots(level, team.getName());
            for (int i = 0; i < budget && i < room; i++) {
                SoldierEntity soldier = new SoldierEntity(com.warmod.registry.EntityRegistry.SOLDIER.get(), level);
                BlockPos spawn = team.getSpawn();
                double x = spawn.getX() + (level.random.nextDouble() - 0.5D) * 12.0D;
                double z = spawn.getZ() + (level.random.nextDouble() - 0.5D) * 12.0D;
                soldier.moveTo(x, spawn.getY() + 1.0D, z, level.random.nextFloat() * 360.0F, 0.0F);
                soldier.setTeamName(team.getName());
                soldier.setFaction(team.getFaction());
                List<GunDefinition> pool = GunRegistry.firearmPool();
                soldier.equip(pool.get(level.random.nextInt(pool.size())).id());
                soldier.setOrder(SoldierEntity.Order.PATROL, spawn);
                level.addFreshEntity(soldier);
                spawned++;
            }
        }
        if (spawned > 0) {
            Warmod.LOGGER.debug("Spawned {} reinforcement bots for {}", spawned, faction.key());
        }
    }

    public static Level levelOf(Entity entity) {
        return entity.level();
    }
}
