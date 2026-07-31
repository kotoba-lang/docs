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

(defn heading-level
  "A block's heading level as a number between 1 and 6.

  Every writer clamps — Markdown, Word and HTML all have six — and every one
  of them did it with `(max 1 (min 6 (or level 1)))`, which throws when the
  level is not a number. A hand-edited payload can carry `\"two\"`, nothing
  validates the type, and the result was a 500 from the exporter rather than
  a heading.

  Here rather than three times over: it is a fact about a heading, not about
  a file format."
  [block]
  (let [level (:docs/level block)]
    (if (number? level) (max 1 (min 6 (long level))) 1)))

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

(defn text-spans
  "A block's text cut into pieces, each with the style that covers it.

  `[{:docs/text \"plain \"} {:docs/text \"bold\" :docs/style {:bold true}}]`.
  Every writer needs the same three answers before it can mark anything up —
  which runs are usable, in what order, and what to do when two overlap —
  and each of the three had its own copy: `docs.html` filtered and sorted
  and gave up on overlap, `docs.markdown` did the same in reverse so its
  inserted characters would not move later offsets, and `docs.docx` did not
  try at all and dropped every run. Three copies of a rule is three chances
  to answer differently, which is what happened.

  The rules, in one place:

  - A run needs numeric `:docs/from` and `:docs/to` inside the text with
    `from` before `to`. Anything else is not a range and is ignored.
  - Overlapping runs mark up nothing. `<strong>a<em>b</strong>c</em>` is not
    HTML and `**a *b** c*` is not Markdown; producing either is worse than
    producing the text, and a writer that resolved overlap its own way would
    disagree with its siblings about what the document says.
  - The gaps between runs are spans too, with no style, so a caller can
    write the whole text by walking the result and never has to index back
    into the original."
  [b]
  (let [text (str (:docs/text b))
        n (count text)
        usable (->> (:docs/text-runs b)
                    (filter #(and (number? (:docs/from %)) (number? (:docs/to %))
                                  (<= 0 (:docs/from %)) (<= (:docs/to %) n)
                                  (< (:docs/from %) (:docs/to %))))
                    (sort-by :docs/from))
        overlapping? (some (fn [[a b]] (> (:docs/to a) (:docs/from b)))
                           (partition 2 1 usable))]
    (if (or overlapping? (empty? usable))
      (if (seq text) [{:docs/text text}] [])
      (let [[spans at]
            (reduce (fn [[spans at] {:keys [docs/from docs/to docs/style]}]
                      [(cond-> spans
                         (< at from) (conj {:docs/text (subs text at from)})
                         true (conj {:docs/text (subs text from to)
                                     :docs/style style}))
                       to])
                    [[] 0]
                    usable)]
        (cond-> spans
          (< at n) (conj {:docs/text (subs text at n)}))))))

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
