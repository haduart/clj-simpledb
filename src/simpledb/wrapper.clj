(ns simpledb.wrapper
  (:use [simpledb.db :only (newDB drop! up? exists? create!)])
  (:require [simpledb.db :as db]
            [clojure.string :as string]
            [clojure.java.jdbc :as jdbc])
  (:import [clojure.lang Keyword]))

;TODO: delete & extract
(def dev-db {:classname   "com.mysql.jdbc.Driver"
             :subprotocol "mysql"
             :subname     "//localhost:3306/hps"
             :user        "root"
             :password    ""})
;TODO: delete & extract
(def prd-db {:name "java:/jboss/datasources/dsq-hps-ds"})

;TODO: delete
(defn temporary-jndi-check
  "Checks if it can use JNDI otherwise it uses a hardcoded connection"
  []
  (try
    (do
      (-> (jdbc/get-connection prd-db)
        (.close))
      prd-db)
    (catch Exception e dev-db)))

(defn count-db [historianDB]
  (count historianDB))

(defn server-is-up? [^String database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (up? historianDB)))

(defn database-exists? [database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (exists? historianDB)))

(defn create-db [^String database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (if-not (exists? historianDB)
      (db/create! historianDB))))

(defn drop-db [^String database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (drop! historianDB)))

(defn first-time? [^String database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (if-not (exists? historianDB)
      (do
        (db/create! historianDB)
        true)
      false)))

(defn store
  ([^String database key map]
   (let [historianDB (newDB (temporary-jndi-check) database)]
     (db/assoc! historianDB key map)))
  ([^String database map]
   (let [historianDB (newDB (temporary-jndi-check) database)]
     (db/assoc! historianDB map))))

(defn remove-value [^String database key]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (db/dissoc! historianDB key)))

(defn take-all [^String database]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (db/take-all historianDB)))

(defn get-value [^String database ^Keyword key]
  (let [historianDB (newDB (temporary-jndi-check) database)]
    (get historianDB key)))

(defn update-value [^String database ^Keyword key map]
  (->
    (newDB (temporary-jndi-check) database)
    (db/assoc! key map)))

;TODO: delete
(defn create-view!
  [^String database ^String design-name ^String view-name ^String javascript-function]
  (let [historian-db (newDB (temporary-jndi-check) database)]
    (db/create-view! historian-db design-name view-name javascript-function)))

;TODO: delete
(defn get-view
  [^String database ^String design-name ^String view-name]
  (db/get-view database design-name (keyword view-name)))

;TODO: delete
(defn create-user-view!
  [^String database ^String username ^String javascript-function]
  (let [historian-db (newDB (temporary-jndi-check) database)]
    (db/create-user-view! historian-db username javascript-function)))

;TODO: delete
(defn get-user-view
  [^String database ^String username]
  (let [historian-db (newDB (temporary-jndi-check) database)]
    (db/get-user-view historian-db username)))

;TODO: delete
(defn remove-user-view!
  [^String database ^String username]
  (let [historian-db (newDB (temporary-jndi-check) database)]
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
                     (update-value (cast-namespace namespace) ":configuration" (dissoc new-state :_id :_rev)))))

;TODO: delete & extract
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
        (store (cast-namespace (:namespace this)) ":configuration" @(:reference this)))
      (->
        (get-value (cast-namespace (:namespace this)) ":configuration")
        (dissoc :_id :_rev)
        (override-reference (:reference this)))
      (add-configuration-watch (:namespace this) (:reference this)))))

(defn database [^clojure.lang.Namespace namespace
                ^clojure.lang.IRef reference]
  (Database. namespace reference))
