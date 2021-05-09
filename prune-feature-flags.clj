#!/usr/bin/env bb

(require '[rewrite-clj.zip :as z]
         '[clojure.java.io :as io]
         '[babashka.fs :as fs])

(defn- supported-conditional?
  [zloc]
  (contains? #{'when 'if 'when-not 'if-not} (z/sexpr (z/down zloc))))

(defn- test-expression
  "Given an if, when, if-not or when-not expression, return the condition"
  [zloc lookup]
  (let [list-zloc (z/down zloc)]
    (assert (contains? #{'when 'if 'when-not 'if-not} (z/sexpr list-zloc)))
    (let [t (z/sexpr (z/right list-zloc))]
      (get lookup t t))))

(defn transform-conditional [zloc test-lookup]
  (let [list-zloc (z/down zloc)]
    (case [(z/sexpr list-zloc) (test-expression zloc test-lookup)]
      [when true] (z/replace zloc (-> list-zloc z/right z/right z/node))
      [when false] (z/remove zloc)

      [when-not true] (z/remove zloc)
      [when-not false] (z/replace zloc (-> list-zloc z/right z/right z/node))

      [if true] (z/replace zloc (-> list-zloc z/right z/right z/node))
      [if false] (z/replace zloc (-> list-zloc z/right z/right z/right z/node))

      [if-not true] (z/replace zloc (-> list-zloc z/right z/right z/right z/node))
      [if-not false] (z/replace zloc (-> list-zloc z/right z/right z/node))

      ;; else
      zloc)))

(defn prune-conditionals
  "Given `code` as a string, remove if, when, if-not and when-not conditionals
  based on the test conditions.

  With no `test-lookup` supplied this will transform code in the following way:

      (if true :a :b)

  will get replaced by just `:a` since the else branch is effectively dead.

  When supplying a `test-lookup` map this becomes more useful with real world
  code, in particular for removing code has become dead due to feature flags
  being changed. Example:

     (prune-conditionals \"(if (flag-one?) :a :b)\"
                         {'(flag-one?) true})

  In this example the transformation code will behave as if all instances of
  '(flag-one?) have been replaced by `true`.

  LIMITATIONS:
  - it only works with `if`, `when` and their `-not` variants
  - extra forms like `and`, `not`, `or` etc. are not detected and therefore
    conditionals using them are left untouched
    (unless one of you test-lookup forms has them)
  - inconsistent namespace aliases could make specifying the `test-lookup` map
    a bit more cumbersome
  - :refer-clojure :exclude [if when] and similar configurations could lead to
    unwanted results"
  ([code]
   (prune-conditionals code {}))
  ([code test-lookup]
   (let [zip (z/of-string code)]
     (loop [zloc zip]
       (if (z/end? zloc)
         (z/root zloc)
         (let [zloc (z/next zloc)]
           (if (and (= :list (z/tag zloc))
                    (supported-conditional? zloc))
             (recur (transform-conditional zloc test-lookup))
             (recur zloc))))))))


(def mapping
  {'(use-new-name?) true})

(doseq [path (fs/glob "example" "**.cljs")
        :let [f (io/file (str path))]]
  (println (str path))
  (when-not (.exists (io/file f))
    (println "File does not exist" f)
    (System/exit 1))
  (let [updated (str (prune-conditionals (slurp f) mapping))]
    (spit f updated)))

(comment
 (prune-conditionals "(defn foo [] (if (some-test) :a :b)))"
                     {'(some-test) true})

 )
