package com.warmod.client;

import com.warmod.item.GunDefinition;
import com.warmod.item.GunRegistry;
import com.warmod.network.NetworkHandler;
import com.warmod.network.OrderPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Commander radio GUI: spawn soldiers, give squad orders, change their weapon, dismiss the squad.
 */
public class RadioScreen extends Screen {
    private final List<GunDefinition> pool = GunRegistry.firearmPool();
    private int botCount = 3;
    private int gunIndex;
    private Button spawnButton;
    private Button equipButton;

    public RadioScreen() {
        super(Component.translatable("warmod.radio.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 78;

        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            this.botCount = Math.max(1, this.botCount - 1);
            this.refreshLabels();
        }).bounds(centerX - 110, y, 20, 20).build());

        this.spawnButton = Button.builder(spawnLabel(), b ->
                NetworkHandler.sendToServer(new OrderPacket(OrderPacket.Action.SPAWN_BOTS, this.botCount))
        ).bounds(centerX - 86, y, 172, 20).build();
        this.addRenderableWidget(this.spawnButton);

        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            this.botCount = Math.min(10, this.botCount + 1);
            this.refreshLabels();
        }).bounds(centerX + 90, y, 20, 20).build());

        // orders
        int orderY = y + 26;
        this.addRenderableWidget(Button.builder(Component.translatable("warmod.order.follow"),
                b -> this.order(0)).bounds(centerX - 110, orderY, 108, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("warmod.order.hold"),
                b -> this.order(1)).bounds(centerX + 2, orderY, 108, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("warmod.order.attack"),
                b -> this.order(2)).bounds(centerX - 110, orderY + 22, 108, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("warmod.order.retreat"),
                b -> this.order(3)).bounds(centerX + 2, orderY + 22, 108, 20).build());

        this.equipButton = Button.builder(equipLabel(), b -> {
            this.gunIndex = (this.gunIndex + 1) % Math.max(1, this.pool.size());
            this.refreshLabels();
            NetworkHandler.sendToServer(new OrderPacket(OrderPacket.Action.EQUIP, this.gunIndex));
        }).bounds(centerX - 110, orderY + 50, 220, 20).build();
        this.addRenderableWidget(this.equipButton);

        this.addRenderableWidget(Button.builder(Component.translatable("warmod.radio.dismiss"),
                b -> NetworkHandler.sendToServer(new OrderPacket(OrderPacket.Action.DISMISS, 0)))
                .bounds(centerX - 110, orderY + 74, 220, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(centerX - 110, orderY + 100, 220, 20).build());
    }

    private Component spawnLabel() {
        return Component.translatable("warmod.radio.spawn", this.botCount);
    }

    private Component equipLabel() {
        GunDefinition def = this.pool.isEmpty() ? null : this.pool.get(this.gunIndex % this.pool.size());
        Component weapon = def == null ? Component.literal("-") : Component.translatable(def.nameKey());
        return Component.translatable("warmod.radio.equip", weapon);
    }

    private void refreshLabels() {
        if (this.spawnButton != null) {
            this.spawnButton.setMessage(this.spawnLabel());
        }
        if (this.equipButton != null) {
            this.equipButton.setMessage(this.equipLabel());
        }
    }

    private void order(int ordinal) {
        NetworkHandler.sendToServer(new OrderPacket(OrderPacket.Action.ORDER, ordinal));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, Component.translatable("warmod.radio.title"),
                this.width / 2, this.height / 2 - 108, 0xE8D9A0);
        String status = ClientTeamCache.inTeam
                ? Component.translatable("warmod.radio.status", ClientTeamCache.teamName,
                ClientTeamCache.bots, ClientTeamCache.teamKills).getString()
                : Component.translatable("warmod.msg.no_team").getString();
        graphics.drawCenteredString(this.font, status, this.width / 2, this.height / 2 - 94, 0x9AA5B1);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
