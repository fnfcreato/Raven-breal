package keystrokesmod.module.impl.movement;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class BHop extends Module {
    private SliderSetting mode;
    public static SliderSetting speedSetting;
    private ButtonSetting autoJump;
    private ButtonSetting disableInInventory;
    private ButtonSetting liquidDisable;
    private ButtonSetting sneakDisable;
    private ButtonSetting stopMotion;
    private final String[] modes = new String[]{"Strafe", "Ground", "FastFall"}; // Added FastFall
    public boolean hopping;
    private int offGroundTicks = 0;
    
    public BHop() {
        super("Bhop", Module.category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 2.0, 0.5, 8.0, 0.1));
        this.registerSetting(autoJump = new ButtonSetting("Auto jump", true));
        this.registerSetting(disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(liquidDisable = new ButtonSetting("Disable in liquid", true));
        this.registerSetting(sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(stopMotion = new ButtonSetting("Stop motion", false));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }
    @Override
public void onUpdate() {
    if (!this.isEnabled()) {
        return; // Prevent the logic from running when BHop is disabled
    }
    
    if (mc.thePlayer == null || mc.theWorld == null) {
        return; // Prevent null access
    }
    if (((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && liquidDisable.isToggled()) || 
        (mc.thePlayer.isSneaking() && sneakDisable.isToggled())) {
        return;
    }
    if (disableInInventory.isToggled() && Settings.inInventory()) {
        return;
    }
    if (ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.disableBHop.isToggled() && 
        ModuleManager.bedAura.currentBlock != null && 
        RotationUtils.inRange(ModuleManager.bedAura.currentBlock, ModuleManager.bedAura.range.getInput())) {
        return;
    }

    if (mc.thePlayer.onGround) {
        offGroundTicks = 0;
    } else {
        offGroundTicks++;
    }

    // Only adjust movement based on the selected mode
    switch ((int) mode.getInput()) {
        case 0: // Strafe Mode
            handleStrafeMode();
            break;

        case 1: // Ground Mode
            handleGroundMode();
            break;

        case 2: // FastFall Mode
            handleFastFallMode();
            break;
    }
}

    private void handleStrafeMode() {
        if (Utils.isMoving()) {
            if (mc.thePlayer.onGround && autoJump.isToggled()) {
                mc.thePlayer.jump();
            }
            mc.thePlayer.setSprinting(true);
            double currentSpeed = Utils.getHorizontalSpeed(mc.thePlayer);
            Utils.setHorizontalSpeed(mc.thePlayer, currentSpeed + 0.005 * speedSetting.getInput());
            hopping = true;
        }
    }

    private void handleGroundMode() {
        if (!Utils.jumpDown() && Utils.isMoving() && mc.currentScreen == null) {
            if (!mc.thePlayer.onGround) {
                return;
            }
            if (autoJump.isToggled()) {
                mc.thePlayer.jump();
            } else if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !autoJump.isToggled()) {
                return;
            }
            mc.thePlayer.setSprinting(true);
            double horizontalSpeed = Utils.getHorizontalSpeed(mc.thePlayer);
            double speedModifier = 0.4847;
            final int speedAmplifier = Utils.getSpeedAmplifier();
            switch (speedAmplifier) {
                case 1:
                    speedModifier = 0.5252;
                    break;
                case 2:
                    speedModifier = 0.587;
                    break;
                case 3:
                    speedModifier = 0.6289;
                    break;
            }
            double additionalSpeed = speedModifier * ((speedSetting.getInput() - 1.0) / 3.0 + 1.0);
            if (horizontalSpeed < additionalSpeed) {
                horizontalSpeed = additionalSpeed;
            }
            Utils.setHorizontalSpeed(mc.thePlayer, horizontalSpeed);
            hopping = true;
        }
    }

    private void handleFastFallMode() {
    if (mc.thePlayer == null || mc.theWorld == null) {
        return; // Prevent null access
    }

    if (mc.thePlayer.onGround) {
        if (Utils.isMoving()) {
            Utils.setHorizontalSpeed(mc.thePlayer, Utils.getHorizontalSpeed(mc.thePlayer) + 0.22f);
            mc.thePlayer.motionY = Utils.getJumpHeight();
            mc.thePlayer.motionX *= 0.94;
            mc.thePlayer.motionZ *= 0.94;
            if (Utils.getHorizontalSpeed(mc.thePlayer) < 0.45f) {
                Utils.setHorizontalSpeed(mc.thePlayer, 0.45f);
            }
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                float amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                Utils.setHorizontalSpeed(mc.thePlayer, 0.46f + 0.023f * (amplifier + 1));
            }
        }
    } else {
        // Declare and retrieve the disabler module before the condition
        Module disabler = ModuleManager.getModule("Disabler");

        if (offGroundTicks == 5 && mc.thePlayer.hurtTime < 5 && disabler != null && !disabler.isEnabled()) {
            mc.thePlayer.motionY = -0.1522351824467155;
        }

        mc.thePlayer.motionX *= 1.0004;
        mc.thePlayer.motionZ *= 1.0004;
    }
    }
    
    @Override
public void onDisable() {
    if (stopMotion.isToggled()) {
        //.
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;
    }
    hopping = false;
}

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (!this.isEnabled()) {
        return; // Prevent the bhop when BHop is disabled
        }
        if (autoJump.isToggled()) {
            e.setSprint(false);
        }
    }
}

