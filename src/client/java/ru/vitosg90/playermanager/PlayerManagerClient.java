package ru.vitosg90.playermanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public final class PlayerManagerClient implements ClientModInitializer {
	private static KeyBinding astralKey;

	@Override
	public void onInitializeClient() {
		astralKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.playermanager.astral",
			GLFW.GLFW_KEY_J,
			KeyBinding.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (astralKey.wasPressed()) {
				AstralManager.toggle(client);
			}

			AstralManager.tick(client);
		});
	}
}

