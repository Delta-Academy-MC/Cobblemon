/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.cooking.effects

import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity

class MentalRestorationEffect : MobEffect(MobEffectCategory.BENEFICIAL, 0xAA66FF) {
    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        if (entity is ServerPlayer) {
            val stats = entity.stats
            val statType = Stats.CUSTOM.get(Stats.TIME_SINCE_REST)
            val current = stats.getValue(statType)
            val reduction = 31 * (amplifier + 1)// reduce by half a minute every second per level (30 ticks removed per tick + 1 to account for the tick passing)
            val newValue = Math.max(current - reduction, 0)
            stats.setValue(entity, statType, newValue)
        }
        return true
    }

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }
}