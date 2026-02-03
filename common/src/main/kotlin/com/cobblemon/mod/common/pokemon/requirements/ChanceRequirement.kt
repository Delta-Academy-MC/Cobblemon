/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.requirements

import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.pokemon.requirement.Requirement
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.genericRuntime
import com.cobblemon.mod.common.util.resolveFloat
import kotlin.random.Random

/**
 * A requirement that will only pass the check if some random chance is satisfied.
 *
 * @author Hiroku
 * @since January 30th, 2026
 */
class ChanceRequirement : Requirement {
    companion object {
        const val ADAPTER_VARIANT = "chance"
    }

    val chance: ExpressionLike = "1".asExpressionLike()

    override fun check(pokemon: Pokemon): Boolean {
        genericRuntime.environment.query.addFunction("pokemon") { pokemon.struct }
        val chance = genericRuntime.resolveFloat(chance)
        return Random.nextFloat() < chance
    }
}