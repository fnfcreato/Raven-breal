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
import keystrokesmod.utility.PacketUtils; 

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

public static boolean isDisabled() {
    // Check if the disabler module is enabled
    if (!ModuleManager.disabler.isEnabled()) return false;

    // Return the status of 'isFinished' as the primary indicator
    return isFinished;
}

@SubscribeEvent
public void onPreMotion(PreMotionEvent event) {
    if (isFinished || !Utils.nullCheck() || mc.thePlayer.ticksExisted < 20) return;

    if (lobbyCheck.isToggled() && Utils.isLobby()) {
        return;
    }

    if (mc.thePlayer.onGround) {
        if (!Utils.jumpDown()) {
            mc.thePlayer.jump();
        }
    } else if (offGroundTicks >= 9) {
        if (offGroundTicks % 2 == 0) {
            event.setPosZ(event.getPosZ() + Utils.randomizeDouble(0.09, 0.12));  // Slight Z-axis adjustment
        }
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0.0;
    }
}

@SubscribeEvent
public void onReceivePacket(@NotNull ReceivePacketEvent event) {
    if (event.getPacket() instanceof S08PacketPlayerPosLook && !isFinished) {
        flagged++;
        if (this.flagged == 20) {
            isFinished = true;
            flagged = 0;
            Notifications.sendNotification(Notifications.NotificationTypes.INFO, "WatchDog Motion is disabled.");
        }
    }
}

@SubscribeEvent
public void onWorldChange(WorldChangeEvent event) {
    isFinished = false;
    this.flagged = 0;
}

@Override
public void onUpdate() {
    if (mc.thePlayer.onGround) {
        offGroundTicks = 0;
    } else {
        offGroundTicks++;
    }
}

@Override
public void onDisable() {
    isFinished = false;
    offGroundTicks = 0;
}

@Override
public void onEnable() {
    onWorldChange(null);
}
