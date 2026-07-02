package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

@Environment(EnvType.CLIENT)
public class LitematicaUtils {
    public static final Minecraft client = Minecraft.getInstance();

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
     * 判断位置是否位于当前加载的投影范围内，且子区域处于启用状态。
     * 仅检查子区域的启用状态，不检查渲染开启状态或渲染层范围。
     * 渲染层限制由 {@link PlayerUtils#isPositionInSelectionRange} 单独处理。
     *
     * @param pos 要检测的方块位置
     * @return 如果位置属于已启用的图纸结构部分，则返回 true，否则返回 false
     */
    public static boolean isSchematicBlock(BlockPos pos) {
        SchematicPlacementManager schematicPlacementManager = DataManager.getSchematicPlacementManager();
        //#if MC < 11900
        //$$ List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingSubChunk(new SubChunkPos(pos));
        //#else
        List<SchematicPlacementManager.PlacementPart> allPlacementsTouchingChunk = schematicPlacementManager.getAllPlacementsTouchingChunk(pos);
        //#endif

        for (SchematicPlacementManager.PlacementPart placementPart : allPlacementsTouchingChunk) {
            //#if MC >= 260200
            //$$ if (placementPart.getBox().contains(pos)) {
            //#else
            if (placementPart.getBox().containsPos(pos)) {
            //#endif
                SubRegionPlacement subRegion = getSubRegionForPlacementPart(placementPart);
                if (subRegion != null) {
                    if (subRegion.isEnabled()) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static SubRegionPlacement getSubRegionForPlacementPart(SchematicPlacementManager.PlacementPart part) {
        SchematicPlacement placement = part.getPlacement();
        String subName = part.getSubRegionName();
        if (placement != null && subName != null) {
            return placement.getRelativeSubRegionPlacement(subName);
        }
        return null;
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

    private static boolean comparePos(Box box, BlockPos pos) {
        if (box == null || box.getPos1() == null || box.getPos2() == null || pos == null) return false;
        PrinterBox printerBox = new PrinterBox(box.getPos1(), box.getPos2());
        return printerBox.contains(pos);
    }

    private static List<PrinterBox> getSelectionBoxes(AreaSelection selection) {
        if (selection == null) {
            return Collections.emptyList();
        }

        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.NORMAL) {
            return selection.getAllSubRegionBoxes().stream().map(LitematicaUtils::toPrinterBox).toList();
        }

        Box box = selection.getSubRegionBox(DataManager.getSimpleArea().getName());
        PrinterBox printerBox = toPrinterBox(box);
        return printerBox != null ? Collections.singletonList(printerBox) : Collections.emptyList();
    }

    /**
     * 获取当前投影选区的联合边界，用于裁剪迭代盒子。
     * @return 选区边界，无选区时返回 null
     */
    @Nullable
    public static PrinterBox getSelectionBounds() {
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) return null;

        List<PrinterBox> boxes = getSelectionBoxes(selection);
        if (boxes.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (PrinterBox box : boxes) {
            if (box.minX < minX) minX = box.minX;
            if (box.minY < minY) minY = box.minY;
            if (box.minZ < minZ) minZ = box.minZ;
            if (box.maxX > maxX) maxX = box.maxX;
            if (box.maxY > maxY) maxY = box.maxY;
            if (box.maxZ > maxZ) maxZ = box.maxZ;
        }

        return new PrinterBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static PrinterBox toPrinterBox(Box box) {
        if (box == null || box.getPos1() == null || box.getPos2() == null) {
            return null;
        }
        return new PrinterBox(box.getPos1(), box.getPos2());
    }

}