(ns acme.core)

(defn use-new-name? []
  true)

(defn print-company-name []
  (if (use-new-name?)
    (println "NEW NAME")
    (println "OLD NAME")))
