/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 fifth_light
 */

package top.fifthlight.touchcontroller.common.platform.ios

import org.slf4j.LoggerFactory

/**
 * In-process transport between the game and an iOS launcher.
 *
 * The native side is provided either by a bundled `proxy_server_ios` library or
 * by the launcher itself: iOS launchers derived from PojavLauncher (Amethyst
 * and friends) register the very same three JNI entry points from their own
 * native code. Both routes end up here, so resolution is done lazily rather
 * than from a class initializer - an [UnsatisfiedLinkError] raised while this
 * object is being initialized would replace every later access with
 * `NoClassDefFoundError` and leave no way to report the real cause.
 */
object Transport {
    private val logger = LoggerFactory.getLogger(Transport::class.java)

    @JvmStatic
    private external fun init()
    @JvmStatic
    external fun receive(buffer: ByteArray): Int
    @JvmStatic
    external fun send(buffer: ByteArray, off: Int, len: Int)

    /**
     * Whether the native iOS transport answered [initialize] successfully.
     */
    @Volatile
    var available: Boolean = false
        private set

    /**
     * Initializes the native transport. Idempotent and never throws; returns
     * `false` when the launcher did not provide the native side.
     */
    @Synchronized
    fun initialize(): Boolean {
        if (available) {
            return true
        }
        return try {
            init()
            available = true
            logger.info("iOS native transport initialized")
            true
        } catch (ex: Throwable) {
            logger.warn(
                "iOS native transport is unavailable, TouchController will stay inactive. " +
                    "The launcher must implement the JNI natives of " +
                    "top.fifthlight.touchcontroller.common.platform.ios.Transport, " +
                    "or ship the proxy_server_ios native library.",
                ex,
            )
            false
        }
    }
}
