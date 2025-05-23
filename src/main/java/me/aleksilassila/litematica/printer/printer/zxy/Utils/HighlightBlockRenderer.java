package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.util.*;

import static fi.dy.masa.litematica.render.RenderUtils.renderAreaSidesBatched;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;
import static net.minecraft.client.render.VertexFormats.POSITION_COLOR;

public class HighlightBlockRenderer implements IRenderer {
    public static HighlightBlockRenderer instance = new HighlightBlockRenderer();
    public record HighlightTheProject(ConfigColor color4f, Set<BlockPos> pos){}
    public static Map<String,HighlightTheProject> highlightTheProjectMap = new HashMap<>();
    public static String threadName = "litematica-printer-render";
    public static boolean shaderIng = false;
    public static void createHighlightBlockList(String id,ConfigColor color4f){
        if (highlightTheProjectMap.get(id) == null) {
            highlightTheProjectMap.put(id,new HighlightTheProject(color4f,new LinkedHashSet <>()));
        }
    }
    public static Set<BlockPos> getHighlightBlockPosList(String id){
        if(highlightTheProjectMap.get(id) != null){
            return highlightTheProjectMap.get(id).pos();
        }
        return null;
    }
    public static List<String> clearList = new LinkedList<>();
    public static void clear(String id){
        if (!clearList.contains(id)) clearList.add(id);
    }
    public static Map<String,Set<BlockPos>> setMap = new HashMap<>();
    public static void setPos(String id,Set<BlockPos> posSet){
        HighlightTheProject highlightTheProject = highlightTheProjectMap.get(id);
        if (highlightTheProject != null && posSet != null) {
            setMap.put(id,posSet);
        }
    }
    public static Method method;

    public static void highlightBlock(Color4f color4f, BlockPos pos) {

        BlockState blockState = client.world.getBlockState(pos);
        Entity cameraEntity = client.cameraEntity;
        if(cameraEntity == null) return;
        VoxelShape voxelShape = blockState.getCollisionShape(client.world, pos,ShapeContext.of(cameraEntity));
        voxelShape = voxelShape.getBoundingBoxes().stream()
                .map(VoxelShapes::cuboid)
                .reduce(VoxelShapes::union)
                .orElse(VoxelShapes.empty()).simplify();
        Vec3d pos1 = client.gameRenderer.getCamera().getPos();
        double x = pos.getX() - pos1.x;
        double y = pos.getY() - pos1.y;
        double z = pos.getZ() - pos1.z;

        RenderSystem.disableDepthTest();

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -1.0F);
        //#if MC > 12006
        BuiltBuffer meshData;
        //#endif
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        //#if MC > 12101

        //#else
        //$$ RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        //#endif

        Tessellator instance = Tessellator.getInstance();
        //#if MC > 12006
        BufferBuilder buffer = instance.begin(VertexFormat.DrawMode.QUADS, POSITION_COLOR);
        //#else
        //$$ BufferBuilder buffer = instance.getBuffer();
        //#endif

        //#if MC > 12006
        voxelShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                RenderUtils.drawBoxAllSidesBatchedQuads(
                        (float)(minX + x),
                        (float)(minY + y),
                        (float)(minZ + z),
                        (float)(maxX + x),
                        (float)(maxY + y),
                        (float)(maxZ + z),
                        color4f, buffer));
        //#else
        //$$ if (!buffer.isBuilding()) buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        //$$ voxelShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
        //$$         RenderUtils.drawBoxAllSidesBatchedQuads(
        //$$                 minX + x,
        //$$                 minY + y,
        //$$                 minZ + z,
        //$$                 maxX + x,
        //$$                 maxY + y,
        //$$                 maxZ + z,
        //$$                 color4f, buffer));
        //#endif

        //#if MC > 12006
        try
        {
            meshData = buffer.end();
            BufferRenderer.drawWithGlobalProgram(meshData);
            meshData.close();
        }
        catch (Exception e)
        {
            Litematica.logger.error("renderSchematicMismatches: Failed to draw Schematic Mismatches (Step 2) (Error: {})", e.getLocalizedMessage());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        //#else
        //$$ instance.draw();
        //$$ RenderSystem.enableCull();
        //$$ RenderSystem.disableBlend();
        //$$ RenderSystem.enableDepthTest();
        //#endif

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    //#if MC > 12004
    public void test3(Matrix4f matrices,Color4f color4f, Set<BlockPos> posSet){
    //#else
    //$$ public void test3(MatrixStack matrices ,Color4f color4f, Set<BlockPos> posSet){
    //#endif
//        for (BlockPos pos : posSet) {
//            renderAreaSides(pos,pos,color4f,matrices,client);
//        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        //#if MC > 12101
        //#else
        //$$ RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        //#endif
        Tessellator tessellator = Tessellator.getInstance();

        //#if MC > 12006
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        BuiltBuffer meshData;
        //#else
        //$$ BufferBuilder buffer = tessellator.getBuffer();
        //$$ buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        //#endif
        for (BlockPos pos : posSet) {
            renderAreaSidesBatched(pos, pos, color4f, 0.002, buffer, client);
        }

        try
        {
            if(buffer != null){
                //#if MC > 12006
                meshData = buffer.end();
                BufferRenderer.drawWithGlobalProgram(meshData);
                meshData.close();
                //#else
                //$$ tessellator.draw();
                //#endif
            }
        }
        catch (Exception e)
        {
//            Litematica.logger.error("renderAreaSides: Failed to draw Area Selection box (Error: {})", e.getLocalizedMessage());
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

//        fi.dy.masa.litematica.render.RenderUtils.renderAreaSides(pos, pos, color4f, matrices, client);
    }

    //如果不注册无法渲染，
    public static void init(){
        RenderEventHandler.getInstance().registerWorldLastRenderer(instance);
//        MyThreadManager.createThread(threadName,new Thread(() -> {
//            while (!Thread.currentThread().isInterrupted()){
//                try {
//                    Thread.sleep(80);
//                } catch (InterruptedException ignored) {}
//
//
//            }
//        }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> {
            for (Map.Entry<String, HighlightTheProject> stringHighlightTheProjectEntry : highlightTheProjectMap.entrySet()) {
                stringHighlightTheProjectEntry.getValue().pos.clear();
            }
        });
    }

    @Override
    //#if MC > 12004
    public void onRenderWorldLast(Matrix4f matrices, Matrix4f projMatrix){
    //#else
    //$$ public void onRenderWorldLast(MatrixStack matrices, Matrix4f projMatrix){
    //#endif
        //更改渲染
        setMap.forEach((k,v) -> {
            HighlightTheProject highlightTheProject = highlightTheProjectMap.get(k);
            if(highlightTheProject != null){
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
            test3(matrices ,color,value.pos);

        });
        shaderIng = false;
    }
}