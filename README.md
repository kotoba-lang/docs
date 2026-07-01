# docs

[![CI](https://github.com/kotoba-lang/docs/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/docs/actions/workflows/ci.yml)

Portable CLJC model for kotoba-lang/docs.

Pages editor: https://kotoba-lang.github.io/docs/

The Pages UI is local to kotoba-lang and does not redirect to external hosts.

## Compatibility direction

The document model keeps document semantics needed for Google Docs and
Word-style roundtrips: ordered blocks, headings, paragraphs, lists, tables,
inline text style runs, comments, suggestions, and refs to sheets/files/decks.
The shared wire format is Kotoba Transit JSON via
`docs.wire/document-envelope`, using `application/transit+json` and the
`:docs/document` resource kind.

## Test

```bash
clojure -M:test
```
