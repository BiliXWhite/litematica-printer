package me.aleksilassila.litematica.printer.utils.bedrock;

import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.utils.ModUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public class BedrockUtils {
    private static Miner bedrockMiner;

    static {
        try {
            if (ModUtils.isBlockMinerLoaded()) {
                bedrockMiner = new BlockMiner();
            } else if (ModUtils.isBedrockMinerLoaded()) {
                bedrockMiner = new BedrockMiner();
            } else {
                bedrockMiner = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            bedrockMiner = null;
        }
    }

    public static void addToBreakList(BlockPos pos, ClientLevel world) {
    if (bedrockMiner == null) return;
    try {
        bedrockMiner.addToBreakList(pos, world);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public static void clearTask() {
    if (bedrockMiner == null) return;
    try {
        bedrockMiner.clearTask();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public static boolean isWorking() {
    if (bedrockMiner == null) return false;
    try {
        return bedrockMiner.isWorking();
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    public static void setWorking(boolean running) {
    setWorking(running, false);
}

    public static void setWorking(boolean running, boolean showMessage) {
    if (BlockUtils.client.player != null && BlockUtils.client.player.isCreative() && running) {
        MessageUtils.setOverlayMessage(I18n.BEDROCK_CREATIVE_MODE.getName());
        return;
    }
    if (bedrockMiner == null) return;
    try {
        bedrockMiner.setWorking(running, showMessage);
        if (!running) clearTask();  // 忘记加了
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public static boolean isBedrockMinerFeatureEnable() {
    if (bedrockMiner == null) return false;
    try {
        return bedrockMiner.isBedrockMinerFeatureEnable();
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    public static void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) {
    if (bedrockMiner == null) return;
    try {
        bedrockMiner.setBedrockMinerFeatureEnable(bedrockMinerFeatureEnable);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}