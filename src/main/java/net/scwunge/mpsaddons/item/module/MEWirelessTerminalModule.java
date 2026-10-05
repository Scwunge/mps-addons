package net.scwunge.mpsaddons.item.module;

import lehjr.numina.common.capabilities.module.powermodule.ModuleCategory;
import lehjr.numina.common.capabilities.module.powermodule.ModuleTarget;
import lehjr.numina.common.capabilities.module.rightclick.RightClickModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.scwunge.mpsaddons.compat.AE2Compat;
import net.scwunge.mpsaddons.config.AddonConfig;

import javax.annotation.Nonnull;

/**
 * Port of the ME Wireless Terminal module. Right-click with the power fist to open an Applied Energistics 2 wireless
 * terminal from the player's inventory, topping up its battery from the suit's FE first.
 */
public class MEWirelessTerminalModule extends Item {
    public static final String ENERGY_PER_OPEN = "mpsaddons.me_terminal.energy_per_open";

    public MEWirelessTerminalModule() {
        super(new Item.Properties().stacksTo(1).setNoRepair());
    }

    public static class Opener extends RightClickModule {
        public Opener(@Nonnull ItemStack module) {
            super(module, ModuleCategory.SPECIAL, ModuleTarget.TOOLONLY);
            addBaseProperty(ENERGY_PER_OPEN, AddonConfig.ME_TERMINAL_ENERGY_PER_OPEN.get(), "FE");
        }

        @Override
        public boolean isAllowed() {
            return AddonConfig.ME_TERMINAL_ALLOWED.get() && ModList.get().isLoaded("ae2");
        }

        @Override
        public InteractionResultHolder<ItemStack> use(ItemStack stack, Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResultHolder.success(stack);
            }
            if (!ModList.get().isLoaded("ae2")) {
                return InteractionResultHolder.fail(stack);
            }
            InteractionResult result = AE2Compat.openTerminal(player, (int) applyPropertyModifiers(ENERGY_PER_OPEN),
                    AddonConfig.ME_TERMINAL_CHARGE_TERMINAL.get());
            if (result == InteractionResult.FAIL) {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.me_terminal.none"));
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.success(stack);
        }
    }
}
