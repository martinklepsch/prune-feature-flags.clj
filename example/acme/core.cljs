(ns acme.core)

(defn use-new-name? []
  true)

(defn use-new-layout? []
  true)

(defn use-beta-features? []
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

(defn complex-nested-flags []
  (let [new-name? (use-new-name?)
        beta? (use-beta-features?)
        layout? (use-new-layout?)]
    (when new-name?
      (println "Using new name")
      (if beta?
        (do
          (println "Beta features enabled")
          (when layout?
            (println "New layout active")))
        (println "Beta features disabled")))
    (when-not new-name?
      (println "Using old name"))))

(defn conditional-bindings []
  (let [new-name? (use-new-name?)
        greeting (if new-name?
                   "Welcome to the future!"
                   "Welcome!")
        layout? (when new-name?
                  (use-new-layout?))
        theme (if layout?
                {:color "blue"
                 :font "modern"}
                {:color "red"
                 :font "classic"})]
    (println greeting)
    (println "Using theme:" theme)))

(defn mixed-flags-and-logic []
  (let [name? (use-new-name?)
        beta? (use-beta-features?)
        count 5]
    (when (and name? (> count 3))
      (println "New name with enough items"))
    (if beta?
      (if (< count 10)
        (println "Beta: Low count warning")
        (println "Beta: All good"))
      (println "Regular view"))))