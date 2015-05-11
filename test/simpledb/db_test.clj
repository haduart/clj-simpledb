(ns simpledb.db-test
  (:refer-clojure :exclude [clojure.core/count])
  (:use clojure.test
        midje.sweet)
  (:require [simpledb.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as time-coerce]))

(fact "SimpleDB gets all key-value pairs"
  (let [db (db/newDB {} "test")]
    (seq db) => [["1" {:id "1" :name "foo"}]]
    (provided
      (jdbc/query {} ["SELECT * FROM test"] :identifiers identity) => [{:id "1" :name "foo"}])))

(fact "SimpleDB converts dashes in table names to underscores"
  (let [db (db/newDB {} "te-st")]
    (seq db) => []
    (provided
      (jdbc/query {} ["SELECT * FROM te_st"] :identifiers identity) => [])))

(fact "SimpleDB stores a new key-value pair"
  (db/update-or-insert-value {} :test "1" {:id "1" :name "foo"}) => anything
  (provided
    (jdbc/execute! {} ["UPDATE test SET name = ?, id = ? WHERE id = ?" "foo" "1" "1"]) => [0]
    (jdbc/execute! {} ["INSERT INTO test (name, id) VALUES (?, ?)" "foo" "1"]) => [1]))

(fact "SimpleDB stores values as JSON"
  (db/update-or-insert-value {} :test "1" {:id "1" :names ["a", "b"]}) => anything
  (provided
    (jdbc/execute! {} ["UPDATE test SET names = ?, id = ? WHERE id = ?" "~json~[\"a\",\"b\"]" "1" "1"]) => [1]))

(fact "SimpleDB stores JodaTime as Strings"
  (let [ts "2010-03-20T01:01:01.000Z"]
    (db/update-or-insert-value {} :test "1" {:id "1" :ts (time-coerce/from-string ts)}) => anything
    (provided
      (jdbc/execute! {} ["UPDATE test SET id = ?, ts = ? WHERE id = ?" "1" ts "1"]) => [1])))

(fact "SimpleDB reads values as JSON"
  (let [db (db/newDB {} "test")]
    (get db "1") => {:id "1" :names ["a", "b"]}
    (provided
      (jdbc/query {} ["SELECT * FROM test WHERE id = ?" "1"] :identifiers identity) => [{:id "1" :names "~json~[\"a\",\"b\"]"}])))

(fact "SimpleDB stores dashes in property names as underscores"
  (db/update-or-insert-value {} :test "1" {:id "1" :allow-search true}) => anything
  (provided
    (jdbc/execute! {} ["UPDATE test SET allow_search = TRUE, id = ? WHERE id = ?" "1" "1"]) => [1]))

(fact "SimpleDB reads underscores in field names as dashes"
  (let [db (db/newDB {} "test")]
    (get db "1") => {:id "1" :allow-search true}
    (provided
      (jdbc/query {} ["SELECT * FROM test WHERE id = ?" "1"] :identifiers identity) => [{:id "1" :allow_search true}])))

(fact "SimpleDB gets user views"
  (let [db (db/newDB {} "test")]
    (simpledb.db/get-user-view db "foo") => [{:id "1" :value {:id "1" :username "foo"}}]
    (provided
      (jdbc/query {} ["SELECT * FROM test WHERE username = ?" "foo"] :identifiers identity) => [{:id "1" :username "foo"}])))

