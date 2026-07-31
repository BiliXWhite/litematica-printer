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

    /** 验证延迟（tick）：等待服务器回包确认方块是否真的被破坏 */
    private static final int VERIFY_DELAY_TICKS = 5;
    /** 最大重试次数：超过后视为破坏失败并通知玩家 */
    private static final int MAX_RETRY_COUNT = 2;
    /** 失败提示冷却（tick），避免刷屏 */
    private static final long FAILURE_NOTIFY_COOLDOWN = 100;

    private final Queue<BlockPos> breakQueue = new LinkedList<>();
    private final Set<BlockPos> breakSet = new HashSet<>(); // O(1) 查询伴侣
    private BlockPos breakPos;

    // 二次扫描验证：记录已发送破坏包但尚未确认的方块
    private final Map<BlockPos, VerifyInfo> pendingVerify = new HashMap<>();
    private long currentTick = 0;
    private long lastNotifyTick = -FAILURE_NOTIFY_COOLDOWN;

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
        if (!ConfigUtils.isPrinterEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
                breakSet.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
            if (!pendingVerify.isEmpty()) {
                pendingVerify.clear();
            }
        }
    }

    public boolean isNeedHandle() {
        // 有待破坏方块、正在破坏方块、或有待验证的方块时，都需要处理
        return !breakQueue.isEmpty() || breakPos != null || !pendingVerify.isEmpty();
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }
        currentTick++;

        // 1. 先处理验证队列：检查已破坏方块是否真的被服务器移除
        processVerificationQueue();

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
                    // 无法破坏（超出范围/不可破坏/黑名单），不进行验证，直接跳过
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
                    // 破坏完成，加入验证队列等待服务器确认
                    queueVerification(pos);
                    break;
                } else if (breakResult == BlockBreakResult.COMPLETED_WAIT) {
                    // 破坏完成但本 tick 不再处理更多，加入验证队列后继续
                    queueVerification(pos);
                } else {
                    // FAILED：破坏失败（无权限/超出边界等），加入验证队列以便重试
                    queueVerification(pos);
                }
            }
        } else {
            BlockBreakResult result = continueDestroyBlock(breakPos, Direction.DOWN);
            if (result != BlockBreakResult.IN_PROGRESS) {
                BlockPos completedPos = breakPos;
                breakPos = null;
                if (result == BlockBreakResult.COMPLETED
                        || result == BlockBreakResult.COMPLETED_WAIT
                        || result == BlockBreakResult.FAILED) {
                    queueVerification(completedPos);
                }
            }
        }
    }

    /**
     * 将方块加入验证队列，等待服务器确认是否真的被破坏。
     * 保留已有的重试计数（若存在），仅更新验证时间。
     */
    private void queueVerification(BlockPos pos) {
        if (pos == null) return;
        BlockPos immutable = pos.immutable();
        VerifyInfo existing = pendingVerify.get(immutable);
        if (existing != null) {
            existing.verifyTick = currentTick + VERIFY_DELAY_TICKS;
        } else {
            pendingVerify.put(immutable, new VerifyInfo(0, currentTick + VERIFY_DELAY_TICKS));
        }
    }

    /**
     * 二次扫描：检查已破坏方块是否真的被服务器移除。
     * 如果方块仍然存在，则重试破坏；超过最大重试次数则通知玩家。
     */
    private void processVerificationQueue() {
        if (pendingVerify.isEmpty()) return;
        ClientLevel level = client.level;
        if (level == null) return;

        List<BlockPos> toRetry = new ArrayList<>();
        List<Integer> retryCounts = new ArrayList<>();
        boolean anyFailed = false;

        Iterator<Map.Entry<BlockPos, VerifyInfo>> iterator = pendingVerify.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, VerifyInfo> entry = iterator.next();
            BlockPos pos = entry.getKey();
            VerifyInfo info = entry.getValue();

            // 还没到验证时间
            if (currentTick < info.verifyTick) continue;

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR)) {
                // 方块已被破坏，验证成功
                iterator.remove();
            } else {
                // 方块仍然存在，说明服务器未确认破坏（可能是反作弊拦截/权限不足等）
                if (info.retryCount < MAX_RETRY_COUNT) {
                    // 重试：累加重试计数，重新加入破坏队列
                    int newRetryCount = info.retryCount + 1;
                    iterator.remove();
                    toRetry.add(pos);
                    retryCounts.add(newRetryCount);
                } else {
                    // 超过最大重试次数，视为破坏失败
                    iterator.remove();
                    anyFailed = true;
                }
            }
        }

        // 重新加入破坏队列，并保留重试计数（破坏完成后 queueVerification 只更新 verifyTick）
        for (int i = 0; i < toRetry.size(); i++) {
            BlockPos pos = toRetry.get(i);
            pendingVerify.put(pos, new VerifyInfo(retryCounts.get(i), currentTick + VERIFY_DELAY_TICKS + 50));
            add(pos);
        }

        // 通知玩家破坏失败
        if (anyFailed && currentTick - lastNotifyTick >= FAILURE_NOTIFY_COOLDOWN) {
            lastNotifyTick = currentTick;
            MessageUtils.setOverlayMessage(I18n.BREAK_FAILED.getName());
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

    /** 验证信息：记录重试次数和验证时间 */
    private static class VerifyInfo {
        int retryCount;
        long verifyTick;

        VerifyInfo(int retryCount, long verifyTick) {
            this.retryCount = retryCount;
            this.verifyTick = verifyTick;
        }
    }
}
