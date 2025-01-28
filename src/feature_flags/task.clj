(ns feature-flags.task
  (:require [babashka.cli :as cli]
            [babashka.process :as p]
            [babashka.fs :as fs]
            [feature-flags.core :as ff]))

(defn prune
  [& args]
  (let [spec {:spec {:file {:desc "Single file to process"
                            :ref "<path>"}
                     :formatter {:desc "Formatter to use (cljstyle or cljfmt)"
                                 :validate #{"cljstyle" "cljfmt"}
                                 :ref "<formatter>"}
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
            mapping {'(use-new-name?) true
                     '(use-new-layout?) true
                     '(use-beta-features?) true}]
        (doseq [file files]
          (binding [*out* *err*]
            (println "Processing" (str file)))
          (let [content (slurp file)
                updated (-> content
                            (ff/prune-conditionals mapping)
                            (ff/prune-conditionals mapping)
                            (str))
                formatted (:out (p/sh {:in updated}
                                      (case formatter
                                        "cljstyle" "cljstyle pipe"
                                        "cljfmt" "cljfmt fix -"
                                        "cat")))]
            (if (:dry-run opts)
              (println formatted)
              (spit file formatted))))))))
