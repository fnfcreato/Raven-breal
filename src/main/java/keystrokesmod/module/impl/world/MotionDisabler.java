package keystrokesmod.module.impl.world;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.WorldChangeEvent;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class MotionDisabler extends Module {
    private boolean isFinished = false; // Keeps track of the module's state
    private int flagged = 0; // Counter for flagged packets
    private int offGroundTicks = 0; // Tracks how long the player is off the ground

    // Define settings
    public static ButtonSetting lobbyCheck = new ButtonSetting("Lobby Check", true);
    public SliderSetting a;

    public MotionDisabler() {
        super("MotionDisabler", category.world, 0);
    }

    // Check if the disabler is enabled and active
    public boolean isDisabled() {
        return isFinished;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (isFinished || !Utils.nullCheck() || mc.thePlayer.ticksExisted < 20) return;

        if (lobbyCheck.isToggled() && Utils.isLobby()) {
            return;
        }

        if (mc.thePlayer.onGround) {
            if (!Utils.jumpDown()) {
                mc.thePlayer.jump();
            }
        } else if (offGroundTicks >= 9) {
            if (offGroundTicks % 2 == 0) {
                event.setPosZ(event.getPosZ() + Utils.randomizeDouble(0.09, 0.12));  // Slight Z-axis adjustment
            }
            mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0.0;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(@NotNull ReceivePacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook && !isFinished) {
            flagged++;
            if (this.flagged == 20) {
                isFinished = true;
                flagged = 0;

                // Send a chat message instead of a notification
                mc.thePlayer.addChatMessage(new ChatComponentText("WatchDog Motion is disabled."));
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        isFinished = false;
        flagged = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }
    }

    @Override
    public void onDisable() {
        isFinished = false;
        offGroundTicks = 0;
    }

    @Override
    public void onEnable() {
        isFinished = false;
        flagged = 0;
    }
}
