package net.scwunge.mpsaddons.item.module;

import lehjr.numina.common.capabilities.module.powermodule.ModuleCategory;
import lehjr.numina.common.capabilities.module.powermodule.ModuleTarget;
import lehjr.numina.common.capabilities.module.rightclick.RightClickModule;
import lehjr.numina.common.utils.ElectricItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.mpsaddons.config.AddonConfig;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of the Ore Scanner from Andrew2448's Modular Powersuits Addons. Right-click a block and the module scans a box
 * that starts at the clicked face and extends into the block, then reports the total value of the ore it contains.
 */
public class OreScannerModule extends Item {
    public static final String RADIUS_X = "mpsaddons.scanner.radius_x";
    public static final String RADIUS_Y = "mpsaddons.scanner.radius_y";
    public static final String RADIUS_Z = "mpsaddons.scanner.radius_z";
    public static final String ENERGY_PER_BLOCK = "mpsaddons.scanner.energy_per_block";

    public OreScannerModule() {
        super(new Item.Properties().stacksTo(1).setNoRepair());
    }

    /** One (block tag, value) pair parsed from the config. */
    private record OreValue(TagKey<Block> tag, int value) {}

    private static List<OreValue> parseValues() {
        List<OreValue> out = new ArrayList<>();
        for (String s : AddonConfig.ORE_SCANNER_VALUES.get()) {
            int eq = s.lastIndexOf('=');
            if (eq < 1) {
                continue;
            }
            try {
                ResourceLocation id = ResourceLocation.parse(s.substring(0, eq).trim());
                out.add(new OreValue(TagKey.create(Registries.BLOCK, id), Integer.parseInt(s.substring(eq + 1).trim())));
            } catch (RuntimeException ignored) {
                // bad config line: skip it
            }
        }
        return out;
    }

    private static int valueOf(BlockState state, List<OreValue> values, TagKey<Block> anyOre) {
        int best = 0;
        for (OreValue ov : values) {
            if (state.is(ov.tag()) && ov.value() > best) {
                best = ov.value();
            }
        }
        if (best == 0 && state.is(anyOre)) {
            best = AddonConfig.ORE_SCANNER_DEFAULT_VALUE.get();
        }
        return best;
    }

    /** Total and highest ore value inside the box of the given radii around {@code center}. */
    public record ScanResult(int total, int highest) {}

    public static ScanResult scan(Level level, BlockPos center, int rx, int ry, int rz) {
        List<OreValue> values = parseValues();
        TagKey<Block> anyOre = TagKey.create(Registries.BLOCK, ResourceLocation.parse("c:ores"));
        int total = 0;
        int highest = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - rx; x <= center.getX() + rx; x++) {
            for (int y = center.getY() - ry; y <= center.getY() + ry; y++) {
                for (int z = center.getZ() - rz; z <= center.getZ() + rz; z++) {
                    pos.set(x, y, z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    int v = valueOf(level.getBlockState(pos), values, anyOre);
                    total += v;
                    if (v > highest) {
                        highest = v;
                    }
                }
            }
        }
        return new ScanResult(total, highest);
    }

    public static class Scanner extends RightClickModule {
        public Scanner(@Nonnull ItemStack module) {
            super(module, ModuleCategory.TOOL, ModuleTarget.TOOLONLY);
            addBaseProperty(ENERGY_PER_BLOCK, AddonConfig.ORE_SCANNER_ENERGY_PER_BLOCK.get(), "FE");
            addBaseProperty(RADIUS_X, 1, "m");
            addBaseProperty(RADIUS_Y, 1, "m");
            addBaseProperty(RADIUS_Z, 1, "m");
            int steps = Math.max(0, AddonConfig.ORE_SCANNER_MAX_RADIUS.get() - 1);
            addIntTradeoffProperty(RADIUS_X, RADIUS_X, steps, "m", 1, 0);
            addIntTradeoffProperty(RADIUS_Y, RADIUS_Y, steps, "m", 1, 0);
            addIntTradeoffProperty(RADIUS_Z, RADIUS_Z, steps, "m", 1, 0);
        }

        @Override
        public boolean isAllowed() {
            return AddonConfig.ORE_SCANNER_ALLOWED.get();
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

            int rx = (int) applyPropertyModifiers(RADIUS_X);
            int ry = (int) applyPropertyModifiers(RADIUS_Y);
            int rz = (int) applyPropertyModifiers(RADIUS_Z);
            double perBlock = applyPropertyModifiers(ENERGY_PER_BLOCK);
            long blocks = (2L * rx + 1) * (2L * ry + 1) * (2L * rz + 1);
            int totalEnergy = (int) Math.ceil(perBlock * blocks);

            if (ElectricItemUtils.getPlayerEnergy(player) < totalEnergy) {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.no_energy", totalEnergy));
                return InteractionResult.FAIL;
            }

            // The box starts at the clicked face and extends into the block, as in the original.
            Direction inward = context.getClickedFace().getOpposite();
            BlockPos origin = context.getClickedPos();
            BlockPos center = origin.offset(inward.getStepX() * rx, inward.getStepY() * ry, inward.getStepZ() * rz);

            ScanResult scan = scan(level, center, rx, ry, rz);
            int total = scan.total();
            int highest = scan.highest();

            ElectricItemUtils.drainPlayerEnergy(player, totalEnergy, false);
            if (AddonConfig.ORE_SCANNER_ADVANCED_MESSAGE.get()) {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.advanced", total, highest,
                        2 * rx + 1, 2 * ry + 1, 2 * rz + 1, totalEnergy));
            } else {
                player.sendSystemMessage(Component.translatable("message.mpsaddons.scanner.basic", total, highest));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
