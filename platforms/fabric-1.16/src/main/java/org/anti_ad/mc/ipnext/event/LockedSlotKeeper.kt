package org.anti_ad.mc.ipnext.event

import org.anti_ad.mc.common.Log
import org.anti_ad.mc.common.vanilla.Vanilla
import org.anti_ad.mc.common.vanilla.glue.VanillaUtil
import org.anti_ad.mc.ipnext.config.ModSettings
import org.anti_ad.mc.ipnext.ingame.`(itemStack)`
import org.anti_ad.mc.ipnext.ingame.`(slots)`
import org.anti_ad.mc.ipnext.ingame.`(vanillaStack)`
import org.anti_ad.mc.ipnext.inventory.AreaTypes
import org.anti_ad.mc.ipnext.inventory.ContainerClicker
import org.anti_ad.mc.ipnext.inventory.ContainerType
import org.anti_ad.mc.ipnext.inventory.ContainerTypes
import org.anti_ad.mc.ipnext.inventory.GeneralInventoryActions
import org.anti_ad.mc.ipnext.item.isEmpty

object LockedSlotKeeper {

    private var screenOpening = false
    private var worldJoined = false
    private var ticksAfterJoin = 0

    val emptyLockedSlots = mutableListOf<Int>()
    val emptyNonLockedSlots = mutableListOf<Int>()

    var processingLockedPickups: Boolean = false
        get() {
            return field
        }
        private set

    val isWholeInventoryEmpty: Boolean
        get() {
            val vanillaContainer = Vanilla.container()
            val types = ContainerTypes.getTypes(vanillaContainer)
            if (types.contains(ContainerType.CREATIVE)) {
                return true
            }
            val slots = vanillaContainer.`(slots)`
            slots.forEach {
                if (!it.`(itemStack)`.isEmpty()) {
                    return false
                }
            }
            return true
        }

    fun onTickInGame() {
        if (ModSettings.ENABLE_LOCK_SLOTS.booleanValue && !ModSettings.LOCKED_SLOTS_ALLOW_PICKUP_INTO_EMPTY.booleanValue) {

            if (worldJoined) {
                if (ticksAfterJoin > ModSettings.LOCKED_SLOTS_DELAY_KEEPER_REINIT_TICKS.integerValue) {
                    Log.trace("Initialising because of timeout!")
                    init()
                    worldJoined = false
                    ticksAfterJoin = 0
                    return
                } else {
                    if (!isWholeInventoryEmpty) {
                        Log.trace("Inventory is NOT empty initialising")
                        init()
                        worldJoined = false
                        ticksAfterJoin = 0
                        return
                    } else {
                        Log.trace("Inventory is empty skipping initialization")
                    }

                }
                ticksAfterJoin++
            }
            //only do stuff if we are in game and there is no open screen
            // this will allow for item pickup into locked slots while the inventory is open
            // however there is no easy way to distinguish between player moved items and picked up ones
            // so people will have to live with that.
            if (VanillaUtil.inGame() && Vanilla.screen() != null) {
                screenOpening = true
            } else {
                if (screenOpening) {
                     screenOpening = false
                    Log.trace("Inventory is NOT empty initialising - 2")
                    init()
                } else {
                    checkNewItems()
                }
            }
        }
    }

    private fun checkNewItems() {
        this.emptyLockedSlots.forEach {
            val stack = Vanilla.container().`(slots)`[it].`(vanillaStack)`
            if (!stack.isEmpty) {
                processingLockedPickups = true
                //items changed so do stuff to keep the slot empty!
                Log.trace("Items were placed int locked slot! $it")
                if (this.emptyNonLockedSlots.size > 0) {

                    GeneralInventoryActions.cleanCursor()
                    val targetSlot = emptyNonLockedSlots[0]
                    if ((it - 36) in 0..8) { // use swap
                        //handles hotbar
                        val hotBarSlot = it - 36
                        Log.trace("Swapping $it to $targetSlot")
                        ContainerClicker.swap(targetSlot,
                                              hotBarSlot)
                    } else {
                        Log.trace("moving stack from $it to $targetSlot")
                        ContainerClicker.leftClick(it)
                        ContainerClicker.leftClick(targetSlot)
                    }
                    this.emptyNonLockedSlots.removeAt(0)
                } else {
                    Log.trace("Trowing away $it since there no free unlocked slots")
                    ContainerClicker.qClick(it)
                }
                processingLockedPickups = false
            }
        }
    }

    fun init() {
        this.emptyLockedSlots.clear()
        this.emptyNonLockedSlots.clear()
        val vanillaContainer = Vanilla.container()
        val types = ContainerTypes.getTypes(vanillaContainer)
        if (types.contains(ContainerType.CREATIVE)) {
            return
        }
        with(AreaTypes) {
            val nonLocked = playerStorage + playerHotbar - lockedSlots
            val slots = vanillaContainer.`(slots)`
            val nonLockedSource = nonLocked.getItemArea(vanillaContainer, slots)
            val lockedSource = lockedSlots.getItemArea(vanillaContainer, slots)
            lockedSource.slotIndices.forEach {
                val stack = slots[it].`(vanillaStack)`
                if (stack.isEmpty) {
                    emptyLockedSlots.add(it)
                }
            }
            Log.trace("empty locked slots $emptyLockedSlots")
            nonLockedSource.slotIndices.forEach {
                if (slots[it].`(vanillaStack)`.isEmpty) {
                    emptyNonLockedSlots.add(it)
                }
            }
            Log.trace("empty NON Locked slots $emptyNonLockedSlots")
        }
    }

    fun onJoinWorld() {
        ticksAfterJoin = 0
        worldJoined = true
    }
}