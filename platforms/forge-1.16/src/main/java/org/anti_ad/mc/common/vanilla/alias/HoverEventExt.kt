package org.anti_ad.mc.common.vanilla.alias

import net.minecraft.util.text.event.HoverEvent

// this is here because the alias system can't handle
// direct usage of HoverEventAction.SHOW_TEXT in
// .withHoverEvent(HoverEvent(HoverEventAction.SHOW_TEXT

fun createHoverEventText(text: String): HoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, LiteralText(text))