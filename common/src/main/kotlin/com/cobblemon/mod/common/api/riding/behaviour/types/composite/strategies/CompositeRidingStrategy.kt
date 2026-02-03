/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.behaviour.types.composite.strategies

import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviours
import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeState
import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

interface CompositeRidingStrategy<T : CompositeSettings> {
    val key: ResourceLocation

    fun tick(
        settings: T,
        state: CompositeState,
        defaultState: RidingBehaviourState,
        alternateState: RidingBehaviourState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    )

    fun transition(
        vehicle: PokemonEntity,
        settings: CompositeSettings,
        state: CompositeState,
        fromState: RidingBehaviourState,
        toState: RidingBehaviourState,
        toSettings: RidingBehaviourSettings
    ) {
        toState.stamina.set(fromState.stamina.get())
        toState.rideVelocity.set(fromState.rideVelocity.get())
        fromState.reset()
        state.activeBehaviour.set(toSettings.key)
        state.lastTransition.set(vehicle.level().gameTime)
        val behaviour = RidingBehaviours.get(toSettings.key)
        vehicle.setBehaviourFlag(PokemonBehaviourFlag.FLYING, behaviour.getRidingStyle(toSettings, toState) == RidingStyle.AIR)
    }

}
