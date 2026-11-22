package com.warmod.client;

import com.warmod.Warmod;
import com.warmod.item.FireMode;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunRegistry;
import com.warmod.item.ScopeType;
import com.warmod.team.Faction;
import com.warmod.util.ClientGunState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** In game HUD: ammo, fire mode, team score, ADS scope overlay. */
public final class WarHud {
    private WarHud() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.level == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        renderAmmo(graphics, minecraft, width, height);
        renderTeam(graphics, minecraft);
        renderScope(graphics, minecraft, width, height);
    }

    private static void renderAmmo(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        if (ClientGunState.gunId == null || ClientGunState.gunId.isEmpty()) {
            return;
        }
        GunDefinition def = GunRegistry.byId(ClientGunState.gunId);
        if (def == null) {
            return;
        }
        String ammo = ClientGunState.reloading ? "RELOAD" : ClientGunState.ammo + " / " + ClientGunState.magSize;
        String name = Component.translatable(def.nameKey()).getString();
        int color = ClientGunState.ammo == 0 ? 0xFF5555 : 0xE8D9A0;
        int x = width - 12;
        graphics.drawString(minecraft.font, ammo, x - minecraft.font.width(ammo), height - 40, color, true);
        graphics.drawString(minecraft.font, name, x - minecraft.font.width(name), height - 52, 0xBFC8D8, true);
        if (def.modes().size() > 1) {
            FireMode mode = FireMode.byOrdinal(ClientGunState.fireMode);
            String modeText = Component.translatable(mode.translationKey()).getString();
            graphics.drawString(minecraft.font, modeText, x - minecraft.font.width(modeText),
                    height - 28, 0x8899AA, true);
        }
    }

    private static void renderTeam(GuiGraphics graphics, Minecraft minecraft) {
        if (!ClientTeamCache.inTeam) {
            return;
        }
        Faction faction = Faction.byOrdinal(ClientTeamCache.faction);
        String line = ClientTeamCache.teamName + "  " + ClientTeamCache.teamKills + " : " + ClientTeamCache.teamDeaths;
        graphics.drawString(minecraft.font, line, 8, 8, faction.color(), true);
        String sub = "BOTS " + ClientTeamCache.bots + "  MEMBERS " + ClientTeamCache.members;
        graphics.drawString(minecraft.font, sub, 8, 19, 0x9AA5B1, true);
    }

    private static void renderScope(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        if (ClientGunState.adsProgress < 0.55F || ClientGunState.gunId.isEmpty()) {
            return;
        }
        GunDefinition def = GunRegistry.byId(ClientGunState.gunId);
        if (def == null || def.scope() == ScopeType.NONE) {
            return;
        }
        ResourceLocation texture = new ResourceLocation(Warmod.MODID, def.scopeTexture());
        graphics.blit(texture, 0, 0, 0, 0, width, height, width, height);
    }
}
