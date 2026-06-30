(ns docs.model)

(def block-kinds #{:heading :paragraph :quote :code :table-ref :file-ref :deck-ref})

(defn document
  ([id] (document id {}))
  ([id attrs]
   (merge {:docs/id id
           :docs/type :document
           :docs/title id
           :docs/blocks []}
          attrs)))

(defn block [kind id attrs]
  (merge {:docs/id id :docs/kind kind} attrs))

(defn heading [id level text]
  (block :heading id {:docs/level level :docs/text text}))

(defn paragraph [id text]
  (block :paragraph id {:docs/text text}))

(defn ref-block [kind id target]
  (block kind id {:docs/target target}))

(defn add-block [doc b]
  (update doc :docs/blocks conj b))

(defn block-by-id [doc id]
  (first (filter #(= id (:docs/id %)) (:docs/blocks doc))))

(defn outline [doc]
  (->> (:docs/blocks doc)
       (filter #(= :heading (:docs/kind %)))
       (mapv #(select-keys % [:docs/id :docs/level :docs/text]))))

(defn seed-document []
  (-> (document "gftd-docs" {:docs/title "GFTD Docs"})
      (add-block (heading "intro" 1 "GFTD Docs"))
      (add-block (paragraph "body" "Documents are EDN blocks with explicit links to sheets, drive files, and slide decks."))
      (add-block (ref-block :deck-ref "slides-link" "slides:intro-deck"))))
