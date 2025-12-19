package me.aleksilassila.litematica.printer.guides;

import me.aleksilassila.litematica.printer.SchematicBlockState;
import me.aleksilassila.litematica.printer.guides.interaction.*;
import me.aleksilassila.litematica.printer.guides.placement.*;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.*;

import java.util.ArrayList;

/**
 * Litematica打印机的指南管理类
 * 负责注册不同方块对应的交互/放置指南类，并根据方块状态获取适用的指南列表
 */
public class Guides {
    /**
     * 存储指南类与对应方块类的映射元组列表
     * Tuple结构：
     * - A：指南类的Class对象（如RotatingBlockGuide.class）
     * - B：该指南适用的方块类数组（如AbstractSkullBlock.class、SignBlock.class）
     */
    protected final static ArrayList<Tuple<Class<? extends Guide>, Class<? extends Block>[]>> guides = new ArrayList<>();

    /**
     * 注册指南类与对应方块类的映射关系
     * @param guideClass 指南类的Class对象
     * @param blocks 该指南适用的方块类数组（可变参数）
     */
    @SafeVarargs
    protected static void registerGuide(Class<? extends Guide> guideClass, Class<? extends Block>... blocks) {
        guides.add(new Tuple<>(guideClass, blocks));
    }

    // 静态代码块：初始化注册所有指南与方块的映射关系
    static {
        // 旋转方块指南：处理需要旋转调整方向的方块
        registerGuide(
                RotatingBlockGuide.class,  // 旋转方块指南类
                AbstractSkullBlock.class,  // 抽象骷髅头方块（骷髅头、玩家头等的父类）
                SignBlock.class,           // 标牌方块
                AbstractBannerBlock.class  // 抽象旗帜方块（立式/墙式旗帜的父类）
        );

        // 台阶指南：处理台阶方块的放置/交互
        registerGuide(
                SlabGuide.class,  // 台阶指南类
                SlabBlock.class   // 台阶方块
        );

        // 火把指南：处理火把方块的放置/交互
        registerGuide(
                TorchGuide.class,  // 火把指南类
                TorchBlock.class   // 火把方块
        );

        // 耕地指南：处理耕地方块的状态检测
        registerGuide(
                FarmlandGuide.class,  // 耕地指南类
                FarmBlock.class       // 耕地方块（官方映射，对应Yarn的FarmlandBlock）
        );

        // 耕地耕作指南：处理耕地的耕作交互逻辑
        registerGuide(
                TillingGuide.class,  // 耕地耕作指南类
                FarmBlock.class      // 耕地方块（官方映射，对应Yarn的FarmlandBlock）
        );

        // 铁轨猜测指南：处理铁轨的方向/连接状态猜测
        registerGuide(
                RailGuesserGuide.class,  // 铁轨猜测指南类
                BaseRailBlock.class      // 基础铁轨方块（所有铁轨的父类）
        );

        // 箱子指南：处理箱子的连接逻辑（单箱/双箱判定）
        registerGuide(
                ChestGuide.class,  // 箱子指南类
                ChestBlock.class   // 箱子方块
        );

        // 花盆指南：处理花盆的基础放置逻辑
        registerGuide(
                FlowerPotGuide.class,  // 花盆指南类
                FlowerPotBlock.class   // 花盆方块
        );

        // 花盆填充指南：处理花盆的植物填充交互
        registerGuide(
                FlowerPotFillGuide.class,  // 花盆填充指南类
                FlowerPotBlock.class       // 花盆方块
        );

        // 特定属性猜测指南：处理需要根据属性调整状态的方块（红石组件、植物、连接类方块等）
        registerGuide(
                PropertySpecificGuesserGuide.class,  // 特定属性猜测指南类
                RepeaterBlock.class,                 // 红石中继器方块
                ComparatorBlock.class,               // 红石比较器方块
                RedStoneWireBlock.class,             // 红石线方块
                RedstoneTorchBlock.class,            // 红石火把方块
                BambooStalkBlock.class,              // 竹子茎方块
                CactusBlock.class,                   // 仙人掌方块
                SaplingBlock.class,                  // 树苗方块
                ScaffoldingBlock.class,              // 脚手架方块
                PointedDripstoneBlock.class,         // 滴水石锥方块
                CrossCollisionBlock.class,           // 十字碰撞方块（栅栏、墙等连接类方块的父类）
                DoorBlock.class,                     // 门方块（抽象父类）
                TrapDoorBlock.class,                 // 活板门方块
                FenceGateBlock.class,                // 栅栏门方块
                ChestBlock.class,                    // 箱子方块（补充属性判定）
                SnowLayerBlock.class,                // 雪层方块
                SeaPickleBlock.class,                // 海泡菜方块
                CandleBlock.class,                   // 蜡烛方块
                LeverBlock.class,                    // 拉杆方块
                EndPortalFrameBlock.class,           // 末地传送门框架方块
                NoteBlock.class,                     // 音符盒方块
                CampfireBlock.class,                 // 营火方块
                PoweredRailBlock.class,              // 充能铁轨方块
                LeavesBlock.class,                   // 树叶方块（抽象父类）
                TripWireHookBlock.class              // 绊线钩方块
        );

        // 重力方块指南：处理重力掉落的方块（沙、砾石等）
        registerGuide(
                FallingBlockGuide.class,  // 重力方块指南类
                FallingBlock.class        // 重力方块（抽象父类，沙、砾石等的父类）
        );

        // 方块无关猜测指南：处理竹子、大滴水草等特殊植物/方块的通用逻辑
        registerGuide(
                BlockIndifferentGuesserGuide.class,  // 方块无关猜测指南类
                BambooStalkBlock.class,              // 竹子茎方块
                BigDripleafStemBlock.class,          // 大滴水草茎方块
                BigDripleafBlock.class,              // 大滴水草方块
                TwistingVinesPlantBlock.class,       // 扭曲藤蔓植株方块
                TripWireBlock.class                  // 绊线方块
        );

        // 营火熄灭指南：处理营火的熄灭交互
        registerGuide(
                CampfireExtinguishGuide.class,  // 营火熄灭指南类
                CampfireBlock.class             // 营火方块
        );

        // 点燃蜡烛指南：处理蜡烛的点燃交互
        registerGuide(
                LightCandleGuide.class,    // 点燃蜡烛指南类
                AbstractCandleBlock.class  // 抽象蜡烛方块（所有蜡烛的父类）
        );

        // 末影之眼指南：处理末地传送门框架的末影之眼放置
        registerGuide(
                EnderEyeGuide.class,        // 末影之眼指南类
                EndPortalFrameBlock.class   // 末地传送门框架方块
        );

        // 状态循环指南：处理可循环切换状态的方块（门、栅栏门、拉杆等）
        registerGuide(
                CycleStateGuide.class,  // 状态循环指南类
                DoorBlock.class,        // 门方块（抽象父类）
                FenceGateBlock.class,   // 栅栏门方块
                TrapDoorBlock.class,    // 活板门方块
                LeverBlock.class,       // 拉杆方块
                RepeaterBlock.class,    // 红石中继器方块（延迟状态循环）
                ComparatorBlock.class,  // 红石比较器方块（模式状态循环）
                NoteBlock.class         // 音符盒方块（音调状态循环）
        );

        // 方块替换指南：处理可被替换的方块（雪层、海泡菜、蜡烛、台阶等）
        registerGuide(
                BlockReplacementGuide.class,  // 方块替换指南类
                SnowLayerBlock.class,         // 雪层方块
                SeaPickleBlock.class,         // 海泡菜方块
                CandleBlock.class,            // 蜡烛方块
                SlabBlock.class               // 台阶方块
        );

        // 原木指南：处理原木的通用交互逻辑（无特定适用方块）
        registerGuide(LogGuide.class);

        // 原木去皮指南：处理原木的去皮交互（无特定适用方块）
        registerGuide(LogStrippingGuide.class);

        // 通用猜测指南：兜底的通用交互指南（无特定适用方块）
        registerGuide(GuesserGuide.class);
    }

