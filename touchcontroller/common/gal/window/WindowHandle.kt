/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 fifth_light
 */

package top.fifthlight.touchcontroller.common.gal.window

import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset
import top.fifthlight.mergetools.api.ExpectFactory

interface WindowHandle {
    val size: IntSize
    val scaledSize: IntSize
    val mouseLeftPressed: Boolean

    /**
     * Whether the right mouse button is currently pressed.
     *
     * Some launchers (e.g. Amethyst iOS) send a quick tap in-game as a right mouse
     * click, so the touch emulation also has to watch this button.
     */
    val mouseRightPressed: Boolean
        get() = false

    val mousePosition: Offset?

    @ExpectFactory
    interface Factory {
        fun of(): WindowHandle
    }
}
