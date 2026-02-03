This is a script that scrapes from our internal riding assignment spreadsheet.

This embeds itself into Cobblemon and forces it to render Pokemon to compute seat locations. Setting this up and running it is a bit complicated. If you need assistance feel free to reach out to Landon (Discord: landonjw123).

How to run:
1. Install the Cursive IntelliJ extension

2. Add Clojure runtime dependencies to the Fabric `build.gradle.kts`.
    1. Add this to plugins: `id("dev.clojurephant.clojure") version "0.8.0"`
    2. Add this to repositories: `maven(url = "https://repo.clojars.org/")`
    3. Add this to dependencies
         ```kt
       modImplementation("org.clojure:clojure:1.11.2")
       modImplementation("org.clojure:tools.namespace:1.3.0")
       modImplementation("nrepl:nrepl:0.8.3")
       modImplementation("clj-http:clj-http:3.12.3")
       modImplementation("org.clojure:data.json:2.5.1")
       modImplementation("org.clojure:data.csv:1.1.0")
         ```

3. Set up a NREPL Server in `CobblemonFabric`
    1. Copy and paste this function
       ```kt
       private fun initialiseREPL() {
         val require = Clojure.`var`("clojure.core", "require")
         require.invoke(Clojure.read("nrepl.server"))
         val startServer = Clojure.`var`("nrepl.server", "start-server")
         startServer.invoke(Clojure.read(":port"), 9080)
         println("Started nREPL server on port 9080")
       }
       ```
    2. Call this at the end of the `initialize` function


4. Modify `PokemonRenderer` so we can hook into the render thread
    1. Add `var runnable: Runnable? = null` to the `PokemonRenderer` companion object
    2. At the beginning of the `render` function, add this code block
       ```kt
       try {
           runnable?.run()
       } catch (e: Exception) {
           e.printStackTrace()
       }
       ```

5. Force locators to update in `PokemonGuiUtils`
    1. In `PokemonGuiUtils#drawProfilePokemon`, add this line after `applyAnimations` (currently L129)
   ```kt
   model.updateLocators(null, state)
   ```


6. Run the Fabric client
7. Add a new configuration (beside 'Play' button, Edit Configurations)
    1. Click the plus, select **Remote Clojure REPL**
    2. Set connection type to nREPL, port to 9080
    3. Run this new configuration
    4. A new window on the right-hand side should open, where you can evaluate code.

8. Go into a superflat singleplayer world, go to [0, -60, 0] (make sure Pokémon are in your field of view)
9. Copy script and paste it into the REPL console
10. Wait a couple seconds, then reload your species directory.