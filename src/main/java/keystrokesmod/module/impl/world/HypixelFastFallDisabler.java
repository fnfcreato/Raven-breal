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
    private boolean jump = false;
    private boolean disabling = false;
    private boolean disabled = false;
    private int offGroundTicks = 0;
    private int testTicks = 0;
    private int timeTicks = 0;

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0);
    }

    @SubscribeEvent
    public void onMotion(MotionEvent event) {
        if (Utils.nullCheck()) return;

        if (mc.thePlayer.onGround) {
            // Reset state when player lands
            offGroundTicks = 0;
            jump = false;
            disabling = false;
        } else {
            // Increment off-ground ticks
            offGroundTicks++;
        }

        if (!jump && mc.thePlayer.onGround) {
            // Simulate a jump
            mc.thePlayer.jump();
            jump = true;
            disabling = true;
            timeTicks = mc.thePlayer.ticksExisted;
            System.out.println("[DEBUG] Player jumped.");
        }

        if (disabling && offGroundTicks >= 10) {
            // Freeze player movement
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0; // Disable upward/downward motion
            mc.thePlayer.motionZ = 0.0;

            if (offGroundTicks % 2 == 0) {
                // Adjust position slightly to mimic natural movement
                event.setX(event.getX() + 0.095);
                event.setZ(event.getZ() + 0.095);
            }
            System.out.println("[DEBUG] Freezing player motion. OffGroundTicks: " + offGroundTicks);
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;

            if (testTicks >= 30) {
                // Disable the disabler after 30 packets
                disabling = false;
                testTicks = 0;
                disabled = true;

                int ticksTaken = mc.thePlayer.ticksExisted - timeTicks;
                sendMessageToPlayer("Jump check disabled in " + ticksTaken + " ticks!");
                System.out.println("[DEBUG] Jump check disabled in " + ticksTaken + " ticks.");
            } else if (disabling) {
                // Freeze player motion when flagged by Hypixel
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;
                System.out.println("[DEBUG] Player motion frozen after S08PacketPlayerPosLook.");
            }
        }
    }

    @Override
    public void onEnable() {
        jump = false;
        disabling = false;
        disabled = false;
        offGroundTicks = 0;
        testTicks = 0;
        timeTicks = 0;

        sendMessageToPlayer("Hypixel Fast Fall enabled.");
        System.out.println("[DEBUG] Hypixel Fast Fall enabled.");
    }

    @Override
    public void onDisable() {
        jump = false;
        disabling = false;
        disabled = false;
        offGroundTicks = 0;
        testTicks = 0;
        timeTicks = 0;

        // Reset motion
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
