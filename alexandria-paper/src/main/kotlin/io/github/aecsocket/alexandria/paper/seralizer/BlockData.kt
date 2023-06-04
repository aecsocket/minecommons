package io.github.aecsocket.alexandria.paper.seralizer

import io.github.aecsocket.alexandria.extension.force
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object BlockDataSerializer : TypeSerializer<BlockData> {
    override fun serialize(type: Type, obj: BlockData?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.set(obj.getAsString(true))
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): BlockData {
        return try {
            Bukkit.createBlockData(node.force<String>())
        } catch (ex: IllegalArgumentException) {
            throw SerializationException(node, type, "Invalid block data", ex)
        }
    }
}
