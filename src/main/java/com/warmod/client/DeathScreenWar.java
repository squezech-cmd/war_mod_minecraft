package com.warmod.client;

import com.warmod.network.NetworkHandler;
import com.warmod.network.RespawnPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * War death screen: "ПОГИБ." headline, big counters, respawn with a 3 second countdown and a
 * "leave the battlefield" button.
 */
public class DeathScreenWar extends Screen {
    private static final int COUNTDOWN_TICKS = 60;
    private int countdown = COUNTDOWN_TICKS;
    private Button respawnButton;

    public DeathScreenWar() {
        super(Component.translatable("warmod.death.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.respawnButton = Button.builder(respawnLabel(), button -> this.respawn())
                .bounds(centerX - 110, centerY + 46, 220, 22).build();
        this.respawnButton.active = false;
        this.addRenderableWidget(this.respawnButton);

        this.addRenderableWidget(Button.builder(Component.translatable("warmod.death.leave").withStyle(ChatFormatting.RED),
                button -> this.leaveBattlefield()).bounds(centerX - 110, centerY + 74, 220, 22).build());
    }

    private Component respawnLabel() {
        if (this.countdown > 0) {
            return Component.translatable("warmod.death.respawn_countdown", Mth.ceil(this.countdown / 20.0F));
        }
        return Component.translatable("warmod.death.respawn");
    }

    private void respawn() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        this.minecraft.player.respawn();
        NetworkHandler.sendToServer(new RespawnPacket(RespawnPacket.Action.RESPAWN_AT_TEAM_SPAWN));
        this.minecraft.setScreen(null);
    }

    private void leaveBattlefield() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        this.minecraft.player.respawn();
        NetworkHandler.sendToServer(new RespawnPacket(RespawnPacket.Action.LEAVE_BATTLEFIELD));
        this.minecraft.setScreen(null);
    }

    @Override
    public void tick() {
        if (this.countdown > 0) {
            this.countdown--;
            if (this.countdown == 0 && this.respawnButton != null) {
                this.respawnButton.active = true;
            }
            if (this.respawnButton != null) {
                this.respawnButton.setMessage(this.respawnLabel());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE6070709);

        int centerX = this.width / 2;
        // huge headline
        graphics.pose().pushPose();
        float scale = 3.5F;
        graphics.pose().scale(scale, scale, 1.0F);
        String title = Component.translatable("warmod.death.title").getString();
        graphics.drawString(this.font, title,
                (int) (centerX / scale - this.font.width(title) / 2.0F), (int) (this.height * 0.18F / scale),
                0xB32020, false);
        graphics.pose().popPose();

        // big counters, styled as "200."
        String kills = ClientTeamCache.personalKills + ".";
        String deaths = ClientTeamCache.personalDeaths + ".";
        graphics.pose().pushPose();
        float counterScale = 2.0F;
        graphics.pose().scale(counterScale, counterScale, 1.0F);
        int lineY = (int) (this.height * 0.38F / counterScale);
        graphics.drawString(this.font, Component.translatable("warmod.death.kills").getString() + "  " + kills,
                (int) (centerX / counterScale - 110), lineY, 0xE8D9A0, false);
        graphics.drawString(this.font, Component.translatable("warmod.death.deaths").getString() + "  " + deaths,
                (int) (centerX / counterScale - 110), lineY + 22, 0x9AA5B1, false);
        graphics.pose().popPose();

        if (ClientTeamCache.inTeam) {
            String team = ClientTeamCache.teamName + "  " + ClientTeamCache.teamKills + " : " + ClientTeamCache.teamDeaths;
            graphics.drawCenteredString(this.font, team, centerX, (int) (this.height * 0.66F),
                    ClientTeamCache.color());
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
