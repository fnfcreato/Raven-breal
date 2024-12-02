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
    private String[] modes = new String[]{"Strafe", "Ground", "FastFall"}; // Added FastFall
    public boolean hopping;

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
        double currentSpeed = Utils.getHorizontalSpeed();
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
            double horizontalSpeed = Utils.getHorizontalSpeed();
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
            Utils.setHorizontalSpeed(horizontalSpeed);
            hopping = true;
        }
    }

    private void handleFastFallMode() {
        if (mc.thePlayer.onGround) {
            if (Utils.isMoving()) {
                Utils.setHorizontalSpeed(Utils.getHorizontalSpeed() + 0.23f);
                mc.thePlayer.motionY = Utils.getJumpHeight();
                mc.thePlayer.motionX *= 0.95;
                mc.thePlayer.motionZ *= 0.95;
                if (Utils.getHorizontalSpeed() < 0.46f) {
                    Utils.setHorizontalSpeed(0.46f);
                }
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    float amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
                    Utils.setHorizontalSpeed(0.47f + 0.024f * (amplifier + 1));
                }
            }
        } else {
            if (mc.thePlayer.offGroundTicks == 5 && mc.thePlayer.hurtTime < 5 && 
                !ModuleManager.getModule("Disabler").isEnabled()) {
                mc.thePlayer.motionY = -0.1523351824467155;
            }
            mc.thePlayer.motionX *= 1.0005;
            mc.thePlayer.motionZ *= 1.0005;
        }
    }

    @Override
    public void onDisable() {
        if (stopMotion.isToggled()) {
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionX = 0;
        }
        hopping = false;
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (autoJump.isToggled()) {
            e.setSprint(false);
        }
    }
}
