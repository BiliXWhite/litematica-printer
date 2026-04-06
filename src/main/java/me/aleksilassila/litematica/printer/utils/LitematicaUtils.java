package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.EasyPlaceProtocol;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

//#if MC < 11900
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "DataFlowIssue", "BooleanMethodIsAlwaysInverted"})
@Environment(EnvType.CLIENT)
public class LitematicaUtils {
    public static final Minecraft client = Minecraft.getInstance();
    public static final LitematicaUtils INSTANCE = new LitematicaUtils();

    private final Queue<BlockPos> breakQueue = new LinkedList<>();
    private BlockPos breakPos;

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

    // Add methods from LitematicaUtils
    // 常量定义：提升可读性，避免魔法值
    private static final char TAG_PREFIX = '#';
    private static final String SPLIT_SEPARATOR = ",";
    private static final String CONTAINS_FLAG = "c";

    /**
     * 字符串匹配规则：支持"包含"（c参数）或"精确相等"
     * @param targetStr 目标字符串（待匹配的字符串）
     * @param matchStr  匹配字符串（要检查的内容）
     * @param matchRules 匹配规则参数（包含"c"则启用包含匹配）
     * @return true=匹配成功，false=匹配失败
     */
    public static boolean matchString(String targetStr, String matchStr, String[] matchRules) {
        // 空值防护：任意字符串为null则直接不匹配
        if (targetStr == null || matchStr == null) {
            return false;
        }
        // 规则1：是否启用"包含"匹配（只要有一个"c"参数就启用）
        boolean enableContainsMatch = Arrays.asList(matchRules).contains(CONTAINS_FLAG);
        boolean containsMatchResult = enableContainsMatch && targetStr.contains(matchStr);
        // 规则2：精确相等匹配
        boolean exactMatchResult = targetStr.equals(matchStr);
        // 满足任一规则即匹配成功
        return containsMatchResult || exactMatchResult;
    }

    /**
     * 匹配方块名称（封装matchName，提升语义）
     */
    public static boolean matchBlockName(String expectedName, BlockState blockState) {
        return matchName(expectedName, blockState);
    }

    /**
     * 匹配物品名称（封装matchName，提升语义）
     */
    public static boolean matchItemName(String expectedName, ItemStack itemStack) {
        return matchName(expectedName, itemStack);
    }

    /**
     * 核心匹配逻辑：支持方块/物品的名称、标签、中文/拼音模糊匹配
     * @param expectedName 期望的名称（支持#开头的标签，逗号分隔规则参数）
     * @param targetObj    目标对象（仅支持BlockState/ItemStack）
     * @return true=匹配成功，false=匹配失败
     */
    public static boolean matchName(String expectedName, Object targetObj) {
        // 空值防护
        if (expectedName == null || targetObj == null) {
            return false;
        }

        // 解析期望名称和匹配规则（逗号分隔，第一个是名称，后续是规则）
        String[] nameAndRules = expectedName.split(SPLIT_SEPARATOR, -1); // -1保留空字符串，避免拆分异常
        String coreName = nameAndRules[0];
        String[] matchRules = nameAndRules.length > 1
                ? Arrays.copyOfRange(nameAndRules, 1, nameAndRules.length)
                : new String[0];

        // 获取目标对象的核心标识（注册表名称）
        String targetRegistryName = getTargetRegistryName(targetObj);
        if (targetRegistryName == null) {
            return false;
        }

        // 优先匹配标签（如果期望名称以#开头）
        if (coreName.startsWith(String.valueOf(TAG_PREFIX))) {
            String tagName = coreName.substring(1);
            // 拆分处理Block和Item的标签，避免泛型转换错误
            if (targetObj instanceof BlockState blockState) {
                return matchBlockTag(blockState, tagName, matchRules);
            } else if (targetObj instanceof ItemStack itemStack) {
                return matchItemTag(itemStack, tagName, matchRules);
            }
            return false;
        }

        // 匹配中文名称、拼音、注册表名称
        String targetDisplayName = getTargetDisplayName(targetObj);
        if (targetDisplayName == null) {
            return false;
        }

        // 中文名称匹配
        boolean displayNameMatch = matchString(targetDisplayName, coreName, matchRules);
        // 拼音匹配
        boolean pinyinMatch = PinYinSearchUtils.getPinYin(targetDisplayName)
                .stream()
                .anyMatch(pinyin -> matchString(pinyin, coreName, matchRules));
        // 注册表名称匹配
        boolean registryNameMatch = matchString(targetRegistryName, coreName, matchRules);

        // 任一匹配成功即返回 true
        return displayNameMatch || pinyinMatch || registryNameMatch;
    }

