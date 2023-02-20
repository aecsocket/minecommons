package io.github.aecsocket.alexandria.api.paper.extension

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.io.IOException
import java.io.InputStream

@Suppress("DEPRECATION")
fun nextEntityId() = Bukkit.getUnsafe().nextEntityId()

fun Plugin.resource(path: String): InputStream {
    val url = javaClass.classLoader.getResource(path)
        ?: throw RuntimeException("Resource at $path does not exist")
    try {
        val connection = url.openConnection()
        connection.useCaches = false
        return connection.getInputStream()
    } catch (ex: IOException) {
        throw RuntimeException("Could not load resource from $path", ex)
    }
}

fun Plugin.registerEvents(listener: Listener) =
    Bukkit.getPluginManager().registerEvents(listener, this)

fun Plugin.runRepeating(period: Long = 1, delay: Long = 0, block: () -> Unit) =
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, block, delay, period)

fun Plugin.runDelayed(delay: Long = 0, block: () -> Unit) =
    Bukkit.getScheduler().scheduleSyncDelayedTask(this, block, delay)
