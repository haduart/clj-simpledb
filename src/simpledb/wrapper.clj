(ns simpledb.wrapper
  (:use [simpledb.db :only (newDB drop! up? exist? create!)])
  (:require [simpledb.db :as db]
            [clojure.string :as string])
  (:import [clojure.lang Keyword]))

(def db-spec (atom {:subprotocol "mysql"
                     :subname     "//localhost:3306/hps"
                     :user        "root"}))

(defn count-db [historianDB]
  (count historianDB))

(defn server-is-up? [^String database]
  (let [historianDB (newDB @db-spec database)]
    (up? historianDB)))

(defn database-exist? [database]
  (let [historianDB (newDB @db-spec database)]
    (exist? historianDB)))

(defn create-db [^String database]
  (let [historianDB (newDB @db-spec database)]
    (if-not (exist? historianDB)
      (db/create! historianDB))))

(defn drop-db [^String database]
  (let [historianDB (newDB @db-spec database)]
    (drop! historianDB)))

(defn first-time? [^String database]
  (let [historianDB (newDB @db-spec database)]
    (if-not (exist? historianDB)
      (do
        (db/create! historianDB)
        true)
      false)))

(defn store [^String database key map]
  (let [historianDB (newDB @db-spec database)]
    (db/assoc! historianDB key map)))

(defn remove-value [^String database key]
  (let [historianDB (newDB @db-spec database)]
    (db/dissoc! historianDB key)))

(defn take-all [^String database]
  (let [historianDB (newDB @db-spec database)]
    (db/take-all historianDB)))

(defn get-value [^String database ^Keyword key]
  (let [historianDB (newDB @db-spec database)]
    (get historianDB key)))

(defn update-value [^String database ^Keyword key map]
  (let [historianDB (newDB @db-spec database)
        keyValue (get historianDB key)]
    (->>
      (if (not (nil? keyValue))
        (assoc map :_rev (:_rev keyValue))
        map)
      (db/assoc! historianDB key))))

(defn create-view!
  "Creates a Javascript view. You have to specify the database name,
  the design name, the view name and the javascript function"
  [^String database ^String design-name ^String view-name ^String javascript-function]
  (let [historian-db (newDB @db-spec database)]
    (db/create-view! historian-db design-name view-name javascript-function)))

(defn get-view
  "Returns a couchdb view. You have to specify the database name,
  the design name and the view name.
  Returns a lazy-seq on the couchdb view"
  [^String database ^String design-name ^String view-name]
  (db/get-view database design-name (keyword view-name)))

(defn create-user-view!
  "The design name it will be in the database name with '-' and username.
  The same for the view name. Creates a concrete view for the user"
  [^String database ^String username ^String javascript-function]
  (let [historian-db (newDB @db-spec database)]
    (db/create-user-view! historian-db username javascript-function)))

(defn get-user-view
  "The design name it will be the database name with '-' and username.
  The same for the view name. Returns a lazy-seq on the couchdb view."
  [^String database ^String username]
  (let [historian-db (newDB @db-spec database)]
    (db/get-user-view historian-db username)))

(defn remove-user-view!
  "Removes a concrete view for the user"
  [^String database ^String username]
  (let [historian-db (newDB @db-spec database)]
    (db/remove-user-view! historian-db database username)))

(defn remove-configuration-watch [^clojure.lang.IRef reference]
  (remove-watch reference :configuration))

(defn cast-namespace [^clojure.lang.Namespace namespace]
  (->
    namespace
    str
    (string/replace #"\." "-")))

(defn override-reference [map ^clojure.lang.IRef reference]
  (swap! reference (fn [old] map)))

(defn add-configuration-watch [^clojure.lang.Namespace namespace
                               ^clojure.lang.IRef reference]
  (add-watch reference
    :configuration (fn [key reference old-state new-state]
                     (update-value (cast-namespace namespace) :configuration new-state))))

(defprotocol DatabaseHandler
  (init [this])
  (destroy [this]))

(defrecord Database [^clojure.lang.Namespace namespace
                     ^clojure.lang.IRef reference]
  DatabaseHandler

  (destroy [this]
    (when (server-is-up? (cast-namespace (:namespace this)))
      (remove-configuration-watch (:reference this))))

  (init [this]
    (when (server-is-up? (cast-namespace (:namespace this)))
      (when (first-time? (cast-namespace (:namespace this)))
        (store (cast-namespace (:namespace this))  ":configuration" @(:reference this)))
      (->
        (get-value (cast-namespace (:namespace this)) ":configuration")
        (dissoc :_id :_rev)
        (override-reference (:reference this)))
      (add-configuration-watch (:namespace this) (:reference this)))))

(defn database [^clojure.lang.Namespace namespace
                ^clojure.lang.IRef reference]
  (Database. namespace reference))
