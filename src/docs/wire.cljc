(ns docs.wire
  "Transit wire helpers for Kotoba Docs documents.

  ## Out is lossy, back is explicit

  `transit.core/write-json` projects onto plain JSON: keywords become bare
  strings and map keys become strings. A document carries three keyword
  *values* the schema has to put back — `:docs/type`, a block's `:docs/kind`,
  and a suggestion's `:docs/op` — and one map whose keys are the author's
  rather than the schema's, the style on a text run.

  A generic keywordizer cannot tell those apart from an id, which is why
  `rehydrate-document` is here next to the model that defines them rather
  than in `transit`. The wire layer does not know what a block is.

  `read-document-envelope` returns the projection unchanged, for callers
  that only want to look at a value. `rehydrate-document` returns something
  `docs.model` and `docs.validate` will accept."
  (:require [transit.core :as transit]))

(defn document-envelope
  ([doc] (document-envelope doc {}))
  ([doc opts]
   (transit/office-envelope :docs/document doc opts)))

(defn read-document-envelope [body]
  (let [envelope (transit/read-office-envelope-body body)]
    (when-not (= :docs/document (:kotoba.resource/kind envelope))
      (throw (ex-info "not a Docs document Transit envelope"
                      {:kind (:kotoba.resource/kind envelope)})))
    (:kotoba.resource/payload envelope)))

;; ── back from plain JSON ────────────────────────────────────────────────────

(defn- keywordize
  "Every key to a keyword, recursively. For maps whose keys belong to whoever
  wrote them — a text-run style, a comment anchor — and never for a map keyed
  by id."
  [m]
  (reduce-kv (fn [acc k v]
               (assoc acc (keyword k) (if (map? v) (keywordize v) v)))
             {} m))

(defn- rehydrate-text-run [run]
  (reduce-kv (fn [acc k v]
               (if (= "docs/style" k)
                 (assoc acc :docs/style (keywordize v))
                 (assoc acc (keyword k) v)))
             {} run))

(defn- rehydrate-block [block]
  (reduce-kv
   (fn [acc k v]
     (case k
       "docs/kind" (assoc acc :docs/kind (keyword v))
       "docs/text-runs" (assoc acc :docs/text-runs (mapv rehydrate-text-run v))
       ;; :docs/rows and :docs/items are vectors of values, not of maps —
       ;; nothing in them was ever a keyword, so they pass through.
       (assoc acc (keyword k) v)))
   {} block))

(defn- rehydrate-comment [comment]
  (reduce-kv (fn [acc k v]
               (if (= "docs/anchor" k)
                 (assoc acc :docs/anchor (keywordize v))
                 (assoc acc (keyword k) v)))
             {} comment))

(defn- rehydrate-suggestion [suggestion]
  (reduce-kv (fn [acc k v]
               (assoc acc (keyword k) (if (= "docs/op" k) (keyword v) v)))
             {} suggestion))

(defn rehydrate-document
  "A plain-JSON payload back into a document."
  [payload]
  (reduce-kv
   (fn [acc k v]
     (case k
       "docs/type" (assoc acc :docs/type (keyword v))
       "docs/blocks" (assoc acc :docs/blocks (mapv rehydrate-block v))
       "docs/comments" (assoc acc :docs/comments (mapv rehydrate-comment v))
       "docs/suggestions" (assoc acc :docs/suggestions (mapv rehydrate-suggestion v))
       (assoc acc (keyword k) v)))
   {} payload))

(defn document-of-envelope
  "Read an envelope body and rehydrate it in one step."
  [body]
  (rehydrate-document (read-document-envelope body)))
