(ns docs.wire
  "Transit wire helpers for Kotoba Docs documents."
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
