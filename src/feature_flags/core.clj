(ns feature-flags.core
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))

(declare transform-conditional)

(defn- supported-conditional?
  [zloc]
  (contains? #{'when 'if 'when-not 'if-not} (z/sexpr (z/down zloc))))

(defn- find-local-binding
  "Given a symbol and a map of local bindings, return the bound value if it exists"
  [sym locals]
  (get locals sym))

(defn- test-expression
  "Given an if, when, if-not or when-not expression, return the condition"
  [zloc lookup locals]
  (let [list-zloc (z/down zloc)
        t (z/sexpr (z/right list-zloc))]
    (if (symbol? t)
      (or (find-local-binding t locals)
          (get lookup t t))
      (get lookup t t))))

(defn- count-forms
  "Count the number of forms after the test expression in a when/when-not form"
  [zloc]
  (loop [zloc (-> zloc z/down z/right z/right)
         count 0]
    (if (nil? zloc)
      count
      (recur (z/right zloc) (inc count)))))

(defn- collect-when-forms
  "Collect all forms after the test expression in a when/when-not form"
  [zloc]
  (loop [zloc (-> zloc z/down z/right z/right)
         forms []]
    (if (nil? zloc)
      forms
      (recur (z/right zloc) (conj forms (z/node zloc))))))

(defn- make-do-node
  "Create a new do node with the given forms"
  [forms]
  (let [do-node (z/of-node (list 'do))]
    (reduce (fn [loc form]
              (z/append-child loc form))
            do-node
            forms)))

(defn transform-conditional [zloc test-lookup locals]
  (let [list-zloc (z/down zloc)
        op (z/sexpr list-zloc)
        test-result (test-expression zloc test-lookup locals)]
    (case [op test-result]
      [when true] (if (> (count-forms zloc) 1)
                    (let [forms (collect-when-forms zloc)
                          do-node (n/list-node (cons (n/token-node 'do) forms))]
                      (z/replace zloc do-node))
                    (z/replace zloc (-> list-zloc z/right z/right z/node)))
      [when false] (z/remove zloc)

      [when-not true] (z/remove zloc)
      [when-not false] (if (> (count-forms zloc) 1)
                         (let [forms (collect-when-forms zloc)
                               do-node (n/list-node (cons (n/token-node 'do) forms))]
                           (z/replace zloc do-node))
                         (z/replace zloc (-> list-zloc z/right z/right z/node)))

      [if true] (z/replace zloc (-> list-zloc z/right z/right z/node))
      [if false] (z/replace zloc (-> list-zloc z/right z/right z/right z/node))

      [if-not true] (z/replace zloc (-> list-zloc z/right z/right z/right z/node))
      [if-not false] (z/replace zloc (-> list-zloc z/right z/right z/node))

      ;; else
      zloc)))

(defn- process-let-bindings
  "Process let bindings sequentially, accumulating locals and transforming conditionals"
  [zloc lookup]
  (let [bindings-vec (-> zloc z/down z/right)
        binding-pairs (partition 2 (z/sexpr bindings-vec))]
    (reduce (fn [acc-locals [sym expr]]
              (assoc acc-locals sym (get lookup expr expr)))
            {}
            binding-pairs)))

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

  The code also supports feature flags bound to local variables in let bindings:

     (let [new-name? (use-new-name?)
           _ (if new-name? :a :b)]  ; This will also be transformed
       (when new-name? :a))

  When `{'(use-new-name?) true}` is provided in the test-lookup, both the conditional
  in the binding and in the body will be transformed.

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
     (loop [zloc zip
            locals {}]
       (if (z/end? zloc)
         (z/root zloc)
         (let [zloc (z/next zloc)
               locals (if (and (= :list (z/tag zloc))
                               (= 'let (-> zloc z/down z/sexpr)))
                        (merge locals (process-let-bindings zloc test-lookup))
                        locals)]
           (if (and (= :list (z/tag zloc))
                    (supported-conditional? zloc))
             (recur (transform-conditional zloc test-lookup locals) locals)
             (recur zloc locals))))))))

(comment
  (prune-conditionals "(defn foo [] (if (some-test) :a :b)))"
                      {'(some-test) true})
  (prune-conditionals "(let [flag? (some-test)] (when flag? :a))"
                      {'(some-test) true})
  (prune-conditionals "(let [flag? (some-test)
                            _ (if flag? :a :b)] 
                        (when flag? :a))"
                      {'(some-test) true}))