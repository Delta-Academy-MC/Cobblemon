/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.client.net.CalculateSeatPositionsHandler
import com.cobblemon.mod.common.client.net.OpenBehaviourEditorHandler
import com.cobblemon.mod.common.client.net.PlayerInteractOptionsHandler
import com.cobblemon.mod.common.client.net.SetClientPlayerDataHandler
import com.cobblemon.mod.common.client.net.animation.PlayPosableAnimationHandler
import com.cobblemon.mod.common.client.net.battle.*
import com.cobblemon.mod.common.client.net.callback.move.OpenMoveCallbackHandler
import com.cobblemon.mod.common.client.net.callback.party.OpenPartyCallbackHandler
import com.cobblemon.mod.common.client.net.callback.partymove.OpenPartyMoveCallbackHandler
import com.cobblemon.mod.common.client.net.cooking.ToggleCookingPotLidHandler
import com.cobblemon.mod.common.client.net.data.CobblemonMechanicsSyncHandler
import com.cobblemon.mod.common.client.net.data.DataRegistrySyncPacketHandler
import com.cobblemon.mod.common.client.net.data.RideSettingsSyncHandler
import com.cobblemon.mod.common.client.net.debug.OpenRidingStatsDebugGUIHandler
import com.cobblemon.mod.common.client.net.dialogue.DialogueClosedHandler
import com.cobblemon.mod.common.client.net.dialogue.DialogueOpenedHandler
import com.cobblemon.mod.common.client.net.effect.PokeSnackBlockParticlesHandler
import com.cobblemon.mod.common.client.net.effect.RunPosableMoLangHandler
import com.cobblemon.mod.common.client.net.effect.SaccharineLogBlockParticlesHandler
import com.cobblemon.mod.common.client.net.effect.SpawnSnowstormEntityParticleHandler
import com.cobblemon.mod.common.client.net.effect.SpawnSnowstormParticleHandler
import com.cobblemon.mod.common.client.net.gui.ExpGainedDataPacketHandler
import com.cobblemon.mod.common.client.net.gui.InteractPokemonUIPacketHandler
import com.cobblemon.mod.common.client.net.gui.PokedexUIPacketHandler
import com.cobblemon.mod.common.client.net.gui.SummaryUIPacketHandler
import com.cobblemon.mod.common.client.net.npc.CloseNPCEditorHandler
import com.cobblemon.mod.common.client.net.npc.OpenNPCEditorHandler
import com.cobblemon.mod.common.client.net.orientation.ClientboundUpdateOrientationHandler
import com.cobblemon.mod.common.client.net.pasture.ClosePastureHandler
import com.cobblemon.mod.common.client.net.pasture.OpenPastureHandler
import com.cobblemon.mod.common.client.net.pasture.PokemonPasturedHandler
import com.cobblemon.mod.common.client.net.pasture.PokemonUnpasturedHandler
import com.cobblemon.mod.common.client.net.pasture.UpdatePastureConflictFlagHandler
import com.cobblemon.mod.common.client.net.pokedex.ServerConfirmedRegisterHandler
import com.cobblemon.mod.common.client.net.pokemon.update.ClientboundUpdateRidingStateHandler
import com.cobblemon.mod.common.client.net.pokemon.update.PokemonUpdatePacketHandler
import com.cobblemon.mod.common.client.net.riding.ClientboundUpdateDriverInputHandler
import com.cobblemon.mod.common.client.net.settings.OpenCobblemonConfigEditorHandler
import com.cobblemon.mod.common.client.net.settings.ServerSettingsPacketHandler
import com.cobblemon.mod.common.client.net.sound.UnvalidatedPlaySoundS2CPacketHandler
import com.cobblemon.mod.common.client.net.spawn.SpawnExtraDataEntityHandler
import com.cobblemon.mod.common.client.net.starter.StarterUIPacketHandler
import com.cobblemon.mod.common.client.net.storage.RemoveClientPokemonHandler
import com.cobblemon.mod.common.client.net.storage.SwapClientPokemonHandler
import com.cobblemon.mod.common.client.net.storage.party.InitializePartyHandler
import com.cobblemon.mod.common.client.net.storage.party.MoveClientPartyPokemonHandler
import com.cobblemon.mod.common.client.net.storage.party.SetPartyPokemonHandler
import com.cobblemon.mod.common.client.net.storage.party.SetPartyReferenceHandler
import com.cobblemon.mod.common.client.net.storage.pc.*
import com.cobblemon.mod.common.client.net.toast.ToastPacketHandler
import com.cobblemon.mod.common.client.net.trade.TradeAcceptanceChangedHandler
import com.cobblemon.mod.common.client.net.trade.TradeCancelledHandler
import com.cobblemon.mod.common.client.net.trade.TradeCompletedHandler
import com.cobblemon.mod.common.client.net.trade.TradeOfferExpiredHandler
import com.cobblemon.mod.common.client.net.trade.TradeOfferNotificationHandler
import com.cobblemon.mod.common.client.net.trade.TradeProcessStartedHandler
import com.cobblemon.mod.common.client.net.trade.TradeStartedHandler
import com.cobblemon.mod.common.client.net.trade.TradeUpdatedHandler
import com.cobblemon.mod.common.net.PacketRegisterInfo
import com.cobblemon.mod.common.net.messages.client.CalculateSeatPositionsPacket
import com.cobblemon.mod.common.net.messages.client.OpenBehaviourEditorPacket
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.battle.*
import com.cobblemon.mod.common.net.messages.client.callback.OpenMoveCallbackPacket
import com.cobblemon.mod.common.net.messages.client.callback.OpenPartyCallbackPacket
import com.cobblemon.mod.common.net.messages.client.callback.OpenPartyMoveCallbackPacket
import com.cobblemon.mod.common.net.messages.client.cooking.SeasoningRegistrySyncPacket
import com.cobblemon.mod.common.net.messages.client.cooking.ToggleCookingPotLidPacket
import com.cobblemon.mod.common.net.messages.client.data.*
import com.cobblemon.mod.common.net.messages.client.debug.RequestOpenRidingStatsDebugGUIPacket
import com.cobblemon.mod.common.net.messages.client.dialogue.DialogueClosedPacket
import com.cobblemon.mod.common.net.messages.client.dialogue.DialogueOpenedPacket
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket
import com.cobblemon.mod.common.net.messages.client.effect.RunPosableMoLangPacket
import com.cobblemon.mod.common.net.messages.client.effect.SaccharineLogBlockParticlesPacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket
import com.cobblemon.mod.common.net.messages.client.npc.CloseNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.npc.OpenNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.orientation.ClientboundUpdateDriverInputPacket
import com.cobblemon.mod.common.net.messages.client.orientation.ClientboundUpdateOrientationPacket
import com.cobblemon.mod.common.net.messages.client.pasture.ClosePasturePacket
import com.cobblemon.mod.common.net.messages.client.pasture.OpenPasturePacket
import com.cobblemon.mod.common.net.messages.client.pasture.PokemonPasturedPacket
import com.cobblemon.mod.common.net.messages.client.pasture.PokemonUnpasturedPacket
import com.cobblemon.mod.common.net.messages.client.pasture.UpdatePastureConflictFlagPacket
import com.cobblemon.mod.common.net.messages.client.pokedex.ServerConfirmedRegisterPacket
import com.cobblemon.mod.common.net.messages.client.pokemon.update.*
import com.cobblemon.mod.common.net.messages.client.pokemon.update.evolution.AddEvolutionPacket
import com.cobblemon.mod.common.net.messages.client.pokemon.update.evolution.ClearEvolutionsPacket
import com.cobblemon.mod.common.net.messages.client.pokemon.update.evolution.RemoveEvolutionPacket
import com.cobblemon.mod.common.net.messages.client.settings.OpenCobblemonConfigScreenPacket
import com.cobblemon.mod.common.net.messages.client.settings.ServerSettingsPacket
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnGenericBedrockPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnNPCPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokeballPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokemonPacket
import com.cobblemon.mod.common.net.messages.client.starter.OpenStarterUIPacket
import com.cobblemon.mod.common.net.messages.client.storage.RemoveClientPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.SwapClientPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.party.InitializePartyPacket
import com.cobblemon.mod.common.net.messages.client.storage.party.MoveClientPartyPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.ClosePCPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.InitializePCPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.MoveClientPCPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.RenamePCBoxPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.SetPCBoxPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.SetPCPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.wallpaper.ChangePCBoxWallpaperPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.wallpaper.RequestPCBoxWallpapersPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.wallpaper.SetPCBoxWallpapersPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.wallpaper.UnlockPCBoxWallpaperPacket
import com.cobblemon.mod.common.net.messages.client.toast.ToastPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeAcceptanceChangedPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeCancelledPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeCompletedPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeOfferExpiredPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeOfferNotificationPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeProcessStartedPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeStartedPacket
import com.cobblemon.mod.common.net.messages.client.trade.TradeUpdatedPacket
import com.cobblemon.mod.common.net.messages.client.ui.ExpGainedDataPacket
import com.cobblemon.mod.common.net.messages.client.ui.InteractPokemonUIPacket
import com.cobblemon.mod.common.net.messages.client.ui.PokedexUIPacket
import com.cobblemon.mod.common.net.messages.client.ui.SummaryUIPacket
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket
import com.cobblemon.mod.common.net.messages.server.BattleChallengeResponsePacket
import com.cobblemon.mod.common.net.messages.server.BenchMovePacket
import com.cobblemon.mod.common.net.messages.server.RequestMoveSwapPacket
import com.cobblemon.mod.common.net.messages.server.RequestPlayerInteractionsPacket
import com.cobblemon.mod.common.net.messages.server.SelectStarterPacket
import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket
import com.cobblemon.mod.common.net.messages.server.battle.BattleSelectActionsPacket
import com.cobblemon.mod.common.net.messages.server.battle.BattleTeamLeavePacket
import com.cobblemon.mod.common.net.messages.server.battle.BattleTeamRequestPacket
import com.cobblemon.mod.common.net.messages.server.battle.BattleTeamResponsePacket
import com.cobblemon.mod.common.net.messages.server.battle.RemoveSpectatorPacket
import com.cobblemon.mod.common.net.messages.server.battle.SpectateBattlePacket
import com.cobblemon.mod.common.net.messages.server.battle.SpectateNearestBattlePacket
import com.cobblemon.mod.common.net.messages.server.battle.TeamPreviewSelectPokemonPacket
import com.cobblemon.mod.common.net.messages.server.behaviour.DamageOnCollisionPacket
import com.cobblemon.mod.common.net.messages.server.behaviour.SetEntityBehaviourPacket
import com.cobblemon.mod.common.net.messages.server.block.AdjustBlockEntityViewerCountPacket
import com.cobblemon.mod.common.net.messages.server.callback.move.MoveSelectCancelledPacket
import com.cobblemon.mod.common.net.messages.server.callback.move.MoveSelectedPacket
import com.cobblemon.mod.common.net.messages.server.callback.party.PartyPokemonSelectedPacket
import com.cobblemon.mod.common.net.messages.server.callback.party.PartySelectCancelledPacket
import com.cobblemon.mod.common.net.messages.server.callback.partymove.PartyMoveSelectCancelledPacket
import com.cobblemon.mod.common.net.messages.server.callback.partymove.PartyPokemonMoveSelectedPacket
import com.cobblemon.mod.common.net.messages.server.debug.OpenRidingStatsDebugGUIPacket
import com.cobblemon.mod.common.net.messages.server.debug.ServerboundUpdateRidingSettingsPacket
import com.cobblemon.mod.common.net.messages.server.debug.ServerboundUpdateRidingStatRangePacket
import com.cobblemon.mod.common.net.messages.server.debug.ServerboundUpdateRidingStatsPacket
import com.cobblemon.mod.common.net.messages.server.dialogue.EscapeDialoguePacket
import com.cobblemon.mod.common.net.messages.server.dialogue.InputToDialoguePacket
import com.cobblemon.mod.common.net.messages.server.megaevolution.C2SMegaEvolvePacket
import com.cobblemon.mod.common.net.messages.server.megaevolution.C2SRemoveMegaEvolutionPacket
import com.cobblemon.mod.common.net.messages.server.npc.SaveNPCPacket
import com.cobblemon.mod.common.net.messages.server.orientation.ServerboundUpdateOrientationPacket
import com.cobblemon.mod.common.net.messages.server.pasture.PasturePokemonPacket
import com.cobblemon.mod.common.net.messages.server.pasture.SetPastureConflictPacket
import com.cobblemon.mod.common.net.messages.server.pasture.UnpastureAllPokemonPacket
import com.cobblemon.mod.common.net.messages.server.pasture.UnpasturePokemonPacket
import com.cobblemon.mod.common.net.messages.server.pokebag.C2SOpenPokeBagPacket
import com.cobblemon.mod.common.net.messages.server.pokedex.scanner.FinishScanningPacket
import com.cobblemon.mod.common.net.messages.server.pokedex.scanner.StartScanningPacket
import com.cobblemon.mod.common.net.messages.server.pokemon.interact.InteractPokemonPacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.ServerboundUpdateRidingStatePacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.SetActiveMarkPacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.SetItemHiddenPacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.SetMarkingsPacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.SetNicknamePacket
import com.cobblemon.mod.common.net.messages.server.pokemon.update.evolution.AcceptEvolutionPacket
import com.cobblemon.mod.common.net.messages.server.riding.DismountPokemonPacket
import com.cobblemon.mod.common.net.messages.server.riding.ServerboundUpdateDriverInputPacket
import com.cobblemon.mod.common.net.messages.server.riding.ServerboundUpdateRiderRotationPacket
import com.cobblemon.mod.common.net.messages.server.starter.RequestStarterScreenPacket
import com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket
import com.cobblemon.mod.common.net.messages.server.storage.party.MovePartyPokemonPacket
import com.cobblemon.mod.common.net.messages.server.storage.party.ReleasePartyPokemonPacket
import com.cobblemon.mod.common.net.messages.server.storage.party.SwapPartyPokemonPacket
import com.cobblemon.mod.common.net.messages.server.storage.pc.*
import com.cobblemon.mod.common.net.messages.server.trade.AcceptTradeRequestPacket
import com.cobblemon.mod.common.net.messages.server.trade.CancelTradePacket
import com.cobblemon.mod.common.net.messages.server.trade.ChangeTradeAcceptancePacket
import com.cobblemon.mod.common.net.messages.server.trade.OfferTradePacket
import com.cobblemon.mod.common.net.messages.server.trade.PerformTradePacket
import com.cobblemon.mod.common.net.messages.server.trade.UpdateTradeOfferPacket
import com.cobblemon.mod.common.net.serverhandling.ChallengeHandler
import com.cobblemon.mod.common.net.serverhandling.ChallengeResponseHandler
import com.cobblemon.mod.common.net.serverhandling.RequestInteractionsHandler
import com.cobblemon.mod.common.net.serverhandling.battle.BattleSelectActionsHandler
import com.cobblemon.mod.common.net.serverhandling.battle.RemoveSpectatorHandler
import com.cobblemon.mod.common.net.serverhandling.battle.SpectateBattleHandler
import com.cobblemon.mod.common.net.serverhandling.battle.SpectateNearestBattleHandler
import com.cobblemon.mod.common.net.serverhandling.battle.TeamLeaveHandler
import com.cobblemon.mod.common.net.serverhandling.battle.TeamPreviewSelectPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.battle.TeamRequestHandler
import com.cobblemon.mod.common.net.serverhandling.battle.TeamRequestResponseHandler
import com.cobblemon.mod.common.net.serverhandling.behaviour.DamageOnCollisionPacketHandler
import com.cobblemon.mod.common.net.serverhandling.behaviour.SetEntityBehaviourHandler
import com.cobblemon.mod.common.net.serverhandling.block.AdjustBlockEntityViewerCountHandler
import com.cobblemon.mod.common.net.serverhandling.callback.move.MoveSelectCancelledHandler
import com.cobblemon.mod.common.net.serverhandling.callback.move.MoveSelectedHandler
import com.cobblemon.mod.common.net.serverhandling.callback.party.PartyPokemonSelectedHandler
import com.cobblemon.mod.common.net.serverhandling.callback.party.PartySelectCancelledHandler
import com.cobblemon.mod.common.net.serverhandling.callback.partymove.PartyMoveSelectCancelledHandler
import com.cobblemon.mod.common.net.serverhandling.callback.partymove.PartyPokemonMoveSelectedHandler
import com.cobblemon.mod.common.net.serverhandling.debug.RequestOpenRidingStatsDebugGUIHandler
import com.cobblemon.mod.common.net.serverhandling.debug.ServerboundUpdateRidingSettingsHandler
import com.cobblemon.mod.common.net.serverhandling.debug.ServerboundUpdateRidingStatRangeHandler
import com.cobblemon.mod.common.net.serverhandling.debug.ServerboundUpdateRidingStatsHandler
import com.cobblemon.mod.common.net.serverhandling.dialogue.EscapeDialogueHandler
import com.cobblemon.mod.common.net.serverhandling.dialogue.InputToDialogueHandler
import com.cobblemon.mod.common.net.serverhandling.evolution.AcceptEvolutionHandler
import com.cobblemon.mod.common.net.serverhandling.megaevolution.C2SMegaEvolveHandler
import com.cobblemon.mod.common.net.serverhandling.megaevolution.C2SRemoveMegaEvolutionHandler
import com.cobblemon.mod.common.net.serverhandling.npc.SaveNPCHandler
import com.cobblemon.mod.common.net.serverhandling.orientation.ServerboundUpdateOrientationHandler
import com.cobblemon.mod.common.net.serverhandling.pasture.PasturePokemonHandler
import com.cobblemon.mod.common.net.serverhandling.pasture.SetPastureConflictHandler
import com.cobblemon.mod.common.net.serverhandling.pasture.UnpastureAllPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.pasture.UnpasturePokemonHandler
import com.cobblemon.mod.common.net.serverhandling.pokebag.C2SOpenPokeBagHandler
import com.cobblemon.mod.common.net.serverhandling.pokedex.scanner.FinishScanningHandler
import com.cobblemon.mod.common.net.serverhandling.pokedex.scanner.StartScanningHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.interact.InteractPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.ServerboundUpdateRidingStateHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.SetActiveMarkHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.SetItemHiddenHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.SetMarkingsHandler
import com.cobblemon.mod.common.net.serverhandling.pokemon.update.SetNicknameHandler
import com.cobblemon.mod.common.net.serverhandling.riding.DismountPokemonPacketHandler
import com.cobblemon.mod.common.net.serverhandling.riding.DriverInputPacketHandler
import com.cobblemon.mod.common.net.serverhandling.riding.ServerboundUpdateRiderRotationHandler
import com.cobblemon.mod.common.net.serverhandling.starter.RequestStarterScreenHandler
import com.cobblemon.mod.common.net.serverhandling.starter.SelectStarterPacketHandler
import com.cobblemon.mod.common.net.serverhandling.storage.BenchMoveHandler
import com.cobblemon.mod.common.net.serverhandling.storage.RequestMoveSwapHandler
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.storage.SwapPCPartyPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.storage.party.MovePartyPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.storage.party.ReleasePCPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.storage.party.SwapPartyPokemonHandler
import com.cobblemon.mod.common.net.serverhandling.storage.pc.*
import com.cobblemon.mod.common.net.serverhandling.trade.AcceptTradeRequestHandler
import com.cobblemon.mod.common.net.serverhandling.trade.CancelTradeHandler
import com.cobblemon.mod.common.net.serverhandling.trade.ChangeTradeAcceptanceHandler
import com.cobblemon.mod.common.net.serverhandling.trade.OfferTradeHandler
import com.cobblemon.mod.common.net.serverhandling.trade.PerformTradeHandler
import com.cobblemon.mod.common.net.serverhandling.trade.UpdateTradeOfferHandler
import com.cobblemon.mod.common.util.server
import net.minecraft.server.level.ServerPlayer

