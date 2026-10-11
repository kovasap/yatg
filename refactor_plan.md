I would like to do some reorganization of the code into more files and
subdirectories.
Specifically:

I want `def`s that contain mostly data (like yatg.items/items) to be placed in
their own files called `..._data.cljc`.
So for items, we would rename the symbol and all it's usages to
yatg.items-data/items.

I want all usages of the word "bot" (like in bot_behavior) to be renamed to "npc".
