//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

/**
 * Executes the real dependency's count/filter methods with our compiled handler bodies.
 * Supports only the RETURN injections and INVOKE redirects used by these mixins;
 * this is a headless behavior fixture, not a substitute for a Fabric launch/Mixin audit.
 */
final class MaterialListTestLoader extends ClassLoader {
    private static final String PREFIX = "fi.dy.masa.litematica.materials.";
    private static final String WIDGET = "fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry";
    private static final String MIXINS = "me/aleksilassila/litematica/printer/mixin/printer/litematica/";
    private static final String CALLBACK = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    private final Set<String> additionalClasses;

    MaterialListTestLoader(String... additionalClasses) {
        super(MaterialListTestLoader.class.getClassLoader());
        this.additionalClasses = Set.of(additionalClasses);
    }

    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(PREFIX) && !name.equals(WIDGET) && !additionalClasses.contains(name)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            var result = findLoadedClass(name);
            if (result == null) {
                try {
                    var target = read(name.replace('.', '/') + ".class");
                    String simple = name.substring(name.lastIndexOf('.') + 1);
                    if (name.equals(WIDGET) && additionalClasses.isEmpty()) keepHoverCountCalculation(target);
                    if (simple.equals("MaterialListEntry") || simple.equals("MaterialListSorter") || name.equals(WIDGET)) {
                        var mixin = read(MIXINS + "Mixin" + simple + ".class");
                        if (mixin != null) apply(target, mixin);
                    }
                    var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    target.accept(writer);
                    byte[] bytes = writer.toByteArray();
                    result = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) { throw new ClassNotFoundException(name, e); }
            }
            if (resolve) resolveClass(result);
            return result;
        }
    }

    /** Keep the dependency's hover count arithmetic, excluding rendering and window initialization. */
    private static void keepHoverCountCalculation(ClassNode target) {
        var hover = target.methods.stream().filter(m -> m.name.equals("postRenderHovered")).findFirst().orElseThrow();
        var calls = new ArrayList<MethodInsnNode>();
        MethodInsnNode multiplier = null;
        for (var instruction : hover.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("getMultiplier")) multiplier = call;
                if (call.name.equals("getFormattedCountString")) calls.add(call);
            }
        }
        if (multiplier == null || calls.size() != 2) throw new AssertionError("Unexpected hover count layout");
        // Formatter calls load this, count, stack, then stack.getMaxStackSize().
        var total = (VarInsnNode) previousCode(previousCode(previousCode(calls.get(0))));
        var missing = (VarInsnNode) previousCode(previousCode(previousCode(calls.get(1))));
        if (total.getOpcode() != Opcodes.ILOAD || missing.getOpcode() != Opcodes.ILOAD) {
            throw new AssertionError("Unexpected formatter arguments");
        }
        var start = previousCode(previousCode(multiplier));
        var end = previousCode(total);
        var labels = new HashMap<LabelNode, LabelNode>();
        for (var instruction : hover.instructions) {
            if (instruction instanceof LabelNode label) labels.put(label, new LabelNode());
        }
        var counts = new MethodNode(Opcodes.ACC_PUBLIC, "postRenderHovered", "()[I", null, null);
        for (var instruction = start; instruction != end; instruction = instruction.getNext()) {
            if (instruction == null) throw new AssertionError("Unbounded hover count slice");
            if (!(instruction instanceof FrameNode) && !(instruction instanceof LineNumberNode)) {
                counts.instructions.add(instruction.clone(labels));
            }
        }
        counts.instructions.add(new InsnNode(Opcodes.ICONST_2));
        counts.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
        int index = 0;
        for (int local : new int[] {total.var, missing.var}) {
            counts.instructions.add(new InsnNode(Opcodes.DUP));
            counts.instructions.add(new InsnNode(index++ == 0 ? Opcodes.ICONST_0 : Opcodes.ICONST_1));
            counts.instructions.add(new VarInsnNode(Opcodes.ILOAD, local));
            counts.instructions.add(new InsnNode(Opcodes.IASTORE));
        }
        counts.instructions.add(new InsnNode(Opcodes.ARETURN));
        target.methods.clear();
        target.methods.add(counts);
        target.fields.removeIf(f -> !f.name.equals("entry") && !f.name.equals("materialList"));
        target.superName = "java/lang/Object";
        target.interfaces.clear();
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode instruction) {
        do { instruction = instruction.getPrevious(); } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private ClassNode read(String resource) throws IOException {
        try (var in = getParent().getResourceAsStream(resource)) {
            if (in == null) return null;
            var node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        }
    }

    private static Object value(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void apply(ClassNode target, ClassNode mixin) {
        var mapped = new ClassNode();
        mixin.accept(new ClassRemapper(mapped, new SimpleRemapper(mixin.name, target.name)));
        var originalMethods = List.copyOf(target.methods);
        for (var handler : mapped.methods) {
            if (handler.name.startsWith("<")) continue;
            var annotations = new ArrayList<AnnotationNode>();
            if (handler.visibleAnnotations != null) annotations.addAll(handler.visibleAnnotations);
            if (handler.invisibleAnnotations != null) annotations.addAll(handler.invisibleAnnotations);
            for (var annotation : annotations) {
                boolean inject = annotation.desc.endsWith("/Inject;");
                if (!inject && !annotation.desc.endsWith("/Redirect;")) continue;
                var methods = (List<String>) value(annotation, "method");
                Object atValue = value(annotation, "at");
                var at = atValue instanceof List<?> list ? (AnnotationNode) list.getFirst() : (AnnotationNode) atValue;
                for (var method : originalMethods) {
                    if (!methods.contains(method.name)) continue;
                    for (var instruction : method.instructions.toArray()) {
                        if (inject && instruction.getOpcode() == Opcodes.IRETURN) {
                            if (!"RETURN".equals(value(at, "value"))) throw new AssertionError("Unsupported injection");
                            int local = method.maxLocals;
                            var code = new InsnList();
                            code.add(new VarInsnNode(Opcodes.ISTORE, local));
                            code.add(new TypeInsnNode(Opcodes.NEW, CALLBACK));
                            code.add(new InsnNode(Opcodes.DUP));
                            code.add(new LdcInsnNode(method.name));
                            code.add(new InsnNode(Opcodes.ICONST_1));
                            code.add(new VarInsnNode(Opcodes.ILOAD, local));
                            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, CALLBACK, "<init>", "(Ljava/lang/String;ZI)V", false));
                            code.add(new VarInsnNode(Opcodes.ASTORE, local + 1));
                            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            code.add(new VarInsnNode(Opcodes.ALOAD, local + 1));
                            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name, handler.name, handler.desc, false));
                            code.add(new VarInsnNode(Opcodes.ALOAD, local + 1));
                            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CALLBACK, "getReturnValueI", "()I", false));
                            method.instructions.insertBefore(instruction, code);
                            method.maxLocals += 2;
                        } else if (!inject && instruction instanceof MethodInsnNode call
                                && ("L" + call.owner + ";" + call.name + call.desc).equals(value(at, "target"))) {
                            var code = new InsnList();
                            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            code.add(new InsnNode(Opcodes.SWAP));
                            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name, handler.name, handler.desc, false));
                            method.instructions.insertBefore(instruction, code);
                            method.instructions.remove(instruction);
                        }
                    }
                }
            }
            target.methods.add(handler);
        }
    }
}
//#endif
