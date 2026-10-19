package dyedresin;

import bcf.guidebook.GuideBookItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class DyedResin implements ModInitializer {
    public static final String MOD_ID = "dyedresin";

    /** Everything the mod adds in one book; given once to every player. */
    public static final Item GUIDE_BOOK = item("guide_book", props -> new GuideBookItem(props.stacksTo(1), GuideBook.BOOK));

    /** A resin clump in each dye colour: placed on any side of a block like the vanilla one, dropped the same way. */
    public static final ColorCollection<Block> CLUMP_BLOCKS = ColorCollection.VALUES.map(DyedResin::clumpBlock);
    public static final ColorCollection<Item> CLUMPS = ColorCollection.zipMap(ColorCollection.VALUES, CLUMP_BLOCKS,
            (colour, block) -> item(colour.getName() + "_resin_clump", props -> new BlockItem(block, props.useBlockDescriptionPrefix())));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), FabricCreativeModeTab.builder()
                .icon(() -> new ItemStack(CLUMPS.lightBlue()))
                .title(Component.translatable("itemGroup." + MOD_ID))
                .displayItems((params, out) -> {
                    out.accept(GUIDE_BOOK);
                    CLUMPS.forEach(out::accept);
                })
                .build());
        DevSelfTest.init();
        DevScene.init();
    }

    /** The properties of vanilla's resin clump, with the dye's map colour. */
    private static Block clumpBlock(DyeColor colour) {
        Identifier id = id(colour.getName() + "_resin_clump");
        return Registry.register(BuiltInRegistries.BLOCK, id, new MultifaceBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(colour.getMapColor()).replaceable().noCollision().sound(SoundType.RESIN).ignitedByLava()
                .pushReaction(PushReaction.DESTROY)));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        Identifier id = id(name);
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
