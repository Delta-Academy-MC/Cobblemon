/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.ModAPI
import com.cobblemon.mod.common.api.snowstorm.ParticleMaterial
import com.cobblemon.mod.common.api.snowstorm.UVDetails
import com.cobblemon.mod.common.client.MountedPokemonAnimationRenderController
import com.cobblemon.mod.common.client.particle.ParticleStorm
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.math.geometry.transformDirection
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.resolveDouble
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import kotlin.math.abs
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.ParticleRenderType.NO_RENDER
import net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.random.Random

class SnowstormParticle(
    val storm: ParticleStorm,
    world: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    initialVelocity: Vec3,
    //Usually, this will be the storms runtime. But sometimes particles need to have variables different from their emitter, so the VariableStruct will differ
    val runtime: MoLangRuntime,
    val matrixWrapper: MatrixWrapper,
    var invisible: Boolean = false,
) : Particle(world, x, y, z) {
    companion object {
        const val MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION = 0.005
    }

    val sprite = getSpriteFromAtlas()

    val particleTextureSheet: ParticleRenderType
    var angularVelocity = 0.0
    var colliding = false

    var texture = storm.effect.particle.texture

    var localX = x - storm.getX()
    var localY = y - storm.getY()
    var localZ = z - storm.getZ()
    var rotatedLocal = Vector3d(localX, localY, localZ)

    var prevLocalX = localX
    var prevLocalY = localY
    var prevLocalZ = localZ
    var prevRotatedLocal = Vector3d(localX, localY, localZ)

    val currentRotation = AxisAngle4d(0.0, 0.0, 1.0, 0.0)

    var oldAxisRotation = AxisAngle4d(0.0, 0.0, 1.0, 0.0)
    var axisRotation = AxisAngle4d(0.0, 0.0, 1.0, 0.0)


    val uvDetails = UVDetails()

    var viewDirection = Vec3.ZERO
    var originPos = Vec3(storm.getX(), storm.getY(), storm.getZ())
    var prevOriginPos = originPos

    fun getX() = x
    fun getY() = y
    fun getZ() = z

    fun getVelocityX() = xd
    fun getVelocityY() = yd
    fun getVelocityZ() = zd

    fun getSpriteFromAtlas(): TextureAtlasSprite {
        val atlas = Minecraft.getInstance().particleEngine.textureAtlas
        val sprite = atlas.getSprite(storm.effect.particle.texture)
        return sprite
    }

    private fun applyRandoms() {
        //Will having these emitter randoms not equal the real emitter randoms be a problem?
        runtime.environment.setSimpleVariable("emitter_random_1", DoubleValue(Random.Default.nextDouble()))
        runtime.environment.setSimpleVariable("emitter_random_2", DoubleValue(Random.Default.nextDouble()))
        runtime.environment.setSimpleVariable("emitter_random_3", DoubleValue(Random.Default.nextDouble()))
        runtime.environment.setSimpleVariable("emitter_random_4", DoubleValue(Random.Default.nextDouble()))
    }

    init {
        setParticleSpeed(initialVelocity.x, initialVelocity.y, initialVelocity.z)
        roll = -storm.effect.particle.rotation.getInitialRotation(runtime).toFloat()
        oRoll = roll
        angularVelocity = storm.effect.particle.rotation.getInitialAngularVelocity(runtime)
        friction = 1F
        lifetime = (runtime.resolveDouble(storm.effect.particle.maxAge) * 20).toInt()
        storm.particles.add(this)
        gravity = 0F
        originPos = matrixWrapper.getOrigin()
        prevOriginPos = originPos
        particleTextureSheet = if (invisible) NO_RENDER else PARTICLE_SHEET_TRANSLUCENT
        storm.effect.particle.creationEvents.forEach { it.trigger(storm, this) }
    }

    override fun render(vertexConsumer: VertexConsumer, camera: Camera, tickDelta: Float) {
        if (invisible) {
            return
        }

        if (Cobblemon.implementation.modAPI != ModAPI.FORGE) {
           if (!Minecraft.getInstance().levelRenderer.cullingFrustum.isVisible(boundingBox)) {
               return
           }
        }

        storm.entity?.let {
            if (it is PokemonEntity) {
                MountedPokemonAnimationRenderController.setup(it, tickDelta)
            }
        }

        applyRandoms()
        setParticleAgeInRuntime()
        storm.effect.curves.forEach { it.apply(runtime) }
        runtime.execute(storm.effect.particle.renderExpressions)

//        // TODO need to implement the other materials but not sure exactly what they are GL wise
        when (storm.effect.particle.material) {
            // Alpha is the usual effect of "Cutout", this needs a shader but fabric fucking sucks so... Ignoring it.
            ParticleMaterial.ALPHA -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
            ParticleMaterial.OPAQUE -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ZERO)
            ParticleMaterial.BLEND -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
            ParticleMaterial.ADD -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F)

        val vec3d = camera.position
        val interpOriginX = Mth.lerp(tickDelta.toDouble(), prevOriginPos.x, originPos.x)
        val interpOriginY = Mth.lerp(tickDelta.toDouble(), prevOriginPos.y, originPos.y)
        val interpOriginZ = Mth.lerp(tickDelta.toDouble(), prevOriginPos.z, originPos.z)

        val interpLocalX = Mth.lerp(tickDelta.toDouble(), prevLocalX, localX)
        val interpLocalY = Mth.lerp(tickDelta.toDouble(), prevLocalY, localY)
        val interpLocalZ = Mth.lerp(tickDelta.toDouble(), prevLocalZ, localZ)

        val pos = if (storm.effect.space.localRotation) {
            val interpRotation = Mth.lerp(tickDelta.toDouble(), 0.0, currentRotation.angle)
            val vec = Vector3d(interpLocalX, interpLocalY, interpLocalZ)
            oldAxisRotation.transform(vec)
            currentRotation.get(AxisAngle4d()).also { it.angle = interpRotation }.transform(vec)
        } else {
            Vector3d(interpLocalX, interpLocalY, interpLocalZ)
        }

        val currentOrigin = matrixWrapper.getOrigin()
        val finalOriginX = if (storm.effect.space.localPosition) currentOrigin.x else interpOriginX
        val finalOriginY = if (storm.effect.space.localPosition) currentOrigin.y else interpOriginY
        val finalOriginZ = if (storm.effect.space.localPosition) currentOrigin.z else interpOriginZ

        val f = (pos.x + finalOriginX - vec3d.x()).toFloat()
        val g = (pos.y + finalOriginY - vec3d.y()).toFloat()
        val h = (pos.z + finalOriginZ - vec3d.z()).toFloat()
        val quaternion = storm.effect.particle.cameraMode.getRotation(
            matrixWrapper = matrixWrapper,
            prevAngle = oRoll,
            angle = roll,
            deltaTicks = tickDelta,
            particlePosition = Vec3(x, y, z),
            cameraPosition = camera.position,
            cameraAngle = camera.rotation(),
            cameraYaw = camera.yRot,
            cameraPitch = camera.xRot,
            viewDirection = viewDirection
        )
        val xSize = runtime.resolveDouble(storm.effect.particle.sizeX).toFloat() / 1.5.toFloat()
        val ySize = runtime.resolveDouble(storm.effect.particle.sizeY).toFloat() / 1.5.toFloat()

        val particleVertices = arrayOf(
            Vector3f(xSize, -ySize, 0.0f),
            Vector3f(xSize, ySize, 0.0f),
            Vector3f(-xSize, ySize, 0.0f),
            Vector3f(-xSize, -ySize, 0.0f)
        )

        for (k in 0..3) {
            val vertex = particleVertices[k]
            vertex.rotate(quaternion)
            vertex.add(f, g, h)
        }

        val uvs = storm.effect.particle.uvMode.get(runtime, age / 20.0, lifetime / 20.0, uvDetails)
        val colour = storm.getParticleColor() ?: storm.effect.particle.tinting.getTint(runtime)

        val spriteURange = sprite.u1 - sprite.u0
        val spriteVRange = sprite.v1 - sprite.v0

        val minU = uvs.startU * spriteURange + sprite.u0
        val maxU = uvs.endU * spriteURange + sprite.u0
        val minV = uvs.startV * spriteVRange + sprite.v0
        val maxV = uvs.endV * spriteVRange + sprite.v0

        val p = if (storm.effect.particle.environmentLighting) getLightColor(tickDelta) else (15 shl 20 or (15 shl 4))
        vertexConsumer
            .addVertex(particleVertices[0].x, particleVertices[0].y, particleVertices[0].z)
            .setUv(maxU, maxV)
            .setColor(colour.x, colour.y, colour.z, colour.w)
            .setLight(p)
        vertexConsumer
            .addVertex(particleVertices[1].x, particleVertices[1].y, particleVertices[1].z)
            .setUv(maxU, minV)
            .setColor(colour.x, colour.y, colour.z, colour.w)
            .setLight(p)
        vertexConsumer
            .addVertex(particleVertices[2].x, particleVertices[2].y, particleVertices[2].z)
            .setUv(minU, minV)
            .setColor(colour.x, colour.y, colour.z, colour.w)
            .setLight(p)
        vertexConsumer
            .addVertex(particleVertices[3].x, particleVertices[3].y, particleVertices[3].z)
            .setUv(minU, maxV)
            .setColor(colour.x, colour.y, colour.z, colour.w)
            .setLight(p)
    }

    fun runExpirationEvents() {
        storm.effect.particle.expirationEvents.forEach { it.trigger(storm, this)}
    }

    override fun tick() {
        if (storm.effect.space.localPosition) {
            prevOriginPos = originPos
            originPos = matrixWrapper.getOrigin()
        }
        else {
            prevOriginPos = originPos
        }

        applyRandoms()
        setParticleAgeInRuntime()
        storm.effect.curves.forEach { it.apply(runtime) }
        runtime.execute(storm.effect.particle.updateExpressions)
        angularVelocity = storm.effect.particle.rotation.getAngularVelocity(runtime, -roll.toDouble(), angularVelocity) / 20

        if (age >= lifetime || runtime.resolveBoolean(storm.effect.particle.killExpression)) {
            runExpirationEvents()
            remove()
            return
        } else {
            val velocity = storm.effect.particle.motion.getVelocity(runtime, this,
                Vec3(xd, yd, zd)
            )
            xd = velocity.x
            yd = velocity.y
            zd = velocity.z
            oRoll = roll
            // Subtract because Bedrock particles are counter-clockwise and Java Edition is clockwise.
            roll = oRoll - angularVelocity.toFloat()
        }

        viewDirection = storm.effect.particle.viewDirection.getDirection(
            runtime = runtime,
            lastDirection = viewDirection,
            currentVelocity = Vec3(xd, yd, zd)
        ).normalize()

        xo = x
        yo = y
        zo = z

        prevLocalX = localX
        prevLocalY = localY
        prevLocalZ = localZ

        oldAxisRotation = axisRotation
        prevRotatedLocal = oldAxisRotation.transform(Vector3d(prevLocalX, prevLocalY, prevLocalZ))

        //This is a bit of a hack. Technically local rotation is supposed to make it so the particle uses
        //the emitter's rotation - but we've made it so the emitter doesn't actually follow the locators rotation, only position.
        //So instead of being bound to the emitter, the particle is actually bound to the locator for local rotation.
        //If you're messing with this - make sure evo particles still rotate with the pokemon
        if (storm.effect.space.localRotation) {
            storm.attachedMatrix.matrix.getRotation(axisRotation)
        }
        else {
            matrixWrapper.matrix.getRotation(axisRotation)
        }

        rotatedLocal = axisRotation.transform(Vector3d(prevLocalX, prevLocalY, prevLocalZ))
        Quaterniond().rotateTo(prevRotatedLocal, rotatedLocal).get(currentRotation)

        age++

        this.move(xd, yd, zd)

        // update originPos if not in local space, since move() might have updated localX/Y/Z
        if (!storm.effect.space.localPosition) {
            originPos = matrixWrapper.getOrigin()
        }

        storm.effect.particle.timeline.check(storm, this, (age - 1) / 20.0, age / 20.0)
    }

    override fun move(dx: Double, dy: Double, dz: Double) {
        val collision = storm.effect.particle.collision
        val radius = runtime.resolveDouble(collision.radius)
        boundingBox = AABB.ofSize(Vec3(x, y, z), radius, radius, radius)
        if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
            updatePosition()
            return
        }

        var dx = dx
        var dy = dy
        var dz = dz

        if (runtime.resolveBoolean(collision.enabled) && radius > 0.0 && !storm.effect.space.isLocalSpace) {
            hasPhysics = true

            val newMovement = checkCollision(Vec3(dx, dy, dz))

            if (removed) {
                return
            }

            dx = newMovement.x
            dy = newMovement.y
            dz = newMovement.z

//            if (collidesWithWorld && (dx != 0.0 || dy != 0.0 || dz != 0.0) && dx * dx + dy * dy + dz * dz < 10000) {
//                val vec3d = Entity.adjustMovementForCollisions(
//                    null,
//                    Vec3d(dx, dy, dz),
//                    boundingBox,
//                    world,
//                    listOf()
//                )
//
//            }

            if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
                boundingBox = boundingBox.move(dx, dy, dz)
                localX += dx
                localY += dy
                localZ += dz
            }

