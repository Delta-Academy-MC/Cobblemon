(ns riding-scraper
  (:require
    [clj-http.client :as client]
    [clojure.data.csv :as csv]
    [clojure.string :as s :refer [upper-case blank? split trim ends-with? includes? lower-case]]
    [clojure.java.io :refer [file]]
    [clojure.data.json :refer [read-str write-str]])
  (:import (com.cobblemon.mod.common.api.pokemon PokemonSpecies)
           (com.cobblemon.mod.common.client.gui PokemonGuiUtilsKt)
           (com.cobblemon.mod.common.client.render.models.blockbench FloatingState)
           (com.cobblemon.mod.common.client.render.pokemon PokemonRenderer)
           (com.cobblemon.mod.common.entity PoseType)
           (java.util Collection HashSet)
           (com.cobblemon.mod.common.pokemon RenderablePokemon)
           (net.minecraft.world.item ItemStack)
           (org.joml Quaternionf)
           (java.lang.reflect Field)
           (org.joml Vector3f)
           (net.minecraft.client Minecraft)
           (com.mojang.blaze3d.vertex PoseStack)))

(defn vector3f->vec
  "Converts a 3D vector into a simple Clojure map."
  [vector3f]
  {:x (.x vector3f)
   :y (.y vector3f)
   :z (.z vector3f)})

(defn get-seat-locator
  "Retrieves the translation for a specific locator from a rendered pokemon."
  [pokemon-state locator]
  (when-let [seat (.get (.getLocatorStates pokemon-state) locator)]
    (let [translation
          (-> seat
              (.getMatrix)
              (.getTranslation (Vector3f.))
              (vector3f->vec))]
      translation)))