    /**
     * 获取目标对象（BlockState/ItemStack）的注册表名称
     */
    private static String getTargetRegistryName(Object targetObj) {
        if (targetObj instanceof BlockState blockState) {
            return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        } else if (targetObj instanceof ItemStack itemStack) {
            return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        }
        return null;
    }

    /**
     * 获取目标对象（BlockState/ItemStack）的显示名称（中文名称）
     */
    private static String getTargetDisplayName(Object targetObj) {
        if (targetObj instanceof BlockState blockState) {
            return blockState.getBlock().getName().getString();
        } else if (targetObj instanceof ItemStack itemStack) {
            return itemStack.getHoverName().getString();
        }
        return null;
    }

    /**
     * 匹配方块的标签（类型安全，无泛型转换）
     */
    private static boolean matchBlockTag(BlockState blockState, String tagName, String[] matchRules) {
        if (tagName.isEmpty()) {
            return false;
        }
        // 直接处理Block类型的TagKey流，无类型转换
        //#if MC >= 260100
        //$$ Stream<TagKey<Block>> blockTagStream = blockState.tags();
        //#else
        Stream<TagKey<Block>> blockTagStream = blockState.getTags();
        //#endif
        return blockTagStream
                .map(tag -> tag.location().toString())
                .anyMatch(tagFullName -> matchString(tagFullName, tagName, matchRules));
    }

    /**
     * 匹配物品的标签（类型安全，无泛型转换）
     */
    private static boolean matchItemTag(ItemStack itemStack, String tagName, String[] matchRules) {
        if (tagName.isEmpty()) {
            return false;
        }
        // 直接处理Item类型的TagKey流，无类型转换
        Stream<TagKey<Item>> itemTagStream = itemStack.getTags();
        return itemTagStream
                .map(tag -> tag.location().toString())
                .anyMatch(tagFullName -> matchString(tagFullName, tagName, matchRules));
    }

    // Add methods from LitematicaUtils
    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world == null || player == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (Configs.Break.BREAK_CHECK_HARDNESS.getBooleanValue() && currentState.getBlock().defaultDestroyTime() < 0) {
            return false;
        }
        return !currentState.isAir() &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !player.blockActionRestricted(client.level, pos, client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.Break.BREAK_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Break.BREAK_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Break.BREAK_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Break.BREAK_WHITELIST.getStrings().stream()
                        .anyMatch(string -> matchBlockName(string, blockState));
            } else {
                return true;
            }
        }
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        breakQueue.add(pos);
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public void preprocess() {
        if (!ConfigUtils.isPrinterEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
        }
    }

    public boolean isNeedHandle() {
        return !breakQueue.isEmpty() || breakPos != null;
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }
        if (breakPos == null && breakQueue.isEmpty()) {
            return;
        }
        if (breakPos == null) {
            while (!breakQueue.isEmpty()) {
                BlockPos pos = breakQueue.poll();
                if (pos == null) {
                    continue;
                }
                if (!ConfigUtils.canInteracted(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                    continue;
                }
                if (ModUtils.isTweakerooLoaded()) {
                    if (ModUtils.isToolSwitchEnabled()) {
                        ModUtils.trySwitchToEffectiveTool(pos);
                    }
                }
                if (continueDestroyBlock(pos, Direction.DOWN) == BlockBreakResult.IN_PROGRESS) {
                    breakPos = pos;
                    break;
                }
            }
        } else if (continueDestroyBlock(breakPos, Direction.DOWN) != BlockBreakResult.IN_PROGRESS) {
            breakPos = null;
            onTick();
        }
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(localPrediction, blockPos, direction);
        if (result == BlockBreakResult.IN_PROGRESS) {
            breakPos = blockPos;
        }
        return result;
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, !Configs.Break.BREAK_USE_PACKET.getBooleanValue());
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos) {
        return this.continueDestroyBlock(blockPos, Direction.DOWN);
    }

    public InteractionResult useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        return gameMode.litematica_printer$useItemOn(localPrediction, hand, blockHit);
    }

    public InteractionResult useItemOn(InteractionHand hand, BlockHitResult blockHit) {
        return this.useItemOn(!Configs.Placement.PRINT_USE_PACKET.getBooleanValue(), hand, blockHit);
    }
}

