package net.scwunge.mpsaddons.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Gameplay settings. This is a SERVER config so a server's values are synced to every client (tooltips and the tweak GUI
 * then match what the server enforces). Module capabilities are also built before any world exists (creative tab, item
 * tooltips on the main menu), when SERVER configs are not loaded yet, so every read goes through {@link #get} and falls
 * back to the default value.
 */
public class AddonConfig {
    public static final ModConfigSpec SPEC;

    // Ore Scanner
    public static final ModConfigSpec.BooleanValue ORE_SCANNER_ALLOWED;
    public static final ModConfigSpec.DoubleValue ORE_SCANNER_ENERGY_PER_BLOCK;
    public static final ModConfigSpec.BooleanValue ORE_SCANNER_ADVANCED_MESSAGE;
    public static final ModConfigSpec.IntValue ORE_SCANNER_MAX_RADIUS;
    public static final ModConfigSpec.IntValue ORE_SCANNER_DEFAULT_VALUE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ORE_SCANNER_VALUES;

    // ME Wireless Terminal
    public static final ModConfigSpec.BooleanValue ME_TERMINAL_ALLOWED;
    public static final ModConfigSpec.DoubleValue ME_TERMINAL_ENERGY_PER_OPEN;
    public static final ModConfigSpec.BooleanValue ME_TERMINAL_CHARGE_TERMINAL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("ore_scanner");
        ORE_SCANNER_ALLOWED = b.comment("Allow the Ore Scanner module to be installed.").define("allowed", true);
        ORE_SCANNER_ENERGY_PER_BLOCK = b.comment("FE used for every block scanned.").defineInRange("energyPerBlock", 5.0, 0.0, 100000.0);
        ORE_SCANNER_MAX_RADIUS = b.comment("Largest scan radius per axis the tweak sliders can reach.").defineInRange("maxRadius", 4, 1, 16);
        ORE_SCANNER_ADVANCED_MESSAGE = b.comment("Also show the search size and energy used in the scan message.").define("advancedMessage", true);
        ORE_SCANNER_DEFAULT_VALUE = b.comment("Value of any ore block (tag c:ores) that is not listed below.").defineInRange("defaultOreValue", 3, 0, 1000);
        ORE_SCANNER_VALUES = b.comment("Block tag = value pairs. The highest matching value of a block counts.")
                .defineListAllowEmpty("oreValues", List.of(
                        "c:ores/coal=1", "c:ores/iron=4", "c:ores/gold=6", "c:ores/redstone=3", "c:ores/diamond=16",
                        "c:ores/emerald=18", "c:ores/lapis=12", "c:ores/quartz=8", "c:ores/copper=4", "c:ores/tin=5",
                        "c:ores/silver=5", "c:ores/lead=6", "c:ores/nickel=14", "c:ores/platinum=8", "c:ores/zinc=1",
                        "c:ores/apatite=2", "c:ores/uranium=14", "c:ores/aluminum=3", "c:ores/certus_quartz=5",
                        "c:ores/netherite_scrap=30"), () -> "c:ores/iron=4", o -> o instanceof String s && s.contains("="));
        b.pop();

        b.push("me_wireless_terminal");
        ME_TERMINAL_ALLOWED = b.comment("Allow the ME Wireless Terminal module to be installed (needs Applied Energistics 2).").define("allowed", true);
        ME_TERMINAL_ENERGY_PER_OPEN = b.comment("FE used from the suit each time the terminal is opened.").defineInRange("energyPerOpen", 100.0, 0.0, 100000.0);
        ME_TERMINAL_CHARGE_TERMINAL = b.comment("Top up the terminal's own AE battery from the suit when opening it.").define("chargeTerminal", true);
        b.pop();

        SPEC = b.build();
    }

    /** The configured value, or its default while the SERVER config isn't loaded (main menu, before joining a world). */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** One (block tag, value) pair from {@link #ORE_SCANNER_VALUES}. */
    public record OreValue(TagKey<Block> tag, int value) {}

    private static volatile List<OreValue> oreValues;

    /** Parsed ore values, cached until the config changes. Bad lines are skipped. */
    public static List<OreValue> oreValues() {
        List<OreValue> cached = oreValues;
        if (cached == null) {
            List<OreValue> out = new ArrayList<>();
            for (String s : get(ORE_SCANNER_VALUES)) {
                int eq = s.lastIndexOf('=');
                if (eq < 1) {
                    continue;
                }
                try {
                    ResourceLocation id = ResourceLocation.parse(s.substring(0, eq).trim());
                    out.add(new OreValue(TagKey.create(Registries.BLOCK, id), Integer.parseInt(s.substring(eq + 1).trim())));
                } catch (RuntimeException ignored) {
                    // not a tag id or not a number: skip the line
                }
            }
            cached = List.copyOf(out);
            oreValues = cached;
        }
        return cached;
    }

    /** Drop cached values when the config is loaded, reloaded or synced from a server. */
    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            oreValues = null;
        }
    }
}
