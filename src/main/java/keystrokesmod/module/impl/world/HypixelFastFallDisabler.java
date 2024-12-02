package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S07PacketRespawn;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HypixelFastFallDisabler extends Module {
    private boolean jump = false;
    private boolean disabling = false;
    private int testTicks = 0;
    private int timeTicks = 0;

    public HypixelFastFallDisabler() {
        super("Hypixel Fast Fall", Module.category.world, 0); // No keybinding by default
    }

    @Override
    public void onEnable() {
        jump = true;
        disabling = false;
        testTicks = 0;
    }

    @Override
    public void onDisable() {
        jump = false;
        disabling = false;
    }

    @SubscribeEvent
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
    @SubscribeEvent
public void onUpdate() {
    if (mc.thePlayer.onGround) {
        offGroundTicks = 0;
    } else {
        offGroundTicks++;
    }
}
    
    @SubscribeEvent
    public void onSendPacket(SendPacketEvent event) {
        if (event.getPacket() instanceof S07PacketRespawn) {
            jump = true;
            // Reset jump state after respawn
            sendMessageToPlayer("Hypixel Fast Fall reset after respawn.");
        } else if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            testTicks++;
            if (testTicks == 30) {
                disabling = false;
                testTicks = 0;
                timeTicks = mc.thePlayer.ticksExisted - timeTicks;
                sendMessageToPlayer("Hypixel Fast Fall disabled in " + timeTicks + " ticks!");
            } else {
                // Maintain freeze while disabling
                mc.thePlayer.motionY = mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
            }
        }
    }

    private void sendMessageToPlayer(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
