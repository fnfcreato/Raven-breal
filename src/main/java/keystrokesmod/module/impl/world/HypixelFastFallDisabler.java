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

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (Utils.nullCheck()) return;

        // Increment or reset offGroundTicks
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }

        if (isDisabling) {
            if (offGroundTicks > 10) {
                // Force freeze the player
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = -0.1; // Freeze with slight downward motion
                mc.thePlayer.motionZ = 0.0;

                // Log the freezing behavior
                System.out.println("[DEBUG] Player frozen. offGroundTicks: " + offGroundTicks);

                // Slight position adjustment to simulate natural motion
                event.setPosX(event.getPosX() + Utils.randomizeDouble(-0.01, 0.01));
                event.setPosZ(event.getPosZ() + Utils.randomizeDouble(-0.01, 0.01));
            }
        }
    }

    @Override
    public void onEnable() {
        isDisabling = true;
        offGroundTicks = 0;

        if (mc.thePlayer != null) {
            sendMessageToPlayer("Hypixel Fast Fall enabled.");
            System.out.println("[DEBUG] Hypixel Fast Fall enabled.");
        }
    }

    @Override
    public void onDisable() {
        isDisabling = false;
        offGroundTicks = 0;

        // Reset motion to normal
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = -0.0784; // Default gravity
            mc.thePlayer.motionZ = 0.0;

            sendMessageToPlayer("Hypixel Fast Fall disabled.");
            System.out.println("[DEBUG] Hypixel Fast Fall disabled.");
        }
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
