(ns re-frame-simple.core
  (:refer-clojure :exclude [get get-in assoc! identity swap!])
  (:require [clojure.core :as core]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            reagent.ratom)
  (:require-macros re-frame-simple.core))

(rf/reg-sub :db/get (fn [db [_ key not-found]]
                      (core/get db key not-found)))

(rf/reg-sub :db/get-in

            ;; `get-in` creates an intermediate subscription for each path segment.
            ;; without this optimization, _every_ `get-in` listener would require
            ;; an additional equality check when the db changes.

            (fn [[_ path]]
              (if (<= (count path) 1)
                app-db
                (rf/subscribe [:db/get-in (drop-last path)])))
            (fn [parent [_ path]]
              (if (empty? path)
                parent
                (core/get parent (last path)))))

(rf/reg-sub :db/identity (fn [db [_]] db))

(rf/reg-event-db :db/update
                 (fn [db [_ key f & args]]
                   (apply update db key f args)))

(rf/reg-event-db :db/update-in
                 (fn [db [_ path f & args]]
                   (apply update-in db path f args)))

(rf/reg-event-db :db/assoc
                 (fn [db [_ & keyvals]]
                   (apply assoc db keyvals)))

(rf/reg-event-db :db/assoc-in
                 (fn [db [_ path value]]
                   (assoc-in db path value)))

(rf/reg-event-db :db/swap
                 (fn [db [_ & args]]
                   (apply core/swap! db args)))

(def ^:dynamic ^boolean *in-query?* false)

(defn get
  "Read a value from db by `key`, not-found or nil if value not present."
  ([key]
   (if *in-query?*
     (core/get @app-db key)
     @(rf/subscribe [:db/get key])))
  ([key not-found]
   (if *in-query?*
     (core/get @app-db key not-found)
     @(rf/subscribe [:db/get key not-found]))))

(defn get-in
  "Read a value from db by `path`, not-found or nil if value not present."
  ([path]
   (if *in-query?*
     (core/get-in @app-db path)
     @(rf/subscribe [:db/get-in path])))
  ([path not-found]
   (if *in-query?*
     (core/get-in @app-db path not-found)
     @(rf/subscribe [:db/get-in path not-found]))))

(defn identity
  "Return current value of db"
  []
  (if *in-query?*
    @app-db
    @(rf/subscribe [:db/identity])))

(defn update!
  "Applies update to db with args"
  [& args]
  (rf/dispatch (into [:db/update] args)))

(defn update-in!
  "Applies update-in to db with args"
  [& args]
  (rf/dispatch (into [:db/update-in] args)))

(defn assoc!
  "Applies assoc to db with args"
  [& args]
  (rf/dispatch (into [:db/assoc] args)))

(defn assoc-in!
  "Applies assoc-in to db with args"
  [& args]
  (rf/dispatch (into [:db/assoc-in] args)))

(defn swap!
  "Applies swap! to db with args."
  [& args]
  (rf/dispatch (into [:db/swap] args)))

(def dispatch "Dispatch a re-frame event." rf/dispatch)
(def dispatch-sync "Synchronous version of `dispatch`" rf/dispatch-sync)

