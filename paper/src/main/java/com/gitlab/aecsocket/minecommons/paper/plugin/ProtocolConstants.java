package com.gitlab.aecsocket.minecommons.paper.plugin;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;

/**
 * Lists constants in ProtocolLib. In a separate class so that this does not get statically intialized.
 */
public final class ProtocolConstants {
    private ProtocolConstants() {}

    /**
     * A map of Bukkit {@link EquipmentSlot}s to protocol {@link EnumWrappers.ItemSlot}s.
     */
    public static final BiMap<EquipmentSlot, EnumWrappers.ItemSlot> SLOTS = HashBiMap.create(CollectionBuilder.map(new HashMap<EquipmentSlot, EnumWrappers.ItemSlot>())
            .put(EquipmentSlot.HAND, EnumWrappers.ItemSlot.MAINHAND)
            .put(EquipmentSlot.OFF_HAND, EnumWrappers.ItemSlot.OFFHAND)
            .put(EquipmentSlot.HEAD, EnumWrappers.ItemSlot.HEAD)
            .put(EquipmentSlot.CHEST, EnumWrappers.ItemSlot.CHEST)
            .put(EquipmentSlot.LEGS, EnumWrappers.ItemSlot.LEGS)
            .put(EquipmentSlot.FEET, EnumWrappers.ItemSlot.FEET)
            .build());
}
