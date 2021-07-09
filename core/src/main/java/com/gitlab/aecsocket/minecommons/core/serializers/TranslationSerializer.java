package com.gitlab.aecsocket.minecommons.core.serializers;

import com.gitlab.aecsocket.minecommons.core.translation.Translation;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

/**
 * Type serializer for a {@link Translation}.
 * <p>
 * Uses {@link Level#parse(String)}.
 */
public class TranslationSerializer implements TypeSerializer<Translation> {
    /** A singleton instance of this serializer. */
    public static final TranslationSerializer INSTANCE = new TranslationSerializer();

    @Override
    public void serialize(Type type, @Nullable Translation obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) node.set(null);
        else {
            for (var entry : obj.entrySet()) {
                ConfigurationNode child = node.node(entry.getKey());
                String value = entry.getValue();
                String[] lines = value.split("\n");
                if (lines.length == 1) {
                    child.set(value);
                } else {
                    child.setList(String.class, Arrays.asList(lines));
                }
            }
            node.node("locale").set(obj.locale());
        }
    }

    private String join(String... path) { return String.join(".", path); }

    private void deserialize(Map<String, String> values, ConfigurationNode node, String... path) throws SerializationException {
        if (node.isMap()) {
            for (var entry : node.childrenMap().entrySet()) {
                String[] newPath = Arrays.copyOf(path, path.length + 1);
                newPath[path.length] = entry.getKey().toString();
                deserialize(values, entry.getValue(), newPath);
            }
        } else if (node.isList()) {
            values.put(join(path), String.join("\n", Serializers.require(node, new TypeToken<List<String>>() {})));
        } else {
            values.put(join(path), node.getString());
        }
    }

    @Override
    public Translation deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Map<String, String> values = new HashMap<>();
        deserialize(values, node);
        return new Translation(values, Serializers.require(node.node("locale"), Locale.class));
    }
}
