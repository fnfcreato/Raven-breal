package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MotionDisabler extends Module {
    private boolean isActive = false;
    private int offGroundTicks = 0;
    private int correctionCount = 0;
    private float progress = 0; 
    private boolean motionRestored = false;

    public MotionDisabler() {
        super("Motion Disabler", ModuleCategory.world);
    }

    @Override
    public void onEnable() {
        isActive = true;
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        isActive = false;
        offGroundTicks = 0;
        correctionCount = 0;
        progress = 0;
        motionRestored = false;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!isActive) return;
        if (!mc.thePlayer.onGround) {
            offGroundTicks++;
            if (offGroundTicks >= 10) {
                PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY + 0.04, mc.thePlayer.posZ, false));
                progress = Math.min(progress + 10, 100);
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            correctionCount++;
            if (correctionCount >= 5) {
                correctionCount = 0;
                progress = Math.max(progress - 20, 0); // Decrease progress, min at 0
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!isActive) return;

        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution sr = new ScaledResolution(mc);
            int width = sr.getScaledWidth();
            int height = sr.getScaledHeight();
            int barWidth = 150, barHeight = 10, x = (width - barWidth) / 2, y = height / 2 + 50;

            drawBar(x, y, barWidth, barHeight, progress / 100);
        }
    }

    private void drawBar(int x, int y, int width, int height, float percentage) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        worldRenderer.startDrawingQuads();
        worldRenderer.setColorOpaque_I(0x90000000); // Semi-transparent black
        worldRenderer.addVertex(x, y + height, 0.0);
        worldRenderer.addVertex(x + width, y + height, 0.0);
        worldRenderer.addVertex(x + width, y, 0.0);
        worldRenderer.addVertex(x, y, 0.0);
        tessellator.draw();
    }
}
