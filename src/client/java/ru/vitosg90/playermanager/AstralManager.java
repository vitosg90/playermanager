package ru.vitosg90.playermanager;

import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AstralManager {
	private static final int DUMMY_ID = -881144;
	private static final int PULSE_INTERVAL_TICKS = 50;

	private static boolean astralActive;
	private static Vec3d anchorPos;
	private static float anchorYaw;
	private static float anchorPitch;
	private static OtherClientPlayerEntity dummyBody;
	private static int ticksSincePulse;
	private static int hudTickGate;
	private static boolean allowOneMovePacket;

	private AstralManager() {}

	public static boolean isAstralActive() {
		return astralActive;
	}

	public static void toggle(MinecraftClient client) {
		if (astralActive) {
			deactivate(client);
		} else {
			activate(client);
		}
	}

	public static void activate(MinecraftClient client) {
		if (client.player == null || client.world == null) return;
		if (astralActive) return;

		anchorPos = client.player.getPos();
		anchorYaw = client.player.getYaw();
		anchorPitch = client.player.getPitch();
		astralActive = true;
		ticksSincePulse = 0;
		hudTickGate = 0;

		spawnDummy(client);
		show(client, "Astral Projection: ON");
	}

	public static void deactivate(MinecraftClient client) {
		if (client.player == null) return;
		if (!astralActive) return;

		astralActive = false;
		ticksSincePulse = 0;
		hudTickGate = 0;
		allowOneMovePacket = false;

		if (anchorPos != null) {
			client.player.refreshPositionAndAngles(anchorPos.x, anchorPos.y, anchorPos.z, anchorYaw, anchorPitch);
		}

		removeDummy(client);
		show(client, "Astral Projection: OFF");
	}

	public static void tick(MinecraftClient client) {
		if (!astralActive) return;
		if (client.player == null || client.world == null) {
			deactivate(client);
			return;
		}

		ensureDummy(client);

		ticksSincePulse++;
		if (ticksSincePulse >= PULSE_INTERVAL_TICKS) {
			ticksSincePulse = 0;
			sendAnchorPulse(client);
		}

		hudTickGate++;
		if (hudTickGate >= 20) {
			hudTickGate = 0;
			client.player.sendMessage(Text.literal("[Astral] active"), true);
		}
	}

	public static boolean shouldCancelOutgoingPacket(Packet<?> packet) {
		if (!astralActive) return false;
		if (!(packet instanceof PlayerMoveC2SPacket)) return false;

		if (allowOneMovePacket) {
			allowOneMovePacket = false;
			return false;
		}
		return true;
	}

	public static boolean shouldIgnoreDoorLikeCollisions(Entity entity, Vec3d movement) {
		if (!astralActive || movement.lengthSquared() <= 1.0E-8) return false;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) return false;
		if (entity != client.player) return false;

		Box swept = entity.getBoundingBox().stretch(movement).expand(0.001);
		int minX = MathHelper.floor(swept.minX);
		int maxX = MathHelper.floor(swept.maxX);
		int minY = MathHelper.floor(swept.minY);
		int maxY = MathHelper.floor(swept.maxY);
		int minZ = MathHelper.floor(swept.minZ);
		int maxZ = MathHelper.floor(swept.maxZ);

		boolean touchesPassThroughBlock = false;
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					var state = client.world.getBlockState(pos);
					if (state.isAir()) continue;

					Block block = state.getBlock();
					if (isAstralPassThroughBlock(block)) {
						touchesPassThroughBlock = true;
						continue;
					}

					if (!state.getCollisionShape(client.world, pos).isEmpty()) {
						return false;
					}
				}
			}
		}

		return touchesPassThroughBlock;
	}

	private static boolean isAstralPassThroughBlock(Block block) {
		return block instanceof DoorBlock
			|| block instanceof TrapdoorBlock
			|| block instanceof FenceGateBlock
			|| block instanceof PaneBlock;
	}

	private static void sendAnchorPulse(MinecraftClient client) {
		if (anchorPos == null) return;
		ClientPlayNetworkHandler nh = client.getNetworkHandler();
		if (nh == null) return;

		allowOneMovePacket = true;
		nh.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
			anchorPos.x, anchorPos.y, anchorPos.z,
			client.player != null && client.player.isOnGround(),
			false
		));
	}

	private static void spawnDummy(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.player == null || anchorPos == null) return;

		removeDummy(client);

		dummyBody = new OtherClientPlayerEntity(world, client.player.getGameProfile());
		dummyBody.setId(DUMMY_ID);
		dummyBody.refreshPositionAndAngles(anchorPos.x, anchorPos.y, anchorPos.z, anchorYaw, anchorPitch);
		dummyBody.setPose(client.player.getPose());
		dummyBody.bodyYaw = client.player.bodyYaw;
		dummyBody.setHeadYaw(client.player.getHeadYaw());
		world.addEntity(dummyBody);
	}

	private static void ensureDummy(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null) return;
		if (dummyBody == null || dummyBody.getEntityWorld() != world) {
			spawnDummy(client);
		}
	}

	private static void removeDummy(MinecraftClient client) {
		if (client.world != null) {
			client.world.removeEntity(DUMMY_ID, Entity.RemovalReason.DISCARDED);
		}
		dummyBody = null;
	}

	private static void show(MinecraftClient client, String msg) {
		if (client.player == null) return;
		client.player.sendMessage(Text.literal("[PlayerManager] " + msg), true);
	}
}

