/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 fifth_light
 */

package top.fifthlight.touchcontroller.common.platform.ios

import org.slf4j.LoggerFactory

object Transport {
    private val logger = LoggerFactory.getLogger(Transport::class.java)

    @JvmStatic
    private external fun init()
    @JvmStatic
    external fun receive(buffer: ByteArray): Int
    @JvmStatic
    external fun send(buffer: ByteArray, off: Int, len: Int)

    private var initialized = false

    /**
     * Initialize the native transport between the game and the iOS launcher.
     *
     * The JNI implementation may come from two places:
     *
     * - The launcher statically links TouchController's XCFramework into its executable
     *   (the recommended way, e.g. for launchers like Amethyst iOS), so the symbols are
     *   already present in the process.
     * - Otherwise, the dylib bundled inside the mod JAR is extracted and loaded with
     *   `System.load`. This only works when the launcher allows loading external dynamic
     *   libraries (for example TrollStore's "no library validation" entitlement, or a
     *   jailbroken device).
     */
    fun ensureInitialized() {
        if (initialized) {
            return
        }
        synchronized(this) {
            if (initialized) {
                return
            }
            try {
                init()
            } catch (e: UnsatisfiedLinkError) {
                logger.info("TouchController iOS transport isn't linked into the launcher, try loading the bundled dylib")
                IosLibraryLoader.loadBundledLibrary()
                init()
            }
            initialized = true
        }
    }
}