(defn render-pokemon
  "Renders a Pokémon with the given species, optional form, and pose type, then
   collects any locator translations whose keys start with \"seat_\".
   This will be nil if there are no locators found."
  [species form pose-type]
  (let [aspects (if form (HashSet. ^Collection (.getAspects form)) (HashSet. 0))
        renderable (RenderablePokemon. species aspects ItemStack/EMPTY)
        pose-stack (PoseStack.)
        quaternion (Quaternionf.)
        player (.player (Minecraft/getInstance))
        translation-offset (update-vals (vector3f->vec (.position player)) #(float (* -1 %)))
        state (FloatingState.)]
    (.rotateZ quaternion (float 3.14))
    (.translate pose-stack ^Float (:x translation-offset) ^Float (:y translation-offset) ^Float (:z translation-offset))
    (.setCurrentAspects state (.getAspects renderable))
    (PokemonGuiUtilsKt/drawProfilePokemon renderable pose-stack quaternion pose-type state 0 (float 1) false true 1 1 1 1 0 0)
    (let [seat-locators (filter #(s/starts-with? % "seat_") (.keySet (.getLocatorStates state)))]
      (when (not-empty seat-locators)
        (into {} (map (fn [x] [x (get-seat-locator state x)]) seat-locators))))))

(def headers
  [:generation :dex-no :species :land-behaviour :liquid-behaviour :air-behaviour :land-ready-to-scrape?
   :liquid-ready-to-scrape? :air-ready-to-scrape? :missing? :animations-approved? :player-pose-type :land-speed :land-acceleration :land-skill :land-jump
   :land-stamina :liquid-speed :liquid-acceleration :liquid-skill :liquid-jump :liquid-stamina :air-speed
   :air-acceleration :air-skill :air-jump :air-stamina])

(def stat-range-headers
  [:generation :dex-no :species :land-behaviour :liquid-behaviour :air-behaviour
   :land-speed-min :land-speed-max
   :land-acceleration-min :land-acceleration-max
   :land-skill-min :land-skill-max
   :land-jump-min :land-jump-max
   :land-stamina-min :land-stamina-max

   :liquid-speed-min :liquid-speed-max
   :liquid-acceleration-min :liquid-acceleration-max
   :liquid-skill-min :liquid-skill-max
   :liquid-jump-min :liquid-jump-max
   :liquid-stamina-min :liquid-stamina-max

   :air-speed-min :air-speed-max
   :air-acceleration-min :air-acceleration-max
   :air-skill-min :air-skill-max
   :air-jump-min :air-jump-max
   :air-stamina-min :air-stamina-max])

(def behaviour-keys
  {"Jet"       "cobblemon:air/jet"
   "Bird"      "cobblemon:air/bird"
   "UFO"       "cobblemon:air/hover"
   "Hover"     "cobblemon:air/hover"
   "Rocket"    "cobblemon:air/rocket"
   "Standard"  "cobblemon:land/horse"
   "Basic"     "cobblemon:land/horse"
   "Vehicle"   "cobblemon:land/vehicle"
   "Minekart"  "cobblemon:land/minekart"
   "Submarine" "cobblemon:liquid/submarine"
   "Burst"     "cobblemon:liquid/burst"
   "Dolphin"   "cobblemon:liquid/dolphin"
   "Boat"      "cobblemon:liquid/boat"})

(def grade-ranges
  {"D" "0-10"
   "C" "10-40"
   "B" "30-65"
   "A" "55-85"
   "S" "80-100"})

(defn csv->map
  "Takes a series of CSV rows and returns a map of the columns with the header as the key"
  [csv-data headers start]
  (let [data (drop start csv-data)]
    (map zipmap (repeat headers) data)))

(defn concat-keyword
  "Takes two keywords or strings and returns a keyword with them concatenated with a hyphen"
  [a b]
  (keyword (str a "-" b)))

(defn condense-behaviour
  "
  Takes a map and creates a new nested map that we can parse easier.
  This will do nothing if a behaviour type is not present

  Ex:
  (condense-behaviour {:air-behaviour \"Boat\" :air-speed \"B\"} \"air\")
    => {:air {:behaviour \"Boat\" :skills {:speed \"B\"}}
  "
  [data type]
  (if (or (not (= "Yes" (get data (concat-keyword  type "ready-to-scrape?")))) (= "N/A" (get data (concat-keyword type "behaviour"))))
    data
    (assoc data (keyword type)
                {:behaviour (get data (concat-keyword type "behaviour"))
                 :stats     {:speed        (get data (concat-keyword type "speed"))
                             :acceleration (get data (concat-keyword type "acceleration"))
                             :skill        (get data (concat-keyword type "skill"))
                             :jump         (get data (concat-keyword type "jump"))
                             :stamina      (get data (concat-keyword type "stamina"))}})))

(defn drop-flat-behaviour-keys
  "Remove the original flat keys (e.g., -behaviour, -speed, -acceleration,
   -skill, -jump, -stamina) for the given type after condensing them into
   a nested structure."
  [data type]
  (-> data
      (dissoc (concat-keyword type "behaviour"))
      (dissoc (concat-keyword type "speed"))
      (dissoc (concat-keyword type "acceleration"))
      (dissoc (concat-keyword type "skill"))
      (dissoc (concat-keyword type "jump"))
      (dissoc (concat-keyword type "stamina"))))

(defn replace-if-equals
  "Replaces a value in a map with a new value if the current value is equal to some value that is provided"
  [map key old-val new-val]
  (update map key #(if (= % old-val) new-val %)))

(defn sanitize-riding-stats
  "Normalizes empty stat grades with a default 'C' grading."
  [data type]
  (if (nil? (get data (keyword type)))
    data
    (assoc-in data [(keyword type) :stats]
              (-> (get-in data [(keyword type) :stats])
                  (replace-if-equals :speed "" "C")
                  (replace-if-equals :acceleration "" "C")
                  (replace-if-equals :skill "" "C")
                  (replace-if-equals :jump "" "C")
                  (replace-if-equals :stamina "" "C")))))

(defn name->form
  "Extract the form name(s) from a bracketed suffix in `name` and join them with hyphens.
   Examples:
   - \"Species [Alpha]\" => \"Alpha\"
   - \"Species [Mega] [X]\" => \"Mega-X\"
   Returns nil when no bracketed form is present or the result is blank."
  [name]
  (let [form (->> (re-seq #"\[([^\[\]]+)\]" name)
                  (map second)
                  (clojure.string/join "-"))]
    (if (blank? form) nil form)))

(defn name->species
  "Return the base species from `name` by taking the portion before the first \"[\".
   Trims surrounding whitespace.
   Example: \"Species [Alpha]\" => \"Species\"."
  [name]
  (-> (split name #"\[")
      (first)
      (trim)))

(defn name->species-form
  "Derive a map with the species and an optional form from the name. If there's no form, it's nil."
  [name]
  {:species (name->species name)
   :form    (name->form name)})

(defn sanitize-riding-data
  "Normalize a single entity's riding data map.
   - Nests flat behaviour/stat fields for \"air\", \"land\", and \"liquid\" into
     :air, :land, and :liquid submaps, respectively.
   - Replaces empty stat grades (\"\") in those sections with the default \"C\".
   - Removes the original flat keys after nesting.
   - Merges in {:species ..., :form ...} derived from the original (:species data) string.
   Returns the transformed data map."
  [data]
  (-> data
      (condense-behaviour "air")
      (sanitize-riding-stats "air")
      (drop-flat-behaviour-keys "air")
      (condense-behaviour "land")
      (sanitize-riding-stats "land")
      (drop-flat-behaviour-keys "land")
      (condense-behaviour "liquid")
      (sanitize-riding-stats "liquid")
      (drop-flat-behaviour-keys "liquid")
      (merge (name->species-form (:species data)))))

(defn fetch-csv
  "Fetch and parse the riding data CSV"
  []
  (let [riding-stat-url "https://docs.google.com/spreadsheets/u/1/d/1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0/export?format=csv&id=1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0&gid=50216943"]
    (->> (client/get riding-stat-url)
         (:body)
         (csv/read-csv))))

(defn normalize-sound-json [v]
      (cond
        (vector? v) v
        (map? v)    [v]
        :else       []))

(defn get-sound-mappings []
  (let [data (->> (client/get "https://docs.google.com/spreadsheets/u/1/d/1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0/export?format=csv&id=1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0&gid=513653624")
                  (:body)
                  (csv/read-csv)
                  (rest)
                  (into {}))]
    (update-vals data #(-> % (read-str :key-fn keyword) normalize-sound-json))))

(def sound-headers
  [:generation :dex-no :species
   :land-riding-sound-1 :land-riding-sound-2 :land-riding-sound-3 :land-riding-sound-4 :land-riding-sound-5
   :liquid-riding-sound-1 :liquid-riding-sound-2 :liquid-riding-sound-3 :liquid-riding-sound-4 :liquid-riding-sound-5
   :air-riding-sound-1 :air-riding-sound-2 :air-riding-sound-3 :air-riding-sound-4 :air-riding-sound-5])

(defn get-sounds []
  (->> (client/get "https://docs.google.com/spreadsheets/u/1/d/1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0/export?format=csv&id=1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0&gid=1378925558")
       (:body)
       (csv/read-csv)
       (#(csv->map % sound-headers 3))
       (map #(merge % (name->species-form (:species %))))
       (map #(assoc % :form (or (:form %) "Normal")))))

(defn condense-sound-type [sound-data sound-mappings riding-type]
      (->> (map #(concat-keyword riding-type (str "riding-sound-" %)) [1 2 3 4 5])
           (map #(get sound-data %))
           (filter not-empty)
           (mapcat #(get sound-mappings %))
           vec))


(defn sound-data->json [sound-mappings sound-data riding-type]
  (->> (map #(assoc % :sound-json (condense-sound-type % sound-mappings riding-type)) sound-data)
       (map (fn [x] {[(:species x) (:form x)] x}))
       (apply merge)
       (#(update-vals % :sound-json))))

(defn get-sounds-map [sound-mappings sounds]
  (->> ["air" "liquid" "land"]
       (map (fn [x] [x (sound-data->json sound-mappings sounds x)]))
       (into {})))

(defn stats->json
  "Builds a JSON-ready map of riding stat grades from the provided stats-data."
  [stats-data]
  {:stats {:SPEED        (get grade-ranges (:speed stats-data))
           :ACCELERATION (get grade-ranges (:acceleration stats-data))
           :SKILL        (get grade-ranges (:skill stats-data))
           :JUMP         (get grade-ranges (:jump stats-data))
           :STAMINA      (get grade-ranges (:stamina stats-data))}})

(defmulti behaviour-type->json
          "Serializes a behaviour type into a JSON-ready map.

          Dispatch:
          - Dispatches on the identity of the provided type (e.g., keyword or string).
            Implementations should return a small map describing the behaviour or nil
            when the type is unknown/unmapped.

          Expected return:
          - A map containing at minimum {:key <mapped-key>} where <mapped-key> comes
            from the behaviour-keys mapping. Implementations may add additional fields
            appropriate to the behaviour type."
          identity)

(defmethod behaviour-type->json :default
  [type]
  (when-let [key (get behaviour-keys type)]
    {:key key}))

(defmethod behaviour-type->json "Basic"
  [type]
  (when-let [key (get behaviour-keys type)]
    {:key       key
     :canJump   false
     :canSprint false
     :stats {:ACCELERATION (get grade-ranges "C")
             :JUMP (get grade-ranges "C")
             :SKILL (get grade-ranges "S")
             :SPEED (get grade-ranges "C")
             :STAMINA (get grade-ranges "C")}}))

(defn get-defined-range [stat-ranges species form]
  (let [result (filter #(and (= species (:species %)) (= form (:form %))) stat-ranges)]
    (first result)))

(defn stat-range->json [stat-ranges species-data behaviour-type]
  (when-let [all-stats (get-defined-range stat-ranges (:species species-data) (:form species-data))]
    (when-let [behaviour-stats (get all-stats behaviour-type)]
      {:stats {:ACCELERATION (:acceleration behaviour-stats)
               :JUMP (:jump behaviour-stats)
               :SKILL (:skill behaviour-stats)
               :SPEED (:speed behaviour-stats)
               :STAMINA (:stamina behaviour-stats)}})))

(defn behaviour->json
  "Convert a single behaviour map into a JSON-ready structure.
   - Expects `behaviour-data` like {:behaviour \"...\" :stats {:speed \"A\" ...}}.
   - Resolves a stable key via `behaviour-keys`; returns nil if behaviour is unknown.
   - Translates letter grades to numeric ranges via `grade-ranges`."
  [species-data behaviour-data behaviour-sounds stat-ranges behaviour-type]
  (let [behaviour (:behaviour behaviour-data)
        behaviour-json (behaviour-type->json behaviour)
        stats (:stats behaviour-data)
        stats-json-from-range (stat-range->json stat-ranges species-data behaviour-type)
        stats-json-from-grade (stats->json stats)]
    (when (and behaviour-json (or stats-json-from-range stats-json-from-grade))
      (merge (or stats-json-from-range stats-json-from-grade) behaviour-json behaviour-sounds))))

(defn assoc-behaviour
  "Conditionally assoc a behaviour block into the accumulator `map`.
   - `species-data` may contain behaviour entries under keys like :air, :land, :liquid.
   - `behaviour` is the keyword to read (e.g., :air).
   - When present and valid, associates under an upper-cased key (e.g., :AIR).
   Returns the updated map or the original map if nothing was added."
  [map species-data behaviour sounds stat-ranges]
  (let [behaviour-sounds (get-in sounds [(name behaviour) [(:species species-data) (or (:form species-data) "Normal")]])
        behaviour-data (get species-data behaviour)
        behaviour-json (behaviour->json species-data behaviour-data {:rideSounds behaviour-sounds} stat-ranges behaviour)]
    (if (and behaviour-data behaviour-json)
      (assoc map (keyword (upper-case (name behaviour))) behaviour-json)
      map)))

(defn deep-merge
  "Recursively merge a series of maps"
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn flip-pose
  "Flips a single pose-to-seats mapping into a seat-centric mapping.

  Example:
  (flip-pose [\"WALK\" {\"seat_1\" {:x 0 :y 1 :z 0}
                        \"seat_2\"  {:x 0 :y 0.5 :z -0.2}}])
  ;; => {\"seat_1\" {\"WALK\" {:x 0, :y 1, :z 0}}
  ;;     \"seat_2\" {\"WALK\" {:x 0, :y 0.5, :z -0.2}}}"
  [[pose seats]]
  (into {}
        (map (fn [[seat offset]]
               [seat {pose offset}])
             seats)))

(defn transform-seat-data
  "Transforms a collection of pose-to-seats entries into a consolidated seat-centric map.\n"
  [seat-data]
  (apply deep-merge (map flip-pose seat-data)))

(defn seat->json
  "Converts a single seat entry (pose -> offset map) into a JSON-ready representation."
  [seat]
  (let [default (or (get seat "NONE") {:x 0.0 :y 0.0 :z 0.0})]
    {:offset      default
     :poseOffsets (map (fn [[k v]] {:offset v :poseTypes [k]}) seat)}))

(defn seats->json
  "Converts full seat data into a sorted vector of JSON-ready seat objects."
  [seat-data]
  (let [restructured-seats (into (sorted-map) (transform-seat-data seat-data))]
    (vec (map #(seat->json (second %)) restructured-seats))))

(defn behaviours->json
  "Assemble the behaviours section for a species in the expected JSON schema.
   Builds a map of available behaviours (:AIR, :LAND, :LIQUID) using `assoc-behaviour`.
   Wraps the result as {:riding {:behaviours {...}}}. Returns nil when no behaviours exist."
  [species-data sounds stat-ranges]
  (let [behaviours
        (-> {}
            (assoc-behaviour species-data :air sounds stat-ranges)
            (assoc-behaviour species-data :land sounds stat-ranges)
            (assoc-behaviour species-data :liquid sounds stat-ranges))]
    (when (not-empty behaviours)
      {:riding {:behaviours behaviours
                :seats      (seats->json (:seat-data species-data))}})))

(defn sanitize-species-name
  "Normalizes `species` by lower-casing and removing spaces, underscores, colons, dots, and hyphens."
  [species]
  (-> species
      (lower-case)
      (s/replace #"([ _:\.\-']+)" "")
      (s/replace "é" "e")))

(defn get-file-for-species
  "Return the first JSON file from `files` whose name matches `species`.
   - Only considers files whose names end with \".json\"
   - Selects the first file whose name contains the normalized species token.
   Returns a java.io.File or nil if no match is found."
  [species files]
  (->> files
       (filter #(ends-with? (.getName %) ".json"))
       (filter #(includes? (.getName %) (sanitize-species-name species)))
       (first)))

(defn read-species-json
  "Read a species JSON file from disk and parse it into Clojure data.
   - Accepts a java.io.File or path-like input.
   - Parses JSON with keywordized keys.
   Returns a Clojure map representing the species JSON."
  [file]
  (-> (slurp file)
      (read-str :key-fn keyword)))

(defn dissoc-in
  "Recursively dissociate an entry from a map.
   - Cleans up any now-empty maps along the path.
   - If the path doesn't exist or an intermediate value isn't a map, returns `m` unchanged.
   Returns the updated map."
  [m [k & ks]]
  (if ks
    (let [submap (get m k)]
      (if (map? submap)
        (let [updated (dissoc-in submap ks)]
          (if (empty? updated)
            (dissoc m k)                                    ;; remove the key entirely if submap is now empty
            (assoc m k updated)))
        m))                                                 ;; if the path doesn't exist or isn't a map, leave unchanged
    (dissoc m k)))

(defn sanitize-old-riding-json
  "Remove old redundant riding fields and ensure a default seat is present.
   - Drops [:riding :behaviours], [:riding :behaviour], and [:riding :stats].
   - Ensures [:riding :seats] is non-empty; if missing/empty, sets to
     [{:offset {:x 0 :y 0 :z 0}}].
   Returns the sanitized JSON map."
  [json-data]
  (-> json-data
      (dissoc-in [:riding :behaviour])
      (dissoc-in [:riding :stats])
      (dissoc-in [:riding :seats])))

(defn reconcile-riding-sounds [a b]
  (update b :behaviours
          (fn [b-behaviours]
            (reduce-kv
              (fn [acc k b-entry]
                (let [a-sounds (get-in a [:behaviours k :rideSounds])
                      b-sounds (:rideSounds b-entry)]
                  (if (seq b-sounds)
                    (let [a-by-loc (into {} (map (juxt :soundLocation identity) a-sounds))
                          b-by-loc (into {} (map (juxt :soundLocation identity) b-sounds))
                          ;; keep sounds that exist in both (from A) + sounds only in B
                          merged   (concat
                                     (vals (select-keys a-by-loc (keys b-by-loc))) ; from A
                                     (vals (apply dissoc b-by-loc (keys a-by-loc))))] ; only B
                      (-> acc
                          (assoc k (get-in b [:behaviours k]))
                          (assoc-in [k :rideSounds] (vec merged))))
                    ;; if B has no sounds, just skip
                    (assoc acc k b-entry))))
              {}
              b-behaviours))))

(defn apply-riding-data
  "Apply computed `riding-data` to an existing species JSON structure.
   - First sanitizes old/legacy riding fields.
   - If `form` is non-nil, updates the matching entry in :forms (by :name) recursively.
   - Otherwise, shallow-merges `riding-data`'s :riding into existing :riding.
   Returns the updated species JSON map."
  [json-data riding-data form]
  (let [sanitized-json-data (sanitize-old-riding-json json-data)]
    (if form
      (assoc sanitized-json-data :forms (map #(if (= (:name %) form) (apply-riding-data % riding-data nil) %) (:forms sanitized-json-data)))
      (let [old-riding-json-data (:riding sanitized-json-data)
            new-riding-json-data (reconcile-riding-sounds old-riding-json-data (get-in riding-data [:riding]))]
        (assoc sanitized-json-data :riding (deep-merge old-riding-json-data new-riding-json-data))))))

(def sort-order
  [:implemented :nationalPokedexNumber :name :primaryType :secondaryType :maleRatio :height :weight
   :pokedex :labels :aspects :abilities :eggGroups :baseStats :evYield :baseExperienceYield :experienceGroup :catchRate
   :eggCycles :baseFriendship :baseScale :hitbox :riding :behaviour :drops :moves :preEvolution :evolutions
   :forms :hp :attack :defence :special_attack :special_defence :speed :key :rideSounds])

(defn key-comparator
  "Create a comparator for map keys using a preferred order.
   - `ordered-keys`: sequence of keys that defines priority.
   Ordering rules:
   - Keys present in `ordered-keys` are ordered by their index and come first.
   - Keys not in `ordered-keys` come after known keys and are compared via `compare`.
   Returns a two-arg comparator suitable for use with `sorted-map-by`.

   We use this so that we can maintain reasonable order in the JSON documents/
   "
  [ordered-keys]
  (let [key->index (zipmap ordered-keys (range))]
    (fn [k1 k2]
      (let [i1 (get key->index k1 ::unknown)
            i2 (get key->index k2 ::unknown)]
        (cond
          ;; both known — compare by index
          (and (number? i1) (number? i2)) (compare i1 i2)
          ;; only k1 is known — it goes first
          (number? i1) -1
          ;; only k2 is known — it goes first
          (number? i2) 1
          ;; both unknown — compare naturally
          :else (compare k1 k2))))))

(defn recursive-sort-map
  "Recursively sort a map and any nested maps using the comparator `cmp`.
   - Returns a `sorted-map-by` at each map level.
   - If a value is a map, sorts it recursively.
   - If a value is a sequential collection, maps over its elements, and
     recursively sorts any maps inside; the collection is returned as a vector.
   - Non-map, non-sequential values are left unchanged."
  [cmp m]
  (letfn [(sort-entry [entry]
            (let [[k v] entry]
              [k (cond
                   (map? v) (recursive-sort-map cmp v)
                   (sequential? v) (mapv #(if (map? %) (recursive-sort-map cmp %) %) v)
                   :else v)]))]
    (into (sorted-map-by cmp)
          (map sort-entry m))))

(defn riding-data->cobblemon-json
  "Loads the species file, applies riding-data modifications, and returns the updated JSON alongside its source file."
  [riding-data files]
  (let [file-for-species (get-file-for-species (:species riding-data) files)
        file-json (read-species-json file-for-species)
        modified-file-json (apply-riding-data file-json (:json riding-data) (:form riding-data))]
    {:new-file-json modified-file-json :file file-for-species}))

(defn collapse-json
  "Combines multiple per-file JSON updates into a single, pretty-printed JSON string.
   This will attempt to maintain a sane order for the json between modifications, but order is not always guaranteed."
  [updates]
  (->> updates
       (map :new-file-json)
       (reduce deep-merge)
       (recursive-sort-map (key-comparator sort-order))
       (#(write-str % :indent true :escape-slash false))))

(def ignored-poses #{PoseType/PROFILE PoseType/NONE PoseType/SLEEP PoseType/OPEN
                     PoseType/PORTRAIT PoseType/SHOULDER_LEFT PoseType/SHOULDER_RIGHT})

(defn generate-seat-data
  "Generates per-pose seat data for a species (and optional form)"
  [species-data]
  (let [species (PokemonSpecies/getByName (sanitize-species-name (:species species-data)))
        form (.getFormByName species (sanitize-species-name (or (:form species-data) "Normal")))
        seat-data (map (fn [x] {(.name x) (render-pokemon species form x)}) (remove ignored-poses (PoseType/getEntries)))
        non-nil-seat-data (filter #(val (first %)) seat-data)]
    (when (not-empty non-nil-seat-data)
      (into {} non-nil-seat-data))))

(defn remove-empty-stats [m]
  (into {}
        (filter (fn [[_ stats]]
                  (not-every? #(= "-" %) (vals stats)))
                m)))

(defn remove-junk-from-stat-ranges [row]
  (merge (remove-empty-stats
          {:liquid {:speed (str (:liquid-speed-min row) "-" (:liquid-speed-max row))
                    :acceleration (str (:liquid-acceleration-min row) "-" (:liquid-acceleration-max row))
                    :skill (str (:liquid-skill-min row) "-" (:liquid-skill-max row))
                    :jump (str (:liquid-jump-min row) "-" (:liquid-jump-max row))
                    :stamina (str (:liquid-stamina-min row) "-" (:liquid-stamina-max row))}
           :land {:speed (str (:land-speed-min row) "-" (:land-speed-max row))
                  :acceleration (str (:land-acceleration-min row) "-" (:land-acceleration-max row))
                  :skill (str (:land-skill-min row) "-" (:land-skill-max row))
                  :jump (str (:land-jump-min row) "-" (:land-jump-max row))
                  :stamina (str (:land-stamina-min row) "-" (:land-stamina-max row))}
           :air {:speed (str (:air-speed-min row) "-" (:air-speed-max row))
                 :acceleration (str (:air-acceleration-min row) "-" (:air-acceleration-max row))
                 :skill (str (:air-skill-min row) "-" (:air-skill-max row))
                 :jump (str (:air-jump-min row) "-" (:air-jump-max row))
                 :stamina (str (:air-stamina-min row) "-" (:air-stamina-max row))}})
        (name->species-form (:species row))))

(defn get-ride-stats-from-csv []
  (->> "https://docs.google.com/spreadsheets/u/1/d/1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0/export?format=csv&id=1YhQlnH6m-tlkVbopIg5KLV8BFaaDCgZNkx3dIAy5PZ0&gid=536460956"
       (client/get)
       (:body)
       (csv/read-csv)
       (#(csv->map % stat-range-headers 3))
       (map remove-junk-from-stat-ranges)
       (filter #(or (:air %) (:land %) (:liquid %)))))

(defn get-file-updates
  "This is the meat of the script.
   Starting by grabbing the CSV data, this pipeline will continuously append data and eventually
   collapse itself into a map of files and the updated JSON contents to write.

   Pipeline:
   - Coerce the CSV data into a Clojure data structure
   - Sanitize the data into a more friendly format for editing
   - Keep entries that have at least one behaviour (:air/:land/:liquid).
   - Convert to JSON-ready behaviours (behaviours->json).
   - Resolve the target file for the species, read existing JSON, and apply riding data
     (respecting form-specific updates when present).
   - Collapse the data into just the file and new updates, and group results by :file.
   - Merge all updates into a single text blob
   Returns a map: java.io.File -> string"
  [csv files sounds stat-ranges]
  (let [file-updates (->> csv
                          (#(csv->map % headers 3))
                          (map sanitize-riding-data)
                          (map #(assoc % :seat-data (generate-seat-data %)))
                          (filter #(:seat-data %))
                          (map #(assoc % :json (behaviours->json % sounds stat-ranges)))
                          (filter #(:json %))
                          (map #(riding-data->cobblemon-json % files))
                          (group-by :file))]
    (update-vals file-updates collapse-json)))

(defn run-script []
  (let [path "../../common/src/main/resources/data/cobblemon/species"
        files (file-seq (file path))
        riding-data-csv (fetch-csv)
        ride-stat-ranges (get-ride-stats-from-csv)
        sounds (get-sounds-map (get-sound-mappings) (get-sounds))
        updates (get-file-updates riding-data-csv files sounds ride-stat-ranges)]
    (doseq [[file contents] updates]
      (spit file contents))))

(defn -main []
  (.setRunnable PokemonRenderer/Companion
                (reify java.lang.Runnable
                  (run [_]
                    (run-script)
                    (.setRunnable PokemonRenderer/Companion nil)))))

(-main)