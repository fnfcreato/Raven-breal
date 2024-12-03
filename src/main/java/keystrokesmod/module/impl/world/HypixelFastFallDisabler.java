package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import keystrokesmod.mixins.interfaces.IOffGroundTicks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HypixelFastFallDisabler extends Module {
    private boolean isFinished = false; // Track module's state
    private int flagged = 0; // Counter for flagged packets
    private int offGroundTicks = 0; // Track off-ground ticks

    public static ButtonSetting disableMessage = new ButtonSetting("Disable Message", true); // Optional setting for messaging

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
        this.registerSetting(disableMessage);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (isFinished || !(mc.thePlayer instanceof IOffGroundTicks)) return;

        offGroundTicks = ((IOffGroundTicks) mc.thePlayer).getOffGroundTicks();

        if (offGroundTicks >= 10) {
            if (offGroundTicks % 2 == 0) {
                event.setPosX(event.getPosX() + 0.095); // Adjust player's X position
            }
            // Freeze motion during disabler
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook && !isFinished) {
            flagged++;
            if (flagged == 20) {
                isFinished = true; // Mark the disabler as finished
                flagged = 0;

                if (disableMessage.isToggled()) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("Hypixel Fast Fall disabled."));
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        // Reset offGroundTicks to ensure proper tracking
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        }
    }

    @Override
    public void onEnable() {
        // Reset state when enabled
        isFinished = false;
        flagged = 0;
        offGroundTicks = 0;

        if (disableMessage.isToggled()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("Hypixel Fast Fall enabled."));
        }
    }

    @Override
public void onDisable() {
    isFinished = false;
    flagged = 0;
    offGroundTicks = 0;

    // Restore player motion
    mc.thePlayer.motionX = 0;
    mc.thePlayer.motionY = 0;
    mc.thePlayer.motionZ = 0;

    if (disableMessage.isToggled()) {
        mc.thePlayer.addChatMessage(new ChatComponentText("Hypixel Fast Fall disabled."));
    }
}
