package org.anti_ad.mc.ipnext.event

import org.anti_ad.mc.common.extensions.detectable
import org.anti_ad.mc.common.math2d.Point
import org.anti_ad.mc.common.math2d.Rectangle
import org.anti_ad.mc.common.math2d.Size
import org.anti_ad.mc.common.math2d.intersects
import org.anti_ad.mc.common.vanilla.Vanilla
import org.anti_ad.mc.common.vanilla.alias.ContainerScreen
import org.anti_ad.mc.common.vanilla.alias.MatrixStack
import org.anti_ad.mc.common.vanilla.alias.PlayerInventory
import org.anti_ad.mc.common.vanilla.alias.RenderSystem
import org.anti_ad.mc.common.vanilla.render.glue.IdentifierHolder
import org.anti_ad.mc.common.vanilla.render.glue.Sprite
import org.anti_ad.mc.common.vanilla.render.glue.rDrawCenteredSprite
import org.anti_ad.mc.common.vanilla.render.rDisableDepth
import org.anti_ad.mc.common.vanilla.render.rEnableDepth
import org.anti_ad.mc.ipnext.config.ModSettings
import org.anti_ad.mc.ipnext.config.SwitchType.HOLD
import org.anti_ad.mc.ipnext.config.SwitchType.TOGGLE
import org.anti_ad.mc.ipnext.ingame.`(containerBounds)`
import org.anti_ad.mc.ipnext.ingame.`(invSlot)`
import org.anti_ad.mc.ipnext.ingame.`(inventory)`
import org.anti_ad.mc.ipnext.ingame.`(itemStack)`
import org.anti_ad.mc.ipnext.ingame.`(slots)`
import org.anti_ad.mc.ipnext.ingame.`(topLeft)`
import org.anti_ad.mc.ipnext.ingame.vPlayerSlotOf
import org.anti_ad.mc.ipnext.item.maxCount
import org.anti_ad.mc.ipnext.parser.LockSlotsLoader

/*
  slots ignored for:
    - clean cursor
    - move match / move crafting
    - sort
    - continuous crafting supplies storage
    - auto refill supplies storage
 */
object LockSlotsHandler {
    val lockedInvSlotsStoredValue = mutableSetOf<Int>() // locked invSlot list
    val enabled: Boolean
        get() = ModSettings.ENABLE_LOCK_SLOTS.booleanValue && !ModSettings.LOCK_SLOTS_QUICK_DISABLE.isPressing()
    val lockedInvSlots: Iterable<Int>
        get() = if (enabled) lockedInvSlotsStoredValue else listOf()

    private val slotLocations: Map<Int, Point>
        get() {
            val screen = Vanilla.screen() as? ContainerScreen<*> ?: return mapOf()
            return Vanilla.container().`(slots)`.mapNotNull { slot ->
                val playerSlot = vPlayerSlotOf(slot,
                                               screen)
                return@mapNotNull if (playerSlot.`(inventory)` is PlayerInventory)
                    playerSlot.`(invSlot)` to slot.`(topLeft)` else null
            }.toMap()
        }

    private var displayingConfig by detectable(false) { _, newValue ->
        if (!newValue) LockSlotsLoader.save() // save on close
    }

    fun isSlotLocked(i: Int) = lockedInvSlots.contains(i)

    // ============
    // render
    // ============
    private val TEXTURE = IdentifierHolder("inventoryprofilesnext",
                                           "textures/gui/overlay_new.png")
    private val backgroundSprite = Sprite(TEXTURE,
                                          Rectangle(40,
                                                    8,
                                                    32,
                                                    32))
    private val hotOutline: Sprite
        get() = backgroundSprite.down()

    private val hotOutlineLeft: Sprite
        get() = backgroundSprite.right(2)

    private val hotOutlineRight: Sprite
        get() = backgroundSprite.right(3)

    private val hotOutlineActive: Sprite
        get() = backgroundSprite.down().right(2)

    private val foregroundSprite: Sprite
        get() = backgroundSprite.right().down(ModSettings.LOCKED_SLOTS_FOREGROUND_STYLE.integerValue - 1)
    private val configSprite = backgroundSprite.left()
    private val configSpriteLocked = configSprite.down()

