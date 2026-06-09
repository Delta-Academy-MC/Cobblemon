package com.cobblemon.mod.common.item

import com.cobblemon.mod.common.entity.hooparing.ThrownHoopaRing
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ProjectileItem
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level

class HoopaRingItem(properties: Properties) : Item(properties), ProjectileItem {

    override fun getUseAnimation(stack: ItemStack?): UseAnim? {
        return UseAnim.BOW
    }

    override fun getUseDuration(stack: ItemStack?, entity: LivingEntity?): Int {
        return 72000
    }

    override fun releaseUsing(stack: ItemStack, level: Level, livingEntity: LivingEntity, timeCharged: Int) {
        if (livingEntity is Player) {
            val useDuration = getUseDuration(stack, livingEntity) - timeCharged
            if (useDuration >= 10) {
                val thrownHoopaRing = ThrownHoopaRing(level)
                thrownHoopaRing.setPos(livingEntity.x, livingEntity.eyeY - 0.1f, livingEntity.z)
                thrownHoopaRing.shootFromRotation(livingEntity, livingEntity.xRot, livingEntity.yRot, 0f, 2.5f, 1f)
                level.addFreshEntity(thrownHoopaRing)
                level.playSound(null, livingEntity, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1f, 1f)
                stack.consume(1, livingEntity)
            }
        }
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack?>? {
        val stack = player.getItemInHand(usedHand)
        player.startUsingItem(usedHand)
        return InteractionResultHolder.consume(stack)
    }

    override fun asProjectile(level: Level, pos: Position, stack: ItemStack, direction: Direction): Projectile {
        val thrownHoopaRing = ThrownHoopaRing(level)
        thrownHoopaRing.setPos(pos.x(), pos.y(), pos.z())
        return thrownHoopaRing
    }

}