package com.gitlab.aecsocket.alexandria.paper

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.*
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.degrees
import com.gitlab.aecsocket.alexandria.core.extension.euler
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitNextEntityId
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

sealed interface Mesh {
    val id: UUID
    var item: ItemStack
    var transform: Transform
    var glowingColor: NamedTextColor

    fun trackedPlayers(): Iterable<Player>

    fun updateTrackedPlayers(getter: () -> Iterable<Player>)

    fun spawn(players: Iterable<Player>)

    fun spawn(player: Player) = spawn(setOf(player))

    fun glowing(state: Boolean, players: Iterable<Player>)

    fun glowing(state: Boolean, player: Player) = glowing(state, setOf(player))

    fun remove(players: Iterable<Player>)

    fun remove(player: Player) = remove(setOf(player))
}

class MeshManager internal constructor() : PacketListener {
    private val _meshes = HashMap<UUID, BaseMesh>()
    val meshes: Map<UUID, Mesh> get() = _meshes

    fun nextMeshId(): UUID {
        var id = UUID.randomUUID()
        while (_meshes.contains(id)) {
            id = UUID.randomUUID()
        }
        return id
    }

    operator fun contains(id: UUID) = _meshes.contains(id)

    operator fun get(id: UUID): Mesh? = _meshes[id]

    fun create(
        item: ItemStack,
        transform: Transform,
        getTrackedPlayers: () -> Iterable<Player>,
        interpolated: Boolean = true
    ): Mesh {
        val id = nextMeshId()
        return (
            if (interpolated) InterpMesh(id, item, transform, getTrackedPlayers)
            else NonInterpMesh(id, item, transform, getTrackedPlayers)
        ).also {
            _meshes[id] = it
        }
    }

    fun remove(id: UUID, update: Boolean = true): Mesh? {
        return _meshes.remove(id)?.also {
            if (update) {
                it.remove(it.lastTrackedPlayers)
            }
        }
    }

    internal fun update() {
        _meshes.forEach { (_, mesh) ->
            mesh.lastTrackedPlayers = mesh.trackedPlayers()
        }
    }

    sealed class BaseMesh(
        override val id: UUID,
        item: ItemStack,
        var getTrackedPlayers: () -> Iterable<Player>,
        private val yOffset: Double,
    ) : Mesh {
        val entityId: UUID = UUID.randomUUID()
        val protocolId = bukkitNextEntityId
        var lastTrackedPlayers: Iterable<Player> = emptySet()

        override var glowingColor: NamedTextColor = NamedTextColor.WHITE
            set(value) {
                // due to how we handle storing the last team applied,
                // we can't send separate glowing colors to separate players
                field = value
                val packets = glowingColor(value)
                update(packets)
            }

        override var item = item
            set(value) {
                field = value
                update(item())
            }

        override fun trackedPlayers() = getTrackedPlayers()

        override fun updateTrackedPlayers(getter: () -> Iterable<Player>) {
            getTrackedPlayers = getter
        }

        protected fun update(packets: () -> Iterable<PacketWrapper<*>>) {
            lastTrackedPlayers.forEach { player ->
                packets().forEach { player.sendPacket(it) }
            }
        }

        protected fun position(transform: Transform) = transform.translation.y { it - yOffset }.run {
            Vector3d(x, y, z)
        }

        protected fun headRotation(transform: Transform) = transform.rotation.euler(EulerOrder.ZYX).x { -it }.degrees.run {
            Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
        }


        protected fun item(): () -> List<PacketWrapper<*>> = {
            val stack = SpigotConversionUtil.fromBukkitItemStack(item)
            listOf(
                WrapperPlayServerEntityEquipment(protocolId, listOf(Equipment(EquipmentSlot.HELMET, stack)))
            )
        }

        protected fun glowingColor(color: NamedTextColor): () -> List<PacketWrapper<*>> = {
            val team = AlexandriaTeams.colorToTeam(color)
            val eid = entityId.toString()
            listOf(
                WrapperPlayServerTeams(team, WrapperPlayServerTeams.TeamMode.ADD_ENTITIES, Optional.empty(), eid)
            )
        }

        protected fun transform(positionId: Int, rotationId: Int): () -> List<PacketWrapper<*>> = {
            val position = position(transform)
            val headRotation = headRotation(transform)
            listOf(
                WrapperPlayServerEntityTeleport(positionId, position, 0f, 0f, false),
                WrapperPlayServerEntityMetadata(rotationId, listOf(
                    EntityData(16, EntityDataTypes.ROTATION, headRotation)
                ))
            )
        }

        protected fun spawnStand(position: Vector3d): () -> List<PacketWrapper<*>> {
            val pItem = item()
            val pGlowingColor = glowingColor(glowingColor)
            return {
                val headRotation = headRotation(transform)
                listOf(
                    WrapperPlayServerSpawnEntity(protocolId,
                        Optional.of(entityId), EntityTypes.ARMOR_STAND,
                        position, 0f, 0f, 0f, 0, Optional.empty()
                    ),
                    WrapperPlayServerEntityMetadata(protocolId, listOf(
                        EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()),
                        EntityData(15, EntityDataTypes.BYTE, (0x10).toByte()),
                        EntityData(16, EntityDataTypes.ROTATION, headRotation),
                    ))
                ) + pItem() + pGlowingColor()
            }
        }