/**
 * Registers Cobblemon network packets.
 *
 * This class also contains short functions for dispatching our packets to a player, all players, or to the entire server.
 *
 * @author Hiroku, Licious
 * @since November 27th, 2021
 */
object CobblemonNetwork {

    fun ServerPlayer.sendPacket(packet: NetworkPacket<*>) {
        sendPacketToPlayer(this, packet)
    }
    @JvmStatic
    fun sendToServer(packet: NetworkPacket<*>) {
        Cobblemon.implementation.networkManager.sendToServer(packet)
    }
    @JvmStatic
    fun sendToAllPlayers(packet: NetworkPacket<*>) = sendPacketToPlayers(server()!!.playerList.players, packet)
    @JvmStatic
    fun sendPacketToPlayers(players: Iterable<ServerPlayer>, packet: NetworkPacket<*>) = players.forEach { sendPacketToPlayer(it, packet) }

    val s2cPayloads = generateS2CPacketInfoList()
    val c2sPayloads = generateC2SPacketInfoList()

    private fun generateS2CPacketInfoList(): List<PacketRegisterInfo<*>> {
        val list = mutableListOf<PacketRegisterInfo<*>>()

        // Pokemon Update Packets
        list.add(PacketRegisterInfo(FriendshipUpdatePacket.ID, FriendshipUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(FullnessUpdatePacket.ID, FullnessUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MoveSetUpdatePacket.ID, MoveSetUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(NatureUpdatePacket.ID, NatureUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(ShinyUpdatePacket.ID, ShinyUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(SpeciesUpdatePacket.ID, SpeciesUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(NicknameUpdatePacket.ID, NicknameUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(HealthUpdatePacket.ID, HealthUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(ExperienceUpdatePacket.ID, ExperienceUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(StatusUpdatePacket.ID, StatusUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(CaughtBallUpdatePacket.ID, CaughtBallUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(BenchedMovesUpdatePacket.ID, BenchedMovesUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(GenderUpdatePacket.ID, GenderUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(AspectsUpdatePacket.ID, AspectsUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(AbilityUpdatePacket.ID, AbilityUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(EVsUpdatePacket.ID, EVsUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(IVsUpdatePacket.ID, IVsUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(HeldItemUpdatePacket.ID, HeldItemUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(PokemonStateUpdatePacket.ID, PokemonStateUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(TetheringUpdatePacket.ID, TetheringUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(TradeableUpdatePacket.ID, TradeableUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(SpeciesFeatureUpdatePacket.ID, SpeciesFeatureUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(OriginalTrainerUpdatePacket.ID, OriginalTrainerUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(FormUpdatePacket.ID, FormUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(TeraTypeUpdatePacket.ID, TeraTypeUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(DmaxLevelUpdatePacket.ID, DmaxLevelUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(GmaxFactorUpdatePacket.ID, GmaxFactorUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(CosmeticItemUpdatePacket.ID, CosmeticItemUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(ActiveMarkUpdatePacket.ID, ActiveMarkUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarkAddUpdatePacket.ID, MarkAddUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarkRemoveUpdatePacket.ID, MarkRemoveUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarkPotentialAddUpdatePacket.ID, MarkPotentialAddUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarksUpdatePacket.ID, MarksUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarksPotentialUpdatePacket.ID, MarksPotentialUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(MarkingsUpdatePacket.ID, MarkingsUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(RideBoostsUpdatePacket.ID, RideBoostsUpdatePacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(RideStaminaUpdatePacket.ID, RideStaminaUpdatePacket::decode, PokemonUpdatePacketHandler()))

        // Evolution start
        list.add(PacketRegisterInfo(AddEvolutionPacket.ID, AddEvolutionPacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(ClearEvolutionsPacket.ID, ClearEvolutionsPacket::decode, PokemonUpdatePacketHandler()))
        list.add(PacketRegisterInfo(RemoveEvolutionPacket.ID, RemoveEvolutionPacket::decode, PokemonUpdatePacketHandler()))
        // Evolution End

        // Storage Packets
        list.add(PacketRegisterInfo(InitializePartyPacket.ID, InitializePartyPacket::decode, InitializePartyHandler))
        list.add(PacketRegisterInfo(SetPartyPokemonPacket.ID, SetPartyPokemonPacket::decode, SetPartyPokemonHandler))
        list.add(PacketRegisterInfo(MoveClientPartyPokemonPacket.ID, MoveClientPartyPokemonPacket::decode, MoveClientPartyPokemonHandler))
        list.add(PacketRegisterInfo(SetPartyReferencePacket.ID, SetPartyReferencePacket::decode, SetPartyReferenceHandler))
        list.add(PacketRegisterInfo(InitializePCPacket.ID, InitializePCPacket::decode, InitializePCHandler))
        list.add(PacketRegisterInfo(MoveClientPCPokemonPacket.ID, MoveClientPCPokemonPacket::decode, MoveClientPCPokemonHandler))
        list.add(PacketRegisterInfo(SetPCBoxPacket.ID, SetPCBoxPacket::decode, SetPCBoxHandler))
        list.add(PacketRegisterInfo(SetPCPokemonPacket.ID, SetPCPokemonPacket::decode, SetPCPokemonHandler))
        list.add(PacketRegisterInfo(OpenPCPacket.ID, OpenPCPacket::decode, OpenPCHandler))
        list.add(PacketRegisterInfo(ClosePCPacket.ID, ClosePCPacket::decode, ClosePCHandler))
        list.add(PacketRegisterInfo(SwapClientPokemonPacket.ID, SwapClientPokemonPacket::decode, SwapClientPokemonHandler))
        list.add(PacketRegisterInfo(RemoveClientPokemonPacket.ID, RemoveClientPokemonPacket::decode, RemoveClientPokemonHandler))

        list.add(PacketRegisterInfo(RenamePCBoxPacket.ID, RenamePCBoxPacket::decode, RenamePCBoxHandler))
        list.add(PacketRegisterInfo(RequestPCBoxWallpapersPacket.ID, RequestPCBoxWallpapersPacket::decode, RequestPCBoxWallpapersHandler))
        list.add(PacketRegisterInfo(SetPCBoxWallpapersPacket.ID, SetPCBoxWallpapersPacket::decode, SetPCBoxWallpapersHandler))
        list.add(PacketRegisterInfo(ChangePCBoxWallpaperPacket.ID, ChangePCBoxWallpaperPacket::decode, ChangePCBoxWallpaperHandler))
        list.add(PacketRegisterInfo(UnlockPCBoxWallpaperPacket.ID, UnlockPCBoxWallpaperPacket::decode, UnlockPCBoxWallpaperHandler))

        // UI Packets
        list.add(PacketRegisterInfo(SummaryUIPacket.ID, SummaryUIPacket::decode, SummaryUIPacketHandler))
        list.add(PacketRegisterInfo(InteractPokemonUIPacket.ID, InteractPokemonUIPacket::decode, InteractPokemonUIPacketHandler))
        list.add(PacketRegisterInfo(PlayerInteractOptionsPacket.ID, PlayerInteractOptionsPacket::decode, PlayerInteractOptionsHandler))
        list.add(PacketRegisterInfo(PokedexUIPacket.ID, PokedexUIPacket::decode, PokedexUIPacketHandler))
        list.add(PacketRegisterInfo(ExpGainedDataPacket.ID, ExpGainedDataPacket::decode, ExpGainedDataPacketHandler))

        // Starter packets
        list.add(PacketRegisterInfo(OpenStarterUIPacket.ID, OpenStarterUIPacket::decode, StarterUIPacketHandler))
        list.add(PacketRegisterInfo(SetClientPlayerDataPacket.ID, SetClientPlayerDataPacket::decode, SetClientPlayerDataHandler))

        // Battle packets
        list.add(PacketRegisterInfo(BattleEndPacket.ID, BattleEndPacket::decode, BattleEndHandler))
        list.add(PacketRegisterInfo(BattleInitializePacket.ID, BattleInitializePacket::decode, BattleInitializeHandler))
        list.add(PacketRegisterInfo(BattleQueueRequestPacket.ID, BattleQueueRequestPacket::decode, BattleQueueRequestHandler))
        list.add(PacketRegisterInfo(BattleFaintPacket.ID, BattleFaintPacket::decode, BattleFaintHandler))
        list.add(PacketRegisterInfo(BattleMakeChoicePacket.ID, BattleMakeChoicePacket::decode, BattleMakeChoiceHandler))
        list.add(PacketRegisterInfo(BattleHealthChangePacket.ID, BattleHealthChangePacket::decode, BattleHealthChangeHandler))
        list.add(PacketRegisterInfo(BattleSetTeamPokemonPacket.ID, BattleSetTeamPokemonPacket::decode, BattleSetTeamPokemonHandler))
        list.add(PacketRegisterInfo(BattleSwitchPokemonPacket.ID, BattleSwitchPokemonPacket::decode, BattleSwitchPokemonHandler))
        list.add(PacketRegisterInfo(BattleSwapPokemonPacket.ID, BattleSwapPokemonPacket::decode, BattleSwapPokemonHandler))
        list.add(PacketRegisterInfo(BattleMessagePacket.ID, BattleMessagePacket::decode, BattleMessageHandler))
        list.add(PacketRegisterInfo(BattleCaptureStartPacket.ID, BattleCaptureStartPacket::decode, BattleCaptureStartHandler))
        list.add(PacketRegisterInfo(BattleCaptureEndPacket.ID, BattleCaptureEndPacket::decode, BattleCaptureEndHandler))
        list.add(PacketRegisterInfo(BattleCaptureShakePacket.ID, BattleCaptureShakePacket::decode, BattleCaptureShakeHandler))
        list.add(PacketRegisterInfo(BattleApplyPassResponsePacket.ID, BattleApplyPassResponsePacket::decode, BattleApplyPassResponseHandler))
        list.add(PacketRegisterInfo(BattleChallengeNotificationPacket.ID, BattleChallengeNotificationPacket::decode, BattleChallengeNotificationHandler))
        list.add(PacketRegisterInfo(BattleUpdateTeamPokemonPacket.ID, BattleUpdateTeamPokemonPacket::decode, BattleUpdateTeamPokemonHandler))
        list.add(PacketRegisterInfo(BattlePersistentStatusPacket.ID, BattlePersistentStatusPacket::decode, BattlePersistentStatusHandler))
        list.add(PacketRegisterInfo(BattleMadeInvalidChoicePacket.ID, BattleMadeInvalidChoicePacket::decode, BattleMadeInvalidChoiceHandler))
        list.add(PacketRegisterInfo(BattleMusicPacket.ID, BattleMusicPacket::decode, BattleMusicHandler))
        list.add(PacketRegisterInfo(BattleChallengeExpiredPacket.ID, BattleChallengeExpiredPacket::decode, BattleChallengeExpiredHandler))
        list.add(PacketRegisterInfo(BattleReplacePokemonPacket.ID, BattleReplacePokemonPacket::decode, BattleReplacePokemonHandler))
        list.add(PacketRegisterInfo(BattleTransformPokemonPacket.ID, BattleTransformPokemonPacket::decode, BattleTransformPokemonHandler))
        list.add(PacketRegisterInfo(BattleActorInformationPacket.ID, BattleActorInformationPacket::decode, BattleActorInformationHandler))
        list.add(PacketRegisterInfo(BattleActorTeamPacket.ID, BattleActorTeamPacket::decode, BattleActorTeamHandler))
        list.add(PacketRegisterInfo(BattleInformationPacket.ID, BattleInformationPacket::decode, BattleInformationHandler))
        list.add(PacketRegisterInfo(BattleTimerPacket.ID, BattleTimerPacket::decode, BattleTimerHandler))
        list.add(PacketRegisterInfo(TeamPreviewPacket.ID, TeamPreviewPacket::decode, TeamPreviewHandler))
        list.add(PacketRegisterInfo(CloseTeamPreviewPacket.ID, CloseTeamPreviewPacket::decode, CloseTeamPreviewHandler))

        // MultiBattleTeam Packets
        list.add(PacketRegisterInfo(TeamRequestNotificationPacket.ID, TeamRequestNotificationPacket::decode, TeamRequestNotificationHandler))
        list.add(PacketRegisterInfo(TeamRequestExpiredPacket.ID, TeamRequestExpiredPacket::decode, TeamRequestExpiredHandler))
        list.add(PacketRegisterInfo(TeamMemberAddNotificationPacket.ID, TeamMemberAddNotificationPacket::decode, TeamMemberAddNotificationHandler))
        list.add(PacketRegisterInfo(TeamMemberRemoveNotificationPacket.ID, TeamMemberRemoveNotificationPacket::decode, TeamMemberRemoveNotificationHandler))
        list.add(PacketRegisterInfo(TeamJoinNotificationPacket.ID, TeamJoinNotificationPacket::decode, TeamJoinNotificationHandler))

        // Settings packets
        list.add(PacketRegisterInfo(ServerSettingsPacket.ID, ServerSettingsPacket::decode, ServerSettingsPacketHandler))
        list.add(PacketRegisterInfo(OpenCobblemonConfigScreenPacket.ID, OpenCobblemonConfigScreenPacket::decode, OpenCobblemonConfigEditorHandler))

        // Data registries
        list.add(PacketRegisterInfo(AbilityRegistrySyncPacket.ID, AbilityRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(CobblemonMechanicsSyncPacket.ID, CobblemonMechanicsSyncPacket::decode, CobblemonMechanicsSyncHandler))
        list.add(PacketRegisterInfo(PokemonInteractionsSyncPacket.ID, PokemonInteractionsSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(MovesRegistrySyncPacket.ID, MovesRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(BerryRegistrySyncPacket.ID, BerryRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(SpeciesRegistrySyncPacket.ID, SpeciesRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(PropertiesCompletionRegistrySyncPacket.ID, PropertiesCompletionRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(StandardSpeciesFeatureSyncPacket.ID, StandardSpeciesFeatureSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(GlobalSpeciesFeatureSyncPacket.ID, GlobalSpeciesFeatureSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(SpeciesFeatureAssignmentSyncPacket.ID, SpeciesFeatureAssignmentSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(NaturalMaterialRegistrySyncPacket.ID, NaturalMaterialRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(FossilRegistrySyncPacket.ID, FossilRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(NPCRegistrySyncPacket.ID, NPCRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(PokeRodRegistrySyncPacket.ID, PokeRodRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(ScriptRegistrySyncPacket.ID, ScriptRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(PokedexDexSyncPacket.ID, PokedexDexSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(DexEntrySyncPacket.ID, DexEntrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(SpawnBaitRegistrySyncPacket.ID, SpawnBaitRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(SeasoningRegistrySyncPacket.ID, SeasoningRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(CallbackRegistrySyncPacket.ID, CallbackRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(CosmeticItemAssignmentSyncPacket.ID, CosmeticItemAssignmentSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(BehaviourSyncPacket.ID, BehaviourSyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(MarkRegistrySyncPacket.ID, MarkRegistrySyncPacket::decode, DataRegistrySyncPacketHandler()))
        list.add(PacketRegisterInfo(RideSettingsSyncPacket.ID, RideSettingsSyncPacket::decode, RideSettingsSyncHandler))

        // Effects
        list.add(PacketRegisterInfo(SpawnSnowstormParticlePacket.ID, SpawnSnowstormParticlePacket::decode, SpawnSnowstormParticleHandler))
        list.add(PacketRegisterInfo(SpawnSnowstormEntityParticlePacket.ID, SpawnSnowstormEntityParticlePacket::decode, SpawnSnowstormEntityParticleHandler))
        list.add(PacketRegisterInfo(RunPosableMoLangPacket.ID, RunPosableMoLangPacket::decode, RunPosableMoLangHandler))
        list.add(PacketRegisterInfo(SaccharineLogBlockParticlesPacket.ID, SaccharineLogBlockParticlesPacket::decode, SaccharineLogBlockParticlesHandler))
        list.add(PacketRegisterInfo(PokeSnackBlockParticlesPacket.ID, PokeSnackBlockParticlesPacket::decode, PokeSnackBlockParticlesHandler))

        // Hax
        list.add(PacketRegisterInfo(UnvalidatedPlaySoundS2CPacket.ID, UnvalidatedPlaySoundS2CPacket::decode, UnvalidatedPlaySoundS2CPacketHandler))
        list.add(PacketRegisterInfo(SpawnPokemonPacket.ID, SpawnPokemonPacket::decode, SpawnExtraDataEntityHandler()))
        list.add(PacketRegisterInfo(SpawnPokeballPacket.ID, SpawnPokeballPacket::decode, SpawnExtraDataEntityHandler()))
        list.add(PacketRegisterInfo(SpawnGenericBedrockPacket.ID, SpawnGenericBedrockPacket::decode, SpawnExtraDataEntityHandler()))
        list.add(PacketRegisterInfo(SpawnNPCPacket.ID, SpawnNPCPacket::decode, SpawnExtraDataEntityHandler()))
        list.add(PacketRegisterInfo(ToastPacket.ID, ToastPacket::decode, ToastPacketHandler))

        // Trade packets
        list.add(PacketRegisterInfo(TradeAcceptanceChangedPacket.ID, TradeAcceptanceChangedPacket::decode, TradeAcceptanceChangedHandler))
        list.add(PacketRegisterInfo(TradeProcessStartedPacket.ID, TradeProcessStartedPacket::decode, TradeProcessStartedHandler))
        list.add(PacketRegisterInfo(TradeCancelledPacket.ID, TradeCancelledPacket::decode, TradeCancelledHandler))
        list.add(PacketRegisterInfo(TradeCompletedPacket.ID, TradeCompletedPacket::decode, TradeCompletedHandler))
        list.add(PacketRegisterInfo(TradeUpdatedPacket.ID, TradeUpdatedPacket::decode, TradeUpdatedHandler))
        list.add(PacketRegisterInfo(TradeOfferNotificationPacket.ID, TradeOfferNotificationPacket::decode, TradeOfferNotificationHandler))
        list.add(PacketRegisterInfo(TradeOfferExpiredPacket.ID, TradeOfferExpiredPacket::decode, TradeOfferExpiredHandler))
        list.add(PacketRegisterInfo(TradeStartedPacket.ID, TradeStartedPacket::decode, TradeStartedHandler))

        // Pasture
        list.add(PacketRegisterInfo(OpenPasturePacket.ID, OpenPasturePacket::decode, OpenPastureHandler))
        list.add(PacketRegisterInfo(ClosePasturePacket.ID, ClosePasturePacket::decode, ClosePastureHandler))
        list.add(PacketRegisterInfo(PokemonPasturedPacket.ID, PokemonPasturedPacket::decode, PokemonPasturedHandler))
        list.add(PacketRegisterInfo(PokemonUnpasturedPacket.ID, PokemonUnpasturedPacket::decode, PokemonUnpasturedHandler))
        list.add(PacketRegisterInfo(UpdatePastureConflictFlagPacket.ID, UpdatePastureConflictFlagPacket::decode, UpdatePastureConflictFlagHandler))

        // Orientation
        list.add(PacketRegisterInfo(ClientboundUpdateOrientationPacket.ID, ClientboundUpdateOrientationPacket::decode, ClientboundUpdateOrientationHandler))

        // Behaviours
        list.add(PacketRegisterInfo(PlayPosableAnimationPacket.ID, PlayPosableAnimationPacket::decode, PlayPosableAnimationHandler))

        // Move select packets
        list.add(PacketRegisterInfo(OpenMoveCallbackPacket.ID, OpenMoveCallbackPacket::decode, OpenMoveCallbackHandler))

        // Party select packets
        list.add(PacketRegisterInfo(OpenPartyCallbackPacket.ID, OpenPartyCallbackPacket::decode, OpenPartyCallbackHandler))

        // Party move select packets
        list.add(PacketRegisterInfo(OpenPartyMoveCallbackPacket.ID, OpenPartyMoveCallbackPacket::decode, OpenPartyMoveCallbackHandler))

        // Dialogue packets
        list.add(PacketRegisterInfo(DialogueClosedPacket.ID, DialogueClosedPacket::decode, DialogueClosedHandler))
        list.add(PacketRegisterInfo(DialogueOpenedPacket.ID, DialogueOpenedPacket::decode, DialogueOpenedHandler))

        // NPCs
        list.add(PacketRegisterInfo(CloseNPCEditorPacket.ID, CloseNPCEditorPacket::decode, CloseNPCEditorHandler))
        list.add(PacketRegisterInfo(OpenNPCEditorPacket.ID, OpenNPCEditorPacket::decode, OpenNPCEditorHandler))

        // Behaviours
        list.add(PacketRegisterInfo(OpenBehaviourEditorPacket.ID, OpenBehaviourEditorPacket::decode, OpenBehaviourEditorHandler))

        // Pokédex scanning
        list.add(PacketRegisterInfo(ServerConfirmedRegisterPacket.ID, ServerConfirmedRegisterPacket::decode, ServerConfirmedRegisterHandler))

        // Debug / cheats
        list.add(PacketRegisterInfo(CalculateSeatPositionsPacket.ID, CalculateSeatPositionsPacket::decode, CalculateSeatPositionsHandler))

        // Riding
        list.add(PacketRegisterInfo(ClientboundUpdateRidingStatePacket.ID, ClientboundUpdateRidingStatePacket::decode, ClientboundUpdateRidingStateHandler))
        list.add(PacketRegisterInfo(ClientboundUpdateDriverInputPacket.ID, ClientboundUpdateDriverInputPacket::decode, ClientboundUpdateDriverInputHandler))

        // Debug
        list.add(PacketRegisterInfo(OpenRidingStatsDebugGUIPacket.ID, OpenRidingStatsDebugGUIPacket::decode, OpenRidingStatsDebugGUIHandler))

        return list
    }

    private fun generateC2SPacketInfoList(): List<PacketRegisterInfo<*>> {
        val list = mutableListOf<PacketRegisterInfo<*>>()
        // Pokemon Update Packets
        list.add(PacketRegisterInfo(SetNicknamePacket.ID, SetNicknamePacket::decode, SetNicknameHandler))
        list.add(PacketRegisterInfo(SetItemHiddenPacket.ID, SetItemHiddenPacket::decode, SetItemHiddenHandler))
        list.add(PacketRegisterInfo(SetActiveMarkPacket.ID, SetActiveMarkPacket::decode, SetActiveMarkHandler))
        list.add(PacketRegisterInfo(SetMarkingsPacket.ID, SetMarkingsPacket::decode, SetMarkingsHandler))

        // Evolution Packets
        list.add(PacketRegisterInfo(AcceptEvolutionPacket.ID, AcceptEvolutionPacket::decode, AcceptEvolutionHandler))

        // Interaction Packets
        list.add(PacketRegisterInfo(InteractPokemonPacket.ID, InteractPokemonPacket::decode, InteractPokemonHandler))
        list.add(PacketRegisterInfo(RequestPlayerInteractionsPacket.ID, RequestPlayerInteractionsPacket::decode, RequestInteractionsHandler))

        // Storage Packets
        list.add(PacketRegisterInfo(SendOutPokemonPacket.ID, SendOutPokemonPacket::decode, SendOutPokemonHandler))
        list.add(PacketRegisterInfo(RequestMoveSwapPacket.ID, RequestMoveSwapPacket::decode, RequestMoveSwapHandler))
        list.add(PacketRegisterInfo(BenchMovePacket.ID, BenchMovePacket::decode, BenchMoveHandler))
        list.add(PacketRegisterInfo(BattleChallengePacket.ID, BattleChallengePacket::decode, ChallengeHandler))
        list.add(PacketRegisterInfo(BattleChallengeResponsePacket.ID, BattleChallengeResponsePacket::decode, ChallengeResponseHandler))
        list.add(PacketRegisterInfo(BattleTeamRequestPacket.ID, BattleTeamRequestPacket::decode, TeamRequestHandler))
        list.add(PacketRegisterInfo(BattleTeamResponsePacket.ID, BattleTeamResponsePacket::decode, TeamRequestResponseHandler))
        list.add(PacketRegisterInfo(BattleTeamLeavePacket.ID, BattleTeamLeavePacket::decode, TeamLeaveHandler))

        list.add(PacketRegisterInfo(MovePCPokemonToPartyPacket.ID, MovePCPokemonToPartyPacket::decode, MovePCPokemonToPartyHandler))
        list.add(PacketRegisterInfo(MovePartyPokemonToPCPacket.ID, MovePartyPokemonToPCPacket::decode, MovePartyPokemonToPCHandler))
        list.add(PacketRegisterInfo(ReleasePartyPokemonPacket.ID, ReleasePartyPokemonPacket::decode, ReleasePartyPokemonHandler))
        list.add(PacketRegisterInfo(ReleasePCPokemonPacket.ID, ReleasePCPokemonPacket::decode, ReleasePCPokemonHandler))
        list.add(PacketRegisterInfo(UnlinkPlayerFromPCPacket.ID, UnlinkPlayerFromPCPacket::decode, UnlinkPlayerFromPCHandler))

        list.add(PacketRegisterInfo(RequestRenamePCBoxPacket.Companion.ID, RequestRenamePCBoxPacket.Companion::decode, RequestRenamePCBoxHandler))
        list.add(PacketRegisterInfo(PCBoxWallpapersPacket.ID, PCBoxWallpapersPacket::decode, PCBoxWallpapersHandler))
        list.add(PacketRegisterInfo(RequestChangePCBoxWallpaperPacket.ID, RequestChangePCBoxWallpaperPacket::decode, RequestChangePCBoxWallpaperHandler))
        list.add(PacketRegisterInfo(MarkPCBoxWallpapersSeenPacket.ID, MarkPCBoxWallpapersSeenPacket::decode, MarkPCBoxWallpapersSeenHandler))

        // Starter packets
        list.add(PacketRegisterInfo(SelectStarterPacket.ID, SelectStarterPacket::decode, SelectStarterPacketHandler))
        list.add(PacketRegisterInfo(RequestStarterScreenPacket.ID, RequestStarterScreenPacket::decode, RequestStarterScreenHandler))

        list.add(PacketRegisterInfo(SwapPCPokemonPacket.ID, SwapPCPokemonPacket::decode, SwapPCPokemonHandler))
        list.add(PacketRegisterInfo(SwapPartyPokemonPacket.ID, SwapPartyPokemonPacket::decode, SwapPartyPokemonHandler))

        list.add(PacketRegisterInfo(MovePCPokemonPacket.ID, MovePCPokemonPacket::decode, MovePCPokemonHandler))
        list.add(PacketRegisterInfo(MovePartyPokemonPacket.ID, MovePartyPokemonPacket::decode, MovePartyPokemonHandler))

        list.add(PacketRegisterInfo(SwapPCPartyPokemonPacket.ID, SwapPCPartyPokemonPacket::decode, SwapPCPartyPokemonHandler))

        list.add(PacketRegisterInfo(SortPCBoxPacket.ID, SortPCBoxPacket::decode, SortPCBoxHandler))

        // Battle packets
        list.add(PacketRegisterInfo(BattleSelectActionsPacket.ID, BattleSelectActionsPacket::decode, BattleSelectActionsHandler))
        list.add(PacketRegisterInfo(SpectateBattlePacket.ID, SpectateBattlePacket::decode, SpectateBattleHandler))
        list.add(PacketRegisterInfo(RemoveSpectatorPacket.ID, RemoveSpectatorPacket::decode, RemoveSpectatorHandler))
        list.add(PacketRegisterInfo(TeamPreviewSelectPokemonPacket.ID, TeamPreviewSelectPokemonPacket::decode, TeamPreviewSelectPokemonHandler))

        // Trade
        list.add(PacketRegisterInfo(AcceptTradeRequestPacket.ID, AcceptTradeRequestPacket::decode, AcceptTradeRequestHandler))
        list.add(PacketRegisterInfo(CancelTradePacket.ID, CancelTradePacket::decode, CancelTradeHandler))
        list.add(PacketRegisterInfo(ChangeTradeAcceptancePacket.ID, ChangeTradeAcceptancePacket::decode, ChangeTradeAcceptanceHandler))
        list.add(PacketRegisterInfo(PerformTradePacket.ID, PerformTradePacket::decode, PerformTradeHandler))
        list.add(PacketRegisterInfo(OfferTradePacket.ID, OfferTradePacket::decode, OfferTradeHandler))
        list.add(PacketRegisterInfo(UpdateTradeOfferPacket.ID, UpdateTradeOfferPacket::decode, UpdateTradeOfferHandler))

        // Pokédex scanning
        list.add(PacketRegisterInfo(StartScanningPacket.ID, StartScanningPacket::decode, StartScanningHandler))
        list.add(PacketRegisterInfo(FinishScanningPacket.ID, FinishScanningPacket::decode, FinishScanningHandler))

        // Pasture
        list.add(PacketRegisterInfo(PasturePokemonPacket.ID, PasturePokemonPacket::decode, PasturePokemonHandler))
        list.add(PacketRegisterInfo(UnpasturePokemonPacket.ID, UnpasturePokemonPacket::decode, UnpasturePokemonHandler))
        list.add(PacketRegisterInfo(UnpastureAllPokemonPacket.ID, UnpastureAllPokemonPacket::decode, UnpastureAllPokemonHandler))
        list.add(PacketRegisterInfo(SetPastureConflictPacket.ID, SetPastureConflictPacket::decode, SetPastureConflictHandler))


        // Block entity
        list.add(PacketRegisterInfo(AdjustBlockEntityViewerCountPacket.ID, AdjustBlockEntityViewerCountPacket::decode, AdjustBlockEntityViewerCountHandler))

        // Move select packets
        list.add(PacketRegisterInfo(MoveSelectedPacket.ID, MoveSelectedPacket::decode, MoveSelectedHandler))
        list.add(PacketRegisterInfo(MoveSelectCancelledPacket.ID, MoveSelectCancelledPacket::decode, MoveSelectCancelledHandler))

        // Party select packets
        list.add(PacketRegisterInfo(PartyPokemonSelectedPacket.ID, PartyPokemonSelectedPacket::decode, PartyPokemonSelectedHandler))
        list.add(PacketRegisterInfo(PartySelectCancelledPacket.ID, PartySelectCancelledPacket::decode, PartySelectCancelledHandler))

        // Party move select packets
        list.add(PacketRegisterInfo(PartyPokemonMoveSelectedPacket.ID, PartyPokemonMoveSelectedPacket::decode, PartyPokemonMoveSelectedHandler))
        list.add(PacketRegisterInfo(PartyMoveSelectCancelledPacket.ID, PartyMoveSelectCancelledPacket::decode, PartyMoveSelectCancelledHandler))

        // Dialogue packets
        list.add(PacketRegisterInfo(EscapeDialoguePacket.ID, EscapeDialoguePacket::decode, EscapeDialogueHandler))
        list.add(PacketRegisterInfo(InputToDialoguePacket.ID, InputToDialoguePacket::decode, InputToDialogueHandler))

        // NPC packets
        list.add(PacketRegisterInfo(SaveNPCPacket.ID, SaveNPCPacket::decode, SaveNPCHandler))

        // Riding packet(s)
        list.add(PacketRegisterInfo(ServerboundUpdateOrientationPacket.ID, ServerboundUpdateOrientationPacket::decode, ServerboundUpdateOrientationHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateRidingStatePacket.ID, ServerboundUpdateRidingStatePacket::decode, ServerboundUpdateRidingStateHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateRidingStatsPacket.ID, ServerboundUpdateRidingStatsPacket::decode, ServerboundUpdateRidingStatsHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateRidingStatRangePacket.ID, ServerboundUpdateRidingStatRangePacket::decode, ServerboundUpdateRidingStatRangeHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateRidingSettingsPacket.ID, ServerboundUpdateRidingSettingsPacket::decode, ServerboundUpdateRidingSettingsHandler))
        list.add(PacketRegisterInfo(DismountPokemonPacket.ID, DismountPokemonPacket::decode, DismountPokemonPacketHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateDriverInputPacket.ID, ServerboundUpdateDriverInputPacket::decode, DriverInputPacketHandler))
        list.add(PacketRegisterInfo(ServerboundUpdateRiderRotationPacket.ID, ServerboundUpdateRiderRotationPacket::decode, ServerboundUpdateRiderRotationHandler))

        // Cooking
        list.add(PacketRegisterInfo(ToggleCookingPotLidPacket.ID, ToggleCookingPotLidPacket::decode, ToggleCookingPotLidHandler))

        // Behaviour Packets
        list.add(PacketRegisterInfo(SetEntityBehaviourPacket.ID, SetEntityBehaviourPacket::decode, SetEntityBehaviourHandler))
        list.add(PacketRegisterInfo(DamageOnCollisionPacket.ID, DamageOnCollisionPacket::decode, DamageOnCollisionPacketHandler))

        // Debug
        list.add(PacketRegisterInfo(RequestOpenRidingStatsDebugGUIPacket.ID, RequestOpenRidingStatsDebugGUIPacket::decode, RequestOpenRidingStatsDebugGUIHandler))

        list.add(PacketRegisterInfo(SpectateNearestBattlePacket.ID, SpectateNearestBattlePacket::decode, SpectateNearestBattleHandler))

        // Mega Evolution
        list.add(PacketRegisterInfo(C2SMegaEvolvePacket.ID, C2SMegaEvolvePacket::decode, C2SMegaEvolveHandler))
        list.add(PacketRegisterInfo(C2SRemoveMegaEvolutionPacket.ID, C2SRemoveMegaEvolutionPacket::decode, C2SRemoveMegaEvolutionHandler))

        // Poke Bags
        list.add(PacketRegisterInfo(C2SOpenPokeBagPacket.ID, C2SOpenPokeBagPacket::decode, C2SOpenPokeBagHandler))
        return list
    }

    fun sendPacketToPlayer(player: ServerPlayer, packet: NetworkPacket<*>) {
        Cobblemon.implementation.networkManager.sendPacketToPlayer(player, packet)
    }
}