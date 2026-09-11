/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 fifth_light
 */

package top.fifthlight.touchcontroller.common.platform.ios

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.fileAttributesView
import kotlin.io.path.outputStream

/**
 * Loads the iOS transport native library bundled inside the mod JAR.
 *
 * The JAR built on macOS contains `proxy_server_ios_ios_aarch64/libproxy_server_ios.dylib`.
 * It provides the same in-process ring buffer transport as the statically linked
 * XCFramework, so launchers that don't embed the framework can still talk to the mod,
 * provided that loading external dynamic libraries is allowed by the launcher
 * (TrollStore, jailbreak, or a dyld library-validation bypass).
 */
internal object IosLibraryLoader {
    private val logger = LoggerFactory.getLogger(IosLibraryLoader::class.java)

    private const val LIBRARY_NAME = "proxy_server_ios"

    fun loadBundledLibrary() {
        val arch = when (val systemArch = System.getProperty("os.arch")) {
            "arm64", "aarch64" -> "aarch64"
            else -> throw UnsupportedOperationException("Unsupported iOS architecture: $systemArch")
        }
        val resourcePath = "${LIBRARY_NAME}_ios_$arch/lib$LIBRARY_NAME.dylib"

        val inputStream = IosLibraryLoader::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IOException("Bundled iOS transport library not found: $resourcePath. Is this mod JAR built on macOS?")

        val destinationFile = try {
            Files.createTempFile("lib$LIBRARY_NAME", ".dylib")
        } catch (e: Exception) {
            throw IOException("Failed to create temporary file for iOS transport library", e)
        }

        try {
            logger.info("Extracting iOS transport library to $destinationFile")
            inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            try {
                // dlopen needs the read and execute permissions, but not the write one.
                destinationFile.fileAttributesView<PosixFileAttributeView>().setPermissions(
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_EXECUTE,
                    )
                )
            } catch (e: Exception) {
                logger.info("Failed to set file $destinationFile read-only", e)
            }

            logger.info("Loading iOS transport library")
            try {
                @Suppress("UnsafeDynamicallyLoadedCode")
                System.load(destinationFile.toAbsolutePath().toString())
            } catch (e: UnsatisfiedLinkError) {
                throw IOException(
                    "Failed to load iOS transport library $destinationFile. " +
                        "The launcher may not allow loading external dynamic libraries. " +
                        "Link TouchController's XCFramework into the launcher instead.",
                    e
                )
            }
            logger.info("Loaded iOS transport library")

            destinationFile.toFile().deleteOnExit()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to extract iOS transport library", e)
        }
    }
}
