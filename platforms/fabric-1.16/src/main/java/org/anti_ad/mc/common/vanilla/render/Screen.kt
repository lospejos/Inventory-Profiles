package org.anti_ad.mc.common.vanilla.render

import org.anti_ad.mc.common.math2d.Rectangle
import org.anti_ad.mc.common.math2d.Size
import org.anti_ad.mc.common.vanilla.Vanilla
import org.anti_ad.mc.common.vanilla.alias.LiteralText
import org.anti_ad.mc.common.vanilla.alias.Screen
import org.anti_ad.mc.common.vanilla.glue.VanillaUtil
import org.anti_ad.mc.common.vanilla.render.glue.__glue_VanillaUtil_inGame
import org.anti_ad.mc.common.vanilla.render.glue.__glue_rDepthMask
import org.anti_ad.mc.common.vanilla.render.glue.__glue_rRenderDirtBackground
import org.anti_ad.mc.common.vanilla.render.glue.__glue_rScreenHeight
import org.anti_ad.mc.common.vanilla.render.glue.__glue_rScreenSize
import org.anti_ad.mc.common.vanilla.render.glue.__glue_rScreenWidth

private val rScreenWidth
    get() = Vanilla.window().scaledWidth
private val rScreenHeight
    get() = Vanilla.window().scaledHeight
private val rScreenSize
    get() = Size(rScreenWidth,
                 rScreenHeight)

private val dummyScreen = object : Screen(
    LiteralText(
        ""
    )
) {}


fun initScreenGlue() {
    __glue_rScreenHeight = { rScreenHeight }
    __glue_rScreenSize = { rScreenSize }
    __glue_rScreenWidth = { rScreenWidth }
    __glue_rDepthMask = { rectangle: Rectangle, block: () -> Unit -> rDepthMask(rectangle, block) }

    __glue_VanillaUtil_inGame = { VanillaUtil.inGame() }
    __glue_rRenderDirtBackground = {
        // Screen.renderDirtBackground
        (Vanilla.screen() ?: dummyScreen).renderBackgroundTexture(0)
    }
}