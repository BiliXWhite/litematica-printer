package me.aleksilassila.litematica.printer.integration.quickshulker;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import me.aleksilassila.litematica.printer.Reference;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;


import java.util.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
//#if MC <= 260100
import com.mojang.blaze3d.vertex.Tesselator;
//#endif


//#if MC <= 12104
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.renderer.GameRenderer;
//#elseif MC > 12101
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
//#endif

//#if MC == 12105
//$$ import net.minecraft.client.renderer.FogParameters;
//$$ import com.mojang.blaze3d.buffers.BufferUsage;
//#endif

//#if MC > 12006
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Vector4f;
//#endif

//#if MC >= 260100
import org.joml.Matrix4fc;
//#if MC >= 260300
//$$ import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
//#else
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//#endif
import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
//$$ import org.joml.Matrix4f;
//#endif

//#if MC <= 12104
//$$ @SuppressWarnings("deprecation")
//#endif
public class HighlightBlockRenderer implements IRenderer {
    public static HighlightBlockRenderer instance = new HighlightBlockRenderer();
    private static final Object HIGHLIGHT_LOCK = new Object();
    public static Map<String, HighlightTheProject> highlightTheProjectMap = new HashMap<>();
    public static String threadName = Reference.MOD_ID + "-render";
    public static boolean shaderIng = false;
    private static boolean renderFailureLogged = false;
    public static List<String> clearList = new LinkedList<>();
    public static Map<String, Set<BlockPos>> setMap = new HashMap<>();

    public static void createHighlightBlockList(String id, ConfigColor color4f) {
        synchronized (HIGHLIGHT_LOCK) {
            if (highlightTheProjectMap.get(id) == null) {
                highlightTheProjectMap.put(id, new HighlightTheProject(color4f, new LinkedHashSet<>()));
            }
        }
    }

    public static Set<BlockPos> getHighlightBlockPosList(String id) {
        synchronized (HIGHLIGHT_LOCK) {
            if (highlightTheProjectMap.get(id) != null) {
                return highlightTheProjectMap.get(id).pos();
            }
        }
        return null;
    }

    public static void clear(String id) {
        synchronized (HIGHLIGHT_LOCK) {
            if (!clearList.contains(id)) clearList.add(id);
        }
    }

    public static void setPos(String id, Set<BlockPos> posSet) {
        synchronized (HIGHLIGHT_LOCK) {
            HighlightTheProject highlightTheProject = highlightTheProjectMap.get(id);
            if (highlightTheProject != null && posSet != null) {
                setMap.put(id, new LinkedHashSet<>(posSet));
            }
        }
    }

