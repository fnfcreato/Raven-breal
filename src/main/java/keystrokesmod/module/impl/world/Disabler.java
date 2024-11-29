package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.WorldChangeEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MotionDisabler extends Module {
    private boolean isActive = false;
    private int offGroundTicks = 0;
    private int correctionCount = 0;
    private float progress = 0; // Progress from 0 to 100

    public MotionDisabler() {
        super("Motion Disabler", ModuleCategory.world);
    }

    @Override
    public void onEnable() {
        isActive = true;
        offGroundTicks = 0;
        correctionCount = 0;
        progress = 0;
        sendMessage("Motion Disabler Activated");
    }

    @Override
    public void onDisable() {
        isActive = false;
        offGroundTicks = 0;
        correctionCount = 0;
        progress = 0;
        sendMessage("Motion Disabler Deactivated");
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!isActive || mc.thePlayer == null || mc.theWorld == null) return;

        // Add small vertical motion and packet manipulation when off-ground
        if (!mc.thePlayer.onGround) {
            offGroundTicks++;

            if (offGroundTicks >= 10) {
                // Simulate jitter to create desync
                double jitter = (offGroundTicks % 2 == 0) ? 0.04 : -0.04;
                event.setPosY(event.getPosY() + jitter);

                // Send manipulated position packet
                PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY + jitter,
                        mc.thePlayer.posZ,
                        mc.thePlayer.onGround
                ));

                // Reset vertical motion to simulate being stuck
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;

                // Update progress
                progress = Math.min(progress + 10, 100); // Increment by 10, max at 100
            }
        } else {
            offGroundTicks = 0; // Reset counter when back on ground
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            correctionCount++;
            if (correctionCount >= 5) {
                sendMessage("Warning: Anti-cheat corrections detected!");
                correctionCount = 0; // Reset counter
                progress = Math.max(progress - 20, 0); // Decrease progress, min at 0
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        // Reset state when changing worlds
        isActive = false;
        offGroundTicks = 0;
        correctionCount = 0;
        progress = 0;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!isActive) return;

        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution scaledRes = new ScaledResolution(mc);

            int width = scaledRes.getScaledWidth();
            int height = scaledRes.getScaledHeight();

            int barWidth = 150; // Width of the progress bar
            int barHeight = 10; // Height of the progress bar
            int x = (width - barWidth) / 2; // Center horizontally
            int y = height / 2 + 50; // Position slightly below center

            
            drawRect(x, y, x + barWidth, y + barHeight, 0x90000000); // Semi-transparent black

            
            drawRect(x, y, x + (int) (progress / 100 * barWidth), y + barHeight, 0xFF00FF00); // Green progress

            
            String progressText = "Disabler " + (int) progress + "%";
            mc.fontRendererObj.drawStringWithShadow(progressText, x + barWidth / 2 - mc.fontRendererObj.getStringWidth(progressText) / 2, y - 10, 0xFFFFFF);
        }
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        // Utility method to draw rectangles on the screen
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        worldRenderer.startDrawingQuads();
        worldRenderer.setColorOpaque_I(color);
        worldRenderer.addVertex(left, bottom, 0.0);
        worldRenderer.addVertex(right, bottom, 0.0);
        worldRenderer.addVertex(right, top, 0.0);
        worldRenderer.addVertex(left, top, 0.0);
        tessellator.draw();
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
    }
}
