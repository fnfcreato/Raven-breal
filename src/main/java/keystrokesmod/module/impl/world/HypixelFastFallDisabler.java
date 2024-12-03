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
        System.out.println("onPreMotion called"); // Debug entry point
        System.out.println("jump: " + jump + ", disabling: " + disabling + ", testTicks: " + testTicks);

        if (jump) {
            // Initialize jump state
            System.out.println("Initializing jump state");
            jump = false;
            disabling = true;
            timeTicks = mc.thePlayer.ticksExisted;
            System.out.println("timeTicks initialized to: " + timeTicks);
        } else if (disabling && mc.thePlayer instanceof IOffGroundTicks) {
            // Handle disabling logic
            int offGroundTicks = ((IOffGroundTicks) mc.thePlayer).getOffGroundTicks();
            System.out.println("offGroundTicks: " + offGroundTicks);

            if (offGroundTicks >= 10) {
                System.out.println("Freezing player motion during disabling");
                if (offGroundTicks % 2 == 0) {
                    event.setPosX(event.getPosX() + 0.095);
                    System.out.println("Adjusting PosX: " + event.getPosX());
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
        System.out.println("onSendPacket called"); // Debug entry point
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            System.out.println("Received S08PacketPlayerPosLook, testTicks: " + testTicks);

            if (testTicks == 30) {
                disabling = false; // Disable the logic after 30 ticks
                testTicks = 0;
                int totalTicks = mc.thePlayer.ticksExisted - timeTicks;
                System.out.println("Disabling complete, totalTicks: " + totalTicks);
                sendMessageToPlayer("Hypixel Fast Fall disabled in " + totalTicks + " ticks!");

                // Restore motion when disabler finishes
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
                System.out.println("Motion restored after disabling");
            } else {
                // Maintain freeze during disabling
                System.out.println("Maintaining freeze during disabling");
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
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
