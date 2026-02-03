/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.detail

import com.cobblemon.mod.common.api.spawning.SpawnBucket
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import java.util.concurrent.CompletableFuture


/**
 * A planned, actionable spawn.
 *
 * @author Hiroku
 * @since February 4th, 2022
 */
abstract class SpawnAction<R>(
    val spawnablePosition: SpawnablePosition,
    val bucket: SpawnBucket,
    open val detail: SpawnDetail
) {
    val labels = mutableListOf<String>()
    val future = CompletableFuture<R>().also { it.thenApply { result -> spawnablePosition.spawner.afterSpawn(this, result) } }

    /**
     * Does whatever action is required to spawn this detail into the spawnable position.
     *
     * @return The result of spawning
     */
    protected abstract fun run(): R?

    /**
     * Runs the spawn action and processes any non-null result.
     */
    open fun complete(): R? {
        if (future.isDone) {
            return null
        }

        spawnablePosition.applyInfluences { it.affectAction(this) }
        val result = run()
        future.complete(result)

        return result
    }
}