        override fun glowing(state: Boolean, players: Iterable<Player>) {
            val state = (0x20 or (if (state) 0x40 else 0)).toByte() // invisible + glowing?

            players.forEach { player ->
                player.sendPacket(WrapperPlayServerEntityMetadata(protocolId, listOf(
                    EntityData(0, EntityDataTypes.BYTE, state) // invisible + glowing?
                )))
            }
        }
    }

    class InterpMesh internal constructor(
        id: UUID,
        item: ItemStack,
        transform: Transform,
        getTrackedPlayers: () -> Iterable<Player>,
    ) : BaseMesh(id, item, getTrackedPlayers, 1.45) {
        override var transform = transform
            set(value) {
                field = value
                update(transform(protocolId, protocolId))
            }

        override fun spawn(players: Iterable<Player>) {
            val packets = spawnStand(position(transform))
            players.forEach { player ->
                packets().forEach { player.sendPacket(it) }
            }
        }

        override fun remove(players: Iterable<Player>) {
            players.forEach { player ->
                player.sendPacket(WrapperPlayServerDestroyEntities(protocolId))
            }
        }
    }

    class NonInterpMesh internal constructor(
        id: UUID,
        item: ItemStack,
        transform: Transform,
        getTrackedPlayers: () -> Iterable<Player>,
    ) : BaseMesh(id, item, getTrackedPlayers, 1.82) {
        val vehicleId = bukkitNextEntityId

        override var transform = transform
            set(value) {
                field = value
                update(transform(vehicleId, protocolId))
            }

        override fun spawn(players: Iterable<Player>) {
            val position = position(transform)
            val pSpawnStand = spawnStand(position)
            val vehicleUuid = UUID.randomUUID()
            val passengersArray = intArrayOf(protocolId)

            players.forEach { player ->
                pSpawnStand().forEach { player.sendPacket(it) }
                player.sendPacket(WrapperPlayServerSpawnEntity(vehicleId,
                    Optional.of(vehicleUuid), EntityTypes.AREA_EFFECT_CLOUD,
                    position, 0f, 0f, 0f, 0, Optional.empty()
                ))
                player.sendPacket(WrapperPlayServerEntityMetadata(vehicleId, listOf(
                    EntityData(8, EntityDataTypes.FLOAT, 0f)
                )))
                player.sendPacket(WrapperPlayServerSetPassengers(vehicleId, passengersArray))
            }
        }

        override fun remove(players: Iterable<Player>) {
            players.forEach { player ->
                player.sendPacket(WrapperPlayServerDestroyEntities(protocolId, vehicleId))
            }
        }
    }
}
