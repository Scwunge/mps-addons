package net.scwunge.mpsaddons.item.module;

import lehjr.numina.common.capabilities.module.powermodule.ModuleCategory;
import lehjr.numina.common.capabilities.module.powermodule.ModuleTarget;
import lehjr.numina.common.capabilities.module.rightclick.RightClickModule;
import lehjr.numina.common.utils.ElectricItemUtils;
import lehjr.powersuits.common.item.module.AbstractPowerModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.mpsaddons.config.AddonConfig;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the Ore Scanner from Andrew2448's Modular Powersuits Addons. Right-click a block and the module scans a box
 * that starts at the clicked face and extends into the block, then reports the total value of the ore it contains.
 * Extends MPS' module base so the tooltip shows the description and stats like every built-in module.
 */
public class OreScannerModule extends AbstractPowerModule {
    public static final String RADIUS_X = "mpsaddons.scanner.radius_x";
    public static final String RADIUS_Y = "mpsaddons.scanner.radius_y";
    public static final String RADIUS_Z = "mpsaddons.scanner.radius_z";
    public static final String ENERGY_PER_BLOCK = "mpsaddons.scanner.energy_per_block";

    private static final TagKey<Block> ANY_ORE = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));

    /** Total and highest ore value found by a scan. */
    public record ScanResult(int total, int highest) {}

    /**
     * Values every block in the box of the given radii around {@code center}. A scan is mostly stone and a few block
     * states repeated thousands of times, so each distinct state is valued once.
     */
    public static ScanResult scan(Level level, BlockPos center, int rx, int ry, int rz) {
        List<AddonConfig.OreValue> values = AddonConfig.oreValues();
        int defaultValue = AddonConfig.get(AddonConfig.ORE_SCANNER_DEFAULT_VALUE);
        Map<BlockState, Integer> seen = new IdentityHashMap<>();
        int total = 0;
        int highest = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - rx; x <= center.getX() + rx; x++) {
            for (int z = center.getZ() - rz; z <= center.getZ() + rz; z++) {
                // whole columns share a chunk, so test loading once per column
                if (!level.isLoaded(pos.set(x, center.getY(), z))) {
                    continue;
                }
                for (int y = center.getY() - ry; y <= center.getY() + ry; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    int v = seen.computeIfAbsent(state, s -> valueOf(s, values, defaultValue));
                    total += v;
                    if (v > highest) {
                        highest = v;
                    }
                }
            }
        }
        return new ScanResult(total, highest);
    }

    private static int valueOf(BlockState state, List<AddonConfig.OreValue> values, int defaultValue) {
        if (state.isAir()) {
            return 0;
        }
        int best = 0;
        for (AddonConfig.OreValue ov : values) {
            if (ov.value() > best && state.is(ov.tag())) {
                best = ov.value();
            }
        }
        if (best == 0 && state.is(ANY_ORE)) {
            best = defaultValue;
        }
        return best;
    }

    public static class Scanner extends RightClickModule {
        public Scanner(@Nonnull ItemStack module) {
            super(module, ModuleCategory.TOOL, ModuleTarget.TOOLONLY);
            addBaseProperty(ENERGY_PER_BLOCK, AddonConfig.get(AddonConfig.ORE_SCANNER_ENERGY_PER_BLOCK), "FE");
            addBaseProperty(RADIUS_X, 1, "m");
            addBaseProperty(RADIUS_Y, 1, "m");
            addBaseProperty(RADIUS_Z, 1, "m");
            int steps = Math.max(0, AddonConfig.get(AddonConfig.ORE_SCANNER_MAX_RADIUS) - 1);
            addIntTradeoffProperty(RADIUS_X, RADIUS_X, steps, "m", 1, 0);
            addIntTradeoffProperty(RADIUS_Y, RADIUS_Y, steps, "m", 1, 0);
            addIntTradeoffProperty(RADIUS_Z, RADIUS_Z, steps, "m", 1, 0);
        }

        @Override
        public boolean isAllowed() {
            return AddonConfig.get(AddonConfig.ORE_SCANNER_ALLOWED);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.FAIL;
            }
            Level level = context.getLevel();
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            int maxRadius = AddonConfig.get(AddonConfig.ORE_SCANNER_MAX_RADIUS);
            // clamp: a module tweaked on a server with a larger limit keeps its stored value
            int rx = Math.min(maxRadius, Math.max(0, (int) applyPropertyModifiers(RADIUS_X)));
            int ry = Math.min(maxRadius, Math.max(0, (int) applyPropertyModifiers(RADIUS_Y)));
            int rz = Math.min(maxRadius, Math.max(0, (int) applyPropertyModifiers(RADIUS_Z)));
            long blocks = (2L * rx + 1) * (2L * ry + 1) * (2L * rz + 1);
            int totalEnergy = (int) Math.ceil(applyPropertyModifiers(ENERGY_PER_BLOCK) * blocks);

            if (ElectricItemUtils.getPlayerEnergy(player) < totalEnergy) {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.no_energy", totalEnergy));
                return InteractionResult.FAIL;
            }

            // The box starts at the clicked face and extends into the block, as in the original.
            Direction inward = context.getClickedFace().getOpposite();
            BlockPos center = context.getClickedPos().offset(inward.getStepX() * rx, inward.getStepY() * ry, inward.getStepZ() * rz);
            ScanResult scan = scan(level, center, rx, ry, rz);

            ElectricItemUtils.drainPlayerEnergy(player, totalEnergy, false);
            if (AddonConfig.get(AddonConfig.ORE_SCANNER_ADVANCED_MESSAGE)) {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.advanced", scan.total(), scan.highest(),
                        2 * rx + 1, 2 * ry + 1, 2 * rz + 1, totalEnergy));
            } else {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.basic", scan.total(), scan.highest()));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
