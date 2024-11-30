package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.WorldChangeEvent;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.Minecraft; // Accesses the Minecraft game instance.
import net.minecraft.client.gui.ScaledResolution; // Handles GUI scaling.
import net.minecraft.client.renderer.GlStateManager; // Used for rendering.
import net.minecraft.client.renderer.Tessellator; // Helps with vertex-based rendering.
import net.minecraft.client.renderer.WorldRenderer; // Handles world rendering.
import net.minecraft.client.renderer.vertex.DefaultVertexFormats; // Defines vertex formats.
import net.minecraft.network.play.client.C03PacketPlayer; // Packet for sending player movement.
import net.minecraft.network.play.server.S08PacketPlayerPosLook; // Packet for receiving server position corrections.
import net.minecraft.util.ChatComponentText; // Handles chat messages.
import keystrokesmod.module.ModuleManager; // fixed this, dawg im so blind Ah
import net.minecraftforge.client.event.RenderGameOverlayEvent; 
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent; 
import keystrokesmod.utility.PacketUtils; // another easy fix that i forgot to add to fix build gradle error.

public class MotionDisabler extends Module {
    private boolean isActive = false;
    private int offGroundTicks = 0;
    private int correctionCount = 0;
    private float progress = 0;
    private boolean motionRestored = false;

    public SliderSetting a;

    // did something wrong here i didnt saw it my bad.
    public MotionDisabler() {
        super("MotionDisabler", category.world, 0);
    }

    @Override
    public void onEnable() {
        isActive = true;
        resetState();
        sendMessage("Motion Disabler Activated");
    }

    @Override
    public void onDisable() {
        resetState();
        sendMessage("Motion Disabler Deactivated");
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
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(
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
                sendMessage("Warning: Anti-cheat corrections detected!");
                correctionCount = 0;
                progress = Math.max(progress - 20, 0);
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
        WorldRenderer buffer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // Background
        buffer.pos(x, y + height, 0).color(0, 0, 0, 150).endVertex();
        buffer.pos(x + width, y + height, 0).color(0, 0, 0, 150).endVertex();
        buffer.pos(x + width, y, 0).color(0, 0, 0, 150).endVertex();
        buffer.pos(x, y, 0).color(0, 0, 0, 150).endVertex();
        tessellator.draw();

        // Progress
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y + height, 0).color(0, 255, 0, 150).endVertex();
        buffer.pos(x + (width * percentage), y + height, 0).color(0, 255, 0, 150).endVertex();
        buffer.pos(x + (width * percentage), y, 0).color(0, 255, 0, 150).endVertex();
        buffer.pos(x, y, 0).color(0, 255, 0, 150).endVertex();
        tessellator.draw();
    }

    protected void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText("[MotionDisabler] " + message));
    }
}
