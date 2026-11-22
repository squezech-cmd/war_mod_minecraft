package com.warmod.team;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.warmod.entity.SoldierEntity;
import com.warmod.item.VehicleType;
import com.warmod.network.TeamPacket;
import com.warmod.world.DimensionRegistration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Locale;

/** /warmod command tree and the server side of the team / radio packets. */
public final class TeamCommands {
    private TeamCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("warmod")
                .then(Commands.literal("join").executes(context -> joinBattlefield(player(context))))
                .then(Commands.literal("leave").executes(context -> leaveBattlefield(player(context))))
                .then(Commands.literal("team")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .executes(context -> create(player(context),
                                                        StringArgumentType.getString(context, "name"),
                                                        StringArgumentType.getString(context, "faction"))))))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> invite(player(context),
                                                EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .executes(context -> accept(player(context),
                                                StringArgumentType.getString(context, "team")))))
                        .then(Commands.literal("leave").executes(context -> leave(player(context))))
                        .then(Commands.literal("info").executes(context -> info(player(context)))))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("vehicle")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(context -> spawnVehicle(player(context),
                                                StringArgumentType.getString(context, "type")))))
                        .then(Commands.literal("bots")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 32))
                                        .executes(context -> spawnBots(player(context),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("order")
                        .then(Commands.argument("order", StringArgumentType.word())
                                .executes(context -> order(player(context),
                                        StringArgumentType.getString(context, "order")))))
                .then(Commands.literal("status")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> status(context))));
    }

    /** Diagnostics: is the battlefield level alive, loaded and ticking entities? */
    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().getLevel(DimensionRegistration.BATTLEFIELD);
        source.sendSuccess(() -> Component.literal("warmod status @ " + source.getLevel().dimension().location()), false);
        if (level == null) {
            source.sendSuccess(() -> Component.literal("battlefield level: MISSING"), false);
            return 0;
        }
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(8, 8);
        BlockPos probe = new BlockPos(8, 120, 8);
        source.sendSuccess(() -> Component.literal("battlefield level: " + level.dimension().location()
                + " players=" + level.players().size()
                + " hasChunk(0,0)=" + level.getChunkSource().hasChunk(chunk.x, chunk.z)
                + " loadedChunks=" + level.getChunkSource().getLoadedChunksCount()
                + " entityTicking=" + level.isPositionEntityTicking(probe)), false);
        net.minecraft.world.entity.decoration.ArmorStand stand =
                new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, level);
        stand.moveTo(8.5D, 120.0D, 8.5D, 0.0F, 0.0F);
        stand.setNoGravity(true);
        stand.addTag("warmod_probe");
        boolean added = level.addFreshEntity(stand);
        source.sendSuccess(() -> Component.literal("probe addFreshEntity=" + added
                + " removed=" + stand.isRemoved()
                + " entitiesInArea=" + level.getEntitiesOfClass(Entity.class,
                        new net.minecraft.world.phys.AABB(4.0D, 116.0D, 4.0D, 12.0D, 124.0D, 12.0D)).size()
                + " totalEntities=" + level.getEntitiesOfClass(Entity.class,
                        new net.minecraft.world.phys.AABB(-3.0E7D, -1.0E7D, -3.0E7D, 3.0E7D, 1.0E7D, 3.0E7D)).size()), false);
        return 1;
    }

    @Nullable
    private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
        return context.getSource().getPlayer();
    }

    // ------------------------------------------------------------------ commands

    private static int create(@Nullable ServerPlayer player, String name, String faction) {
        if (player == null) {
            return 0;
        }
        TeamManager.createTeam(player, name, Faction.byKey(faction));
        return 1;
    }

    private static int invite(@Nullable ServerPlayer player, ServerPlayer target) {
        if (player == null) {
            return 0;
        }
        TeamManager.invite(player, target);
        return 1;
    }

    private static int accept(@Nullable ServerPlayer player, String team) {
        if (player == null) {
            return 0;
        }
        TeamManager.accept(player, team);
        return 1;
    }

    private static int leave(@Nullable ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        TeamManager.leave(player);
        return 1;
    }

    private static int info(@Nullable ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        Team team = TeamManager.getTeam(player);
        if (team == null) {
            TeamManager.message(player, "warmod.msg.no_team");
            return 0;
        }
        TeamManager.message(player, "warmod.msg.team_info", team.getName(),
                Component.translatable(team.getFaction().translationKey()), team.getKills(), team.getDeaths(),
                team.getMembers().size());
        return 1;
    }

    private static int spawnVehicle(@Nullable ServerPlayer player, String typeName) {
        if (player == null) {
            return 0;
        }
        VehicleType type = VehicleType.byId(typeName);
        if (type == null) {
            TeamManager.message(player, "warmod.msg.unknown_vehicle", typeName);
            return 0;
        }
        Entity vehicle = type.create(player.level());
        vehicle.moveTo(player.getX() + 2.0D, player.getY() + 0.3D, player.getZ() + 2.0D, player.getYRot(), 0.0F);
        player.level().addFreshEntity(vehicle);
        TeamManager.message(player, "warmod.msg.vehicle_spawned",
                Component.translatable("item.warmod." + type.id() + "_key"));
        return 1;
    }

    private static int spawnBots(@Nullable ServerPlayer player, int count) {
        if (player == null) {
            return 0;
        }
        Team team = TeamManager.getTeam(player);
        if (team == null) {
            TeamManager.message(player, "warmod.msg.no_team");
            return 0;
        }
        TeamManager.spawnBots(player, team, count);
        return 1;
    }

    private static int order(@Nullable ServerPlayer player, String orderName) {
        if (player == null) {
            return 0;
        }
        Team team = TeamManager.getTeam(player);
        if (team == null) {
            TeamManager.message(player, "warmod.msg.no_team");
            return 0;
        }
        SoldierEntity.Order order;
        try {
            order = SoldierEntity.Order.valueOf(orderName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            TeamManager.message(player, "warmod.msg.unknown_order", orderName);
            return 0;
        }
        TeamManager.issueOrder(player, team, order);
        return 1;
    }

    // ------------------------------------------------------------------ dimension travel

    public static int joinBattlefield(@Nullable ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.getServer() == null ? null
                : player.getServer().getLevel(DimensionRegistration.BATTLEFIELD);
        if (level == null) {
            TeamManager.message(player, "warmod.msg.no_dimension");
            return 0;
        }
        BlockPos spawn = TeamManager.spawnFor(player);
        player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        TeamManager.message(player, "warmod.msg.joined_battlefield");
        return 1;
    }

    public static int leaveBattlefield(@Nullable ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        if (player.getServer() == null) {
            return 0;
        }
        ServerLevel overworld = player.getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        TeamManager.message(player, "warmod.msg.left_battlefield");
        return 1;
    }

    // ------------------------------------------------------------------ packets

    public static void handlePacket(ServerPlayer player, TeamPacket.Action action, String arg, String arg2) {
        switch (action) {
            case CREATE -> {
                Faction faction = Faction.byKey(arg2.isEmpty() ? "alpha" : arg2);
                TeamManager.createTeam(player, arg, faction);
            }
            case INVITE -> {
                if (player.getServer() == null) {
                    return;
                }
                ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(arg);
                if (target != null) {
                    TeamManager.invite(player, target);
                } else {
                    TeamManager.message(player, "warmod.msg.player_not_found", arg);
                }
            }
            case ACCEPT -> TeamManager.accept(player, arg);
            case LEAVE -> TeamManager.leave(player);
            case INFO -> TeamManager.sync(player);
        }
    }

    public static Level levelOf(ServerPlayer player) {
        return player.level();
    }

}
