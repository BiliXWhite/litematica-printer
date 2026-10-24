package me.aleksilassila.litematica.printer.search;

import me.aleksilassila.litematica.printer.enums.RadiusShapeType;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// 单个模块一轮扫描的不可变描述，主线程构造后搜索线程只读
public record SearchSpec(
        String moduleId,
        PrinterBox box,
        RadiusShapeType shape,
        @Nullable Vec3 eyePos,
        double range,
        ScanTarget target
) {
    public SearchSpec {
        if (shape == null) shape = RadiusShapeType.SPHERE;
    }
}
