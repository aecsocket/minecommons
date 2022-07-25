package com.gitlab.aecsocket.alexandria.paper

import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.TableAlign
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.paper.plugin.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.plugin.ConfigOptionsAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.map.MapFont
import org.bukkit.map.MinecraftFont
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val PADDING = "padding"
private const val CHAR_WIDTHS = "char_widths"

private lateinit var instance: Alexandria
val AlexandriaAPI get() = instance

class Alexandria : BasePlugin<Alexandria.LoadScope>() {
    interface LoadScope : BasePlugin.LoadScope

    override fun createLoadScope(configOptionActions: MutableList<ConfigOptionsAction>) = object : LoadScope {
        override fun onConfigOptionsSetup(action: ConfigOptionsAction) {
            configOptionActions.add(action)
        }
    }

    lateinit var padding: String
    lateinit var charSizes: MapFont

    var paddingWidth: Int = 0

    fun widthOf(text: String) =
        charSizes.getWidth(text)

    fun widthOf(component: Component) =
        widthOf(PlainTextComponentSerializer.plainText().serialize(component))

    fun paddingOf(width: Int) =
        padding.repeat(width / (paddingWidth + 1))

    inner class StringTableRenderer(
        align: (Int) -> TableAlign = { TableAlign.START },
        justify: (Int) -> TableAlign = { TableAlign.START },
        colSeparator: String = "",
        rowSeparator: (List<Int>) -> Iterable<String> = { emptySet() },
    ) : com.gitlab.aecsocket.alexandria.core.StringTableRenderer(align, justify, colSeparator, rowSeparator) {
        override fun widthOf(value: String) =
            this@Alexandria.widthOf(value)

        override fun paddingOf(width: Int) =
            this@Alexandria.paddingOf(width)
    }

    inner class ComponentTableRenderer(
        align: (Int) -> TableAlign = { TableAlign.START },
        justify: (Int) -> TableAlign = { TableAlign.START },
        colSeparator: Component = empty(),
        rowSeparator: (List<Int>) -> Iterable<Component> = { emptySet() },
    ) : com.gitlab.aecsocket.alexandria.core.ComponentTableRenderer(align, justify, colSeparator, rowSeparator) {
        override fun widthOf(value: Component) =
            this@Alexandria.widthOf(value)

        override fun paddingOf(width: Int) =
            text(this@Alexandria.paddingOf(width))
    }

    init {
        instance = this
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
            padding = settings.node(PADDING).get { " " }
            charSizes = MinecraftFont()
            settings.node(CHAR_WIDTHS).childrenMap().forEach { (char, width) ->
                charSizes.setChar(
                    char.toString()[0],
                    MapFont.CharacterSprite(width.force(), 0, booleanArrayOf())
                )
            }

            paddingWidth = widthOf(padding)

            return true
        }
        return false
    }
}
