package net.scwunge.mpsaddons.compat;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;
import lehjr.numina.common.utils.ElectricItemUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Only loaded when Applied Energistics 2 is present. */
public final class AE2Compat {
    private AE2Compat() {}

    public static InteractionResult openTerminal(Player player, int energyPerOpen, boolean chargeTerminal) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!(stack.getItem() instanceof WirelessTerminalItem terminal)) {
                continue;
            }

            if (ElectricItemUtils.getPlayerEnergy(player) < energyPerOpen) {
                return InteractionResult.FAIL;
            }
            ElectricItemUtils.drainPlayerEnergy(player, energyPerOpen, false);

            if (chargeTerminal) {
                double missingAe = terminal.getAEMaxPower(stack) - terminal.getAECurrentPower(stack);
                if (missingAe > 0) {
                    double wantFe = PowerUnit.AE.convertTo(PowerUnit.FE, missingAe);
                    double useFe = Math.min(wantFe, ElectricItemUtils.getPlayerEnergy(player));
                    if (useFe > 0) {
                        double takenFe = ElectricItemUtils.drainPlayerEnergy(player, useFe, false);
                        terminal.injectAEPower(stack, PowerUnit.FE.convertTo(PowerUnit.AE, takenFe), Actionable.MODULATE);
                    }
                }
            }
            return terminal.openFromInventory(player, MenuLocators.forInventorySlot(slot)) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }
}
