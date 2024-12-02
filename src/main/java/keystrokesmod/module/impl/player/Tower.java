package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Tower extends Module {
    private SliderSetting mode;
    private SliderSetting speed;
    private SliderSetting diagonalSpeed;
    private SliderSetting slowedSpeed;
    private SliderSetting slowedTicks;
    private ButtonSetting disableInLiquid;
    private ButtonSetting disableWhileCollided;
    private ButtonSetting disableWhileHurt;
    private ButtonSetting sprintJumpForward;

    private String[] modes = new String[]{"Vanilla", "Low", "Hypixel"};
    private int offGroundTicks;
    private boolean isTowering;

    public Tower() {
        super("Tower", category.player);
        this.registerSetting(new DescriptionSetting("Works with Safewalk & Scaffold"));
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(speed = new SliderSetting("Speed", 5, 0, 10, 0.1));
        this.registerSetting(diagonalSpeed = new SliderSetting("Diagonal speed", 5, 0, 10, 0.1));
        this.registerSetting(slowedSpeed = new SliderSetting("Slowed speed", 2, 0, 9, 0.1));
        this.registerSetting(slowedTicks = new SliderSetting("Slowed ticks", 1, 0, 20, 1));
        this.registerSetting(disableInLiquid = new ButtonSetting("Disable in liquid", false));
        this.registerSetting(disableWhileCollided = new ButtonSetting("Disable while collided", false));
        this.registerSetting(disableWhileHurt = new ButtonSetting("Disable while hurt", false));
        this.registerSetting(sprintJumpForward = new ButtonSetting("Sprint jump forward", false));
    }
    
    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }

        if (canTower()) {
            switch ((int) mode.getInput()) {
                case 0: // Vanilla
                    handleVanillaMode();
                    break;
                case 1: // Low
                    handleLowMode();
                    break;
                case 2: // Hypixel
                    handleHypixelMode();
                    break;
            }
        } else {
            handleSlowSpeed();
            reset();
        }
    }

    public boolean canSprint() {
        if (disableWhileHurt.isToggled() && mc.thePlayer.hurtTime >= 5) {
            return false;
        }
        if (disableWhileCollided.isToggled() && mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && disableInLiquid.isToggled()) {
            return false;
        }
        return true;
    }
    
    public boolean canTower() {
        if (!Utils.nullCheck()) {
            return false;
        } else if (disableWhileHurt.isToggled() && mc.thePlayer.hurtTime >= 9) {
            return false;
        } else if (disableWhileCollided.isToggled() && mc.thePlayer.isCollidedHorizontally) {
            return false;
        } else if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && disableInLiquid.isToggled()) {
            return false;
        } else if (Utils.jumpDown()) { // Use Utils.jumpDown
            return true;
        }
        return false;
    }

    private void handleVanillaMode() {
        mc.thePlayer.motionY = 0.41965;
        switch (offGroundTicks) {
            case 1:
                mc.thePlayer.motionY = 0.33;
                break;
            case 2:
                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                break;
        }
        if (offGroundTicks >= 3) {
            offGroundTicks = 0;
        }
    }

    private void handleLowMode() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.4196;
        } else {
            switch (offGroundTicks) {
                case 3:
                case 4:
                    mc.thePlayer.motionY = 0;
                    break;
                case 5:
                    mc.thePlayer.motionY = 0.4191;
                    break;
                case 6:
                    mc.thePlayer.motionY = 0.3275;
                    break;
                case 11:
                    mc.thePlayer.motionY = -0.5;
                    break;
            }
        }
    }

    private void handleHypixelMode() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42; // Initial jump height
            offGroundTicks = 0; // Reset off-ground ticks
        }

        double[] motions = new double[]{
            0.399999006,
            0.3536000119,
            0.2681280169,
            0.1843654552,
            -0.0807218421,
            -0.3175074179,
            -0.3145572677,
            -0.3866661346
        };

        if (offGroundTicks < motions.length) {
            mc.thePlayer.motionY = motions[offGroundTicks];
        }

        if (offGroundTicks == 5) {
            mc.thePlayer.motionY = -0.1523351824467155; // Additional downward motion for Hypixel logic
        }
    }

    private void handleSlowSpeed() {
        // Logic for handling slow-down after towering
    }

    private void reset() {
        offGroundTicks = 0;
    }

    public void onDisable() {
        // Reset state when the module is disabled
        isTowering = false;
        offGroundTicks = 0;
    }
}
