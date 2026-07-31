package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.MiningFilterType;
import me.aleksilassila.litematica.printer.mixin.extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin.extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class BreakUtils {
    private static final Minecraft client = Minecraft.getInstance();
    public static final BreakUtils INSTANCE = new BreakUtils();

    private final Queue<BlockPos> breakQueue = new LinkedList<>();
    private final Set<BlockPos> breakSet = new HashSet<>(); // O(1) 查询伴侣
    private BlockPos breakPos;

    // === 二次扫描验证机制 ===
    // 验证队列：记录已"完成"破坏的位置，延迟后检查方块是否真的被破坏
    private final Map<BlockPos, BreakVerificationEntry> verificationMap = new HashMap<>();
    // tick 计数器（preprocess 每 tick 递增一次）
    private long tickCounter = 0;
    // 提示冷却（避免刷屏）
    private long lastWarnTick = 0;
    // 验证延迟（tick）：等待服务器处理破坏包并回传方块更新
    private static final int VERIFY_DELAY_TICKS = 3;
    // 最大重试次数（首次破坏 + 1 次重试 = 共 2 次尝试）
    private static final int MAX_RETRY = 1;
    // 提示冷却 tick 数
    private static final int WARN_COOLDOWN_TICKS = 60;

    private static class BreakVerificationEntry {
        final BlockState originalState;  // 破坏前的方块状态
        int retryCount;                  // 已重试次数
        long verifyAtTick;               // 验证时间（tick）
        boolean pending;                 // true: 等待破坏完成; false: 等待验证

        BreakVerificationEntry(BlockState originalState, int retryCount, long verifyAtTick, boolean pending) {
            this.originalState = originalState;
            this.retryCount = retryCount;
            this.verifyAtTick = verifyAtTick;
            this.pending = pending;
        }
    }

    private BreakUtils() {}

    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = LitematicaUtils.client.level;
        LocalPlayer player = LitematicaUtils.client.player;
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
                !player.blockActionRestricted(LitematicaUtils.client.level, pos, LitematicaUtils.client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.Break.BREAK_LIMITER.getOptionListValue().equals(MiningFilterType.TWEAKEROO)) {
            if (!ModUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Break.BREAK_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Break.BREAK_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Break.BREAK_WHITELIST.getStrings().stream()
                        .anyMatch(string -> PinYinSearchUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        }
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        BlockPos immutable = pos.immutable();
        breakQueue.add(immutable);
        breakSet.add(immutable);
        // 记录原始方块状态用于二次扫描验证
        if (client.level != null) {
            BreakVerificationEntry existing = verificationMap.get(immutable);
            if (existing == null) {
                BlockState state = client.level.getBlockState(immutable);
                if (!state.isAir()) {
                    verificationMap.put(immutable, new BreakVerificationEntry(state, 0, Long.MAX_VALUE, true));
                }
            } else {
                // 已有条目（重试场景），标记为等待破坏完成
                existing.pending = true;
                existing.verifyAtTick = Long.MAX_VALUE;
            }
        }
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public boolean inQueue(BlockPos pos) {
        return breakSet.contains(pos);
    }

    public boolean inQueue(SchematicBlockContext ctx) {
        return inQueue(ctx.blockPos);
    }

    public void preprocess() {
        tickCounter++;
        if (!ConfigUtils.isPrinterEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
                breakSet.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
            // 打印机禁用时清空验证队列
            if (!verificationMap.isEmpty()) {
                verificationMap.clear();
            }
        }
    }

    public boolean isNeedHandle() {
        if (!breakQueue.isEmpty() || breakPos != null) return true;
        // 仅当存在"已到验证时间"的条目时才需要处理
        if (verificationMap.isEmpty()) return false;
        for (BreakVerificationEntry entry : verificationMap.values()) {
            if (!entry.pending && tickCounter >= entry.verifyAtTick) {
                return true;
            }
        }
        return false;
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }

        // 优先处理验证队列（二次扫描）
        processVerification();

        if (breakPos == null && breakQueue.isEmpty()) {
            return;
        }
        if (breakPos == null) {
            while (!breakQueue.isEmpty()) {
                BlockPos pos = breakQueue.poll();
                if (pos == null) {
                    continue;
                }
                breakSet.remove(pos);
                if (!PlayerUtils.canInteracted(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                    continue;
                }
                if (ModUtils.isTweakerooLoaded()) {
                    if (ModUtils.isToolSwitchEnabled()) {
                        ModUtils.trySwitchToEffectiveTool(pos);
                    }
                }
                BlockBreakResult breakResult = continueDestroyBlock(pos, Direction.DOWN);
                if (breakResult == BlockBreakResult.IN_PROGRESS) {
                    breakPos = pos;
                    break;
                } else if (breakResult == BlockBreakResult.COMPLETED) {
                    onBreakCompleted(pos);
                    break;
                } else if (breakResult == BlockBreakResult.COMPLETED_WAIT) {
                    onBreakCompleted(pos);
                    // COMPLETED_WAIT：继续处理下一个位置
                }
                // FAILED：继续处理下一个位置
            }
        } else {
            BlockBreakResult result = continueDestroyBlock(breakPos, Direction.DOWN);
            if (result != BlockBreakResult.IN_PROGRESS) {
                if (result == BlockBreakResult.COMPLETED || result == BlockBreakResult.COMPLETED_WAIT) {
                    onBreakCompleted(breakPos);
                }
                breakPos = null;
            }
        }
    }

    /**
     * 破坏完成后，设置验证时间，等待二次扫描。
     */
    private void onBreakCompleted(BlockPos pos) {
        if (pos == null) return;
        BlockPos immutable = pos.immutable();
        BreakVerificationEntry entry = verificationMap.get(immutable);
        if (entry != null) {
            entry.pending = false;
            entry.verifyAtTick = tickCounter + VERIFY_DELAY_TICKS;
        }
    }

    /**
     * 二次扫描：检查已"完成"破坏的方块是否真的被破坏。
     * 如果方块仍然存在，则重试破坏；超过最大重试次数则弹出提示。
     */
    private void processVerification() {
        if (verificationMap.isEmpty() || client.level == null) return;

        Iterator<Map.Entry<BlockPos, BreakVerificationEntry>> it = verificationMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, BreakVerificationEntry> entry = it.next();
            BlockPos pos = entry.getKey();
            BreakVerificationEntry verification = entry.getValue();

            // 等待破坏完成的不处理
            if (verification.pending) continue;
            // 还没到验证时间
            if (tickCounter < verification.verifyAtTick) continue;

            BlockState currentState = client.level.getBlockState(pos);
            // 检查方块是否仍然是原来的方块（未被破坏）
            if (currentState.getBlock() == verification.originalState.getBlock()) {
                // 方块仍然存在，破坏失败
                if (verification.retryCount < MAX_RETRY) {
                    // 第二次扫描失败，再次进行破坏
                    verification.retryCount++;
                    verification.pending = true;
                    verification.verifyAtTick = Long.MAX_VALUE;
                    breakQueue.add(pos.immutable());
                    breakSet.add(pos.immutable());
                } else {
                    // 仍然失败，弹出提示
                    if (tickCounter - lastWarnTick > WARN_COOLDOWN_TICKS) {
                        MessageUtils.setOverlayMessage(I18n.BREAK_FAILED_RETRY.getName());
                        lastWarnTick = tickCounter;
                    }
                    it.remove();
                }
            } else {
                // 方块已改变或为空气，破坏成功
                it.remove();
            }
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
}
