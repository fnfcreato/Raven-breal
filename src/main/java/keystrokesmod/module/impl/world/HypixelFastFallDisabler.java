package .modules.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S07PacketRespawn;

public class HypixelFastFallDisabler {
    private boolean jump = false;
    private boolean disabling = false;
    private int testTicks = 0;
    private int timeTicks = 0;

    public void onEnable() {
        jump = true;
        disabling = false;
        testTicks = 0;
    }

    public void onPreMotion(PreMotionEvent event) {
        if (jump) {
            // Start jump logic
            jump = false;
            disabling = true;
            timeTicks = mc.thePlayer.ticksExisted;
        } else if (disabling && mc.thePlayer.offGroundTicks >= 10) {
            if (mc.thePlayer.offGroundTicks % 2 == 0) {
                // Adjust X position for fast-fall
                event.setPosX(event.getPosX() + 0.095);
            }
            // Freeze player motion
            mc.thePlayer.motionY = mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
        }
    }

    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S07PacketRespawn) {
            jump = true; // Reset jump state after respawn
        } else if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            if (testTicks == 30) {
                disabling = false;
                testTicks = 0;
                timeTicks = mc.thePlayer.ticksExisted - timeTicks;
                System.out.println("Hypixel Fast Fall Disabled in " + timeTicks + " ticks!");
            } else {
                // Maintain freeze while disabling
                mc.thePlayer.motionY = mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
            }
        }
    }
}
