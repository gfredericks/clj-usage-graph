# clj-usage-graph

A Clojure library that emits usage diagrams for your project.

![Example](g.svg)

## Caveats

1. It will load all the source files you give it, so beware of any top-level side-effects.
2. It doesn't reflect macro usage at all. Any macros in your project
   will appear unused, as only the expanded code is analyzed.
3. It's probably not totally correct? Of course.

## Obtention

Leiningen coordinates:

``` clojure
[com.gfredericks/clj-usage-graph "0.1.3"]
```

## Usage

Make sure you have graphviz installed and thus the `dot` command
available; then:

```
find src -type f | \
xargs lein run -m com.gfredericks.clj-usage-graph/generate | \
dot -Tsvg -o g.svg
```

## License

Copyright © 2013 Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.
