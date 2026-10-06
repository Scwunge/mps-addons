package net.scwunge.mpsaddons;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.scwunge.mpsaddons.config.AddonConfig;
import net.scwunge.mpsaddons.registry.AddonCapabilities;
import net.scwunge.mpsaddons.registry.AddonItems;

@Mod(MPSAddons.MOD_ID)
public class MPSAddons {
    public static final String MOD_ID = "mpsaddons";

    /** Modular Powersuits' own creative tab: the modules are listed there too, next to the built-in ones. */
    private static final ResourceKey<CreativeModeTab> MPS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("powersuits", "creative.mode.tab"));

    public MPSAddons(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AddonConfig.SPEC, "mpsaddons-server.toml");
        AddonItems.ITEMS.register(modBus);
        AddonItems.TABS.register(modBus);
        modBus.addListener(AddonCapabilities::register);
        modBus.addListener(ModConfigEvent.Loading.class, AddonConfig::onConfigChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, AddonConfig::onConfigChanged);
        modBus.addListener(MPSAddons::addToMpsTab);
    }

    private static void addToMpsTab(BuildCreativeModeTabContentsEvent event) {
        if (MPS_TAB.equals(event.getTabKey())) {
            event.accept(AddonItems.ORE_SCANNER.get());
            event.accept(AddonItems.ME_WIRELESS_TERMINAL.get());
        }
    }
}
