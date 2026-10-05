package net.scwunge.mpsaddons.registry;

import lehjr.numina.common.registration.NuminaCapabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scwunge.mpsaddons.item.module.MEWirelessTerminalModule;
import net.scwunge.mpsaddons.item.module.OreScannerModule;

public class AddonCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(NuminaCapabilities.Module.POWER_MODULE, (stack, ctx) -> new OreScannerModule.Scanner(stack), AddonItems.ORE_SCANNER.get());
        event.registerItem(NuminaCapabilities.Module.POWER_MODULE, (stack, ctx) -> new MEWirelessTerminalModule.Opener(stack), AddonItems.ME_WIRELESS_TERMINAL.get());
    }
}
