package me.aleksilassila.litematica.printer.printer.zxy.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

import java.util.*;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;


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
//#endif

//#if MC <= 12104
//$$ @SuppressWarnings("deprecation")
//#endif
public class HighlightBlockRenderer implements IRenderer {
    public static HighlightBlockRenderer instance = new HighlightBlockRenderer();
    public static Map<String, HighlightTheProject> highlightTheProjectMap = new HashMap<>();
    public static String threadName = "litematica-printer-render";
    public static boolean shaderIng = false;
    public static List<String> clearList = new LinkedList<>();
    public static Map<String, Set<BlockPos>> setMap = new HashMap<>();

    public static void createHighlightBlockList(String id, ConfigColor color4f) {
        if (highlightTheProjectMap.get(id) == null) {
            highlightTheProjectMap.put(id, new HighlightTheProject(color4f, new LinkedHashSet<>()));
        }
    }

    public static Set<BlockPos> getHighlightBlockPosList(String id) {
        if (highlightTheProjectMap.get(id) != null) {
            return highlightTheProjectMap.get(id).pos();
        }
        return null;
    }

    public static void clear(String id) {
        if (!clearList.contains(id)) clearList.add(id);
    }

    public static void setPos(String id, Set<BlockPos> posSet) {
        HighlightTheProject highlightTheProject = highlightTheProjectMap.get(id);
        if (highlightTheProject != null && posSet != null) {
            setMap.put(id, posSet);
        }
    }

    //如果不注册无法渲染，
    public static void init() {
        RenderEventHandler.getInstance().registerWorldLastRenderer(instance);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> {
            for (Map.Entry<String, HighlightTheProject> stringHighlightTheProjectEntry : highlightTheProjectMap.entrySet()) {
                stringHighlightTheProjectEntry.getValue().pos.clear();
            }
        });
    }

    // @formatter:off

    //#if MC > 12004
    public void test3(Matrix4f matrices, Color4f color4f, Set<BlockPos> posSet) {
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
        Tesselator tesselator = Tesselator.getInstance();

        //#if MC > 12006
            //#if MC > 12104
                //#if MC == 12105
                //$$ RenderContext ctx = new RenderContext(MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT, BufferUsage.STATIC_WRITE);
                //#else
                RenderContext ctx = new RenderContext(() -> threadName, MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT);
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
                    ctx.upload(meshData, true);
                    ctx.startResorting(meshData, ctx.createVertexSorter(fi.dy.masa.malilib.render.RenderUtils.camPos()));
                    meshData.close();
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
//            Litematica.logger.error("renderAreaSides: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
        }

        //#if MC > 12104
        RenderSystem.setShaderFog(RenderSystem.getShaderFog());
        //#else
        //$$ RenderSystem.enableCull();
        //$$ RenderSystem.disableBlend();
        //#endif


//        fi.dy.masa.litematica.render.RenderUtils.renderAreaSides(pos, pos, color4f, matrices, client);
    }

    @Override
    //#if MC > 12004
    public void onRenderWorldLast(Matrix4f matrices, Matrix4f projMatrix) {
    //#else
    //$$ public void onRenderWorldLast(PoseStack matrices, Matrix4f projMatrix){
    //#endif
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

        shaderIng = true;
        highlightTheProjectMap.entrySet().stream().parallel().forEach(stringHighlightTheProjectEntry -> {
            String key = stringHighlightTheProjectEntry.getKey();
            HighlightTheProject value = stringHighlightTheProjectEntry.getValue();

            Color4f color = value.color4f.getColor();
            test3(matrices, color, value.pos);

        });
        shaderIng = false;
    }

    // @formatter:on

    public record HighlightTheProject(ConfigColor color4f, Set<BlockPos> pos) {
    }
}