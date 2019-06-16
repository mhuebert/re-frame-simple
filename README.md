# Re-Frame Simple ![latest release](https://img.shields.io/github/tag/mhuebert/re-frame-simple.svg?color=%23309631&label=release)

Lightweight syntax for interacting with the `re-frame` db.

## Goals:

1. No boilerplate: eliminate tedious, repetitive code.
2. Fewer invented names: many operations are adequately described by the structure of your data + functions that operate on it.
3. A functional interface: interact with the re-frame-db by calling functions (named events are also available to `dispatch`).

## Reading data

```
(db/get :a)
(db/get-in [:a :b])
```

These functions are backed by subscriptions, which update reactively in a Reagent component or reaction.

To maintain performance as your db grows, `get-in` uses an intermediate subscription for each segment of the path. This can dramatically reduce the number of comparisons performed each time the db changes.

## Writing data

```
(db/assoc! :a 1)
(db/assoc-in! [:b :c] 1)

(db/update! :counter inc)
(db/update-in! [:counters :a] inc)
```

## Named events

`re-frame-simple` functions delegate to a standard set of re-frame events based on core Clojure functions:

```
:db/get
:db/get-in

:db/update
:db/update-in

:db/assoc
:db/assoc-in

:db/swap
```

----

## Motivation and Approach

`re-frame-simple` is a light syntax on top of `re-frame` which introduces fewer words and concepts and feels more like ordinary Clojure. Using paths into data and plain functions as 'identifiers' for transactions means that whether your system remains legible depends on the _structure_ of your data and the _functions_ you define to manipulate it, rather than names you invent to represent events and queries.

`re-frame-simple` is a library, not a fork. It can be happily used in conjunction with existing `re-frame` code.

## Get Started

Add the dependency in your `deps.edn` or `project.clj` ([instructions](https://clojars.org/mhuebert/re-frame-simple))

Require the namespace:

```clj
(ns my-app.core
  (:require [re-frame-simple.core :as db]))
```

**Note** we've aliased `re-frame-simple.core` as `db`.

Read from the db using `get`, `get-in` and `identity` functions:

```clj
;; value of the entire db
(db/get :a)        => (get @app-db :a)

;; swap! the entire db
(db/get-in [:a :b]) => (get-in @app-db [:a :b])
```

Well, that was simple. What's so special?

Behind the scenes, `get` and `get-in` map to re-frame **subscriptions**, so when you use these functions to read data, the component you're in will **automatically update** when that data changes.


Write using `assoc!`, `update!`, `assoc-in!`, `update-in!`:


```clj
(db/assoc! :a 1)
(db/assoc-in! [:a :b] 1)

(db/update! :a inc)
(db/update-in! [:a :b] inc)
```

These functions map to re-frame **event handlers** which mutate the current state of the world (the db). They end in `!` as a reminder that you're mutating the world.

Also available are some operations for the whole db (rather than at a particular key or path). We'll use these less often, as it's easier to understand, inspect, and debug your app when operations are scoped to specific paths.

```clj

(db/identity)     => @app-db

(db/swap! merge {:a 1})
```


## Example

Here is a counter widget which uses `get-in` and `update-in` to read and write a counter, given an id.

```clj
(defn counter
  "Render an interactive counter for `id`"
  [id]
                   ;; NOTICE: `db/update-in!` to write
  [:div {:on-click #(db/update-in! [::counters id] inc)}
   (str "Counter " id ": ")

   ;; NOTICE: `db/get-in` to read
   (db/get-in [::counters id])])
```

1. We put counters in a namespaced path in the db (`::counters`). This means I can search across my app for the namespaced `::counters` keyword, and find every instance where a counter is read or mutated. This makes up for some of the explicitness that is lost by moving away from typical, named re-frame events.
2. We didn't have to "register" anything to get a simple example like this to work. There is no inventing of names for transactions as simple as incrementing an integer at a path.

Using these tools, legibility of the reactivity system is built in to the design of your data structure, instead of added via explicitly named events/actions. (but we can still add those, when desired.)


## Named updates

`defupdate` associates a keyword with an update function. This can be dispatched like any other re-frame handler.

```clj
(db/defupdate :initialize [db start-val]
  (assoc db ::counters start-val))
```

Use with `re-frame.core/dispatch` (it's copied into the db namespace):

```clj
(db/dispatch [:initialize {"A" 0
                           "B" 1
                           "C" 2}])
```

## Named queries

Use `defquery` to create named queries that read data using `db/get` and `db/get-in`:

```clj
(db/defquery counter-list
  "Return the list of counters in the db, by id."
  []
  (-> (db/get ::counters)
      (keys)))
```

The function returns a plain value, but uses a reactive subscription behind the scenes
to trigger reactivity, so a component that uses the query will update when its data changes.

Usage:

```clj
(defn root-view
  "Render the page"
  []
  [:div
   "Click to count!"
                   ;; NOTICE: using our query
   (doall (for [id (counter-list)]
            ^{:key id} [counter id]))])
```