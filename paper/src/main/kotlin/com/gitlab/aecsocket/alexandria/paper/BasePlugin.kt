package com.gitlab.aecsocket.alexandria.paper

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.Logging
import com.gitlab.aecsocket.alexandria.core.extension.walkFile
import com.gitlab.aecsocket.alexandria.paper.extension.disable
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleDelayed
import com.gitlab.aecsocket.glossa.core.I18N
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.TextColor
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.NamingSchemes
import java.io.File
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

const val PATH_SETTINGS = "settings.conf"
const val PATH_LANG = "lang"

private const val LOG_LEVEL = "log_level"
private const val CONFIG_FILE_EXT = ".conf"
private const val THREAD_NAME_WIDTH = 10

private fun chatPrefixOf(name: String, color: TextColor) =
    Component.text("{$name} ", color)

data class PluginManifest(
    val name: String,
    val displayName: String = name,
    val accentColor: TextColor,
    val langPaths: List<String>,
    val savedPaths: List<String>
)

abstract class BasePlugin(
    val manifest: PluginManifest
) : JavaPlugin() {
    val log = Logging({
        fun String.crop(target: Int, padder: String.(Int) -> String) = if (length > target) substring(0, target)
            else padder(this, target)

        val threadName = Thread.currentThread().name.crop(THREAD_NAME_WIDTH, String::padEnd)

        logger.info("$threadName $it")
    })

    val chatPrefix = chatPrefixOf(manifest.displayName, manifest.accentColor)

    override fun onEnable() {
        if (!dataFolder.exists()) {
            manifest.savedPaths.forEach { path ->
                saveResource(path, false)
            }
        }
        scheduleDelayed { init() }
    }

    private fun init() {
        val shouldDisable = if (!initInternal())
            true
        else {
            val (log, success) = load()
            log.forEach { this.log.record(it) }
            !success
        }

        if (shouldDisable)
            scheduleDelayed { disable() }
    }

    protected open fun initInternal(): Boolean {
        return true
    }

    data class LoadResult(val log: LogList, val success: Boolean)

    fun load(): LoadResult {
        val log = LogList()

        val config = try {
            AlexandriaAPI.configLoader().file(dataFolder.resolve(PATH_SETTINGS)).build().load()
        } catch (ex: Exception) {
            log.line(LogLevel.Error, ex) { "Could not load settings from $PATH_SETTINGS" }
            return LoadResult(log, false)
        }

        return try {
            LoadResult(log, loadInternal(log, config))
        } catch (ex: Exception) {
            log.line(LogLevel.Error, ex) { "Could not load plugin" }
            LoadResult(log, false)
        }
    }

    protected open fun loadInternal(log: LogList, config: ConfigurationNode): Boolean {
        val logLevel = LogLevel.valueOf(config.node(LOG_LEVEL).get { LogLevel.Verbose.name })
        this.log.level = logLevel

        return true
    }

    protected data class ConfigData(
        val node: ConfigurationNode,
        val path: Path,
    )

    protected fun walkConfigs(
        root: File,
        onVisit: (node: ConfigurationNode, path: Path) -> Unit = { _, _ -> },
        onError: (ex: Exception, path: Path) -> Unit = { _, _ -> }
    ): List<ConfigData> {
        if (!root.exists()) return emptyList()

        val configs = ArrayList<ConfigData>()
        walkFile(root.toPath(),
            onVisit = { path, _ ->
                if (path.isRegularFile() && path.name.endsWith(CONFIG_FILE_EXT)) {
                    try {
                        val node = AlexandriaAPI.configLoader().path(path).build().load()
                        onVisit(node, path)
                        configs.add(ConfigData(node, path))
                    } catch (ex: Exception) {
                        onError(ex, path)
                    }
                }
                FileVisitResult.CONTINUE
            },
            onFail = { path, ex ->
                onError(ex, path)
                FileVisitResult.CONTINUE
            }
        )
        return configs
    }

    fun resource(path: String): InputStream {
        val url = classLoader.getResource(path)
            ?: throw IllegalArgumentException("No URL found for $path")
        val conn = url.openConnection()
        conn.useCaches = false
        return conn.getInputStream()
    }

    fun i18n(value: String) = "${manifest.name}.$value"

    fun chatMessage(content: Component): Component {
        return Component.empty()
            .append(chatPrefix)
            .append(content)
    }

    fun chatMessages(content: Iterable<Component>) = content.map { chatMessage(it) }

    fun sendMessage(audience: Audience, content: Iterable<Component>) {
        // send individually so lines appear right in console
        chatMessages(content).forEach {
            audience.sendMessage(it)
        }
    }

    fun sendMessage(audience: Audience, content: I18N<Component>.() -> List<Component>) {
        sendMessage(audience, content(AlexandriaAPI.i18nFor(audience)))
    }
}
