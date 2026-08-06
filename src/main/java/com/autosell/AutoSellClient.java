package com.autosell;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoSellClient implements ClientModInitializer {

    private static final String SELL_COMMAND = "sell";
    private static final int SELL_COOLDOWN_TICKS = 1;
    private static final int INVENTORY_SIZE_TO_CHECK = 36;

    private static boolean enabled = false;
    private static int cooldown = 0;
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
                    ? "§a[AutoSell] Đã BẬT - sẽ tự /sell khi kho đầy"
                    : "§c[AutoSell] Đã TẮT");
        }

        if (!enabled) return;
        if (client.player == null || client.getNetworkHandler() == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (isInventoryFull(client)) {
            client.player.networkHandler.sendChatCommand(SELL_COMMAND);
            cooldown = SELL_COOLDOWN_TICKS;
        }
    }

    private static boolean isInventoryFull(MinecraftClient client) {
        var inventory = client.player.getInventory();
        for (int i = 0; i < INVENTORY_SIZE_TO_CHECK; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
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
