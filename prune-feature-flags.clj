#!/usr/bin/env bb

(require '[feature-flags.core :as ff]
         '[clojure.java.io :as io]
         '[babashka.fs :as fs])

(def mapping
  {'(use-new-name?) true})

(doseq [path (fs/glob "example" "**.cljs")
        :let [f (io/file (str path))]]
  (println (str path))
  (when-not (.exists (io/file f))
    (println "File does not exist" f)
    (System/exit 1))
  (let [updated (str (ff/prune-conditionals (slurp f) mapping))]
    (spit f updated)))