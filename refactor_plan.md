I would like to do some reorganization of the code into more files and
subdirectories.

I want `def`s that contain mostly data (like yatg.items/items) to be placed in
their own files called `data.cljc`.
So for items, we would rename the symbol and all it's usages to
yatg.items.data/items.

I want all usages of the word "bot" (like in bot_behavior) to be renamed to "npc".

I want my directory tree to look like this:

```
init.cljc
core.cljc
utils/
  core.cljc
  specter_with_better_errors.cljc
ui/
schemas/
  validation.cljc
  characters.cljc
  battle.cljc
  overworld.cljc
  graphics.cljc
  misc.cljc
graphics/
event_handling/
  overworld.cljc
  battle/
    turn_flow.cljc
    hexgrid.cljc
    timeline.cljc
    abilities.cljc
  infra.cljc
battle/
  hex_grid/
  timeline.cljc
  log.cljc
  core.cljc
characters/
  npc_behavior/
    core.cljc
  resources.cljc
  generation.cljc
  experience.cljc
  tokens/
    core.cljc
    data.cljc
  wounds/
    core.cljc
    data.cljc
  items/
    core.cljc
    data.cljc
  paths/
    core.cljc
    data.cljc
  abilities/
    use.cljc
    priming.cljc
    consequences.cljc
```

Any unspecified files should just stay in their current directories.

Please rename all usages and namespaces so that the code will still run.
Use tests to make sure this works.
