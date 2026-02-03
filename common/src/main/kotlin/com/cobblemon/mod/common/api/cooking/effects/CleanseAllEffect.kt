/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.cooking.effects

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

class CleanseAllEffect : MobEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF) {
    override fun isInstantenous() = true
    override fun applyInstantenousEffect(source: Entity?, indirect: Entity?, target: LivingEntity, amplifier: Int, proximity: Double) {
        target.removeAllEffects()
    }
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int) = false
}