/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.grim

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.RotationUtils2
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.ccbluex.liquidbounce.value.BoolValue
import kotlin.math.cos
import kotlin.math.sin

object Velocity2 : Module("Velocity2", ModuleCategory.GRIM) {

    private val horizontalValue = FloatValue("Horizontal", 0F, 0F..1F)
    private val verticalValue = FloatValue("Vertical", 0F, 0F..1F)
    private val modeValue = ListValue(
        "Mode", arrayOf(
            "Simple",
            "JumpReset",
            "Vertical",
            "Jump"
        ),
        "Simple"
    )
    private val jumpreset = BoolValue("jumpReset", false)
    private val swing = BoolValue("Swing", false)

    /**
     * VALUES
     */
    private var velocityTimer = MSTimer()
    private var velocityInput = false
    private var jumped = 0

    override val tag: String
        get() = modeValue.get()
    private var start = 0


    override fun onDisable() {
        mc.thePlayer?.speedInAir = 0.02F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb)
            return

        when (modeValue.get().toLowerCase()) {
            "jumpreset" -> {
                if (mc.thePlayer.hurtTime > 0) {
                    mc.thePlayer.motionX += -1.0E-7
                    mc.thePlayer.motionY += -1.0E-7
                    mc.thePlayer.motionZ += -1.0E-7
                    mc.thePlayer.isAirBorne = true
                }
            }

            "jump" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) {
                thePlayer.motionY = 0.42

                val yaw = thePlayer.rotationYaw * 0.017453292F

                thePlayer.motionX -= sin(yaw) * 0.2
                thePlayer.motionZ += cos(yaw) * 0.2
            }
        }
        if (jumpreset.get()) {
            if (mc.thePlayer.hurtTime >= 8) {
                mc.gameSettings.keyBindJump.pressed = true
            }

            if (mc.thePlayer.hurtTime >= 7 && !mc.gameSettings.keyBindForward.pressed) {
                mc.gameSettings.keyBindForward.pressed = true
                start = 1
            }
            if (mc.thePlayer.hurtTime in 1..6) {
                mc.gameSettings.keyBindJump.pressed = false
                if (start == 1) {
                    mc.gameSettings.keyBindForward.pressed = false
                    start = 0
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.thePlayer ?: return

        val packet = event.packet

        if (packet is S12PacketEntityVelocity) {
            if ((mc.theWorld?.getEntityByID(packet.entityID) ?: return) != thePlayer)
                return

            velocityTimer.reset()

            when (modeValue.get().lowercase()) {
                "simple" -> {
                    val horizontal = horizontalValue.get()
                    val vertical = verticalValue.get()

                    if (horizontal == 0F && vertical == 0F)
                        event.cancelEvent()

                    packet.motionX = (packet.motionX * horizontal).toInt()
                    packet.motionY = (packet.motionY * vertical).toInt()
                    packet.motionZ = (packet.motionZ * horizontal).toInt()
                }


                "vertical" -> {
                    val aura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
                    val target = KillAura.target
                    if (target == null || RotationUtils2.targetRotation == null)
                        return
                    repeat(12) {
                        mc.netHandler.addToSendQueue(C0FPacketConfirmTransaction(100, 100, true))
                        mc.thePlayer.sendQueue.addToSendQueue(
                            C02PacketUseEntity(
                                target,
                                C02PacketUseEntity.Action.ATTACK
                            )
                        )
                        if (!swing.get()) {
                            mc.thePlayer.sendQueue.addToSendQueue(C0APacketAnimation())
                        } else {
                            mc.thePlayer.swingItem()
                        }
                    }
                    event.cancelEvent()
                    mc.thePlayer.motionY = packet.getMotionY().toDouble() / 8000.0
                    mc.thePlayer.motionX *= 0.077760000
                    mc.thePlayer.motionZ *= 0.077760000
                }
            }
        }

        @EventTarget
        fun onJump(event: JumpEvent) {
            val thePlayer = mc.thePlayer

            if (thePlayer == null || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb)
                return

            when (modeValue.get().lowercase()) {
            }
        }
    }
}

