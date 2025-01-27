(ns feature-flags.task
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [feature-flags.core :as ff]))

(defn prune
  [& args]
  (let [spec {:spec {:file {:desc "Single file to process"
                            :ref "<path>"}
                     :pattern {:desc "Glob pattern to find files"
                               :ref "<pattern>"}
                     :dry-run {:desc "Only print the transformed result"
                               :coerce :boolean}}}
        opts (cli/parse-opts args spec)]
    (if (and (not (:file opts))
             (not (:pattern opts)))
      (println (str "Please provide either --file or --pattern\n\n"
                    (cli/format-opts spec)))
      (let [files (if-let [pattern (:pattern opts)]
                    (fs/glob "." pattern)
                    [(:file opts)])
            mapping {'(use-new-name?) true
                     '(use-new-layout?) true
                     '(use-beta-features?) true}]
        (doseq [file files]
          (println "Processing" (str file))
          (let [content (slurp file)
                updated (ff/prune-conditionals content mapping)]
            (if (:dry-run opts)
              (println updated)
              (spit file updated))))))))