//#if MC > 12104
package me.aleksilassila.litematica.printer.render;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

/**
 * Keeps HighlightBlockRenderer.test3's real control flow, including its exception handlers.
 * Only rendering dependencies are replaced, since those require a live GPU/client.
 * RenderContext owns its allocator and uploaded buffers until close(), whereas drawPost()
 * only closes its render pass. MeshData owns a separate result that also needs closing.
 */
public final class HighlightRenderFixture extends ClassLoader {
    private static final String RENDERER = "me/aleksilassila/litematica/printer/integration/quickshulker/HighlightBlockRenderer";
    static int contexts, meshes, draws;
    static Fog fog;
    static Failure failure;
    static RuntimeException injected;
    static Throwable logged;
    private final Object renderer;
    private final Method draw;

    public enum Failure { NONE, GET_BUILDER, GEOMETRY, BUILD, UPLOAD, SORT, DRAW }

    static void reset() {
        contexts = meshes = draws = 0;
        fog = new Fog();
        failure = Failure.NONE;
        injected = new IllegalStateException("injected GPU boundary failure");
        logged = null;
    }

    @SuppressWarnings("deprecation") // The older ASM versions in the supported matrix use this constructor.
    public HighlightRenderFixture() throws Exception {
        super(HighlightRenderFixture.class.getClassLoader());
        ClassNode source = new ClassNode();
        try (var input = getParent().getResourceAsStream(RENDERER + ".class")) {
            if (input == null) throw new AssertionError("Missing compiled highlight renderer");
            new ClassReader(input).accept(source, 0);
        }
        source.interfaces.clear();
        source.innerClasses.clear();
        source.nestMembers = null;
        source.fields.removeIf(field -> !field.name.equals("threadName"));
        source.methods.removeIf(method -> !method.name.equals("test3") && !method.name.startsWith("lambda$test3"));

        // Initialization and logging are unrelated to GPU ownership and would bootstrap Minecraft.
        var constructor = source.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        var logger = source.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "logRenderFailure", "(Ljava/lang/Throwable;)V", null, null);
        logger.visitVarInsn(Opcodes.ALOAD, 0);
        logger.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HighlightRenderFixture.class),
                "log", "(Ljava/lang/Throwable;)V", false);
        logger.visitInsn(Opcodes.RETURN);
        logger.visitMaxs(1, 1);
        logger.visitEnd();

        Map<String, String> types = new HashMap<>();
        types.put("fi/dy/masa/malilib/render/RenderContext", Type.getInternalName(Context.class));
        types.put("fi/dy/masa/malilib/render/RenderUtils", Type.getInternalName(Geometry.class));
        types.put("fi/dy/masa/malilib/render/MaLiLibPipelines", Type.getInternalName(Pipelines.class));
        types.put("com/mojang/blaze3d/systems/RenderSystem", Type.getInternalName(RenderState.class));
        types.put("com/mojang/blaze3d/vertex/Tesselator", Type.getInternalName(Tess.class));
        types.put("com/mojang/blaze3d/vertex/BufferBuilder", Type.getInternalName(Buffer.class));
        types.put("com/mojang/blaze3d/vertex/MeshData", Type.getInternalName(Mesh.class));
        types.put("com/mojang/blaze3d/buffers/BufferUsage", Type.getInternalName(Usage.class));
        for (String name : new String[] {"net/minecraft/client/renderer/FogParameters",
                "com/mojang/blaze3d/buffers/GpuBufferSlice", "com/mojang/renderpearl/api/buffers/GpuBufferSlice"}) {
            types.put(name, Type.getInternalName(Fog.class));
        }
        for (String name : new String[] {"net/minecraft/core/BlockPos", "net/minecraft/world/phys/Vec3",
                "org/joml/Matrix4f", "org/joml/Matrix4fc", "fi/dy/masa/malilib/util/data/Color4f",
                "com/mojang/blaze3d/vertex/VertexSorting", "com/mojang/blaze3d/pipeline/RenderPipeline",
                "com/mojang/renderpearl/api/pipeline/RenderPipeline"}) {
            types.put(name, "java/lang/Object");
        }
        // Descriptors only change reference types; retaining original frames avoids loading game classes.
        var writer = new ClassWriter(0);
        source.accept(new ClassRemapper(writer, new SimpleRemapper(types)));
        byte[] bytes = writer.toByteArray();
        Class<?> type = defineClass(RENDERER.replace('/', '.'), bytes, 0, bytes.length);
        renderer = type.getConstructor().newInstance();
        draw = java.util.Arrays.stream(type.getMethods()).filter(m -> m.getName().equals("test3")).findFirst().orElseThrow();
    }

    void draw() throws Exception {
        if (draw.getParameterCount() == 2) draw.invoke(renderer, new Object(), Set.of(new Object()));
        else draw.invoke(renderer, new Object(), new Object(), Set.of(new Object()));
    }

    public static void log(Throwable error) { logged = error; }
    private static void fail(Failure stage) { if (failure == stage) throw injected; }

    public static final class Fog { public static final Fog NO_FOG = new Fog(); }
    public static final class RenderState {
        public static Fog getShaderFog() { return fog; }
        public static void setShaderFog(Fog value) { fog = value; }
    }
    public static final class Pipelines { public static final Object POSITION_COLOR_TRANSLUCENT = new Object(); }
    public static final class Usage { public static final Usage STATIC_WRITE = new Usage(); }
    public static final class Tess { public static Tess getInstance() { return new Tess(); } }
    public static final class Geometry {
        public static void renderAreaSides(Object a, Object b, Object color, Object matrices) {}
        public static void renderAreaSidesBatched(Object a, Object b, Object color, double offset, Buffer buffer) {
            fail(Failure.GEOMETRY);
        }
        public static Object camPos() { return new Object(); }
    }
    public static final class Buffer {
        public Mesh buildOrThrow() { fail(Failure.BUILD); return new Mesh(); }
    }
    public static final class Mesh implements AutoCloseable {
        private boolean closed;
        public Mesh() { meshes++; }
        @Override public void close() { if (!closed) { closed = true; meshes--; } }
    }
    public static final class Context implements AutoCloseable {
        private boolean closed;
        public Context(Object pipeline, Usage usage) { contexts++; }
        public Context(Supplier<String> name, Object pipeline) { contexts++; }
        public Context(Supplier<String> name, Object pipeline, int size) { contexts++; }
        public Buffer getBuilder() { fail(Failure.GET_BUILDER); return new Buffer(); }
        public void upload(Mesh mesh, boolean sort) { fail(Failure.UPLOAD); }
        public Object createVertexSorter(Object position) { return new Object(); }
        public void startResorting(Mesh mesh, Object sorter) { fail(Failure.SORT); }
        public void drawPost() {
            if (closed) throw new AssertionError("Context closed before draw");
            fail(Failure.DRAW);
            draws++;
        }
        @Override public void close() {
            if (closed) throw new AssertionError("Context closed twice");
            closed = true;
            contexts--;
        }
    }
}
//#endif
