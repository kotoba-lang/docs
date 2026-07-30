#!/usr/bin/env nbb
;; The same test namespaces, on the other host.
;;
;; `docs.markdown` is regular expressions and string surgery, which is where
;; two hosts differ most quietly: a lookbehind, a character class, the
;; semantics of a `$1` replacement. A `.cljc` library that only ever runs on
;; one host is a `.clj` library with extra reader conditionals.
;;
;;   nbb --classpath "src:test:$(clojure -Spath)" scripts/test-cljs.cljs

(require '[clojure.test :as t]
         'docs.docx-test
         'docs.markdown-test
         'docs.model-test)

(let [{:keys [fail error]} (t/run-tests 'docs.docx-test 'docs.markdown-test 'docs.model-test)]
  (when (pos? (+ (or fail 0) (or error 0)))
    (js/process.exit 1)))
