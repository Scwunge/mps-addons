package net.scwunge.mpsaddons.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.mpsaddons.MPSAddons;
import net.scwunge.mpsaddons.item.module.MEWirelessTerminalModule;
import net.scwunge.mpsaddons.item.module.OreScannerModule;

public class AddonItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MPSAddons.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MPSAddons.MOD_ID);

    public static final DeferredHolder<Item, OreScannerModule> ORE_SCANNER = ITEMS.register("ore_scanner", OreScannerModule::new);
    public static final DeferredHolder<Item, MEWirelessTerminalModule> ME_WIRELESS_TERMINAL = ITEMS.register("me_wireless_terminal_module", MEWirelessTerminalModule::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mpsaddons"))
            .icon(() -> new ItemStack(ORE_SCANNER.get()))
            .displayItems((params, output) -> {
                output.accept(ORE_SCANNER.get());
                output.accept(ME_WIRELESS_TERMINAL.get());
            }).build());
}
