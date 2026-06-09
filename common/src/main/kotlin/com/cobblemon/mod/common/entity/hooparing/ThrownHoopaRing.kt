package com.cobblemon.mod.common.entity.hooparing

import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.entity.HoopaRingActivateEvent
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

class ThrownHoopaRing(level: Level) : ThrowableItemProjectile(CobblemonEntities.HOOPA_RING, level) {

    override fun getDefaultItem(): Item {
        return CobblemonItems.HOOPA_RING
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        result.entity.hurt(this.damageSources().thrown(this, this.owner), 1.0f)
    }

    override fun onHit(result: HitResult) {
        super.onHit(result)
        if (!this.level().isClientSide) {
            CobblemonEvents.HOOPA_RING_ACTIVATED.post(HoopaRingActivateEvent(this))
            this.level().broadcastEntityEvent(this, 3)
            this.discard()
        }
    }

}