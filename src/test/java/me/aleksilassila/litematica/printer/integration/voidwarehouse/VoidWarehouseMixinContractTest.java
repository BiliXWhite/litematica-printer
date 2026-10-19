//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

/** Checks compiled injection selectors against the actual dependency shipped for each MC version. */
class VoidWarehouseMixinContractTest {
    @org.junit.jupiter.api.Test
    void browserAccessorMatchesTheActualMalilibField() throws Exception {
        var target = read("fi/dy/masa/malilib/gui/GuiListBase");
        var accessor = read("me/aleksilassila/litematica/printer/mixin/printer/malilib/GuiListBaseAccessor");
        var method = accessor.methods.stream().filter(m -> m.name.equals("litematicaPrinter$getListWidget")).findFirst().orElseThrow();
        var annotation = annotations(method.visibleAnnotations, method.invisibleAnnotations).stream()
                .filter(a -> a.desc.endsWith("/Accessor;")).findFirst().orElseThrow();
        assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(value(annotation, "value"))
                && method.desc.equals("()" + f.desc)), "Accessor must match the real widget field and return type");
    }

    @ParameterizedTest @ValueSource(strings = {"MaterialListEntry", "MaterialListSorter", "MaterialListHudRenderer", "WidgetMaterialListEntry"})
    @SuppressWarnings("unchecked")
    void allShadowFieldsAndInjectionPointsExistInThisDependency(String simpleName) throws Exception {
        String targetName = "fi/dy/masa/litematica/" + (simpleName.startsWith("Widget") ? "gui/widgets/" : "materials/") + simpleName;
        var target = read(targetName);
        var mixin = read("me/aleksilassila/litematica/printer/mixin/printer/litematica/Mixin" + simpleName);
        for (var field : mixin.fields) {
            if (annotations(field.visibleAnnotations, field.invisibleAnnotations).stream().noneMatch(a -> a.desc.endsWith("/Shadow;"))) continue;
            assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)),
                    "Missing shadow field " + targetName + "." + field.name);
        }
        int verified = 0;
        for (var handler : mixin.methods) {
            for (var annotation : annotations(handler.visibleAnnotations, handler.invisibleAnnotations)) {
                if (!annotation.desc.endsWith("/Inject;") && !annotation.desc.endsWith("/Redirect;")) continue;
                var methods = (List<String>) value(annotation, "method");
                Object atValue = value(annotation, "at");
                var at = atValue instanceof List<?> list ? (AnnotationNode) list.getFirst() : (AnnotationNode) atValue;
                for (String name : methods) {
                    var matched = target.methods.stream().filter(m -> m.name.equals(name)).toList();
                    assertFalse(matched.isEmpty(), "Missing target method " + name);
                    long count = matched.stream().flatMap(m -> List.of(m.instructions.toArray()).stream()).filter(instruction -> {
                        if ("RETURN".equals(value(at, "value"))) return instruction.getOpcode() == Opcodes.IRETURN;
                        if (instruction instanceof MethodInsnNode call) {
                            return ("L" + call.owner + ";" + call.name + call.desc).equals(value(at, "target"));
                        }
                        if (instruction instanceof FieldInsnNode field) {
                            return ("L" + field.owner + ";" + field.name + ":" + field.desc).equals(value(at, "target"))
                                    && instruction.getOpcode() == (int) value(at, "opcode");
                        }
                        return false;
                    }).count();
                    assertTrue(count > 0, "Unmatched injection " + handler.name + " in " + targetName + "." + name);
                    if (simpleName.equals("MaterialListHudRenderer")) {
                        assertEquals(2, count, "Both HUD width calculation and rendered text must use the same gap");
                    }
                    verified++;
                }
            }
        }
        assertTrue(verified > 0);
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var result = new ArrayList<AnnotationNode>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }

    private static Object value(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static ClassNode read(String name) throws IOException {
        try (var in = VoidWarehouseMixinContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(in, "Missing class " + name);
            var node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        }
    }
}
//#endif
