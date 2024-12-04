package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import keystrokesmod.utility.Utils;

public class HypixelFastFallDisabler extends Module {
    private boolean isDisabling = false;
    private int offGroundTicks = 0;
    private int flaggedTicks = 0;

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (Utils.nullCheck()) return;

        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }

        if (isDisabling) {
            if (offGroundTicks > 10) {
                // Forcefully freeze player movement to prevent detection
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = -0.1; // Simulates fall, slightly stronger than default gravity
                mc.thePlayer.motionZ = 0.0;

                // Adjust position slightly to mimic natural movement
                event.setPosX(event.getPosX() + Utils.randomizeDouble(-0.01, 0.01));
                event.setPosZ(event.getPosZ() + Utils.randomizeDouble(-0.01, 0.01));
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            flaggedTicks++;
            if (flaggedTicks > 5) {
                // Reset the disabling process after excessive flagging
                isDisabling = false;
                flaggedTicks = 0;

                sendMessageToPlayer("Fast Fall detected by Hypixel, resetting...");
                System.out.println("[DEBUG] Resetting Fast Fall disabler.");
            }
        }
    }

    @Override
    public void onEnable() {
        isDisabling = true;
        offGroundTicks = 0;
        flaggedTicks = 0;

        sendMessageToPlayer("Hypixel Fast Fall enabled.");
    }

    @Override
    public void onDisable() {
        isDisabling = false;
        offGroundTicks = 0;
        flaggedTicks = 0;

        // Restore normal motion
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = -0.0784; // Default gravity
            mc.thePlayer.motionZ = 0.0;
        }

        sendMessageToPlayer("Hypixel Fast Fall disabled.");
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
