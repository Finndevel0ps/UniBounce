/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.event.ClickEvent
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.IChatComponent
import java.util.*
import kotlin.concurrent.schedule

@ModuleInfo(name = "AutoKnight", spacedName = "Auto Knight", description = "Automatically selects Knight kit for you in BlocksMC Skywars.", category = ModuleCategory.MISC)
class AutoKnight : Module() {

    private val debugValue = BoolValue("Debug", false)

    private var clickStage = 0
    private var duplication = 0

    private var availableForSelect = false

    private fun debug(s: String) {
        if (debugValue.get()) ClientUtils.displayChatMessage("[AK] $s")
    }

    override fun onEnable() {
        clickStage = 0
        duplication = 0
        availableForSelect = false
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (availableForSelect && packet is S2DPacketOpenWindow && clickStage < 2)
            event.cancelEvent()

        if (packet is S2FPacketSetSlot) {
            val item = packet.func_149174_e() ?: return
            val windowId = packet.func_149175_c()
            val slot = packet.func_149173_d()
            val itemName = item.unlocalizedName
            val displayName = item.displayName

            if (clickStage == 0 && windowId == 0 && itemName.contains("bow", true) && displayName.contains("kit selector", true)) {
                availableForSelect = true
                duplication++
                if (duplication < 2) {
                    debug("detected kit selector v2")
                    Timer().schedule(200L) {
                        clickStage = 1
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot - 36))
                        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(slot).getStack()))
                        debug("clicked")
                        //mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    }
                }
                
            }
            if (clickStage == 1 && displayName.contains("Knight", true)) {
                debug("detected knight kit selection")
                Timer().schedule(10L) {
                    mc.netHandler.addToSendQueue(C0EPacketClickWindow(windowId, slot, 0, 0, item, 1919))
                    mc.netHandler.addToSendQueue(C0EPacketClickWindow(windowId, slot, 0, 0, item, 1919))
                    mc.netHandler.addToSendQueue(C0DPacketCloseWindow(windowId))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    debug("selected")
                }
            }
        }
        
        if (packet is S02PacketChat) {
            val text = packet.chatComponent.unformattedText

            if (text.contains("has been selected", true)) {
                debug("finished")
                clickStage = 2
                LiquidBounce.hud.addNotification(Notification("Successfully selected Knight kit.", Notification.Type.SUCCESS))
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clickStage = 0
        duplication = 0
        availableForSelect = false
    }
}