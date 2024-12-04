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
        // Cast mc.thePlayer to IOffGroundTicks
        IOffGroundTicks player = (IOffGroundTicks) mc.thePlayer;

        // Access offGroundTicks using the interface method
        int offGroundTicks = player.getOffGroundTicks();
            
        if (!disabling && jump) {
            jump = false;
            disabling = true;
            timeTicks = mc.thePlayer.ticksExisted;
            System.out.println("[DEBUG] Disabler started, ticks: " + timeTicks);
        }

        if (disabling) {
            if (offGroundTicks >= 10) {
                // Freeze player
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;
            } else {
                disabling = false; // Properly exit disabling state
                mc.thePlayer.motionY = -0.0784; // Reset gravity
            }
        }
        }
    }
    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            if (testTicks == 30) { // End disabling after 30 ticks
                disabling = false;
                testTicks = 0;

                int totalTicks = mc.thePlayer.ticksExisted - timeTicks;
                sendMessageToPlayer("Hypixel Fast Fall disabled in " + totalTicks + " ticks!");
                System.out.println("[DEBUG] Disabler ended after " + totalTicks + " ticks.");

                // Ensure player regains control
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = -0.0784; // Allow natural gravity to resume
                mc.thePlayer.motionZ = 0.0;
            }
        }
    }

    @Override
    public void onDisable() {
        System.out.println("onDisable called"); // Debug entry point
        jump = true;
        disabling = false;
        testTicks = 0;
        timeTicks = 0;

        // Restore player motion on module disable
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;

        sendMessageToPlayer("Hypixel Fast Fall disabled.");
        System.out.println("Module disabled, states reset");
    }

    @Override
    public void onEnable() {
        System.out.println("onEnable called"); // Debug entry point
        jump = true;
        disabling = false;
        testTicks = 0;
        timeTicks = 0;

        sendMessageToPlayer("Hypixel Fast Fall enabled.");
        System.out.println("Module enabled, states reset");
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
            System.out.println("Message sent to player: " + message);
        } else {
            System.out.println("mc.thePlayer is null, message not sent");
        }
    }
}
