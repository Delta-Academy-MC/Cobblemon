/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench.repository

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.Cobblemon.LOGGER
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.client.render.ModelLayer
import com.cobblemon.mod.common.client.render.ModelVariationSet
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.VaryingRenderableResolver
import com.cobblemon.mod.common.client.render.models.blockbench.*
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository
import com.cobblemon.mod.common.client.render.models.blockbench.blockentity.BlockEntityModel
import com.cobblemon.mod.common.client.render.models.blockbench.fossil.FossilModel
import com.cobblemon.mod.common.client.render.models.blockbench.pokeball.AncientPokeBallModel
import com.cobblemon.mod.common.client.render.models.blockbench.pokeball.BeastBallModel
import com.cobblemon.mod.common.client.render.models.blockbench.pokeball.PokeBallModel
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen1.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen2.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen3.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen4.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen5.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen6.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen7.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen8.*
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen9.*
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Bone
import com.cobblemon.mod.common.client.render.models.blockbench.pose.ModelPartTransformation
import com.cobblemon.mod.common.client.render.models.blockbench.pose.Pose
import com.cobblemon.mod.common.client.util.exists
import com.cobblemon.mod.common.util.adapters.ExpressionAdapter
import com.cobblemon.mod.common.util.adapters.ExpressionLikeAdapter
import com.cobblemon.mod.common.util.adapters.ModelPartTransformationAdapter
import com.cobblemon.mod.common.util.adapters.Vec3dAdapter
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.endsWith
import com.cobblemon.mod.common.util.fromJson
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.function.BiFunction
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.phys.Vec3

/**
 * A repository for [PosableModel]s. Can be parameterized with [PosableModel] itself or a subclass.
 * This will handle the loading of all factors of [PosableModel]s, including variations, posers, models, and indirectly
 * the animations by providing directories for the [BedrockAnimationRepository] to read from. This class will also
 * hang onto poser instances for reuse.
 *
 * @author Hiroku
 * @since February 28th, 2023
 */
object VaryingModelRepository {
    val posers = mutableMapOf<ResourceLocation, (Bone) -> PosableModel>()
    val variations = mutableMapOf<ResourceLocation, VaryingRenderableResolver>()
    val texturedModels = mutableMapOf<ResourceLocation, Bone>()

    private val types = listOf(
        "pokemon",
        "fossils",
        "npcs",
        "poke_balls",
        "generic",
        "block_entities",
    )

    val poserDirectories: List<Pair<String, Class<out PosableModel>>> = listOf(
        "bedrock/posers" to PosableModel::class.java,
        "bedrock/pokemon/posers" to PokemonPosableModel::class.java,
        "bedrock/fossils/posers" to FossilModel::class.java,
        "bedrock/block_entities/posers" to BlockEntityModel::class.java,
        "bedrock/npcs/posers" to PosableModel::class.java,
        "bedrock/poke_balls/posers" to PosableModel::class.java,
        "bedrock/generic/posers" to PosableModel::class.java,
    )

    val variationDirectories: List<String> = listOf(
        "bedrock/species",
        "bedrock/pokemon/resolvers"
    ) + types.map { "bedrock/$it/variations" }

    val modelDirectories: List<String> = listOf(
        "bedrock/models"
    ) + types.map { "bedrock/$it/models" }

    val animationDirectories: List<String> = listOf(
        "bedrock/animations"
    ) + types.map { "bedrock/$it/animations" }

