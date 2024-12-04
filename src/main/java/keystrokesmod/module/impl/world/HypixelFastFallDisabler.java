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
    private boolean hasJumped = false;
    private boolean isFreezing = false;

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
    }
    
    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (Utils.nullCheck()) return;

        if (mc.thePlayer.onGround) {
            // Reset when on ground
            offGroundTicks = 0;
            hasJumped = false;
            isFreezing = false;
        } else {
            // Increment off-ground ticks
            offGroundTicks++;
        }

        // Trigger jump if we haven't already jumped
        if (!hasJumped) {
            mc.thePlayer.jump();
            hasJumped = true;
            System.out.println("[DEBUG] Player jumped.");
        }

        // After jumping, freeze motion to disable jump checks
        if (offGroundTicks >= 5 && !isFreezing) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = -0.1; // Simulate gentle downward motion
            mc.thePlayer.motionZ = 0.0;
            isFreezing = true;
            System.out.println("[DEBUG] Player motion frozen to bypass jump checks.");
        }
    }

    @Override
    public void onEnable() {
        offGroundTicks = 0;
        hasJumped = false;
        isFreezing = false;

        sendMessageToPlayer("Hypixel Fast Fall enabled.");
        System.out.println("[DEBUG] Hypixel Fast Fall enabled.");
    }

    @Override
    public void onDisable() {
        // Reset player motion and state
        offGroundTicks = 0;
        hasJumped = false;
        isFreezing = false;

        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = -0.0784; // Default gravity
            mc.thePlayer.motionZ = 0.0;
        }

        sendMessageToPlayer("Hypixel Fast Fall disabled.");
        System.out.println("[DEBUG] Hypixel Fast Fall disabled.");
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
