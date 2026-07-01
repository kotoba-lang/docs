(ns docs.model)

(def block-kinds #{:heading :paragraph :quote :code :list :table :table-ref :file-ref :deck-ref})

(defn document
  ([id] (document id {}))
  ([id attrs]
   (merge {:docs/id id
           :docs/type :document
           :docs/title id
           :docs/blocks []
           :docs/comments []
           :docs/suggestions []}
          attrs)))

(defn block [kind id attrs]
  (merge {:docs/id id :docs/kind kind} attrs))

(defn heading [id level text]
  (block :heading id {:docs/level level :docs/text text}))

(defn paragraph [id text]
  (block :paragraph id {:docs/text text}))

(defn list-block [id items attrs]
  (block :list id (merge {:docs/items (vec items)} attrs)))

(defn table [id rows attrs]
  (block :table id (merge {:docs/rows (vec rows)} attrs)))

(defn add-text-style [b from to style]
  (update b :docs/text-runs (fnil conj [])
          {:docs/from from
           :docs/to to
           :docs/style style}))

(defn ref-block [kind id target]
  (block kind id {:docs/target target}))

(defn add-block [doc b]
  (update doc :docs/blocks conj b))

(defn add-comment [doc comment]
  (update doc :docs/comments conj comment))

(defn add-suggestion [doc suggestion]
  (update doc :docs/suggestions conj suggestion))

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
