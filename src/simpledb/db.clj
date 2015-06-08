(ns simpledb.db
  (:refer-clojure :exclude [dissoc! assoc!])
  (:require [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all])
  (:import [org.joda.time DateTime]))

(defn- with-result-meta
  [db result]
  (vary-meta db assoc :result result))

;TODO: delete & extract
(defprotocol SimpleViewsOps
  "Defines side-effecting operations on a CouchDB database.
  It extends clutch with some extra methods."
  (create-view! [this design-name view-name javascript-function] "Creates View")
  (get-view [this design-name view-name] "Returns a lazy-seq on the couchdb view")
  (create-user-view! [this username javascript-function] "The design name it will in be the database name with '-' and username. The same for the view name. Creates a concrete view for the user")
  (get-user-view [this username] "The design name it will be the database name with '-' and username. The same for the view name. Returns a lazy-seq on the couchdb view.")
  (remove-user-view! [this database username] "Removes user View")
  (remove-view! [this design-name view-name] "Removes View"))

(defprotocol SimpleOps
  "Defines side-effecting operations on a CouchDB database.
   All operations return the CouchDB database reference —
   with the return value from the underlying clutch function
   added to its :result metadata — for easy threading and
   reduction usage."
  (create! [this] "Ensures that the database exists, and returns the database's meta info.")
  (assoc!
    [this document]
    [this id document]
    "PUTs a document into CouchDB. Equivalent to (conj! couch [id document]).")
  (dissoc! [this id-or-doc]
    "DELETEs a document from CouchDB. Uses a given document map's :_id and :_rev
     if provided; alternatively, if passed a string, will blindly attempt to
     delete the current revision of the corresponding document."))

(defprotocol SimpleExtOps
  "Create / Delete views.
  It extends clutch with some extra methods."
  (drop! [this] "DELETES a database. It returns {:ok true} if it works")
  (up? [this] "Checks if the server is up")                 ;TODO: delete & extract
  (exists? [this] "Checks if the database exists")
  (take-all [this] "Returns all the items of a database"))

(defn- apply-to-values [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn- apply-to-keys [m f]
  (into {} (for [[k v] m] [(f k) v])))

(defn- encode-value [v]
  (cond
    (coll? v) (str "~json~" (json/write-str v))
    (instance? DateTime v) (str v)
    :else v))

(defn- decode-value [v]
  (if (and (string? v) (.startsWith v "~json~"))
    (json/read-str (.substring v 6))
    v))

(defn- convert-underscores [v]
  (-> v
      name
      (clojure.string/replace "_" "-")
      keyword))

(defn- encode-keys [m]
  (apply-to-values m encode-value))

(defn- decode-keys [m]
  (-> m
      (apply-to-values decode-value)
      (apply-to-keys convert-underscores)))

(defn get-everything [db-spec table]
  (map decode-keys (jdbc/query db-spec (->
                                         (select :*)
                                         (from table)
                                         sql/format) :identifiers identity)))

(defn get-values [db-spec table id]
  (map decode-keys (jdbc/query db-spec (->
                                         (select :*)
                                         (from table)
                                         (where [:= :id id])
                                         sql/format) :identifiers identity)))

(defn count-all-values [db-spec table]
  (jdbc/query db-spec (->
                        (select :%count.id)
                        (from table)
                        sql/format) :identifiers identity))

(defn- update-value [db-spec table id value]
  (jdbc/execute! db-spec (->
                           (update table)
                           (sset value)
                           (where [:= :id id])
                           sql/format)))

(defn- insert-value [db-spec table id value]
  (jdbc/execute! db-spec (->
                           (insert-into table)
                           (values [value])
                           sql/format)))

(defn update-or-insert-value [trans-conn table id value]
  (let [id-value (-> value
                     (assoc :id id)
                     (encode-keys))
        updated? (> (first (update-value trans-conn table id id-value)) 0)]
    (when-not updated?
      (insert-value trans-conn table id id-value))))

(defn delete-value [db-spec table id]
  (jdbc/execute! db-spec (->
                           (delete-from table)
                           (where [:= :id id])
                           sql/format)))

(defn clean-table [db-spec table]
  (jdbc/execute! db-spec (->
                           (delete-from table)
                           sql/format)))

;TODO: delete & extract
(defn get-everything-user [db-spec table username]
  (map decode-keys (jdbc/query db-spec (->
                                         (select :*)
                                         (from table)
                                         (where [:= :username username])
                                         sql/format) :identifiers identity)))

(deftype SimpleDB [db-spec table meta]
  clojure.lang.Counted
  (count [this]
    (get (first (count-all-values db-spec table)) (keyword "count(id)")))

  clojure.lang.Seqable
  (seq [this]
    (->>
      (get-everything db-spec table)
      (map (fn [doc] [(:id doc) doc]))))

  clojure.lang.ILookup
  (valAt [this id] (first (get-values db-spec table id)))
  (valAt [this id default] (or (.valAt this id) default))

  clojure.lang.IFn
  (invoke [this key] (.valAt this key))
  (invoke [this key default] (.valAt this key default))

  clojure.lang.IMeta
  (meta [this] meta)

  clojure.lang.IObj
  (withMeta [this meta] (SimpleDB. db-spec table meta))

  SimpleOps
  (create! [this] this)
  (assoc! [this value]
    (do
      (jdbc/with-db-transaction [trans-conn db-spec]
                                (->>
                                  value
                                  encode-keys
                                  (insert-value trans-conn table nil)))
      (with-result-meta this value)))
  (assoc! [this id value]
    (do
      (jdbc/with-db-transaction [trans-conn db-spec]
                                (update-or-insert-value trans-conn table id value))
      (with-result-meta this
                        (assoc value :id id))))

  (dissoc! [this id-or-doc]
    (let [id (if (map? id-or-doc)
               (:id id-or-doc)
               id-or-doc)]
      (delete-value db-spec table id)))

  SimpleExtOps
  (drop! [this] (clean-table db-spec table))
  (up? [this] true)                                         ;TODO: delete & extract
  (exists? [this] true)                                     ;TODO: implement
  (take-all [this] (take (count this) this))

  ;TODO: delete & extract
  SimpleViewsOps
  (create-view! [this design-name view-name javascript-function]
    nil)
  (get-view [this design-name view-name]
    nil)
  (remove-view! [this design-view view-name]
    nil)
  (remove-user-view! [this database username]
    nil)
  (create-user-view! [this username javascript-function]
    nil)
  (get-user-view [this username]
    (->>
      (get-everything-user db-spec table username)
      (map (fn [doc] {:id (:id doc) :value doc})))))

(defn newDB
  ([db-spec table]
   (->SimpleDB db-spec (keyword table) nil))
  ([db-spec table meta]
   (->SimpleDB db-spec (keyword table) meta)))
