package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import keystrokesmod.mixins.interfaces.IOffGroundTicks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import keystrokesmod.utility.Utils;

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
        if (mc.thePlayer != null && mc.thePlayer instanceof IOffGroundTicks) {
            IOffGroundTicks player = (IOffGroundTicks) mc.thePlayer;
            int offGroundTicks = player.getOffGroundTicks();

            if (!disabling && jump) {
                jump = false;
                disabling = true;
                timeTicks = mc.thePlayer.ticksExisted;
                System.out.println("[DEBUG] Disabler started, ticks: " + timeTicks);
            }

            if (disabling) {
                if (offGroundTicks >= 10) {
                    // Reduce freezing to temporary
                    mc.thePlayer.motionX = 0.0;
                    mc.thePlayer.motionY = -0.01; // Gentle gravity effect
                    mc.thePlayer.motionZ = 0.0;
                } else if (offGroundTicks == 0) {
                    disabling = false; // Reset disabling when player lands
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            if (testTicks >= 30) { // Timeout condition
                disabling = false;
                testTicks = 0;

                int totalTicks = mc.thePlayer.ticksExisted - timeTicks;
                sendMessageToPlayer("Hypixel Fast Fall disabled in " + totalTicks + " ticks!");
                System.out.println("[DEBUG] Disabler ended after " + totalTicks + " ticks.");

                // Restore normal motion
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = -0.0784; // Natural gravity
                mc.thePlayer.motionZ = 0.0;
            }
        }
    }

    @Override
    public void onDisable() {
        jump = true;
        disabling = false;
        testTicks = 0;
        timeTicks = 0;

        mc.thePlayer.motionX = 0.0;
        mc.thePlayer.motionY = -0.0784; // Natural gravity
        mc.thePlayer.motionZ = 0.0;

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
