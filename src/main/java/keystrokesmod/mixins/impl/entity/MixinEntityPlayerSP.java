package keystrokesmod.mixins.impl.entity;

import com.mojang.authlib.GameProfile;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.WTap;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import keystrokesmod.mixins.interfaces.IOffGroundTicks;


@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer implements IOffGroundTicks {
    private int offGroundTicks; 
    
    @Shadow
    public int sprintingTicksLeft;

    @Shadow
    protected int sprintToggleTimer;

    @Shadow
    public float prevTimeInPortal;

    @Shadow
    public float timeInPortal;

    @Shadow
    protected Minecraft mc;

    @Shadow
    public MovementInput movementInput;

    @Shadow
    @Final
    public NetHandlerPlayClient sendQueue;

    @Shadow
    private boolean serverSprintState;

    @Shadow
    private boolean serverSneakState;

    @Shadow
    private double lastReportedPosX;

    @Shadow
    private double lastReportedPosY;

    @Shadow
    private double lastReportedPosZ;

    @Shadow
    private float lastReportedYaw;

    @Shadow
    private float lastReportedPitch;

    @Shadow
    private int positionUpdateTicks;
    
    @Shadow
    public abstract void setSprinting(boolean sprinting);

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    @Shadow
    public abstract boolean isRidingHorse();

    @Shadow
    protected abstract void sendHorseJump();

    @Shadow
    public abstract void sendPlayerAbilities();

    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Override
    public int getOffGroundTicks() {
        System.out.println("getOffGroundTicks called: " + offGroundTicks); // Debug
        return this.offGroundTicks;
    }

    @Overwrite
public void onUpdate() {
    if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0, this.posZ))) {
        RotationUtils.prevRenderPitch = RotationUtils.renderPitch;
        RotationUtils.prevRenderYaw = RotationUtils.renderYaw;

        // Trigger PreUpdateEvent
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PreUpdateEvent());
        super.onUpdate();

        // Update offGroundTicks
        if (this.onGround) {
            this.offGroundTicks = 0;
            // Log only once when player lands
            if (this.offGroundTicks == 0) {
                System.out.println("[DEBUG] Player is on the ground");
            }
        } else {
            this.offGroundTicks++;
            // Log only once when player leaves ground
            if (this.offGroundTicks == 1) {
                System.out.println("[DEBUG] Player is off the ground");
            }
        }

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PostUpdateEvent());
    }
}

    @Overwrite
    public void onUpdateWalkingPlayer() {
        System.out.println("onUpdateWalkingPlayer called"); // Debug entry point

        PreMotionEvent preMotionEvent = new PreMotionEvent(
                this.posX,
                this.getEntityBoundingBox().minY,
                this.posZ,
                this.rotationYaw,
                this.rotationPitch,
                this.onGround,
                this.isSprinting(),
                this.isSneaking()
        );

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(preMotionEvent);
        System.out.println("PreMotionEvent: " + preMotionEvent); // Debug

        boolean flag = preMotionEvent.isSprinting();
        if (flag != this.serverSprintState) {
            if (flag) {
                System.out.println("Starting sprinting"); // Debug
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                System.out.println("Stopping sprinting"); // Debug
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }
            this.serverSprintState = flag;
        }

        boolean flag1 = preMotionEvent.isSneaking();
        if (flag1 != this.serverSneakState) {
            if (flag1) {
                System.out.println("Starting sneaking"); // Debug
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
                System.out.println("Stopping sneaking"); // Debug
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }
            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity()) {
            double deltaX = preMotionEvent.getPosX() - this.posX;
            double deltaY = preMotionEvent.getPosY() - this.posY;
            double deltaZ = preMotionEvent.getPosZ() - this.posZ;
            boolean positionChanged = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 9.0E-4 || this.positionUpdateTicks >= 20;
            boolean rotationChanged = preMotionEvent.getYaw() != this.rotationYaw || preMotionEvent.getPitch() != this.rotationPitch;

            if (positionChanged && rotationChanged) {
                System.out.println("Sending C06PacketPlayerPosLook"); // Debug
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
            } else if (positionChanged) {
                System.out.println("Sending C04PacketPlayerPosition"); // Debug
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.isOnGround()));
            } else if (rotationChanged) {
                System.out.println("Sending C05PacketPlayerLook"); // Debug
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
            } else {
                System.out.println("Sending C03PacketPlayer"); // Debug
                this.sendQueue.addToSendQueue(new C03PacketPlayer(preMotionEvent.isOnGround()));
            }

            this.posX = preMotionEvent.getPosX();
            this.posY = preMotionEvent.getPosY();
            this.posZ = preMotionEvent.getPosZ();
            this.rotationYaw = preMotionEvent.getYaw();
            this.rotationPitch = preMotionEvent.getPitch();
            this.positionUpdateTicks = 0;
        }

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PostMotionEvent());
        System.out.println("PostMotionEvent triggered"); // Debug
    }

    @Overwrite
    public void onLivingUpdate() {
        if (this.sprintingTicksLeft > 0) {
            --this.sprintingTicksLeft;
            if (this.sprintingTicksLeft == 0) {
                this.setSprinting(false);
                System.out.println("Sprinting ended"); // Debug
            }
        }

        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        super.onLivingUpdate();
    }
}
