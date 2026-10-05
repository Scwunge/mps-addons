package net.scwunge.mpsaddons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.scwunge.mpsaddons.config.AddonConfig;
import net.scwunge.mpsaddons.registry.AddonCapabilities;
import net.scwunge.mpsaddons.registry.AddonItems;

@Mod(MPSAddons.MOD_ID)
public class MPSAddons {
    public static final String MOD_ID = "mpsaddons";

    public MPSAddons(IEventBus modBus, ModContainer container) {
        // COMMON, not SERVER: module capabilities are built on the client's main menu (creative tab, tooltips),
        // before any world, and read these values.
        container.registerConfig(ModConfig.Type.COMMON, AddonConfig.SPEC, "mpsaddons-common.toml");
        AddonItems.ITEMS.register(modBus);
        AddonItems.TABS.register(modBus);
        modBus.addListener(AddonCapabilities::register);
    }
}
