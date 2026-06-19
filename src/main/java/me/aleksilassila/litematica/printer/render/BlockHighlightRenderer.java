package me.aleksilassila.litematica.printer.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
//#if MC < 260200
import com.mojang.blaze3d.vertex.Tesselator;
//#endif
import fi.dy.masa.malilib.interfaces.IRenderer;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.HighlightStyleType;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickHandler;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

import org.joml.Matrix4f;

// MC >= 1.21.5: MaLiLibPipelines + RenderContext (malilib >= 0.24.3)
// MC >= 1.21.1: MeshData, BufferUploader (new Tesselator/BufferBuilder API)
// MC < 1.21.1: Old Tesselator API (getBuilder, end, vertex)

//#if MC >= 12101
import com.mojang.blaze3d.vertex.MeshData;
//#endif
//#if MC >= 12101 && MC < 12105
//$$ import com.mojang.blaze3d.vertex.BufferUploader;
//#endif
//#if MC >= 12105
import com.mojang.blaze3d.pipeline.RenderPipeline;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
//#endif
//#if MC >= 12108
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
//#endif
//#if MC >= 260100
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//$$ import org.joml.Matrix4fc;
//$$ import org.joml.Vector4f;
//#endif
//#if MC < 12006
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class BlockHighlightRenderer implements IRenderer {

    // ===== Render Entry Points =====

    //#if MC >= 260100
    //$$ @Override
    //$$ public void onRenderWorldLast(
    //$$         RenderTarget renderTarget,
    //$$         Matrix4fc projMatrix,
    //$$         CameraRenderState cameraRenderState,
    //$$         Frustum frustum,
    //$$         RenderBuffers renderBuffers,
    //$$         GpuBufferSlice gpuBufferSlice,
    //$$         Vector4f vector4f,
    //$$         ProfilerFiller profiler
    //$$ ) {
    //$$     renderInternal(cameraRenderState.pos);
    //$$ }
    //#elseif MC >= 12108
    @Override
    public void onRenderWorldLastAdvanced(
            RenderTarget renderTarget,
            Matrix4f posMatrix,
            Matrix4f projMatrix,
            Frustum frustum,
            Camera camera,
            RenderBuffers buffers,
            ProfilerFiller profiler
    ) {
        renderInternal(camera.position());
    }
    //#elseif MC >= 12006
    //$$ @Override
    //$$ public void onRenderWorldLast(Matrix4f posMatrix, Matrix4f projMatrix) {
    //$$     renderInternal(getRenderCameraPos());
    //$$ }
    //#endif
    //#if MC < 12006
    //$$ @Override
    //$$ public void onRenderWorldLast(PoseStack poseStack, Matrix4f projMatrix) {
    //$$     renderInternal(getRenderCameraPos());
    //$$ }
    //#endif

    /** Match malilib's camPos() pattern: rendering camera, not player eye */
    //#if MC < 12108
    //$$ private static Vec3 getRenderCameraPos() {
    //$$     var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
    //$$     return cam != null ? cam.getPosition() : Vec3.ZERO;
    //$$ }
    //#endif

    // ===== Shared Render Logic =====

    private void renderInternal(Vec3 cameraPos) {
        if (!Configs.Highlight.HIGHLIGHT_ENABLED.getBooleanValue()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        HighlightStyleType style = (HighlightStyleType) Configs.Highlight.HIGHLIGHT_STYLE.getOptionListValue();
        long fadeDurationMs = Configs.Highlight.HIGHLIGHT_FADE_DURATION.getIntegerValue() * 100L;
        boolean seeThrough = Configs.Highlight.HIGHLIGHT_THROUGH_WALLS.getBooleanValue();
        long now = System.currentTimeMillis();

        int[][] colors = new int[4][4];
        colors[0] = extractArgb(Configs.Highlight.HIGHLIGHT_COLOR_PLACE.getIntegerValue());
        colors[1] = extractArgb(Configs.Highlight.HIGHLIGHT_COLOR_ADJUST.getIntegerValue());
        colors[2] = extractArgb(Configs.Highlight.HIGHLIGHT_COLOR_BREAK.getIntegerValue());
        colors[3] = extractArgb(Configs.Highlight.HIGHLIGHT_COLOR_FAILED.getIntegerValue());

        double expand = 0.005;
        List<HighlightEntry> entries = new ArrayList<>();

        for (ClientPlayerTickHandler handler : ClientPlayerTickManager.VALUES) {
            Queue<ClientPlayerTickHandler.PendingHighlight> pending = handler.getPendingHighlights();
            if (pending.isEmpty()) continue;
            for (ClientPlayerTickHandler.PendingHighlight ph : pending) {
                long elapsed = now - ph.time();
                if (elapsed >= fadeDurationMs) continue;

                int[] c = colors[ph.type().ordinal()];
                int typeA = c[0], typeR = c[1], typeG = c[2], typeB = c[3];
                float baseAlpha = typeA / 255.0f;
                float fadeAlpha = elapsed <= 0 ? baseAlpha : baseAlpha * (1.0f - (float) elapsed / fadeDurationMs);
                if (fadeAlpha <= 0.001f) continue;

                float dx = (float) (ph.pos().getX() + 0.5 - cameraPos.x);
                float dy = (float) (ph.pos().getY() + 0.5 - cameraPos.y);
                float dz = (float) (ph.pos().getZ() + 0.5 - cameraPos.z);
                float distSq = dx * dx + dy * dy + dz * dz;

                entries.add(new HighlightEntry(ph.pos(), fadeAlpha, distSq, typeR, typeG, typeB, style));
            }
        }

        if (entries.isEmpty()) return;

        boolean hasOutline = false;
        boolean hasFilled = false;
        for (HighlightEntry e : entries) {
            if (e.style == HighlightStyleType.OUTLINE || e.style == HighlightStyleType.BOTH) hasOutline = true;
            if (e.style == HighlightStyleType.FILLED || e.style == HighlightStyleType.BOTH) hasFilled = true;
        }

        // Filled translucent rendering needs back-to-front sort for correct alpha blending;
        // outline-only rendering relies on the depth test for order, no sort needed.
        if (hasFilled) {
            entries.sort(Comparator.comparingDouble(e -> -e.distSq));
        }

        //#if MC >= 12105
        drawWithMaLiLib(cameraPos, entries, expand, seeThrough, hasOutline, hasFilled);
        //#elseif MC >= 12101
        //$$ drawDirect(cameraPos, entries, expand, seeThrough, hasOutline, hasFilled);
        //#else
        //$$ drawLegacy(cameraPos, entries, expand, seeThrough, hasOutline, hasFilled);
        //#endif
    }

    // ===== Modern path: MC >= 1.21.5 (MaLiLibPipelines + RenderContext) =====

    //#if MC >= 12105
    private void drawWithMaLiLib(Vec3 cameraPos, List<HighlightEntry> entries, double expand,
                                 boolean seeThrough, boolean hasOutline, boolean hasFilled) {
        RenderPipeline linePipeline = seeThrough
                ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL
                : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH;
        //#if MC >= 12108
        //$$ RenderPipeline filledPipeline = seeThrough
        //$$         ? MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL
        //$$         : MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_NO_CULL;
        //#else
        RenderPipeline filledPipeline = seeThrough
                ? MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL
                : MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH;
        //#endif

        //#if MC >= 260200
        //$$ RenderContext ctx = new RenderContext(() -> "litematica_printer:highlight", linePipeline, 0);
        //#elseif MC >= 12105 && MC < 12108
        //$$ RenderContext ctx = new RenderContext(linePipeline);
        //#else
        RenderContext ctx = new RenderContext(() -> "litematica_printer:highlight", linePipeline);
        //#endif
        try {
            if (hasOutline) {
                //#if MC >= 260200
                //$$ ctx.start(() -> "highlight_outline", linePipeline, 0);
                //#elseif MC >= 12108
                //$$ ctx.start(() -> "highlight_outline", linePipeline);
                //#else
                // BufferBuilder already set in constructor
                //#endif
                BufferBuilder lineBuf = ctx.getBuilder();
                for (HighlightEntry e : entries) {
                    if (e.style == HighlightStyleType.FILLED) continue;
                    addOutlineBoxLines(lineBuf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
                }
                MeshData mesh = lineBuf.build();
                if (mesh != null) {
                    ctx.draw(mesh, false, true);
                    mesh.close();
                }
                ctx.reset();
            }

            if (hasFilled) {
                //#if MC >= 260200
                //$$ BufferBuilder filledBuf = ctx.start(() -> "highlight_filled", filledPipeline, 0);
                //#elseif MC >= 12108
                BufferBuilder filledBuf = ctx.start(() -> "highlight_filled", filledPipeline);
                //#else
                //$$ BufferBuilder filledBuf = ctx.start(filledPipeline);
                //#endif
                for (HighlightEntry e : entries) {
                    if (e.style == HighlightStyleType.OUTLINE) continue;
                    addFilledBoxModern(filledBuf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
                }
                MeshData mesh = filledBuf.build();
                if (mesh != null) {
                    ctx.draw(mesh, false, false);
                    mesh.close();
                }
                ctx.reset();
            }
        } catch (Exception e) {
            Reference.LOGGER.error("BlockHighlight: drawWithMaLiLib exception: {}", e.getMessage());
        } finally {
            try { ctx.close(); } catch (Exception ignored) {}
        }
    }

    private void addFilledBoxModern(BufferBuilder buf, BlockPos pos, double expand,
                                    int r, int g, int b, int a, Vec3 cameraPos) {
        float x1 = (float) (pos.getX() - cameraPos.x - expand);
        float y1 = (float) (pos.getY() - cameraPos.y - expand);
        float z1 = (float) (pos.getZ() - cameraPos.z - expand);
        float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
        float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
        float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);

        quadModern(buf, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        quadModern(buf, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        quadModern(buf, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
        quadModern(buf, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
        quadModern(buf, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a);
        quadModern(buf, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    }

    private void quadModern(BufferBuilder buf, float x1, float y1, float z1,
                            float x2, float y2, float z2, float x3, float y3, float z3,
                            float x4, float y4, float z4, int r, int g, int b, int a) {
        buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(x2, y2, z2).setColor(r, g, b, a);
        buf.addVertex(x3, y3, z3).setColor(r, g, b, a);
        buf.addVertex(x4, y4, z4).setColor(r, g, b, a);
    }

    private void addOutlineBoxLines(BufferBuilder buf, BlockPos pos, double expand,
                                    int r, int g, int b, int a, Vec3 cameraPos) {
        float x1 = (float) (pos.getX() - cameraPos.x - expand);
        float y1 = (float) (pos.getY() - cameraPos.y - expand);
        float z1 = (float) (pos.getZ() - cameraPos.z - expand);
        float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
        float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
        float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);

        line(buf, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buf, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buf, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buf, x1, y1, z2, x1, y1, z1, r, g, b, a);
        line(buf, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buf, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buf, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buf, x1, y2, z2, x1, y2, z1, r, g, b, a);
        line(buf, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buf, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buf, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buf, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    //#if MC >= 12111
    private void line(BufferBuilder buf, float x1, float y1, float z1,
                      float x2, float y2, float z2, int r, int g, int b, int a) {
        buf.addVertex(x1, y1, z1).setColor(r, g, b, a).setLineWidth(1.0f);
        buf.addVertex(x2, y2, z2).setColor(r, g, b, a).setLineWidth(1.0f);
    }
    //#else
    //$$ private void line(BufferBuilder buf, float x1, float y1, float z1,
    //$$                   float x2, float y2, float z2, int r, int g, int b, int a) {
    //$$     buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
    //$$     buf.addVertex(x2, y2, z2).setColor(r, g, b, a);
    //$$ }
    //#endif
    //#endif

    // ===== Intermediate path: MC >= 1.21.1 && < 1.21.5 (direct BufferUploader) =====

    //#if MC >= 12101 && MC < 12105
    //$$ private void drawDirect(Vec3 cameraPos, List<HighlightEntry> entries, double expand,
    //$$                         boolean seeThrough, boolean hasOutline, boolean hasFilled) {
    //$$
    //$$     try {
    //$$         if (hasOutline) {
    //$$             BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
    //$$             for (HighlightEntry e : entries) {
    //$$                 if (e.style == HighlightStyleType.FILLED) continue;
    //$$                 addOutlineBoxDirect(buf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
    //$$             }
    //$$             MeshData mesh = buf.build();
    //$$             if (mesh != null) {
    //$$                 if (seeThrough) RenderSystem.disableDepthTest();
    //$$                 RenderSystem.enableBlend();
    //$$                 RenderSystem.defaultBlendFunc();
    //$$                 BufferUploader.drawWithShader(mesh);
    //$$                 RenderSystem.disableBlend();
    //$$                 if (seeThrough) RenderSystem.enableDepthTest();
    //$$                 mesh.close();
    //$$             }
    //$$         }
    //$$
    //$$         if (hasFilled) {
    //$$             BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    //$$             for (HighlightEntry e : entries) {
    //$$                 if (e.style == HighlightStyleType.OUTLINE) continue;
    //$$                 addFilledBoxDirect(buf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
    //$$             }
    //$$             MeshData mesh = buf.build();
    //$$             if (mesh != null) {
    //$$                 if (seeThrough) RenderSystem.disableDepthTest();
    //$$                 RenderSystem.enableBlend();
    //$$                 RenderSystem.defaultBlendFunc();
    //$$                 BufferUploader.drawWithShader(mesh);
    //$$                 RenderSystem.disableBlend();
    //$$                 if (seeThrough) RenderSystem.enableDepthTest();
    //$$                 mesh.close();
    //$$             }
    //$$         }
    //$$     } catch (Exception e) {
    //$$         Reference.LOGGER.error("BlockHighlight: drawDirect exception: {}", e.getMessage());
    //$$     }
    //$$ }
    //$$
    //$$ private void addFilledBoxDirect(BufferBuilder buf, BlockPos pos, double expand,
    //$$                                 int r, int g, int b, int a, Vec3 cameraPos) {
    //$$     float x1 = (float) (pos.getX() - cameraPos.x - expand);
    //$$     float y1 = (float) (pos.getY() - cameraPos.y - expand);
    //$$     float z1 = (float) (pos.getZ() - cameraPos.z - expand);
    //$$     float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
    //$$     float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
    //$$     float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);
    //$$
    //$$     triDirect(buf, x1, y1, z1, x2, y1, z1, x2, y1, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y2, z1, x2, y2, z2, x2, y2, z1, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z2, x2, y1, z2, x2, y2, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z1, x1, y1, z2, x1, y2, z2, r, g, b, a);
    //$$     triDirect(buf, x1, y1, z1, x1, y2, z2, x1, y2, z1, r, g, b, a);
    //$$     triDirect(buf, x2, y1, z1, x2, y2, z1, x2, y2, z2, r, g, b, a);
    //$$     triDirect(buf, x2, y1, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    //$$ }
    //$$
    //$$ private void triDirect(BufferBuilder buf, float x1, float y1, float z1,
    //$$                        float x2, float y2, float z2, float x3, float y3, float z3,
    //$$                        int r, int g, int b, int a) {
    //$$     buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
    //$$     buf.addVertex(x2, y2, z2).setColor(r, g, b, a);
    //$$     buf.addVertex(x3, y3, z3).setColor(r, g, b, a);
    //$$ }
    //$$
    //$$ private void addOutlineBoxDirect(BufferBuilder buf, BlockPos pos, double expand,
    //$$                                  int r, int g, int b, int a, Vec3 cameraPos) {
    //$$     float x1 = (float) (pos.getX() - cameraPos.x - expand);
    //$$     float y1 = (float) (pos.getY() - cameraPos.y - expand);
    //$$     float z1 = (float) (pos.getZ() - cameraPos.z - expand);
    //$$     float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
    //$$     float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
    //$$     float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);
    //$$
    //$$     edge3Direct(buf, x1, y1, z1, x2, y1, z1, r, g, b, a);
    //$$     edge3Direct(buf, x2, y1, z1, x2, y1, z2, r, g, b, a);
    //$$     edge3Direct(buf, x2, y1, z2, x1, y1, z2, r, g, b, a);
    //$$     edge3Direct(buf, x1, y1, z2, x1, y1, z1, r, g, b, a);
    //$$     edge3Direct(buf, x1, y2, z1, x2, y2, z1, r, g, b, a);
    //$$     edge3Direct(buf, x2, y2, z1, x2, y2, z2, r, g, b, a);
    //$$     edge3Direct(buf, x2, y2, z2, x1, y2, z2, r, g, b, a);
    //$$     edge3Direct(buf, x1, y2, z2, x1, y2, z1, r, g, b, a);
    //$$     edge3Direct(buf, x1, y1, z1, x1, y2, z1, r, g, b, a);
    //$$     edge3Direct(buf, x2, y1, z1, x2, y2, z1, r, g, b, a);
    //$$     edge3Direct(buf, x2, y1, z2, x2, y2, z2, r, g, b, a);
    //$$     edge3Direct(buf, x1, y1, z2, x1, y2, z2, r, g, b, a);
    //$$ }
    //$$
    //$$ private void edge3Direct(BufferBuilder buf, float x1, float y1, float z1,
    //$$                           float x2, float y2, float z2, int r, int g, int b, int a) {
    //$$     buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
    //$$     buf.addVertex(x2, y2, z2).setColor(r, g, b, a);
    //$$ }
    //#endif

    // ===== Legacy path: MC < 1.21.1 (old Tesselator API) =====

    //#if MC < 12101
    //$$ private void drawLegacy(Vec3 cameraPos, List<HighlightEntry> entries, double expand,
    //$$                         boolean seeThrough, boolean hasOutline, boolean hasFilled) {
    //$$
    //$$     Tesselator tesselator = Tesselator.getInstance();
    //$$     BufferBuilder buf = tesselator.getBuilder();
    //$$
    //$$     try {
    //$$         if (hasOutline) {
    //$$             buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
    //$$             for (HighlightEntry e : entries) {
    //$$                 if (e.style == HighlightStyleType.FILLED) continue;
    //$$                 addOutlineBoxLegacy(buf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
    //$$             }
    //$$             if (seeThrough) RenderSystem.disableDepthTest();
    //$$             RenderSystem.enableBlend();
    //$$             RenderSystem.defaultBlendFunc();
    //$$             tesselator.end();
    //$$             RenderSystem.disableBlend();
    //$$             if (seeThrough) RenderSystem.enableDepthTest();
    //$$         }
    //$$
    //$$         if (hasFilled) {
    //$$             buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    //$$             for (HighlightEntry e : entries) {
    //$$                 if (e.style == HighlightStyleType.OUTLINE) continue;
    //$$                 addFilledBoxLegacy(buf, e.pos, expand, e.r, e.g, e.b, (int)(e.alpha * 255), cameraPos);
    //$$             }
    //$$             if (seeThrough) RenderSystem.disableDepthTest();
    //$$             RenderSystem.enableBlend();
    //$$             RenderSystem.defaultBlendFunc();
    //$$             tesselator.end();
    //$$             RenderSystem.disableBlend();
    //$$             if (seeThrough) RenderSystem.enableDepthTest();
    //$$         }
    //$$     } catch (Exception e) {
    //$$         Reference.LOGGER.error("BlockHighlight: drawLegacy exception: {}", e.getMessage());
    //$$     }
    //$$ }
    //$$
    //$$ private void addFilledBoxLegacy(BufferBuilder buf, BlockPos pos, double expand,
    //$$                                 int r, int g, int b, int a, Vec3 cameraPos) {
    //$$     float x1 = (float) (pos.getX() - cameraPos.x - expand);
    //$$     float y1 = (float) (pos.getY() - cameraPos.y - expand);
    //$$     float z1 = (float) (pos.getZ() - cameraPos.z - expand);
    //$$     float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
    //$$     float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
    //$$     float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);
    //$$
    //$$     triLegacy(buf, x1, y1, z1, x2, y1, z1, x2, y1, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y2, z1, x2, y2, z2, x2, y2, z1, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z2, x2, y1, z2, x2, y2, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z1, x1, y1, z2, x1, y2, z2, r, g, b, a);
    //$$     triLegacy(buf, x1, y1, z1, x1, y2, z2, x1, y2, z1, r, g, b, a);
    //$$     triLegacy(buf, x2, y1, z1, x2, y2, z1, x2, y2, z2, r, g, b, a);
    //$$     triLegacy(buf, x2, y1, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    //$$ }
    //$$
    //$$ private void triLegacy(BufferBuilder buf, float x1, float y1, float z1,
    //$$                        float x2, float y2, float z2, float x3, float y3, float z3,
    //$$                        int r, int g, int b, int a) {
    //$$     buf.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
    //$$     buf.vertex(x2, y2, z2).color(r, g, b, a).endVertex();
    //$$     buf.vertex(x3, y3, z3).color(r, g, b, a).endVertex();
    //$$ }
    //$$
    //$$ private void addOutlineBoxLegacy(BufferBuilder buf, BlockPos pos, double expand,
    //$$                                  int r, int g, int b, int a, Vec3 cameraPos) {
    //$$     float x1 = (float) (pos.getX() - cameraPos.x - expand);
    //$$     float y1 = (float) (pos.getY() - cameraPos.y - expand);
    //$$     float z1 = (float) (pos.getZ() - cameraPos.z - expand);
    //$$     float x2 = (float) (pos.getX() - cameraPos.x + 1 + expand);
    //$$     float y2 = (float) (pos.getY() - cameraPos.y + 1 + expand);
    //$$     float z2 = (float) (pos.getZ() - cameraPos.z + 1 + expand);
    //$$
    //$$     edge3Legacy(buf, x1, y1, z1, x2, y1, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y1, z1, x2, y1, z2, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y1, z2, x1, y1, z2, r, g, b, a);
    //$$     edge3Legacy(buf, x1, y1, z2, x1, y1, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x1, y2, z1, x2, y2, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y2, z1, x2, y2, z2, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y2, z2, x1, y2, z2, r, g, b, a);
    //$$     edge3Legacy(buf, x1, y2, z2, x1, y2, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x1, y1, z1, x1, y2, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y1, z1, x2, y2, z1, r, g, b, a);
    //$$     edge3Legacy(buf, x2, y1, z2, x2, y2, z2, r, g, b, a);
    //$$     edge3Legacy(buf, x1, y1, z2, x1, y2, z2, r, g, b, a);
    //$$ }
    //$$
    //$$ private void edge3Legacy(BufferBuilder buf, float x1, float y1, float z1,
    //$$                           float x2, float y2, float z2, int r, int g, int b, int a) {
    //$$     buf.vertex(x1, y1, z1).color(r, g, b, a).endVertex();
    //$$     buf.vertex(x2, y2, z2).color(r, g, b, a).endVertex();
    //$$ }
    //#endif

    // ===== Shared Helpers =====

    private static int[] extractArgb(int argb) {
        return new int[]{
                (argb >> 24) & 0xFF,  // alpha
                (argb >> 16) & 0xFF,  // red
                (argb >> 8) & 0xFF,   // green
                argb & 0xFF            // blue
        };
    }

    private record HighlightEntry(BlockPos pos, float alpha, float distSq,
                                   int r, int g, int b, HighlightStyleType style) {}
}
