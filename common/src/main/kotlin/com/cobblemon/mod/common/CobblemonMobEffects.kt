/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.api.cooking.effects.CleanseAllEffect
import com.cobblemon.mod.common.api.cooking.effects.CleanseNegativeEffect
import com.cobblemon.mod.common.api.cooking.effects.MentalRestorationEffect
import com.cobblemon.mod.common.platform.PlatformRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffect

object CobblemonMobEffects : PlatformRegistry<Registry<MobEffect>, ResourceKey<Registry<MobEffect>>, MobEffect>() {
    override val registry: Registry<MobEffect> = BuiltInRegistries.MOB_EFFECT
    override val resourceKey: ResourceKey<Registry<MobEffect>> = Registries.MOB_EFFECT

    @JvmField
    val CLEANSE_NEGATIVE = create("cleanse_negative", CleanseNegativeEffect())

    @JvmField
    val MENTAL_RESTORATION = create("mental_restoration", MentalRestorationEffect())

    @JvmField
    val CLEANSE_ALL = create("cleanse_all", CleanseAllEffect())
}