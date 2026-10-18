package dyedresin;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/**
 * Dev only ({@code -Ddyedresin.dev.scene}, `./gradlew runClient -PdevScene`): one second after the player joins,
 * builds a pale oak wall with one clump of every colour on it, in front of the player, and turns them towards it.
 * {@link DevSceneClient} takes the screenshot.
 */
final class DevScene {
    private DevScene() {
    }

    static void init() {
        if (System.getProperty("dyedresin.dev.scene") == null) {
            return;
        }
        int[] ticks = {0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks[0] == 20 && !server.getPlayerList().getPlayers().isEmpty()) {
                build(server.overworld(), server.getPlayerList().getPlayers().get(0));
            }
        });
    }

    private static void build(ServerLevel level, ServerPlayer player) {
        BlockPos origin = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
        int floor = origin.getY();
        for (int x = -6; x <= 6; x++) {
            for (int z = -2; z <= 7; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z).atY(floor - 1), Blocks.PALE_OAK_PLANKS.defaultBlockState());
                for (int y = 0; y < 6; y++) {
                    level.setBlockAndUpdate(origin.offset(x, 0, z).atY(floor + y), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (DyeColor colour : DyeColor.values()) {
            int x = colour.getId() % 8 - 4, y = 1 - colour.getId() / 8;
            BlockPos wall = origin.offset(x, 0, 0).atY(floor + y);
            level.setBlockAndUpdate(wall, Blocks.PALE_OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(wall.south(), DyedResin.CLUMP_BLOCKS.pick(colour).defaultBlockState()
                    .setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true));
        }
        // a row of mixed clumps on the floor
        for (int x = -4; x < 4; x++) {
            level.setBlockAndUpdate(origin.offset(x, 0, 2).atY(floor), DyedResin.CLUMP_BLOCKS.pick(DyeColor.byId(x + 4 + (x % 2 == 0 ? 8 : 0)))
                    .defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
        }
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon");
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "weather clear");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DyedResin.CLUMPS.lightBlue(), 16));
        BlockPos stand = origin.offset(-1, 0, 6).atY(floor);
        player.teleportTo(level, stand.getX() + 1.0, floor, stand.getZ() + 0.5, Set.<Relative>of(), 180, 22, false);
    }
}
