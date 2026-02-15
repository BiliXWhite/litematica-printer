package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class BlockPosCooldownManager {
    public static BlockPosCooldownManager INSTANCE = new BlockPosCooldownManager();

    private final Map<Info, Integer> cooldownMap = new HashMap<>();

    /**
     * 冷却刻数递减核心方法（可抽离到玩家交互最顶层统一调用，无任何业务依赖）
     * 遍历所有冷却项，递减刻数，自动移除到期项（≤0）
     * Iterator遍历避免ConcurrentModificationException，适配高频调用
     */
    public void tick() {
        if (!ConfigUtils.isEnable()) {
            if (!cooldownMap.isEmpty()) {
                cooldownMap.clear();
            }
            return;
        }
        if (cooldownMap.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Info, Integer>> iterator = cooldownMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Info, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    /**
     * 设置冷却
     */
    public void setCooldown(ClientLevel level, String type, BlockPos pos, int cooldownTicks) {
        if (cooldownTicks <= 0) return;
        Identifier dimension = level.dimension().identifier();
        Info key = new Info(dimension, type, pos);
        cooldownMap.put(key, cooldownTicks);
    }

    /**
     * 判断指定方块是否处于冷却中
     *
     * @return true=冷却中，false=未冷却/无冷却
     */
    public boolean isOnCooldown(ClientLevel level, String type, BlockPos pos) {
        Identifier dimension = level.dimension().identifier();
        Info key = new Info(dimension, type, pos);
        return cooldownMap.containsKey(key);
    }

    /**
     * 手动移除指定方块的冷却（强制取消冷却）
     */
    public void removeCooldown(ClientLevel level, String type, BlockPos pos) {
        Identifier dimension = level.dimension().identifier();
        Info key = new Info(dimension, type, pos);
        cooldownMap.remove(key);
    }

    /**
     * 获取指定方块的剩余冷却刻数
     *
     * @return 剩余冷却刻数，未冷却则返回0
     */
    public int getRemainingCooldown(ClientLevel level, String type, BlockPos pos) {
        Identifier dimension = level.dimension().identifier();
        Info key = new Info(dimension, type, pos);
        return cooldownMap.getOrDefault(key, 0);
    }

    /**
     * 清空指定维度的所有冷却数据
     */
    public void clearDimensionCooldowns(ClientLevel level) {
        Identifier dimension = level.dimension().identifier();
        cooldownMap.keySet().removeIf(info -> info.dimension.equals(dimension));
    }

    /**
     * 清空指定维度+指定类型的所有冷却数据（如清空某维度所有打印冷却）
     */
    public void clearTypeCooldowns(ClientLevel level, String type) {
        Identifier dimension = level.dimension().identifier();
        cooldownMap.keySet().removeIf(info -> info.dimension.equals(dimension) && info.type.equals(type));
    }

    /**
     * 清空所有冷却数据（模组重载/退出游戏/全局重置时调用）
     */
    public void clearAllCooldowns() {
        cooldownMap.clear();
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class Info {
        private final Identifier dimension;
        private final String type;
        private final BlockPos pos;

        private Info(Identifier dimension, String type, BlockPos pos) {
            this.dimension = Objects.requireNonNull(dimension, "Dimension Identifier cannot be null!");
            this.type = Objects.requireNonNull(type, "Cool down type cannot be null!");
            this.pos = Objects.requireNonNull(pos, "BlockPos cannot be null!");
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, type, pos);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Info info = (Info) obj;
            return Objects.equals(dimension, info.dimension)
                    && Objects.equals(type, info.type)
                    && Objects.equals(pos, info.pos);
        }
    }
}