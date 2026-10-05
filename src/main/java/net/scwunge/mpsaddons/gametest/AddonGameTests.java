package net.scwunge.mpsaddons.gametest;

import com.mojang.authlib.GameProfile;
import lehjr.numina.common.capabilities.inventory.modularitem.IModularItem;
import lehjr.numina.common.registration.NuminaCapabilities;
import lehjr.numina.common.utils.ElectricItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.mpsaddons.MPSAddons;
import net.scwunge.mpsaddons.item.module.OreScannerModule;
import net.scwunge.mpsaddons.registry.AddonItems;

import java.util.UUID;

/**
 * Server-side checks for the addon. Run with: gradlew runGameTestServer
 */
@GameTestHolder(MPSAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public class AddonGameTests {
    static final String TEMPLATE = "empty5x4x5";

    static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    static ServerPlayer testPlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "mpsaddons-gametest"));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 1))));
        return player;
    }

    /** Power armor chestplate with a battery installed and charged, so the player has suit energy. */
    static void giveSuitEnergy(ServerPlayer player, int fe) {
        ItemStack chest = new ItemStack(item("powersuits:powerarmor_torso4"));
        IModularItem modular = NuminaCapabilities.getModularItemOrModeChangingCapability(chest);
        ItemStack battery = new ItemStack(item("numina:battery_4"));
        boolean installed = false;
        for (int i = 0; i < modular.getSlots() && !installed; i++) {
            installed = modular.isModuleValidForPlacement(i, battery) && modular.insertItem(i, battery.copy(), false).isEmpty();
        }
        if (!installed) {
            throw new GameTestAssertException("could not install a battery in the chestplate");
        }
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        IEnergyStorage energy = chest.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy == null || energy.receiveEnergy(fe, false) != fe) {
            throw new GameTestAssertException("could not charge the chestplate with " + fe + " FE");
        }
    }

    static UseOnContext clickTop(ServerPlayer player, BlockPos pos, ItemStack stack) {
        return new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = TEMPLATE)
    public static void modulesRegisterAsPowerModules(GameTestHelper helper) {
        for (Item module : new Item[]{AddonItems.ORE_SCANNER.get(), AddonItems.ME_WIRELESS_TERMINAL.get()}) {
            helper.assertTrue(new ItemStack(module).getCapability(NuminaCapabilities.Module.POWER_MODULE) != null,
                    module + " has no power module capability");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oreScannerValuesOre(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base.above(), Blocks.IRON_ORE);                 // 4
        helper.setBlock(base.east(), Blocks.DIAMOND_ORE);               // 16
        helper.setBlock(base.west(), Blocks.NETHER_GOLD_ORE);           // c:ores/gold = 6
        helper.setBlock(base.north(), Blocks.STONE);                    // 0
        OreScannerModule.ScanResult r = OreScannerModule.scan(helper.getLevel(), helper.absolutePos(base), 1, 1, 1);
        helper.assertTrue(r.total() == 4 + 16 + 6, "total ore value was " + r.total() + ", expected 26");
        helper.assertTrue(r.highest() == 16, "highest ore value was " + r.highest() + ", expected 16");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oreScannerOutOfRangeIgnored(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base.east(2), Blocks.DIAMOND_ORE);
        OreScannerModule.ScanResult r = OreScannerModule.scan(helper.getLevel(), helper.absolutePos(base), 1, 1, 1);
        helper.assertTrue(r.total() == 0, "ore outside the box was counted: " + r.total());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oreScannerUsesEnergy(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        giveSuitEnergy(player, 5000);
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base, Blocks.DIAMOND_ORE);
        ItemStack scannerStack = new ItemStack(AddonItems.ORE_SCANNER.get());
        OreScannerModule.Scanner scanner = (OreScannerModule.Scanner) scannerStack.getCapability(NuminaCapabilities.Module.POWER_MODULE);
        helper.assertTrue(scanner != null, "no scanner capability");

        double before = ElectricItemUtils.getPlayerEnergy(player);
        InteractionResult result = scanner.useOn(clickTop(player, helper.absolutePos(base), scannerStack));
        double used = before - ElectricItemUtils.getPlayerEnergy(player);
        helper.assertTrue(result.consumesAction(), "scan did not succeed: " + result);
        // default: 3x3x3 box (radius 1), 5 FE per block
        helper.assertTrue(Math.abs(used - 135) < 1, "scan used " + used + " FE, expected 135");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oreScannerNeedsEnergy(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base, Blocks.DIAMOND_ORE);
        ItemStack scannerStack = new ItemStack(AddonItems.ORE_SCANNER.get());
        OreScannerModule.Scanner scanner = (OreScannerModule.Scanner) scannerStack.getCapability(NuminaCapabilities.Module.POWER_MODULE);
        InteractionResult result = scanner.useOn(clickTop(player, helper.absolutePos(base), scannerStack));
        helper.assertTrue(!result.consumesAction(), "scan ran without any suit energy");
        helper.succeed();
    }
}
