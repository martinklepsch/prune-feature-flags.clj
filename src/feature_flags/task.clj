(ns feature-flags.task
  (:require [babashka.cli :as cli]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [babashka.fs :as fs]
            [feature-flags.core :as ff]))

(defn prune
  [& args]
  (let [spec {:spec {:file {:desc "Single file to process"
                            :ref "<path>"}
                     :formatter {:desc "Formatter to use (cljstyle or cljfmt)"
                                 :validate #{"cljstyle" "cljfmt"}
                                 :ref "<formatter>"}
                     :test-lookup {:desc "Test lookup map, e.g. `{(my-flag?) true}`"
                                   :ref "<test-lookup>"}
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
            formatter (:formatter opts)
            mapping (when-let [test-lookup (:test-lookup opts)]
                      (edn/read-string test-lookup))]
        (if-not mapping
          (do
            (println "Please provide a test-lookup map which sets literal values for your feature flag expressions\n\n")

            (println (string/join " " args) "--test-lookup '{(my-flag?) true}'"))
          (doseq [file files]
            (binding [*out* *err*]
              (println "Processing" (str file)))
            (let [content (slurp file)
                  updated (-> content
                              (ff/prune-conditionals {:test-lookup mapping})
                              (ff/prune-conditionals {:test-lookup mapping})
                              (str))
                  formatted (:out (p/sh {:in updated}
                                        (case formatter
                                          "cljstyle" "cljstyle pipe"
                                          "cljfmt" "cljfmt fix -"
                                          "cat")))]
              (if (:dry-run opts)
                (println formatted)
                (spit file formatted)))))))))
