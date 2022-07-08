package com.github.aecsocket.alexandria.core.serializer

import com.github.aecsocket.alexandria.core.DoubleMod
import com.github.aecsocket.alexandria.core.IntMod
import com.github.aecsocket.alexandria.core.effect.SoundEffect
import com.github.aecsocket.alexandria.core.extension.registerExact
import com.github.aecsocket.alexandria.core.physics.Shape
import com.github.aecsocket.alexandria.core.physics.SimpleBody
import com.github.aecsocket.alexandria.core.spatial.*
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.util.*

object Serializers {
    val ALL: TypeSerializerCollection = TypeSerializerCollection.builder()
        .registerExact(Locale::class, LocaleSerializer)
        .registerExact(WDuration::class, DurationSerializer)
        .registerExact(Vector2::class, Vector2Serializer)
        .registerExact(Vector3::class, Vector3Serializer)
        .registerExact(Point2::class, Point2Serializer)
        .registerExact(Point3::class, Point3Serializer)
        .registerExact(SoundEffect::class, SoundEffectSerializer)
        .registerExact(DoubleMod::class, DoubleModSerializer)
        .registerExact(IntMod::class, IntModSerializer)
        .registerExact(Shape::class, ShapeSerializer)
        .registerExact(Transform::class, TransformSerializer)
        .registerExact(SimpleBody::class, SimpleBodySerializer)
        .build()
}
