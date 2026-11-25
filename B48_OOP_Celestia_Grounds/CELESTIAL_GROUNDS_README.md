# Celestial Grounds (built inside the PlayPokemon project)

The PlayPokemon project now contains **Celestial Grounds**, which follows the
"Celestial Grounds Game Script: Adventure and Battle" scene by scene: 18 scenes, 9 Hayop-Diwata,
the Tapang (courage) light system, befriending with Pandesal and the Banga, and the Bakunawa finale.

The original PlayPokemon code is still in the project and still compiles. The Celestial Grounds
code is in a new package, `src/celestial`.

## How to run

**Eclipse:** import the folder as an existing project (it is still called `PlayPokemon`).
Then run either of these as a Java Application:

- `celestial.CelestialGrounds`, or
- `main.Main`, which now launches Celestial Grounds. To play the old Pokémon game, pass the
  program argument `legacy`.

The working directory must be the project folder (Eclipse's default) because images, font and music
are loaded from `res/`.

**Without Eclipse:** from the project folder, run `java -jar CelestialGrounds.jar`.

## Controls

| Key | Action |
|---|---|
| Arrow keys / WASD | Move (tap to turn) |
| Enter / Space / Z / X | Talk, interact, confirm |
| Esc / Backspace / C | Back |
| P | Team (swap who is in your team of 3 and who rests in the Banga) |
| J | Hayop-Journal |
| F12 | Debug chapter select, for testing (jumps to any chapter with a ready team) |

## What follows the script

| Script | In game |
|---|---|
| Scenes 1-3, Barrio Tulog | Pre-dawn porch (bench/moon, dark yard, coop, door), Lola, Propesor's lab, starter choice with confirm, Dali tutorial battle at the gate (with the goat) |
| Scenes 4-8, Map 1 Earth | Palayan Trail Kalabaw befriending tutorial, Tala and Bituin, Team Tambay tricycle, Gusmo videoke battle, the Kawayan Maze (fireflies mark the path; dark moment at the halfway point, Tapang 2), Tamaraw at sunset with the Pandesal calm option, Hiyas ng Lupa |
| Scenes 9-12, Map 1 Fire | Jeepney with Dali on the roof, lava-river puzzle (Earth makes a stone bridge, Water cools the lava), dark tunnel (Tapang 3; walls glow if a Fire creature is in your team), Doña's gold cage, Dali opens it, Haribon (always acts first), Hiyas ng Apoy |
| Scenes 13-15, Map 2 Water | Ferry, seagrass encounters (shy Dugong flees if attacked), Sunken Temple in total darkness, walking by chimes (panned left/right, plus faint notes on screen), the Banga starts glowing (Tapang 4), Babaylan statue, Butanding (heals each turn), Hiyas ng Tubig |
| Scenes 16-18, Tuktok ng Celestia | Bituin becomes Apolaki (Araw) or Mayari (Buwan), final team of 3, the Bakunawa in Water, Fire then Earth form (dodge the waves, burning moonbeams, stone armor), Lunok-Liwanag blackout, Bayanihan drum (tap 8 beats), ending in the barrio, seven moons, closing quote card, credits |

**Battle menu:** LABAN | PANDESAL | BANGA | PALIT | TAKBO | HINT (HINT appears after Tala joins).
Fire beats Earth, Earth beats Water, Water beats Fire (1.5x / 0.5x). All nine abilities work as
described in the script. Your starter never appears wild; its rare slot gives a free Pandesal.
The party limit is 3; extra friends rest in the Banga.

**Losing:** you wake at the last Banga Shrine you passed, and Lola's voice says "Kumain ka muna, apo."

### Additions the script didn't specify

- Banga Shrines also heal your team and refill Pandesal to 3, so Pandesal can't run out for good.
- EXP is shared with the whole team. Members two or more levels behind earn double, so new friends catch up.
- Between Bakunawa phases, the three Hiyas heal your team by 30%.
- If your team is missing an element before the finale, or you lose to the Bakunawa, Tala says where
  the missing Common creature lives.

## Where things are

```
src/celestial/
  CelestialGrounds.java   window + 60 FPS loop
  Game.java               screen stack, cutscene runner, fades
  story/Scenes.java       ALL 18 SCENES (dialogue is here, in script order)
  story/BattleScripts.java the scripted battles (Dali, Kalabaw, Gusmo, Guardians, Doña, Bakunawa)
  story/Story.java        progress: party, Pandesal, Tapang, Hiyas, flags
  battle/Battle.java      battle rules, abilities, befriending, Bakunawa phases, dodge QTE
  data/                   Species (the nine + story creatures), Move, Element, Ability, Creature
  world/Maps.java         warps, NPCs, lights, encounters for every area
  world/MapGrids.java     tile layouts (GENERATED - edit tools/build_maps.py instead)
  world/TileArt.java      procedural pixel-art tiles
  ui/                     title, text boxes, tutorial pop-ups, team, journal, rhythm drum, story cards
res/celestial/            sprites (cast sheets are recolours of the PlayPokemon trainer sprites)
tools/                    make_sprites.py, build_maps.py, harness/ (automated playthrough + balance sim)
```

To change a line of dialogue, edit `Scenes.java` or `BattleScripts.java`. Placeholders such as
`[STARTER]`, `[RIVAL PARTNER]` and `[PLAYER NAME]` are filled automatically.

To change a map, edit `tools/build_maps.py` and run `python3 tools/build_maps.py .`. The script
checks that every exit, trigger and NPC can still be reached, then regenerates `MapGrids.java`.

## Testing that was done

- An automated bot played the whole game from the title screen to the credits for all three starters
  (Tarsi, Binturo, Dugong) and both heroes. It walked with pathfinding, read every text box, fought,
  befriended, solved the lava puzzle, dodged waves and tapped the drum. All runs finished.
- Every F12 debug chapter was played to the ending.
- Boss balance was simulated 60 times per starter at expected levels. Every Guardian and the
  Bakunawa is winnable with a three-element team. The hardest fight is Tamaraw on the Dugong route
  (about 88% with the weakest team; that route has the type disadvantage).
- The full project, old code included, compiles with Java 14 and runs in a real window.
