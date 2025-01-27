(ns acme.core)

(defn use-new-name? []
  true)

(defn print-company-name []
  (if (use-new-name?)
    (println "NEW NAME")
    (println "OLD NAME")))

(defn using-in-let []
  (let [enabled? (use-new-name?)
        _ (if enabled?
            (println "NEW")
            (println "OLD"))]
    (if enabled?
      (println "NEW")
      (println "OLD"))))