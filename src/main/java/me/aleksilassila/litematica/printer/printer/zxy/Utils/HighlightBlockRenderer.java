package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;


public class HighlightBlockRenderer implements IRenderer {
    public static HighlightBlockRenderer instance = new HighlightBlockRenderer();
    public static Map<Color4f,List<BlockPos>> highlightMap = new HashMap<>();
    public static void addHighlightMap(Color4f color4f){
        if(highlightMap.get(color4f) != null) return;
        highlightMap.put(color4f, new LinkedList<>());
    }
    public static List<BlockPos> getPosList(Color4f color4f){
        return highlightMap.get(color4f);
    }
    public void highlightBlock(Color4f color4f, BlockPos pos) {
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
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator instance = Tessellator.getInstance();
        BufferBuilder buffer = instance.getBuffer();

        if (!buffer.isBuilding()) buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        voxelShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                RenderUtils.drawBoxAllSidesBatchedQuads(
                        minX + x,
                        minY + y,
                        minZ + z,
                        maxX + x,
                        maxY + y,
                        maxZ + z,
                        color4f, buffer));

        instance.draw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }
    public void test3(MatrixStack matrices){

        BlockPos pos1 = client.player.getBlockPos().up(-1);
        BlockPos pos2 = client.player.getBlockPos().up(-2);
        fi.dy.masa.litematica.render.RenderUtils.renderAreaSides(pos1, pos1, new Color4f(1,1,0,0.5F), matrices, client);
    }

    //如果不注册无法渲染，
    public static void init(){
        RenderEventHandler.getInstance().registerGameOverlayRenderer(instance);
        RenderEventHandler.getInstance().registerWorldLastRenderer(instance);
    }

    @Override
    public void onRenderGameOverlayPost(DrawContext drawContext){

    }
    @Override
    public void onRenderWorldLast(MatrixStack matrices, Matrix4f projMatrix){
        for (Map.Entry<Color4f, List<BlockPos>> map : highlightMap.entrySet()) {
            Color4f key = map.getKey();
            for (BlockPos blockPos : map.getValue()) {
                instance.highlightBlock(key,blockPos);
            }
        }
    }
}
