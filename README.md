# feature-flags.clj

A Babashka task to prune feature flag conditionals from your codebase. Removes dead code branches based on flag evaluations, keeping your code clean.

![a demo of the pruning](/docs/demo.png)

[This blogpost explains the basic idea in a bit more detail](https://martinklepsch.org/posts/homoiconicity-and-feature-flags.html).

## Installation

In `bb.edn` add the following:

```clojure
{:deps {feature-flags {:git/url "https://github.com/martinklepsch/feature-flags.clj"
                       :sha "38dffe4b32fc33765bafd7e9f8aba027ba454e98"}}
 :tasks {prune feature-flags.task/prune}}
```

## Usage

```sh
bb prune --file example/acme/core.cljs \
         --formatter cljfmt \
         --test-lookup '{(use-new-name?) true}' \
         --dry-run
```

It is recommended to remove feature flags one-by-one and verify results inbetween.

More examples

```bash
# Prune flags in a single file
bb prune --file example/acme/core.cljs --test-lookup '{(use-new-name?) true}'

# Process multiple files with a glob pattern
bb prune --pattern "src/**/*.clj" --test-lookup '{(use-beta-features?) false}'

# Dry-run to see changes without saving
bb prune --file example.clj --test-lookup '{(use-new-layout?) true}' --dry-run
```

## Features

- Removes `if`, `when`, `if-not`, and `when-not` branches for resolved flags
- Supports local bindings in `let` forms
- Integrates with `cljstyle`/`cljfmt` for formatting
- Dry-run mode for safe previews

## Current Limitations (WIP)

- it will transform code like `(if true :a :b)` to `:a`
- extra forms like `and`, `not`, `or` etc. are not detected and therefore
  conditionals using them are left untouched (unless one of you test-lookup forms has them)
- inconsistent namespace aliases could make specifying the `test-lookup` map
  a bit more cumbersome
- `:refer-clojure :exclude [if when]` and similar configurations could lead to
  unwanted results"