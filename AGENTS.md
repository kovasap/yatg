# AGENTS

## Rules

 - NEVER edit code outside of this root directory.

## Testing

 - Run `npx shadow-cljs compile test` to execute tests.
 - New tests should always be written in test/yatg/

## Refactoring

 - When renaming files or symbols, use `clojure-lsp rename` to do renames
   instead of trying to do it yourself.
 - Use `clojure-lsp diagnostics` to find issues to fix if asked.
 - Use `clojure-lsp references` to learn more about where symbols are used.

## Style

 - Format code in cljc, cljs, and clj files with `zprint -w <file>` after
   editing them.

### Malli Schemas

 - Ensure all functions have `{:malli/schema [:-> ]}` metadata and a docstring.
 - All malli schemas should be placed in src/yatg/schemas.cljc, and organized
   under common comment headers.
   Functions that help navigate schemas should also be placed in this file.

### Code Size

 - Individual functions and files should be kept small.
   If a function or a file is getting too large, please split it up into several
   pieces.
   For files, use new subdirectories when necessary to keep things organized.
 - Re-use code as much as possible.
   Add new general-use functions to src/yatg/utils.cljc to help with this.

### Data Structures

 - When organizing data, prefer to use vectors of maps of the same schema over
   other data structures (like raw maps).
   This makes it easier to dynamically filter on arbitrary properties.

### Version Control

 - I use jutusu (jj) for version control in this repo.
   Prefer to use jj commands over git when possible.
 - Never modify version history or create new revisions unless I ask you to.
