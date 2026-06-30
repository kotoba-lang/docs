# docs

Portable CLJC document workspace model for `docs.gftd.ai`.

The core is plain EDN: documents contain ordered blocks, blocks can cite
workspace objects, and hosts decide how to persist or render them.

```clojure
(require '[docs.model :as d])

(-> (d/document "memo" {:title "Strategy memo"})
    (d/add-block (d/heading "h1" 1 "Strategy"))
    (d/add-block (d/paragraph "p1" "One shared graph for docs, slides, drive, and sheets.")))
```

## Test

```bash
clojure -X:test
```
