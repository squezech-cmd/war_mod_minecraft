package com.warmod.client;

import com.warmod.network.TeamSyncPacket;

/** Client mirror of the player's team state, used by the HUD, radio screen and death screen. */
public final class ClientTeamCache {
    public static boolean inTeam;
    public static String teamName = "";
    public static int faction;
    public static int teamKills;
    public static int teamDeaths;
    public static int members;
    public static int bots;
    public static int personalKills;
    public static int personalDeaths;

    private ClientTeamCache() {}

    public static void apply(TeamSyncPacket msg) {
        inTeam = msg.inTeam();
        teamName = msg.teamName();
        faction = msg.faction();
        teamKills = msg.kills();
        teamDeaths = msg.deaths();
        members = msg.members();
        bots = msg.bots();
        personalKills = msg.personalKills();
        personalDeaths = msg.personalDeaths();
    }

    public static int color() {
        return faction == 1 ? 0xB04A3A : 0x3A6EA5;
    }
}