    fun onBackgroundRender() {
        if (displayingConfig) return
        if (!ModSettings.SHOW_LOCKED_SLOTS_BACKGROUND.booleanValue) return
        drawSprite(backgroundSprite,
                   null)
    }

    fun onForegroundRender() {
        if (!enabled) return
        val screen = Vanilla.screen() as? ContainerScreen<*> ?: return
        val matrixStack2: MatrixStack = RenderSystem.getModelViewStack()
        matrixStack2.push()  // see HandledScreen.render()
        //rMatrixStack = matrixStack2
        val topLeft = screen.`(containerBounds)`.topLeft
        matrixStack2.translate(-topLeft.x.toDouble(),
                               -topLeft.y.toDouble(),
                               0.0)
        RenderSystem.applyModelViewMatrix()

        //gTranslatef(-topLeft.x.toFloat(), -topLeft.y.toFloat(), 0f)
        drawForeground()
        drawConfig()
        matrixStack2.pop() //gPopMatrix()
        RenderSystem.applyModelViewMatrix()
    }

    fun postRender() { // display config
    }

    private fun drawForeground() {
        if (!ModSettings.SHOW_LOCKED_SLOTS_FOREGROUND.booleanValue) return
        drawSprite(foregroundSprite,
                   null)
    }

    private fun drawConfig() {
        if (!displayingConfig) return
        drawSprite(configSpriteLocked,
                   configSprite)
    }

    private fun drawSprite(lockedSprite: Sprite?,
                           openSprite: Sprite?) {
        if (!enabled) return
        val screen = Vanilla.screen() as? ContainerScreen<*> ?: return
//    rClearDepth() // use translate or zOffset
        rDisableDepth()
        RenderSystem.enableBlend()
        val topLeft = screen.`(containerBounds)`.topLeft + Point(8,
                                                                 8) // slot center offset
        for ((invSlot, slotTopLeft) in slotLocations) {
            if (invSlot in lockedInvSlotsStoredValue) {
                lockedSprite?.let {
                    rDrawCenteredSprite(it, topLeft + slotTopLeft)
                }
            } else {
                openSprite?.let {
                    rDrawCenteredSprite(it, topLeft + slotTopLeft)
                }
            }
        }
        RenderSystem.disableBlend()
        rEnableDepth()
    }

    private fun drawHotSprite(matrixStack: MatrixStack) {
        if (!enabled) return
        //    rClearDepth() // use translate or zOffset
        rDisableDepth()
        RenderSystem.enableBlend()
        val screenWidth = Vanilla.mc().window.scaledWidth
        val screenHeight = Vanilla.mc().window.scaledHeight
        val i = screenWidth / 2;
        for (j1 in 0..8) {
            if (j1 in lockedInvSlotsStoredValue) {
                val lockedSprite: Sprite = if (j1 - Vanilla.playerInventory().selectedSlot == -1) {
                    hotOutlineLeft
                } else if (j1 - Vanilla.playerInventory().selectedSlot == 1) {
                    hotOutlineRight
                } else if (j1 == Vanilla.playerInventory().selectedSlot) {
                    hotOutlineActive
                } else {
                    hotOutline
                }
                val k1: Int = i - 90 + j1 * 20 + 2
                val l1: Int = screenHeight - 16 - 3
                val topLeft = Point(k1, l1) + Point(8, 8)

                rDrawCenteredSprite(lockedSprite, 0, topLeft)
                if (ModSettings.SHOW_LOCKED_SLOTS_FOREGROUND.booleanValue) {
                    rDrawCenteredSprite(foregroundSprite, 0, topLeft)
                }
            }
        }
        RenderSystem.disableBlend()
        rEnableDepth()
    }

    // ============
    // input
    // ============

    var clicked = false
    var mode = 0 // 0 set lock slot 1 clear lock slot