    val fallback: ResourceLocation = cobblemonResource("substitute")

    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Vec3::class.java, Vec3dAdapter)
            .registerTypeAdapter(Expression::class.java, ExpressionAdapter)
            .registerTypeAdapter(ExpressionLike::class.java, ExpressionLikeAdapter)
            .registerTypeAdapter(PosableModel::class.java, JsonModelAdapter(::PosableModel))
            .registerTypeAdapter(PokemonPosableModel::class.java, JsonModelAdapter(::PokemonPosableModel))
            .registerTypeAdapter(FossilModel::class.java, JsonModelAdapter(::FossilModel))
            .registerTypeAdapter(BlockEntityModel::class.java, JsonModelAdapter(::BlockEntityModel))
            .registerTypeAdapter(Pose::class.java, PoseAdapter { JsonModelAdapter.model!! })
            .registerTypeAdapter(ModelPartTransformation::class.java, ModelPartTransformationAdapter)
            .addDeserializationExclusionStrategy(MixinCompatibilityExclusionStrategy)
            .create()
    }

    //Some mods will inject extra properties into the model part, which we use GSON for (through the Bone interface)
    //If through a 3rd party mixin fields get injected that cant be deserialized by default (e.g. optional), we crash
    //this strategy aims to skip the known 3rd party libraries that do this to avoid crashing
    object MixinCompatibilityExclusionStrategy : ExclusionStrategy {
        private var known3rdPartyMixins = listOf("embeddium")

        private var knownUnusedClasses = listOf(Optional::class.java)

        override fun shouldSkipField(field: FieldAttributes?): Boolean {
            if (known3rdPartyMixins.any { field?.name?.contains(it) == true }) {
                Cobblemon.LOGGER.debug("Skipping non-vanilla field encountered during model deserialization ${field?.name}")
                return true
            }
            return false
        }

        override fun shouldSkipClass(p0: Class<*>?): Boolean {
            if (p0 in knownUnusedClasses) {
                Cobblemon.LOGGER.debug("Skipping non-vanilla class encountered during model deserialization: ${p0?.name}")
                return true
            }
            return false
        }
    }

    fun loadJsonPoser(fileName: String, json: String, poserClass: Class<out PosableModel>): (Bone) -> PosableModel {
        // Faster to deserialize during asset load rather than rerunning this every time a poser is constructed.
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        return {
            var boneName = jsonObject.getAsJsonPrimitive("rootBone")
            JsonModelAdapter.modelPart = if (boneName != null && it.children[boneName.asString] != null) it.children[boneName.asString] else it.children[fileName] ?: it.children.entries.filter { LocatorAccess.PREFIX !in it.key }.first().value
            gson.fromJson(jsonObject, poserClass).also {
                it.poses.forEach { (poseName, pose) -> pose.poseName = poseName }
            }
        }
    }

    fun registerPosers(resourceManager: ResourceManager) {
        posers.clear()
        registerInBuiltPosers()
        registerJsonPosers(resourceManager)
        Cobblemon.LOGGER.info("Loaded ${posers.size} posers.")
    }

    fun registerInBuiltPosers() {
        inbuilt("azure_ball", ::PokeBallModel)
        inbuilt("beast_ball", ::BeastBallModel)
        inbuilt("cherish_ball", ::PokeBallModel)
        inbuilt("citrine_ball", ::PokeBallModel)
        inbuilt("dive_ball", ::PokeBallModel)
        inbuilt("dream_ball", ::PokeBallModel)
        inbuilt("dusk_ball", ::PokeBallModel)
        inbuilt("fast_ball", ::PokeBallModel)
        inbuilt("friend_ball", ::PokeBallModel)
        inbuilt("great_ball", ::PokeBallModel)
        inbuilt("heal_ball", ::PokeBallModel)
        inbuilt("heavy_ball", ::PokeBallModel)
        inbuilt("level_ball", ::PokeBallModel)
        inbuilt("love_ball", ::PokeBallModel)
        inbuilt("lure_ball", ::PokeBallModel)
        inbuilt("luxury_ball", ::PokeBallModel)
        inbuilt("master_ball", ::PokeBallModel)
        inbuilt("moon_ball", ::PokeBallModel)
        inbuilt("nest_ball", ::PokeBallModel)
        inbuilt("net_ball", ::PokeBallModel)
        inbuilt("park_ball", ::PokeBallModel)
        inbuilt("poke_ball", ::PokeBallModel)
        inbuilt("premier_ball", ::PokeBallModel)
        inbuilt("quick_ball", ::PokeBallModel)
        inbuilt("repeat_ball", ::PokeBallModel)
        inbuilt("roseate_ball", ::PokeBallModel)
        inbuilt("safari_ball", ::PokeBallModel)
        inbuilt("slate_ball", ::PokeBallModel)
        inbuilt("sport_ball", ::PokeBallModel)
        inbuilt("strange_ball", ::PokeBallModel)
        inbuilt("timer_ball", ::PokeBallModel)
        inbuilt("ultra_ball", ::PokeBallModel)
        inbuilt("verdant_ball", ::PokeBallModel)
        inbuilt("ancient_poke_ball", ::AncientPokeBallModel)
        inbuilt("ancient_citrine_ball", ::AncientPokeBallModel)
        inbuilt("ancient_verdant_ball", ::AncientPokeBallModel)
        inbuilt("ancient_azure_ball", ::AncientPokeBallModel)
        inbuilt("ancient_roseate_ball", ::AncientPokeBallModel)
        inbuilt("ancient_slate_ball", ::AncientPokeBallModel)
        inbuilt("ancient_ivory_ball", ::AncientPokeBallModel)
        inbuilt("ancient_great_ball", ::AncientPokeBallModel)
        inbuilt("ancient_ultra_ball", ::AncientPokeBallModel)
        inbuilt("ancient_feather_ball", ::AncientPokeBallModel)
        inbuilt("ancient_wing_ball", ::AncientPokeBallModel)
        inbuilt("ancient_jet_ball", ::AncientPokeBallModel)
        inbuilt("ancient_heavy_ball", ::AncientPokeBallModel)
        inbuilt("ancient_leaden_ball", ::AncientPokeBallModel)
        inbuilt("ancient_gigaton_ball", ::AncientPokeBallModel)

        inbuilt("squirtle", ::SquirtleModel)
        inbuilt("wartortle", ::WartortleModel)
        inbuilt("caterpie", ::CaterpieModel)
        inbuilt("metapod", ::MetapodModel)
        inbuilt("butterfree", ::ButterfreeModel)
        inbuilt("weedle", ::WeedleModel)
        inbuilt("kakuna", ::KakunaModel)
        inbuilt("beedrill", ::BeedrillModel)
        inbuilt("rattata", ::RattataModel)
        inbuilt("raticate", ::RaticateModel)
        inbuilt("rattata_alolan", ::RattataAlolanModel)
        inbuilt("raticate_alolan", ::RaticateAlolanModel)
        inbuilt("pidgey", ::PidgeyModel)
        inbuilt("pidgeotto", ::PidgeottoModel)
        inbuilt("diglett", ::DiglettModel)
        inbuilt("dugtrio", ::DugtrioModel)
        inbuilt("zubat", ::ZubatModel)
        inbuilt("krabby", ::KrabbyModel)
        inbuilt("paras", ::ParasModel)
        inbuilt("mankey", ::MankeyModel)
        inbuilt("primeape", ::PrimeapeModel)
        inbuilt("oddish", ::OddishModel)
        inbuilt("gloom", ::GloomModel)
        inbuilt("vileplume", ::VileplumeModel)
        inbuilt("bellossom", ::BellossomModel)
        inbuilt("voltorb", ::VoltorbModel)
        inbuilt("electrode", ::ElectrodeModel)
        inbuilt("ekans", ::EkansModel)
        inbuilt("machop", ::MachopModel)
        inbuilt("machoke", ::MachokeModel)
        inbuilt("machamp", ::MachampModel)
        inbuilt("arbok", ::ArbokModel)
        inbuilt("chansey", ::ChanseyModel)
        inbuilt("cubone", ::CuboneModel)
        inbuilt("ditto", ::DittoModel)
        inbuilt("dodrio", ::DodrioModel)
        inbuilt("doduo", ::DoduoModel)
        inbuilt("electabuzz", ::ElectabuzzModel)
        inbuilt("exeggcute", ::ExeggcuteModel)
        inbuilt("exeggutor", ::ExeggutorModel)
        inbuilt("farfetchd", ::FarfetchdModel)
        inbuilt("farfetchd_galarian", ::FarfetchdGalarianModel)
        inbuilt("gengar", ::GengarModel)
        inbuilt("geodude", ::GeodudeModel)
        inbuilt("golbat", ::GolbatModel)
        inbuilt("golduck", ::GolduckModel)
        inbuilt("golem", ::GolemModel)
        inbuilt("graveler", ::GravelerModel)
        inbuilt("growlithe", ::GrowlitheModel)
        inbuilt("hitmonchan", ::HitmonchanModel)
        inbuilt("hitmonlee", ::HitmonleeModel)
        inbuilt("jynx", ::JynxModel)
        inbuilt("kabuto", ::KabutoModel)
        inbuilt("kabutops", ::KabutopsModel)
        inbuilt("koffing", ::KoffingModel)
        inbuilt("krabby", ::KrabbyModel)
        inbuilt("lickitung", ::LickitungModel)
        inbuilt("marowak", ::MarowakModel)
        inbuilt("meowth", ::MeowthModel)
        inbuilt("nidoqueen", ::NidoqueenModel)
        inbuilt("nidoranf", ::NidoranfModel)
        inbuilt("nidoranm", ::NidoranmModel)
        inbuilt("nidorina", ::NidorinaModel)
        inbuilt("nidorino", ::NidorinoModel)
        inbuilt("omanyte", ::OmanyteModel)
        inbuilt("omastar", ::OmastarModel)
        inbuilt("persian", ::PersianModel)
        inbuilt("pinsir", ::PinsirModel)
        inbuilt("poliwag", ::PoliwagModel)
        inbuilt("politoed", ::PolitoedModel)
        inbuilt("ponyta", ::PonytaModel)
        inbuilt("psyduck", ::PsyduckModel)
        inbuilt("rapidash", ::RapidashModel)
        inbuilt("sandshrew", ::SandshrewModel)
        inbuilt("sandslash", ::SandslashModel)
        inbuilt("tangela", ::TangelaModel)
        inbuilt("tentacool", ::TentacoolModel)
        inbuilt("venomoth", ::VenomothModel)
        inbuilt("venonat", ::VenonatModel)
        inbuilt("elekid", ::ElekidModel)
        inbuilt("smoochum", ::SmoochumModel)
        inbuilt("hitmontop", ::HitmontopModel)
        inbuilt("electivire", ::ElectivireModel)
        inbuilt("tangrowth", ::TangrowthModel)
        inbuilt("blissey", ::BlisseyModel)
        inbuilt("piloswine", ::PiloswineModel)
        inbuilt("quagsire", ::QuagsireModel)
        inbuilt("swinub", ::SwinubModel)
        inbuilt("wooper", ::WooperModel)
        inbuilt("wooper_paldean", ::WooperPaldeanModel)
        inbuilt("yanma", ::YanmaModel)
        inbuilt("bibarel", ::BibarelModel)
        inbuilt("bidoof", ::BidoofModel)
        inbuilt("buneary", ::BunearyModel)
        inbuilt("lopunny", ::LopunnyModel)
        inbuilt("mamoswine", ::MamoswineModel)
        inbuilt("piplup", ::PiplupModel)
        inbuilt("yanmega", ::YanmegaModel)
        inbuilt("emolga", ::EmolgaModel)
        inbuilt("bounsweet", ::BounsweetModel)
        inbuilt("dartrix", ::DartrixModel)
        inbuilt("decidueye", ::DecidueyeModel)
        inbuilt("naganadel", ::NaganadelModel)
        inbuilt("poipole", ::PoipoleModel)
        inbuilt("rowlet", ::RowletModel)
        inbuilt("steenee", ::SteeneeModel)
        inbuilt("tsareena", ::TsareenaModel)
        inbuilt("centiskorch", ::CentiskorchModel)
        inbuilt("sizzlipede", ::SizzlipedeModel)
        inbuilt("kleavor", ::KleavorModel)
        inbuilt("pyukumuku", ::PyukumukuModel)
        inbuilt("deerling", ::DeerlingModel)
        inbuilt("sawsbuck", ::SawsbuckModel)
        inbuilt("sableye", ::SableyeModel)
        inbuilt("natu", ::NatuModel)
        inbuilt("xatu", ::XatuModel)
        inbuilt("nacli", :: NacliModel)
        inbuilt("naclstack", :: NaclstackModel)
        inbuilt("garganacl", ::GarganaclModel)
        inbuilt("milcery", :: MilceryModel)
        inbuilt("turtwig", :: TurtwigModel)
        inbuilt("grotle", :: GrotleModel)
        inbuilt("torterra", :: TorterraModel)
        inbuilt("torterra_cherry", :: TorterraCherryModel)
        inbuilt("xerneas", :: XerneasModel)
        inbuilt("klink", :: KlinkModel)
        inbuilt("klang", :: KlangModel)
        inbuilt("klinklang", :: KlinklangModel)
        inbuilt("morelull", :: MorelullModel)
        inbuilt("shiinotic", :: ShiinoticModel)
        inbuilt("spiritomb", :: SpiritombModel)
        inbuilt("chespin", :: ChespinModel)
        inbuilt("quilladin", :: QuilladinModel)
        inbuilt("chesnaught", :: ChesnaughtModel)
        inbuilt("pineco", :: PinecoModel)
        inbuilt("lotad", :: LotadModel)
        inbuilt("lombre", :: LombreModel)
        inbuilt("ludicolo", :: LudicoloModel)
        inbuilt("golett", :: GolettModel)
        inbuilt("stantler", :: StantlerModel)
        inbuilt("bergmite", :: BergmiteModel)
        inbuilt("avalugg", :: AvaluggModel)
        inbuilt("misdreavus", :: MisdreavusModel)
        inbuilt("mismagius", :: MismagiusModel)
        inbuilt("whismur", :: WhismurModel)
        inbuilt("loudred", :: LoudredModel)
        inbuilt("exploud", :: ExploudModel)
        inbuilt("luvdisc", :: LuvdiscModel)
        inbuilt("cryogonal", :: CryogonalModel)
        inbuilt("pumpkaboo", :: PumpkabooModel)
        inbuilt("gourgeist", :: GourgeistModel)
        inbuilt("eiscue", :: EiscueModel)
        inbuilt("wooloo", :: WoolooModel)
        inbuilt("dubwool", :: DubwoolModel)
        inbuilt("chimchar", :: ChimcharModel)
        inbuilt("monferno", :: MonfernoModel)
        inbuilt("infernape", :: InfernapeModel)
        inbuilt("kricketot", ::KricketotModel)
        inbuilt("kricketune", ::KricketuneModel)
        inbuilt("durant", ::DurantModel)
        inbuilt("mawile", ::MawileModel)
        inbuilt("walkingwake", ::WalkingwakeModel)
        inbuilt("ironleaves", ::IronleavesModel)
        inbuilt("miltank", ::MiltankModel)
        inbuilt("froakie", ::FroakieModel)
        inbuilt("frogadier", ::FrogadierModel)
        inbuilt("greninja", ::GreninjaModel)
        inbuilt("grookey", ::GrookeyModel)
        inbuilt("thwackey", ::ThwackeyModel)
        inbuilt("rillaboom", ::RillaboomModel)
        inbuilt("oshawott", ::OshawottModel)
        inbuilt("dewott", ::DewottModel)
        inbuilt("samurott", ::SamurottModel)
        inbuilt("snivy", ::SnivyModel)
        inbuilt("servine", ::ServineModel)
        inbuilt("serperior", ::SerperiorModel)
        inbuilt("slugma", ::SlugmaModel)
        inbuilt("magcargo", ::MagcargoModel)
        inbuilt("slugma_shiny", ::SlugmaShinyModel)
        inbuilt("magcargo_shiny", ::MagcargoShinyModel)
        inbuilt("chinchou", ::ChinchouModel)
        inbuilt("clamperl", ::ClamperlModel)
        inbuilt("huntail", ::HuntailModel)
        inbuilt("gorebyss", ::GorebyssModel)
        inbuilt("shuckle", ::ShuckleModel)
        inbuilt("mudbray", ::MudbrayModel)
        inbuilt("comfey", ::ComfeyModel)
        inbuilt("tandemaus", ::TandemausModel)
        inbuilt("maushold", ::MausholdModel)
        inbuilt("mausholdfour", ::MausholdfourModel)
        inbuilt("fidough", ::FidoughModel)
        inbuilt("dachsbun", ::DachsbunModel)
        inbuilt("chatot", ::ChatotModel)
        inbuilt("gligar", ::GligarModel)
        inbuilt("gliscor", ::GliscorModel)
        inbuilt("poochyena", ::PoochyenaModel)
        inbuilt("mightyena", ::MightyenaModel)
        inbuilt("shroomish", ::ShroomishModel)
        inbuilt("breloom", ::BreloomModel)
        inbuilt("charcadet", ::CharcadetModel)
        inbuilt("flittle", ::FlittleModel)
        inbuilt("surskit", ::SurskitModel)
        inbuilt("masquerain", ::MasquerainModel)
        inbuilt("carnivine", ::CarnivineModel)
        inbuilt("stufful", ::StuffulModel)
        inbuilt("bewear", ::BewearModel)
        inbuilt("scatterbug", ::ScatterbugModel)
        inbuilt("spewpa", ::SpewpaModel)
        inbuilt("vivillon", ::VivillonModel)
        inbuilt("barboach", ::BarboachModel)
        inbuilt("whiscash", ::WhiscashModel)
        inbuilt("combee", ::CombeeModel)
        inbuilt("vespiquen", ::VespiquenModel)
        inbuilt("lillipup", ::LillipupModel)
        inbuilt("herdier", ::HerdierModel)
        inbuilt("stoutland", ::StoutlandModel)
        inbuilt("sirfetchd", ::SirfetchdModel)
        inbuilt("duskull", ::DuskullModel)
        inbuilt("dusclops", ::DusclopsModel)
        inbuilt("glimmet", ::GlimmetModel)
        inbuilt("glimmora", ::GlimmoraModel)
        inbuilt("bonsly", ::BonslyModel)
        inbuilt("sudowoodo", ::SudowoodoModel)
        inbuilt("cetoddle", ::CetoddleModel)
        inbuilt("cetitan", ::CetitanModel)
        inbuilt("aipom", ::AipomModel)
        inbuilt("ambipom", ::AmbipomModel)
        inbuilt("hoothoot", ::HoothootModel)
        inbuilt("wingull", ::WingullModel)
        inbuilt("pelipper", ::PelipperModel)
        inbuilt("shinx", ::ShinxModel)
        inbuilt("luxio", ::LuxioModel)
        inbuilt("luxray", ::LuxrayModel)
        inbuilt("numel", ::NumelModel)
        inbuilt("roggenrola", ::RoggenrolaModel)
        inbuilt("boldore", ::BoldoreModel)
        inbuilt("gigalith", ::GigalithModel)
        inbuilt("yamask", ::YamaskModel)
        inbuilt("cofagrigus", ::CofagrigusModel)
        inbuilt("mareep", ::MareepModel)
        inbuilt("flaaffy", ::FlaaffyModel)
        inbuilt("ampharos", ::AmpharosModel)
        inbuilt("patrat", ::PatratModel)
        inbuilt("watchog", ::WatchogModel)
        inbuilt("skrelp", ::SkrelpModel)
        inbuilt("dragalge", ::DragalgeModel)
        inbuilt("bunnelby", ::BunnelbyModel)
        inbuilt("arrokuda", ::ArrokudaModel)
        inbuilt("barraskewda", ::BarraskewdaModel)
        inbuilt("squawkabilly", ::SquawkabillyModel)
        inbuilt("ponyta_galarian", ::PonytaGalarianModel)
        inbuilt("rapidash_galarian", ::RapidashGalarianModel)
        inbuilt("volbeat", ::VolbeatModel)
        inbuilt("illumise", ::IllumiseModel)
        inbuilt("yamper", ::YamperModel)
        inbuilt("boltund", ::BoltundModel)
        inbuilt("fuecoco", :: FuecocoModel)
        inbuilt("crocalor", :: CrocalorModel)
        inbuilt("skeledirge", :: SkeledirgeModel)
        inbuilt("quaxwell", :: QuaxwellModel)
        inbuilt("quaquaval", :: QuaquavalModel)
        inbuilt("snubbull", :: SnubbullModel)
        inbuilt("granbull", :: GranbullModel)
        inbuilt("maschiff", :: MaschiffModel)
        inbuilt("mabosstiff", :: MabosstiffModel)
        inbuilt("phanpy", :: PhanpyModel)
        inbuilt("donphan", :: DonphanModel)
        inbuilt("buizel", :: BuizelModel)
        inbuilt("floatzel", :: FloatzelModel)
        inbuilt("zigzagoon", :: ZigzagoonModel)
        inbuilt("linoone", :: LinooneModel)
        inbuilt("zigzagoon_galarian", :: ZigzagoonGalarianModel)
        inbuilt("linoone_galarian", :: LinooneGalarianModel)
        inbuilt("obstagoon", :: ObstagoonModel)
        inbuilt("wishiwashi_solo", :: WishiwashiSoloModel)
        inbuilt("wishiwashi_schooling", :: WishiwashiSchoolingModel)
        inbuilt("meowth_alolan", ::MeowthAlolanModel)
        inbuilt("meowth_galarian", ::MeowthGalarianModel)
        inbuilt("persian_alolan", ::PersianAlolanModel)
        inbuilt("perrserker", ::PerrserkerModel)
        inbuilt("komala", ::KomalaModel)
        inbuilt("phantump", ::PhantumpModel)
        inbuilt("totodile", ::TotodileModel)
        inbuilt("croconaw", ::CroconawModel)
        inbuilt("feraligatr", ::FeraligatrModel)
        inbuilt("cyndaquil", ::CyndaquilModel)
        inbuilt("quilava", ::QuilavaModel)
        inbuilt("typhlosion", ::TyphlosionModel)
        inbuilt("chikorita", ::ChikoritaModel)
        inbuilt("bayleef", ::BayleefModel)
        inbuilt("meganium", ::MeganiumModel)
        inbuilt("fletchling", ::FletchlingModel)
        inbuilt("fletchinder", ::FletchinderModel)
        inbuilt("talonflame", ::TalonflameModel)
        inbuilt("crabrawler", ::CrabrawlerModel)
        inbuilt("crabominable", ::CrabominableModel)
        inbuilt("wimpod", ::WimpodModel)
        inbuilt("golisopod", ::GolisopodModel)
        inbuilt("shedinja", ::ShedinjaModel)
        inbuilt("ralts", ::RaltsModel)
        inbuilt("kirlia", ::KirliaModel)
        inbuilt("gardevoir", ::GardevoirModel)
        inbuilt("gallade", ::GalladeModel)
        inbuilt("pidove", ::PidoveModel)
        inbuilt("tranquill", ::TranquillModel)
        inbuilt("unfezant", ::UnfezantModel)
        inbuilt("timburr", ::TimburrModel)
        inbuilt("gurdurr", ::GurdurrModel)
        inbuilt("conkeldurr", ::ConkeldurrModel)
        inbuilt("clodsire", ::ClodsireModel)
        inbuilt("gimmighoulchest", ::GimmighoulChestModel)
        inbuilt("drifloon", ::DrifloonModel)
        inbuilt("lileep", ::LileepModel)
        inbuilt("cradily", ::CradilyModel)
        inbuilt("tirtouga", ::TirtougaModel)
        inbuilt("arctovish", ::ArctovishModel)
        inbuilt("dracovish", ::DracovishModel)
        inbuilt("arctozolt", ::ArctozoltModel)
        inbuilt("dracozolt", ::DracozoltModel)
        inbuilt("shieldon", ::ShieldonModel)
        inbuilt("cranidos", ::CranidosModel)
        inbuilt("tyrunt", ::TyruntModel)
        inbuilt("anorith", ::AnorithModel)
        inbuilt("armaldo", ::ArmaldoModel)
        inbuilt("archen", ::ArchenModel)
        inbuilt("archeops", ::ArcheopsModel)
        inbuilt("aron", ::AronModel)
        inbuilt("lairon", ::LaironModel)
        inbuilt("aggron", ::AggronModel)
        inbuilt("hippopotas", ::HippopotasModel)
        inbuilt("hippowdon", ::HippowdonModel)
        inbuilt("zorua", ::ZoruaModel)
        inbuilt("zorua_hisuian", ::ZoruaHisuianModel)
        inbuilt("zoroark", ::ZoroarkModel)
        inbuilt("zoroark_hisuian", ::ZoroarkHisuianModel)
        inbuilt("aurorus", ::AurorusModel)
        inbuilt("voltorb_hisuian", ::VoltorbHisuianModel)
        inbuilt("electrode_hisuian", ::ElectrodeHisuianModel)
        inbuilt("sentret", ::SentretModel)
        inbuilt("qwilfish", ::QwilfishModel)
        inbuilt("qwilfish_hisuian", ::QwilfishHisuianModel)
        inbuilt("overqwil", ::OverqwilModel)
        inbuilt("petilil", ::PetililModel)
        inbuilt("lilligant", ::LilligantModel)
        inbuilt("petilil_hisui_bias", ::PetililHisuiBiasModel)
        inbuilt("lilligant_hisuian", ::LilligantHisuianModel)
        inbuilt("darumaka", ::DarumakaModel)
        inbuilt("turtonator", ::TurtonatorModel)
        inbuilt("cufant", ::CufantModel)
        inbuilt("copperajah", ::CopperajahModel)
        inbuilt("budew", ::BudewModel)
        inbuilt("roselia", ::RoseliaModel)
        inbuilt("roserade", ::RoseradeModel)
        inbuilt("woobat", ::WoobatModel)
        inbuilt("swoobat", ::SwoobatModel)
        inbuilt("frillish", ::FrillishModel)
        inbuilt("jellicent", ::JellicentModel)
        inbuilt("cubchoo", ::CubchooModel)
        inbuilt("beartic", ::BearticModel)
        inbuilt("dreepy", ::DreepyModel)
        inbuilt("drakloak", ::DrakloakModel)
        inbuilt("diglett_alolan", ::DiglettAlolanModel)
        inbuilt("dugtrio_alolan", ::DugtrioAlolanModel)
        inbuilt("makuhita", ::MakuhitaModel)
        inbuilt("hariyama", ::HariyamaModel)
        inbuilt("alomomola", ::AlomomolaModel)
        inbuilt("carbink", ::CarbinkModel)
        inbuilt("goomy", ::GoomyModel)
        inbuilt("goomy_hisui_bias", ::GoomyHisuiBiasModel)
        inbuilt("sliggoo", ::SliggooModel)
        inbuilt("sliggoo_hisuian", ::SliggooHisuianModel)
        inbuilt("goodra", ::GoodraModel)
        inbuilt("goodra_hisuian", ::GoodraHisuianModel)
        inbuilt("salandit", ::SalanditModel)
        inbuilt("salazzle", ::SalazzleModel)
        inbuilt("jangmo-o", ::JangmoOModel)
        inbuilt("hakamo-o", ::HakamoOModel)
        inbuilt("kommo-o", ::KommoOModel)
        inbuilt("trapinch", ::TrapinchModel)
        inbuilt("vibrava", ::VibravaModel)
        inbuilt("larvitar", ::LarvitarModel)
        inbuilt("pupitar", ::PupitarModel)
        inbuilt("tyranitar", ::TyranitarModel)
        inbuilt("impidimp", ::ImpidimpModel)
        inbuilt("morgrem", ::MorgremModel)
        inbuilt("grimmsnarl", ::GrimmsnarlModel)
        inbuilt("klefki", ::KlefkiModel)
        inbuilt("oshawott_hisui_bias", ::OshawottHisuiBiasModel)
        inbuilt("dewott_hisui_bias", ::DewottHisuiBiasModel)
        inbuilt("samurott_hisuian", ::SamurottHisuianModel)
        inbuilt("cyndaquil_hisui_bias", ::CyndaquilHisuiBiasModel)
        inbuilt("quilava_hisui_bias", ::QuilavaHisuiBiasModel)
        inbuilt("typhlosion_hisuian", ::TyphlosionHisuianModel)
        inbuilt("rowlet_hisui_bias", ::RowletHisuiBiasModel)
        inbuilt("dartrix_hisui_bias", ::DartrixHisuiBiasModel)
        inbuilt("decidueye_hisuian", ::DecidueyeHisuianModel)
    }

    fun registerJsonPosers(resourceManager: ResourceManager) {
        for ((directory, poserClass) in poserDirectories) {
            resourceManager
                .listResources(directory) { path -> path.endsWith(".json") }
                .forEach { (identifier, resource) ->
                    resource.open().use { stream ->
                        val json = String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        val resolvedIdentifier = ResourceLocation.fromNamespaceAndPath(identifier.namespace, File(identifier.path).nameWithoutExtension)
                        posers[resolvedIdentifier] = loadJsonPoser(resolvedIdentifier.path, json, poserClass)
                    }
                }
        }
    }

    fun inbuilt(name: String, model: (ModelPart) -> PosableModel) {
        posers[cobblemonResource(name)] = { bone -> model.invoke(bone as ModelPart) }
    }

    fun registerVariations(resourceManager: ResourceManager) {
        var variationCount = 0
        val nameToModelVariationSets = mutableMapOf<ResourceLocation, MutableList<ModelVariationSet>>()
        for (directory in variationDirectories) {
            resourceManager
                .listResources(directory) { path -> path.endsWith(".json") }
                .forEach { (_, resource) ->
                    resource.open().use { stream ->
                        val json = String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        val modelVariationSet = VaryingRenderableResolver.GSON.fromJson<ModelVariationSet>(json)
                        nameToModelVariationSets.getOrPut(modelVariationSet.name) { mutableListOf() }.add(modelVariationSet)
                        variationCount += modelVariationSet.variations.size
                    }
                }
        }

        for ((species, speciesVariationSets) in nameToModelVariationSets) {
            val variations = speciesVariationSets.sortedBy { it.order }.flatMap { it.variations }.toMutableList()
            this.variations[species] = VaryingRenderableResolver(species, variations)
        }

        variations.values.forEach { it.initialize(this) }

        Cobblemon.LOGGER.info("Loaded $variationCount variations.")
    }

    fun registerModels(resourceManager: ResourceManager) {
        var models = 0
        for (directory in modelDirectories) {
            MODEL_FACTORIES.forEach { (key, func) ->
                resourceManager.listResources(directory) { path -> path.endsWith(key) }
                    .mapNotNull { func.apply(it.key, it.value) }
                    .forEach {
                        texturedModels[it.first] = it.second
                        models++
                    }
            }

        }

        Cobblemon.LOGGER.info("Loaded $models models.")
    }

    fun reload(resourceManager: ResourceManager) {
        Cobblemon.LOGGER.info("Loading varying Bedrock assets...")
        this.variations.clear()
        this.posers.clear()
        registerModels(resourceManager)
        registerPosers(resourceManager)
        registerVariations(resourceManager)
    }

    fun getPoser(name: ResourceLocation, state: PosableState): PosableModel {
        try {
            val poser = this.variations[name]?.getPoser(state)
            if (poser != null) {
                return poser
            }
        } catch(e: IllegalStateException) {
            e.printStackTrace()
        }
        return this.variations[fallback]!!.getPoser(state)
    }

    fun getTexture(name: ResourceLocation, state: PosableState): ResourceLocation {
        try {
            val texture = this.variations[name]?.getTexture(state)
            if (texture != null) {
                return texture
            }
        } catch(_: IllegalStateException) { }
        return this.variations[fallback]!!.getTexture(state)
    }

    fun getTextureNoSubstitute(name: ResourceLocation, state: PosableState): ResourceLocation? {
        try {
            val texture = this.variations[name]?.getTexture(state)
            if (texture != null && texture.exists()) {
                return texture
            }
        } catch(_: IllegalStateException) {}
        return null
    }

    fun getLayers(name: ResourceLocation, state: PosableState): Iterable<ModelLayer> {
        try {
            val layers = this.variations[name]?.getLayers(state)
            if (layers != null) {
                return layers
            }
        } catch(_: IllegalStateException) { }
        return this.variations[fallback]!!.getLayers(state)
    }

    fun getSprite(name: ResourceLocation, state: PosableState, type: SpriteType): ResourceLocation? {
        try {
            return this.variations[name]?.getSprite(state, type)
        } catch (_: IllegalStateException) {}
        return null
    }

    fun registerFactory(id: String, factory: BiFunction<ResourceLocation, Resource, Pair<ResourceLocation, Bone>?>) {
        MODEL_FACTORIES[id] = factory
    }

    /*
        Needs to be java function to work with non kotlin sidemods.
        - Waterpicker
     */
    private var MODEL_FACTORIES = mutableMapOf<String, BiFunction<ResourceLocation, Resource, Pair<ResourceLocation, Bone>?>>().also {
        it[".geo.json"] = BiFunction<ResourceLocation, Resource, Pair<ResourceLocation, Bone>?> { identifier: ResourceLocation, resource: Resource ->
            resource.open().use { stream ->
                val json = String(stream.readAllBytes(), StandardCharsets.UTF_8)
                val resolvedIdentifier = ResourceLocation.fromNamespaceAndPath(identifier.namespace, File(identifier.path).nameWithoutExtension)

                val texturedModel = TexturedModel.from(json)
                if (texturedModel == null) {
                    LOGGER.warn("Failed to load model file with identifier $identifier You can ignore this (and the above message) if this is not a cobblemon model")
                    return@BiFunction null
                }
                resolvedIdentifier to texturedModel.create().bakeRoot()
            }
        }
    }
}