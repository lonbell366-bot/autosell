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
    private static final int ACTION_INTERVAL_TICKS = 30; // 1.5 giay
    private static final int WORK_HOTBAR_SLOT = 8;
    private static final int MAX_SELL_ATTEMPTS_PER_BATCH = 40;
    private static final int BATCH_RESTART_COOLDOWN_TICKS = 100;

    private static boolean enabled = false;
    private static boolean sellingInProgress = false;
    private static int actionCooldown = 0;
    private static int restartCooldown = 0;
    private static int attemptsThisBatch = 0;

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
            sellingInProgress = false;
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

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        if (!sellingInProgress) {
            if (isInventoryFull(client)) {
                sellingInProgress = true;
                attemptsThisBatch = 0;
            } else {
                return;
            }
        }

        int slot = findFirstOccupiedSlot(client);

        if (slot == -1 || attemptsThisBatch >= MAX_SELL_ATTEMPTS_PER_BATCH) {
            sellingInProgress = false;
            restartCooldown = BATCH_RESTART_COOLDOWN_TICKS;
            client.setScreen(null);
            return;
        }

        var inventory = client.player.getInventory();

        if (slot < 9) {
            inventory.selectedSlot = slot;
            client.player.networkHandler.sendChatCommand(SELL_COMMAND);
            attemptsThisBatch++;
        } else {
            int syncId = client.player.playerScreenHandler.syncId;
            client.interactionManager.clickSlot(syncId, slot, WORK_HOTBAR_SLOT,
                    SlotActionType.SWAP, client.player);
        }

        actionCooldown = ACTION_INTERVAL_TICKS;
    }

    private static int findFirstOccupiedSlot(MinecraftClient client) {
        var inventory = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                return i;
            }
        }
        return -1;
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
