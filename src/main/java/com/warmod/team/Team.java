package com.warmod.team;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    private final String name;
    private Faction faction;
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private BlockPos spawn;
    private int kills;
    private int deaths;

    public Team(String name, Faction faction, UUID leader, BlockPos spawn) {
        this.name = name;
        this.faction = faction;
        this.leader = leader;
        this.spawn = spawn;
        this.members.add(leader);
    }

    public String getName() {
        return this.name;
    }

    public Faction getFaction() {
        return this.faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public UUID getLeader() {
        return this.leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return this.members;
    }

    public BlockPos getSpawn() {
        return this.spawn;
    }

    public void setSpawn(BlockPos spawn) {
        this.spawn = spawn;
    }

    public int getKills() {
        return this.kills;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public void addKill() {
        this.kills++;
    }

    public void addDeath() {
        this.deaths++;
    }

    public boolean isMember(UUID uuid) {
        return this.members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        this.members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        this.members.remove(uuid);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", this.name);
        tag.putString("Faction", this.faction.key());
        tag.putUUID("Leader", this.leader);
        tag.putInt("SpawnX", this.spawn.getX());
        tag.putInt("SpawnY", this.spawn.getY());
        tag.putInt("SpawnZ", this.spawn.getZ());
        tag.putInt("Kills", this.kills);
        tag.putInt("Deaths", this.deaths);
        ListTag list = new ListTag();
        for (UUID uuid : this.members) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", uuid);
            list.add(entry);
        }
        tag.put("Members", list);
        return tag;
    }

    public static Team load(CompoundTag tag) {
        String name = tag.getString("Name");
        Faction faction = Faction.byKey(tag.getString("Faction"));
        UUID leader = tag.hasUUID("Leader") ? tag.getUUID("Leader") : UUID.randomUUID();
        BlockPos spawn = new BlockPos(tag.getInt("SpawnX"), tag.getInt("SpawnY"), tag.getInt("SpawnZ"));
        Team team = new Team(name, faction, leader, spawn);
        team.kills = tag.getInt("Kills");
        team.deaths = tag.getInt("Deaths");
        ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
        team.members.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("Id")) {
                team.members.add(entry.getUUID("Id"));
            }
        }
        if (team.members.isEmpty()) {
            team.members.add(leader);
        }
        return team;
    }
}