    /**
     * 获取所有注册的指南与方块的映射列表
     * @return 存储指南-方块映射元组的ArrayList
     */
    public ArrayList<Tuple<Class<? extends Guide>, Class<? extends Block>[]>> getGuides() {
        return guides;
    }

    /**
     * 根据方块状态获取适用的交互指南数组
     * @param state 原理图中的方块状态（包含目标方块的类型、属性等信息）
     * @return 适用的Guide对象数组
     */
    public Guide[] getInteractionGuides(SchematicBlockState state) {
        ArrayList<Tuple<Class<? extends Guide>, Class<? extends Block>[]>> guides = getGuides();
        ArrayList<Guide> applicableGuides = new ArrayList<>();
        // 遍历所有注册的指南-方块映射
        for (Tuple<Class<? extends Guide>, Class<? extends Block>[]> guideTuple : guides) {
            try {
                // 情况1：指南无指定适用方块（直接实例化指南）
                if (guideTuple.getB().length == 0) {
                    applicableGuides.add(guideTuple.getA().getConstructor(SchematicBlockState.class).newInstance(state));
                    continue;
                }
                // 情况2：指南有指定适用方块，判断目标方块是否匹配
                for (Class<? extends Block> clazz : guideTuple.getB()) {
                    // 若目标方块是当前类的实例，则实例化该指南并加入列表
                    if (clazz.isInstance(state.targetState.getBlock())) {
                        applicableGuides.add(guideTuple.getA().getConstructor(SchematicBlockState.class).newInstance(state));
                    }
                }
            } catch (Exception ignored) {
                // 捕获反射异常（如指南类无对应构造器），忽略不影响整体逻辑
            }
        }
        return applicableGuides.toArray(Guide[]::new);
    }
}