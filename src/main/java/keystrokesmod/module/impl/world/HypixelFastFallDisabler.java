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
    private boolean jump = true;
    private boolean disabling = false;
    private int testTicks = 0;
    private int timeTicks = 0;

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (jump) {
            // Initialize jump state
            jump = false;
            disabling = true;
            timeTicks = mc.thePlayer.ticksExisted;
        } else if (disabling && mc.thePlayer instanceof IOffGroundTicks) {
            // Handle disabling logic
            int offGroundTicks = ((IOffGroundTicks) mc.thePlayer).getOffGroundTicks();
            if (offGroundTicks >= 10) {
                if (offGroundTicks % 2 == 0) {
                    event.setPosX(event.getPosX() + 0.095);
                }
                // Freeze player motion during disabler
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            if (testTicks == 30) {
                disabling = false; // Disable the logic after 30 ticks
                testTicks = 0;
                int totalTicks = mc.thePlayer.ticksExisted - timeTicks;
                sendMessageToPlayer("Hypixel Fast Fall disabled in " + totalTicks + " ticks!");

                // Restore motion when disabler finishes
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            } else {
                // Maintain freeze during disabling
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            }
        }
    }

    @Override
    public void onDisable() {
        jump = true;
        disabling = false;
        testTicks = 0;
        timeTicks = 0;

        // Restore player motion on module disable
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;

        sendMessageToPlayer("Hypixel Fast Fall disabled.");
    }

    @Override
    public void onEnable() {
        jump = true;
        disabling = false;
        testTicks = 0;
        timeTicks = 0;

        sendMessageToPlayer("Hypixel Fast Fall enabled.");
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
