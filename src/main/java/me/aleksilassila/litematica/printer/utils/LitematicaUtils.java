package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "BooleanMethodIsAlwaysInverted"})
@Environment(EnvType.CLIENT)
public class LitematicaUtils {
    public static final Minecraft client = Minecraft.getInstance();
    public static final LitematicaUtils INSTANCE = new LitematicaUtils();

    private LitematicaUtils() {
    }

    public static boolean isPositionWithinRange(BlockPos pos) {
        return DataManager.getRenderLayerRange().isPositionWithinRange(pos);
    }

    public static Vec3 usePrecisionPlacement(BlockPos pos, BlockState stateSchematic) {
        if (Configs.Print.EASY_PLACE_PROTOCOL.getBooleanValue()) {
            EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();
            Vec3 hitPos = Vec3.atLowerCornerOf(pos);
            if (protocol == EasyPlaceProtocol.V3) {
                return WorldUtils.applyPlacementProtocolV3(pos, stateSchematic, hitPos);
            } else if (protocol == EasyPlaceProtocol.V2) {
                // Carpet Accurate Block placements protocol support, plus slab support
                return WorldUtils.applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
            }
        }
        return null;
    }
    /**
     * 判断位置是否位于当前加载的投影范围内。
     *
     * @param pos 要检测的方块位置
     * @return 如果位置属于图纸结构的一部分，则返回 true，否则返回 false
     */
    public static boolean isSchematicBlock(BlockPos pos) {
        SchematicPlacementManager schematicPlacementManager = DataManager.getSchematicPlacementManager();
        //#if MC < 11900
        //$$ List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingSubChunk(new SubChunkPos(pos));
        //#else
        List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingChunk(pos);
        //#endif

        for (SchematicPlacementManager.PlacementPart placementPart : allPlacementsTouchingChunk) {
            if (placementPart.getBox().containsPos(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWithinSelection1ModeRange(BlockPos pos) {
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) return false;
        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.NORMAL) {
            List<Box> arr = selection.getAllSubRegionBoxes();
            for (Box box : arr) {
                if (comparePos(box, pos)) {
                    return true;
                }
            }
            return false;
        } else {
            Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
            return comparePos(box, pos);
        }
    }

    static boolean comparePos(Box box, BlockPos pos) {
        if (box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
        PrinterBox printerBox = new PrinterBox(box.getPos1(), box.getPos2());
        return printerBox.contains(pos);
    }

    /**
     * 获取当前 Litematica 选区内所有容器方块的物品内容。
     * <p>
     * 返回结构：容器方块位置 -> (槽位 -> 物品)
     * 数据来源是投影世界（schematic world），不是主世界容器实时库存。
     */
    public static List<Object2IntOpenHashMap<ItemType>> getSelectionContainerItems() {
        List<Object2IntOpenHashMap<ItemType>> result = new ObjectArrayList<>();

        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (selection == null || schematicWorld == null) {
            return result;
        }

        List<Box> boxes;
        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.NORMAL) {
            boxes = selection.getAllSubRegionBoxes();
        } else {
            Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
            boxes = box != null ? Collections.singletonList(box) : Collections.emptyList();
        }

        for (Box box : boxes) {
            if (box == null || box.getPos1() == null || box.getPos2() == null) {
                continue;
            }

            PrinterBox printerBox = new PrinterBox(box.getPos1(), box.getPos2());
            for (BlockPos pos : printerBox) {
                if (!(schematicWorld.getBlockEntity(pos) instanceof Container container)) {
                    continue;
                }

                result.add(MaterialListUtils.getInventoryItemCounts(container));
            }
        }

        return result;
    }

}