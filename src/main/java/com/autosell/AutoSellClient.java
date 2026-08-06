package com.autosell;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoSellClient implements ClientModInitializer {

    private static final String SELL_COMMAND = "sell";
    private static final int WORK_HOTBAR_SLOT = 8;
    private static final int RESTART_COOLDOWN_TICKS = 30; // 1,5 giay

    private static boolean enabled = false;
    private static int restartCooldown = 0;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "category.autosell"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(AutoSellClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            notifyPlayer(client, enabled
                    ? "\u00a7a[AutoSell] Da BAT"
                    : "\u00a7c[AutoSell] Da TAT");
        }

        if (!enabled) return;
        if (client.player == null || client.getNetworkHandler() == null) return;

        if (restartCooldown > 0) {
            restartCooldown--;
            return;
        }

        if (isInventoryFull(client)) {
            sellEverythingNow(client);
            restartCooldown = RESTART_COOLDOWN_TICKS;
        }
    }

    private static void sellEverythingNow(MinecraftClient client) {
        var inventory = client.player.getInventory();
        int syncId = client.player.playerScreenHandler.syncId;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (i < 9) {
                inventory.selectedSlot = i;
            } else {
                client.interactionManager.clickSlot(syncId, i, WORK_HOTBAR_SLOT,
                        SlotActionType.SWAP, client.player);
                inventory.selectedSlot = WORK_HOTBAR_SLOT;
            }

            client.player.networkHandler.sendChatCommand(SELL_COMMAND);
        }

        // Gia lap bam ESC de dong man hinh (neu dang mo GUI nao do)
        client.setScreen(null);
    }

    private static boolean isInventoryFull(MinecraftClient client) {
        var inventory = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inventory.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void notifyPlayer(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}
