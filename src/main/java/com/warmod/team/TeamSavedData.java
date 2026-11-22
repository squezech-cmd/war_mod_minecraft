package com.warmod.team;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persistent team registry stored on the overworld. */
public class TeamSavedData extends SavedData {
    public static final String DATA_NAME = "warmod_teams";

    private final Map<String, Team> teams = new LinkedHashMap<>();
    /** pending invitations: player -> team name */
    private final Map<UUID, String> invites = new HashMap<>();
    /** personal battlefield counters: player -> {kills, deaths} */
    private final Map<UUID, int[]> playerStats = new HashMap<>();

    public Map<String, Team> getTeams() {
        return this.teams;
    }

    public Team get(String name) {
        if (name == null) {
            return null;
        }
        return this.teams.get(name.toLowerCase(Locale.ROOT));
    }

    public Team getByPlayer(UUID uuid) {
        for (Team team : this.teams.values()) {
            if (team.isMember(uuid)) {
                return team;
            }
        }
        return null;
    }

    public Team create(String name, Faction faction, UUID leader, BlockPos spawn) {
        Team team = new Team(name, faction, leader, spawn);
        this.teams.put(name.toLowerCase(Locale.ROOT), team);
        this.setDirty();
        return team;
    }

    public void remove(String name) {
        this.teams.remove(name.toLowerCase(Locale.ROOT));
        this.setDirty();
    }

    public Map<UUID, String> getInvites() {
        return this.invites;
    }

    public void addPlayerKill(UUID uuid) {
        this.playerStats.computeIfAbsent(uuid, key -> new int[2])[0]++;
        this.setDirty();
    }

    public void addPlayerDeath(UUID uuid) {
        this.playerStats.computeIfAbsent(uuid, key -> new int[2])[1]++;
        this.setDirty();
    }

    public int getPlayerKills(UUID uuid) {
        int[] stats = this.playerStats.get(uuid);
        return stats == null ? 0 : stats[0];
    }

    public int getPlayerDeaths(UUID uuid) {
        int[] stats = this.playerStats.get(uuid);
        return stats == null ? 0 : stats[1];
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Team team : this.teams.values()) {
            list.add(team.save());
        }
        tag.put("Teams", list);
        ListTag inviteList = new ListTag();
        for (Map.Entry<UUID, String> entry : this.invites.entrySet()) {
            CompoundTag invite = new CompoundTag();
            invite.putUUID("Player", entry.getKey());
            invite.putString("Team", entry.getValue());
            inviteList.add(invite);
        }
        tag.put("Invites", inviteList);
        ListTag statsList = new ListTag();
        for (Map.Entry<UUID, int[]> entry : this.playerStats.entrySet()) {
            CompoundTag stats = new CompoundTag();
            stats.putUUID("Player", entry.getKey());
            stats.putInt("Kills", entry.getValue()[0]);
            stats.putInt("Deaths", entry.getValue()[1]);
            statsList.add(stats);
        }
        tag.put("PlayerStats", statsList);
        return tag;
    }

    public static TeamSavedData load(CompoundTag tag) {
        TeamSavedData data = new TeamSavedData();
        ListTag list = tag.getList("Teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Team team = Team.load(list.getCompound(i));
            data.teams.put(team.getName().toLowerCase(Locale.ROOT), team);
        }
        ListTag inviteList = tag.getList("Invites", Tag.TAG_COMPOUND);
        for (int i = 0; i < inviteList.size(); i++) {
            CompoundTag invite = inviteList.getCompound(i);
            if (invite.hasUUID("Player")) {
                data.invites.put(invite.getUUID("Player"), invite.getString("Team"));
            }
        }
        ListTag statsList = tag.getList("PlayerStats", Tag.TAG_COMPOUND);
        for (int i = 0; i < statsList.size(); i++) {
            CompoundTag stats = statsList.getCompound(i);
            if (stats.hasUUID("Player")) {
                data.playerStats.put(stats.getUUID("Player"),
                        new int[]{stats.getInt("Kills"), stats.getInt("Deaths")});
            }
        }
        return data;
    }
}
