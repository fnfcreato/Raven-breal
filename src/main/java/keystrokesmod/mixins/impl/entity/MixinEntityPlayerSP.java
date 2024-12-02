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
    private int offGroundTicks;

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
            } else {
                this.offGroundTicks++;
            }

            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PostUpdateEvent());
        }
    }

    @Overwrite
    public void onUpdateWalkingPlayer() {
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

        boolean flag = preMotionEvent.isSprinting();
        if (flag != this.serverSprintState) {
            if (flag) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = flag;
        }

        boolean flag1 = preMotionEvent.isSneaking();
        if (flag1 != this.serverSneakState) {
            if (flag1) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
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
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
            } else if (positionChanged) {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(preMotionEvent.getPosX(), preMotionEvent.getPosY(), preMotionEvent.getPosZ(), preMotionEvent.isOnGround()));
            } else if (rotationChanged) {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround()));
            } else {
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
    }

    @Overwrite
    public void onLivingUpdate() {
        if (this.sprintingTicksLeft > 0) {
            --this.sprintingTicksLeft;
            if (this.sprintingTicksLeft == 0) {
                this.setSprinting(false);
            }
        }

        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        super.onLivingUpdate();
    }
}
