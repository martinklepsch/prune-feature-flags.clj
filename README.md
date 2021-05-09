# prune-feature-flags.clj

This is a little babashka utility script to remove conditionals based on static mappings to their test forms. 

- [This blogpost explains the basic idea in a bit more detail](https://martinklepsch.org/posts/homoiconicity-and-feature-flags.html).
- Take a look at `prune-feature-flags.clj`, there are less than 100 lines of code and about 30 of them are docstrings.
- Clone the repo and run `./prune-feature-flags.clj` to see how the file in the `example` directory is transformed. (Contributions to make the example more interesting very welcome!)

### From the `prune-conditionals` docstring:

With no `test-lookup` supplied this will transform code in the following way:

```clj
(prune-conditionals "(if true :a :b)")
```

the entire `if` form will get replaced by just `:a` since the else branch is
effectively dead.

When supplying a `test-lookup` map this becomes more useful with real world
code, in particular for removing code has become dead due to feature flags
being changed. Example:

```clj
(prune-conditionals "(if (flag-one?) :a :b)"
                    {'(flag-one?) true})
```

In this example the transformation code will behave as if all instances of
'(flag-one?) have been replaced by `true`.

### Limitations

- it only works with `if`, `when` and their `-not` variants
- extra forms like `and`, `not`, `or` etc. are not detected and therefore
  conditionals using them are left untouched
  (unless one of you test-lookup forms has them)
- inconsistent namespace aliases could make specifying the `test-lookup` map
  a bit more cumbersome
- `:refer-clojure :exclude [if when]` and similar configurations could lead to
  unwanted results"

