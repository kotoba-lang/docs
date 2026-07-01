(ns docs.validate
  (:require [docs.model :as model]))

(defn problem [severity code id msg]
  {:docs/severity severity :docs/code code :docs/id id :docs/msg msg})

(defn problems [doc]
  (let [ids (map :docs/id (:docs/blocks doc))]
    (vec
     (concat
      (when (or (nil? (:docs/title doc)) (= "" (:docs/title doc)))
        [(problem :warning :document/missing-title (:docs/id doc) "document has no title")])
      (for [b (:docs/blocks doc)
            :when (not (contains? model/block-kinds (:docs/kind b)))]
        (problem :error :block/unknown-kind (:docs/id b) "unknown block kind"))
      (for [[id n] (frequencies ids)
            :when (> n 1)]
        (problem :error :block/duplicate-id id "duplicate block id"))
      (for [b (:docs/blocks doc)
            run (:docs/text-runs b)
            :when (or (nil? (:docs/from run))
                      (nil? (:docs/to run))
                      (neg? (:docs/from run))
                      (< (:docs/to run) (:docs/from run)))]
        (problem :error :text-run/invalid (:docs/id b) "invalid text run range"))
      (for [comment (:docs/comments doc)
            :when (or (nil? (:docs/id comment))
                      (nil? (:docs/anchor comment)))]
        (problem :error :comment/invalid (:docs/id comment) "comment requires id and anchor"))
      (for [suggestion (:docs/suggestions doc)
            :when (or (nil? (:docs/id suggestion))
                      (nil? (:docs/op suggestion)))]
        (problem :error :suggestion/invalid (:docs/id suggestion) "suggestion requires id and op"))))))

(defn valid? [doc]
  (not-any? #(= :error (:docs/severity %)) (problems doc)))
