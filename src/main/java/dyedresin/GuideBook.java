package dyedresin;

import bcf.guidebook.Book;
import bcf.guidebook.BookTheme;
import bcf.guidebook.Chapter;
import bcf.guidebook.Section.Break;
import bcf.guidebook.Section.Heading;
import bcf.guidebook.Section.ItemLine;
import bcf.guidebook.Section.Quote;
import bcf.guidebook.Section.Text;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * The content of the guide book, as data: chapters of sections, which the screen of bcf-guide-book-lib wraps and
 * breaks into pages. Texts are translation keys {@code dyedresin.book.<key>} in the language files. What goes into
 * which chapter is in STANDARDS.md of bcf-mod-template.
 */
public final class GuideBook {
    /** A resin-orange panel with a warm, pale page. */
    public static final Book BOOK = Book.of(DyedResin.MOD_ID, BookTheme.builder()
            .panel(0xFFD9620F, 0xFFF5A84A, 0xFF8A3A08).tabs(0xFF7A3206, 0xFFA8480C)
            .page(0xFFF7EFE4, 0xFFD6BFA4).ink(0xFF8A3A08, 0xFFC0520C, 0xFF2A1E16, 0xFF7A6656)
            .slots(0xFFEADFD0, 0xFFB09A82, 0xFFC0520C)
            .buttons(0xFFC0520C, 0xFFE57A22, 0xFF9A8676, 0xFF6A2A04, 0xFFF5A84A, 0xFF8A3A08, 0xFFFFFFFF, 0xFFE0D2C4)
            .build(), GuideBook::chapters)
            .standIns(Map.of("#dyedresin:resin_clumps", "minecraft:resin_clump",
                    "#dyedresin:dyed_resin_clumps", "dyedresin:light_blue_resin_clump"));

    private GuideBook() {
    }

    public static String key(String name) {
        return BOOK.key(name);
    }

    /** Built when the book opens: item stacks need the registries. */
    public static List<Chapter> chapters() {
        ItemStack plain = new ItemStack(Items.RESIN_CLUMP);
        List<ItemStack> dyed = DyedResin.CLUMPS.asList().stream().map(ItemStack::new).toList();
        return List.of(
                new Chapter(key("start"), new ItemStack(DyedResin.GUIDE_BOOK), Chapter.Side.RIGHT, List.of(
                        new Text(key("start.text")), new Quote(key("start.motto")),
                        new Heading(key("heading.steps")),
                        new ItemLine(List.of(plain, new ItemStack(Items.DYE.lightBlue())), key("start.step1")),
                        new ItemLine(List.of(dyed.get(11)), key("start.step2")),
                        new ItemLine(List.of(dyed.get(11), plain), key("start.step3")),
                        new Heading(key("heading.crafting")), BOOK.recipe("guide_book"),
                        new Text(key("start.recipe")))),
                new Chapter(key("clump"), dyed.get(3), Chapter.Side.LEFT, List.of(
                        new Text(key("clump.text")), new Quote(key("clump.motto")),
                        new Heading(key("heading.crafting")), BOOK.recipe("light_blue_resin_clump"),
                        new Text(key("clump.recipe")), BOOK.recipe("light_blue_resin_clump_from_eight"),
                        new Text(key("clump.recipe_eight")),
                        new Break(), new Heading(key("heading.use")),
                        new Text(key("clump.place")),
                        new ItemLine(List.of(dyed.get(14), dyed.get(4)), key("clump.redye")),
                        new Heading(key("heading.wash")), BOOK.recipe("resin_clump_from_dyed"),
                        new Text(key("clump.wash")))),
                new Chapter(key("colours"), new ItemStack(Items.DYE.magenta()), Chapter.Side.RIGHT, List.of(
                        new Text(key("colours.text")),
                        new ItemLine(dyed.subList(0, 4), key("colours.row1")),
                        new ItemLine(dyed.subList(4, 8), key("colours.row2")),
                        new ItemLine(dyed.subList(8, 12), key("colours.row3")),
                        new ItemLine(dyed.subList(12, 16), key("colours.row4")))));
    }
}