//            if (abs(dy) >= 9.999999747378752E-6 && abs(dy) < 9.999999747378752E-6) {
//                field_21507 = true
//            }
//            onGround = dy != dy && e < 0.0
//            if (d != dx) {
//                xd = 0.0
//            }
//            if (dz != dz) {
//                zd = 0.0
//            }
        } else {
            hasPhysics = false
            if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
                localX += dx
                localY += dy
                localZ += dz
            }
        }
        updatePosition()
    }

    fun updatePosition() {
        val localVector = if (storm.effect.space.localRotation) storm.attachedMatrix.matrix.transformDirection(
            Vec3(
                localX,
                localY,
                localZ
            )
        ) else Vec3(localX, localY, localZ)
        x = localVector.x + originPos.x
        y = localVector.y + originPos.y
        z = localVector.z + originPos.z
    }

    private fun checkCollision(movement: Vec3): Vec3 {
        val collision = storm.effect.particle.collision
        var box = boundingBox
        val bounciness = runtime.resolveDouble(collision.bounciness)
        val friction = runtime.resolveDouble(collision.friction)
        val expiresOnContact = collision.expiresOnContact

        val collisions = level.getBlockCollisions(null, box.expandTowards(movement))
        if (collisions.none()) {
            colliding = false
            return movement
        } else if (expiresOnContact) {
            runExpirationEvents()
            remove()
            return movement
        }

//        println("Collisions with Y values: ${collisionProvider.map { it.boundingBox.center.y }.distinct().joinToString() }")

        var xMovement = movement.x
        var yMovement = movement.y
        var zMovement = movement.z

        var bouncing = false
        var sliding = false

        if (yMovement != 0.0) {
//            // If it would have avoided collisions if not for the Y movement, then it's bouncing off a vertical-normal surface
//            val originalCollisionYs = collisionProvider.map { it.boundingBox.center.y }.distinct()
//            val yCollisions = world.getBlockCollisions(null, box.stretch(movement.multiply(1.0, 0.0, 1.0))).toList()
////            println("Compared to new Y values: ${yCollisions.map { it.boundingBox.center.y }.distinct().joinToString()}")
//            if (yCollisions.none { it.boundingBox.center.y in originalCollisionYs }) {
//                yMovement = 0.0
//                if (bounciness > 0.0 && abs(movement.y) > MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION) {
//                    yd *= -1 * bounciness
//                    yMovement = -1 * bounciness * movement.y
//                    bouncing = true
//                } else if (friction > 0.0) {
//                    sliding = true
//                    yd = 0.0
//                } else {
//                    yd = 0.0
//                }
//            } else {
//            }


            yMovement = Shapes.collide(Direction.Axis.Y, box, collisions, yMovement)
            if (yMovement != 0.0) {
                box = box.move(0.0, 0.0, zMovement)
            } else {
                if (bounciness > 0.0 && abs(movement.y) > MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION) {
                    yd *= -1 * bounciness
                    yMovement = -1 * bounciness * movement.y
                    bouncing = true
                } else if (friction > 0.0) {
                    sliding = true
                    yd = 0.0
                } else {
                    yd = 0.0
                }
            }
        }

        val mostlyIsZMovement = abs(xMovement) < abs(zMovement)
        if (mostlyIsZMovement && zMovement != 0.0) {
            zMovement = Shapes.collide(Direction.Axis.Z, box, collisions, zMovement)
            if (zMovement != 0.0) {
                box = box.move(0.0, 0.0, zMovement)
            } else {
                if (bounciness > 0.0 && abs(movement.z) > MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION) {
                    zd *= -1 * bounciness
                    zMovement = -1 * bounciness * movement.z
                    bouncing = true
                } else if (friction > 0.0) {
                    sliding = true
                    zd = 0.0
                } else {
                    zd = 0.0
                }
            }
        }

        if (xMovement != 0.0) {
            xMovement = Shapes.collide(Direction.Axis.X, box, collisions, xMovement)
            if (!mostlyIsZMovement && xMovement != 0.0) {
                box = box.move(xMovement, 0.0, 0.0)
            } else {
                if (bounciness > 0.0 && abs(movement.x) > MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION) {
                    xd *= -1 * bounciness
                    xMovement = -1 * bounciness * movement.x
                    bouncing = true
                } else if (friction > 0.0) {
                    sliding = true
                    zd = 0.0
                } else {
                    zd = 0.0
                }
            }
        }

        if (!mostlyIsZMovement && zMovement != 0.0) {
            zMovement = Shapes.collide(Direction.Axis.Z, box, collisions, zMovement)
            if (zMovement != 0.0) {
            } else {
                if (bounciness > 0.0 && abs(movement.z) > MAXIMUM_DISTANCE_CHANGE_PER_TICK_FOR_FRICTION) {
                    zd *= -1 * bounciness
                    zMovement = -1 * bounciness * movement.z
                    bouncing = true
                } else if (friction > 0.0) {
                    sliding = true
                    zd = 0.0
                } else {
                    zd = 0.0
                }
            }
        }

        var newMovement = Vec3(xMovement, yMovement, zMovement)

        if (sliding && !bouncing) {
            // If it's moving slower than the friction per second, time to stop
            newMovement = if (newMovement.length() * 20 < friction) {
                Vec3.ZERO
            } else {
                newMovement.subtract(newMovement.normalize().scale(friction / 20))
            }

            var velocity = Vec3(xd, yd, zd)
            if (velocity.length() * 20 < friction) {
                setParticleSpeed(0.0, 0.0, 0.0)
            } else {
                velocity = velocity.subtract(velocity.normalize().scale(friction / 20))
                setParticleSpeed(velocity.x, velocity.y, velocity.z)
            }
        }

        return newMovement
    }


    private fun setParticleAgeInRuntime() {
        runtime.environment.variable.setDirectly("particle_age", DoubleValue(age / 20.0))
        runtime.environment.variable.setDirectly("particle_lifetime", DoubleValue(lifetime / 20.0))
    }

    override fun getRenderType() = particleTextureSheet

    override fun remove() {
        super.remove()
        storm.particles.remove(this)
    }
}