    fun onTickInGame() {
        if (!enabled) return
        val screen = Vanilla.screen() as? ContainerScreen<*> ?: run {
            displayingConfig = false
            clicked = false
            return
        }
        if (clicked) {
            val line = MouseTracer.asLine
            val topLeft = screen.`(containerBounds)`.topLeft - Size(1,
                                                                    1)
            for ((invSlot, slotTopLeft) in slotLocations) {
                if ((mode == 0) == (invSlot !in lockedInvSlotsStoredValue)
                    && line.intersects(Rectangle(topLeft + slotTopLeft,
                                                 Size(18,
                                                      18)))) {
                    if (mode == 0)
                        lockedInvSlotsStoredValue.add(invSlot)
                    else
                        lockedInvSlotsStoredValue.remove(invSlot)
                }
            }
        }
    }

    fun onCancellableInput(): Boolean {
        if (!enabled) return false
        val screen = Vanilla.screen() as? ContainerScreen<*> ?: return false
        when (ModSettings.LOCK_SLOTS_CONFIG_SWITCH_TYPE.value) {
            TOGGLE -> if (ModSettings.LOCK_SLOTS_SWITCH_CONFIG_MODIFIER.isActivated()) displayingConfig =
                !displayingConfig
            HOLD -> displayingConfig = ModSettings.LOCK_SLOTS_SWITCH_CONFIG_MODIFIER.isPressing()
        }
        val currentClicked = (displayingConfig && ModSettings.LOCK_SLOTS_CONFIG_KEY.isPressing())
                || ModSettings.LOCK_SLOTS_QUICK_CONFIG_KEY.isPressing()
        if (currentClicked != clicked) {
            if (!currentClicked) {
                clicked = false
                return true
            } // else currentClicked == true
            val topLeft = screen.`(containerBounds)`.topLeft - Size(1,
                                                                    1)
            // check if on slot
            val focused = slotLocations.asIterable().firstOrNull { (_, slotTopLeft) ->
                Rectangle(topLeft + slotTopLeft,
                          Size(18,
                               18)).contains(MouseTracer.location)
            }
            focused?.let { (invSlot, _) ->
                clicked = true
                mode = if (invSlot in lockedInvSlotsStoredValue) 1 else 0
                return true
            }
        }
        return false
    }

    private val qMoveSlotMapping = mapOf(36 to 0,
                                         37 to 1,
                                         38 to 2,
                                         39 to 3,
                                         40 to 4,
                                         41 to 5,
                                         42 to 6,
                                         43 to 7,
                                         44 to 8,
                                         27 to 27,
                                         28 to 28,
                                         29 to 29,
                                         30 to 30,
                                         31 to 31,
                                         32 to 32,
                                         33 to 33,
                                         34 to 34,
                                         35 to 35,
                                         18 to 18,
                                         19 to 19,
                                         20 to 20,
                                         21 to 21,
                                         22 to 22,
                                         23 to 23,
                                         24 to 24,
                                         25 to 25,
                                         26 to 26,
                                         9 to 9,
                                         10 to 10,
                                         10 to 10,
                                         11 to 11,
                                         12 to 12,
                                         13 to 13,
                                         14 to 14,
                                         15 to 15,
                                         16 to 16,
                                         17 to 17,
                                         8 to 36,
                                         45 to 40,
                                         7 to 37,
                                         6 to 38,
                                         5 to 39)

    fun isQMoveActionAllowed(slot: Int, isThrow: Boolean, button: Int): Boolean {

        if (slot == -1) return true
        val locked = lockedInvSlots.contains(qMoveSlotMapping[slot])
        if (!locked) return true
        if (ModSettings.LOCKED_SLOTS_DISABLE_QUICK_MOVE_THROW.value && button == 1) {
            return false
        }
        if (isThrow && ModSettings.LOCKED_SLOTS_DISABLE_THROW_FOR_NON_STACKABLE.value) {
            val slots = Vanilla.playerContainer().`(slots)`
            if (slot <= slots.size) {
                val itemSlot = slots[slot].`(itemStack)`
                val itemType = itemSlot.itemType
                return itemType.maxCount > 1
            }
        }
        return true
    }

    fun postRenderHud(matrixStack: MatrixStack) {
        if (ModSettings.ALSO_SHOW_LOCKED_SLOTS_IN_HOTBAR.value) {
            drawHotSprite(matrixStack)
        }
    }

    fun preRenderHud(matrixStack: MatrixStack) {
        //drawHotSprite(matrixStack)
    }
}