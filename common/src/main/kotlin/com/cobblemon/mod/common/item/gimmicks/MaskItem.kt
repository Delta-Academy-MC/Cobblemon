package com.cobblemon.mod.common.item.gimmicks

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Equipable

class MaskItem : LegendaryItem(2), Equipable {
    override fun getEquipmentSlot(): EquipmentSlot {
        return EquipmentSlot.HEAD
    }
}