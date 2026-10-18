package dyedresin;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dev only ({@code -Ddyedresin.dev.selftest}, `./gradlew runServer -PselfTest`): two seconds after the server
 * starts, crafts through the real recipe manager, places and breaks clumps, logs SELFTEST PASS/FAIL per check and
 * stops the server.
 */
final class DevSelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("DyedResin");
    private static int passed, failed;

    private DevSelfTest() {
    }

    static void init() {
        if (System.getProperty("dyedresin.dev.selftest") == null) {
            return;
        }
        int[] ticks = {0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks[0] == 40) {
                run(server);
            }
        });
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (DyeColor colour : DyeColor.values()) {
            Item dye = Items.DYE.pick(colour);
            Item clump = DyedResin.CLUMPS.pick(colour);
            check(colour + ": resin clump + dye", craft(level, Items.RESIN_CLUMP, dye), new ItemStack(clump));
            check(colour + ": dyed clump alone washes out", craft(level, clump), new ItemStack(Items.RESIN_CLUMP));
            DyeColor other = DyeColor.byId((colour.getId() + 5) % 16);
            check(colour + ": re-dyed " + other, craft(level, clump,
                    Items.DYE.pick(other)),
                    new ItemStack(DyedResin.CLUMPS.pick(other)));
            List<Item> ring = new ArrayList<>(Collections.nCopies(9, Items.RESIN_CLUMP));
            ring.set(4, dye);
            check(colour + ": eight around a dye", craft(level, ring.toArray(Item[]::new)), new ItemStack(clump, 8));
        }
        List<Item> mixed = new ArrayList<>(Collections.nCopies(9, DyedResin.CLUMPS.pick(DyeColor.RED)));
        mixed.set(0, Items.RESIN_CLUMP);
        mixed.set(4, Items.DYE.blue());
        check("eight mixed clumps around a dye", craft(level, mixed.toArray(Item[]::new)),
                new ItemStack(DyedResin.CLUMPS.pick(DyeColor.BLUE), 8));
        check("a plain clump alone makes nothing", craft(level, Items.RESIN_CLUMP), ItemStack.EMPTY);
        check("two dyed clumps make nothing", craft(level, DyedResin.CLUMPS.pick(DyeColor.RED),
                DyedResin.CLUMPS.pick(DyeColor.RED)), ItemStack.EMPTY);
        placeAndBreak(level);
        LOGGER.info("SELFTEST done: {} passed, {} failed", passed, failed);
        server.halt(false);
    }

    /** A fake player puts a clump on the side of a stone block and another on its top, then the clump is broken. */
    private static void placeAndBreak(ServerLevel level) {
        BlockPos stone = new BlockPos(0, 100, 0), at = stone.north();
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(stone.below().north(), Blocks.STONE.defaultBlockState());
        level.removeBlock(at, false);
        FakePlayer player = FakePlayer.get(level);
        player.setPos(0.5, 100, -3.5);
        Block block = DyedResin.CLUMP_BLOCKS.pick(DyeColor.CYAN);
        ItemStack stack = new ItemStack(DyedResin.CLUMPS.pick(DyeColor.CYAN), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0, -0.5), Direction.NORTH, stone, false)));
        BlockState state = level.getBlockState(at);
        check("placed on the side of a block", state.is(block) && MultifaceBlock.hasFace(state, Direction.SOUTH), true);
        BlockPos floor = at.below();
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        state = level.getBlockState(at);
        check("a second face on the same block", state.is(block) && MultifaceBlock.hasFace(state, Direction.DOWN)
                && MultifaceBlock.hasFace(state, Direction.SOUTH), true);
        check("two clumps used", stack.getCount() == 2, true);
        List<ItemStack> drops = Block.getDrops(state, level, at, null);
        check("broken, it drops both clumps", drops.size() == 1 && ItemStack.matches(drops.get(0),
                new ItemStack(DyedResin.CLUMPS.pick(DyeColor.CYAN), 2)), true);
        check("breaks at once", state.getDestroySpeed(level, at) == 0, true);
        level.removeBlock(at, false);
    }

    /** Crafts the items, row by row in a 3x3 grid (fewer than nine fill the first slots). */
    private static ItemStack craft(ServerLevel level, Item... items) {
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int i = 0; i < items.length; i++) {
            grid.set(i, new ItemStack(items[i]));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static void check(String name, ItemStack actual, ItemStack expected) {
        check(name + " -> " + actual, ItemStack.matches(actual, expected), true);
    }

    private static void check(String name, boolean actual, boolean expected) {
        if (actual == expected) {
            passed++;
            LOGGER.info("SELFTEST PASS {}", name);
        } else {
            failed++;
            LOGGER.info("SELFTEST FAIL {}", name);
        }
    }
}
