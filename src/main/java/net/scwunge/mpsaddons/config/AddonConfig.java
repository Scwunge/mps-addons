package net.scwunge.mpsaddons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

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
}
