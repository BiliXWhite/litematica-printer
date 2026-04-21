package me.aleksilassila.litematica.printer.guide;

import me.aleksilassila.litematica.printer.enums.BlockMatchResult;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.printer.action.Action;
import net.minecraft.world.level.block.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Guides {
    public static final Guides INSTANCE = new Guides();
    private final List<GuideRegistration> registrations = new ArrayList<>();
    private final AtomicReference<Boolean> skipOtherGuide = new AtomicReference<>();

    private Guides() {
        // 跳过指南应优先被注册, 当没有需要跳过的方块需要进行注释掉, 否者会拦截所有方块
        register(SkipGuide.class, SkullBlock.class, LiquidBlock.class, BubbleColumnBlock.class, LilyPadBlock.class);

        // 默认指南需要最后被注册, 它的作用是用于兜底
        register(DefaultGuide.class);
    }

    @SafeVarargs
    public final void register(Class<? extends Guide> guideClass, Class<? extends Block>... supportedBlocks) {
        registrations.add(new GuideRegistration(guideClass, supportedBlocks));
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private List<Guide> getGuides(SchematicBlockContext context) {
        List<Guide> guides = new ArrayList<>();
        for (GuideRegistration reg : registrations) {
            boolean b = false;
            if (reg.blockClass.length == 0) {
                b = true;
            } else {
                for (Class<? extends Block> clazz : reg.blockClass) {
                    if (clazz.isInstance(context.requiredState.getBlock())) {
                        b = true;
                    }
                }
            }
            if (b) {
                try {
                    Guide guide = reg.guideClass
                            .getConstructor(SchematicBlockContext.class)
                            .newInstance(context);
                    if (guide.canExecute(this.skipOtherGuide)) {
                        guides.add(guide);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return guides;
    }

    public final Optional<Action> buildAction(SchematicBlockContext context) {
        this.skipOtherGuide.set(false);
        BlockMatchResult blockMatchResult = BlockMatchResult.compare(context);
        List<Guide> guides = this.getGuides(context);
        for (Guide guide : guides) {
            Optional<Action> action = guide.buildAction(blockMatchResult, this.skipOtherGuide);
            if (action.isPresent()) {
                return action;
            }
            if (this.skipOtherGuide.get()) {    // 跳过其他指南
                break;
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class GuideRegistration {
        public final Class<? extends Guide> guideClass;
        public final Class<? extends Block>[] blockClass;

        public GuideRegistration(Class<? extends Guide> guideClass, Class<? extends Block>[] blockClass) {
            this.guideClass = guideClass;
            this.blockClass = blockClass;
        }
    }
}