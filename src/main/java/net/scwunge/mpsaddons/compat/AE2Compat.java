package net.scwunge.mpsaddons.compat;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;
import lehjr.numina.common.utils.ElectricItemUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Only loaded when Applied Energistics 2 is present. */
public final class AE2Compat {
    private AE2Compat() {}

    public enum Result { OPENED, NO_TERMINAL, NO_ENERGY, NOT_OPENED }

    /**
     * Opens the first wireless terminal in the player's inventory (AE2's own terminals and every ae2wtlib terminal, which
     * all extend {@link WirelessTerminalItem}). The terminal is charged first, because AE2 refuses to open a terminal with an
     * empty battery; the per-use cost is only taken when the terminal actually opened (not linked or out of range costs nothing).
     */
    public static Result openTerminal(Player player, int energyPerOpen, boolean chargeTerminal) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!(stack.getItem() instanceof WirelessTerminalItem terminal)) {
                continue;
            }
            if (ElectricItemUtils.getPlayerEnergy(player) < energyPerOpen) {
                return Result.NO_ENERGY;
            }

            if (chargeTerminal) {
                double missingAe = terminal.getAEMaxPower(stack) - terminal.getAECurrentPower(stack);
                if (missingAe > 0) {
                    // keep the per-use cost in reserve so charging never leaves too little to open
                    double spareFe = ElectricItemUtils.getPlayerEnergy(player) - energyPerOpen;
                    double useFe = Math.min(PowerUnit.AE.convertTo(PowerUnit.FE, missingAe), spareFe);
                    if (useFe > 0) {
                        double takenFe = ElectricItemUtils.drainPlayerEnergy(player, useFe, false);
                        terminal.injectAEPower(stack, PowerUnit.FE.convertTo(PowerUnit.AE, takenFe), Actionable.MODULATE);
                    }
                }
            }

            if (!terminal.openFromInventory(player, MenuLocators.forInventorySlot(slot))) {
                // AE2 has already told the player why (not linked, out of range, ...)
                return Result.NOT_OPENED;
            }
            ElectricItemUtils.drainPlayerEnergy(player, energyPerOpen, false);
            return Result.OPENED;
        }
        return Result.NO_TERMINAL;
    }
}