    //如果不注册无法渲染，
    public static void init() {
        RenderEventHandler.getInstance().registerWorldLastRenderer(instance);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> {
            synchronized (HIGHLIGHT_LOCK) {
                for (Map.Entry<String, HighlightTheProject> stringHighlightTheProjectEntry : highlightTheProjectMap.entrySet()) {
                    stringHighlightTheProjectEntry.getValue().pos.clear();
                }
            }
        });
    }

    // @formatter:off
    //#if MC >= 260300
    //$$ public void test3(Color4f color4f, Set<BlockPos> posSet) {
    //#elseif MC > 260100
    public void test3(Matrix4fc matrices, Color4f color4f, Set<BlockPos> posSet) {
    //#elseif MC > 12004
    //$$ public void test3(Matrix4f matrices, Color4f color4f, Set<BlockPos> posSet) {
    //#else
    //$$ public void test3(PoseStack matrices, Color4f color4f, Set<BlockPos> posSet){
    //#endif
        for (BlockPos pos : posSet) {
            //#if MC > 12104
                //#if MC == 12105
                //$$ RenderUtils.renderAreaSides(pos, pos, color4f, matrices);
                //#endif
            RenderSystem.setShaderFog(RenderSystem.getShaderFog());
            //#else
            //$$ RenderUtils.renderAreaSides(pos, pos, color4f, matrices, Minecraft.getInstance());
            //#endif
        }

        //#if MC == 12105
        // This version uses a global fog value shared with later world renderers.
        //$$ FogParameters previousFog = RenderSystem.getShaderFog();
        //$$ try {
        //#endif
        //#if MC > 12104
            //#if MC > 12105
            RenderSystem.setShaderFog(RenderSystem.getShaderFog());
            //#else
            //$$ RenderSystem.setShaderFog(FogParameters.NO_FOG);
            //#endif
        //#else
        //$$ RenderSystem.enableBlend();
        //$$ RenderSystem.disableCull();
        //#endif

        //#if MC > 12101
        //#else
        //$$ RenderSystem.setShader(GameRenderer::getPositionColorShader);
        //#endif
        //#if MC <= 260100
        Tesselator tesselator = Tesselator.getInstance();
        //#endif

        //#if MC > 12006
            //#if MC > 12104
                // drawPost() closes the render pass, not this context's buffers or allocator.
                //#if MC == 12105
                //$$ try (RenderContext ctx = new RenderContext(MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT, BufferUsage.STATIC_WRITE)) {
                //#elseif MC > 260100
                //$$ try (RenderContext ctx = new RenderContext(() -> threadName, MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT, 0)) {
                //#else
                try (RenderContext ctx = new RenderContext(() -> threadName, MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT)) {
                //#endif
            BufferBuilder buffer = ctx.getBuilder();
            //#else
            //$$ BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            //#endif
        MeshData meshData;
        //#else
        //$$ BufferBuilder buffer = tesselator.getBuilder();
        //$$ buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        //#endif
        for (BlockPos pos : posSet) {
            //#if MC >= 12105
            RenderUtils.renderAreaSidesBatched(pos, pos, color4f, 0.002, buffer);
            //#else
            //$$ RenderUtils.renderAreaSidesBatched(pos, pos, color4f, 0.002, buffer, Minecraft.getInstance());
            //#endif
        }
        try {
            if (buffer != null) {
                //#if MC > 12006
                meshData = buffer.buildOrThrow();
                    //#if MC > 12104
                    try (meshData) {
                        ctx.upload(meshData, true);
                        ctx.startResorting(meshData, ctx.createVertexSorter(fi.dy.masa.malilib.render.RenderUtils.camPos()));
                    }
                    ctx.drawPost();
                    //#else
                    //$$ BufferUploader.drawWithShader(meshData);
                    //$$ meshData.close();
                    //#endif
                //#else
                //$$ tesselator.end();
                //#endif
            }
        } catch (Exception e) {
            logRenderFailure(e);
        }

        //#if MC > 12104
        } catch (Exception e) {
            logRenderFailure(e);
        }
        //#if MC == 12105
        //$$ } finally {
        //$$     RenderSystem.setShaderFog(previousFog);
        //$$ }
        //#else
        RenderSystem.setShaderFog(RenderSystem.getShaderFog());
        //#endif
        //#else
        //$$ RenderSystem.enableCull();
        //$$ RenderSystem.disableBlend();
        //#endif


//        fi.dy.masa.litematica.render.RenderUtils.renderAreaSides(pos, pos, color4f, matrices, client);
    }

    @Override
    //#if MC >= 260300
    //$$ public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
    //#elseif MC >= 260100
    public void onRenderWorldLast(RenderTarget fb, Matrix4fc matrices, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
    //#elseif MC > 12004
    //$$ public void onRenderWorldLast(Matrix4f matrices, Matrix4f projMatrix) {
    //#else
    //$$ public void onRenderWorldLast(PoseStack matrices, Matrix4f projMatrix){
    //#endif
        List<HighlightRenderSnapshot> snapshots = new ArrayList<>();
        synchronized (HIGHLIGHT_LOCK) {
            //更改渲染
            setMap.forEach((k, v) -> {
                HighlightTheProject highlightTheProject = highlightTheProjectMap.get(k);
                if (highlightTheProject != null) {
                    highlightTheProject.pos.clear();
                    highlightTheProject.pos.addAll(v);
                }
            });
            setMap.clear();

            for (String string : clearList) {
                HighlightTheProject highlightTheProject = highlightTheProjectMap.get(string);
                if (highlightTheProject != null) {
                    highlightTheProject.pos.clear();
                }
            }
            clearList.clear();

            for (HighlightTheProject value : highlightTheProjectMap.values()) {
                if (!value.pos.isEmpty()) {
                    snapshots.add(new HighlightRenderSnapshot(value.color4f.getColor(), new LinkedHashSet<>(value.pos)));
                }
            }
        }

        shaderIng = true;
        try {
            for (HighlightRenderSnapshot snapshot : snapshots) {
                //#if MC >= 260300
                //$$ test3(snapshot.color, snapshot.pos);
                //#else
                test3(matrices, snapshot.color, snapshot.pos);
                //#endif
            }
        } catch (RuntimeException exception) {
            logRenderFailure(exception);
        } finally {
            shaderIng = false;
        }
    }

    private static void logRenderFailure(Throwable exception) {
        if (!renderFailureLogged) {
            renderFailureLogged = true;
            Reference.LOGGER.warn("潜影盒高亮渲染失败，已跳过本次渲染", exception);
        }
    }

    // @formatter:on

    public record HighlightTheProject(ConfigColor color4f, Set<BlockPos> pos) {
    }

    private record HighlightRenderSnapshot(Color4f color, Set<BlockPos> pos) {
    }
}
