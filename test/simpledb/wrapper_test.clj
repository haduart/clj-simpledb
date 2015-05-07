(ns simpledb.wrapper-test
  (:refer-clojure :exclude [clojure.core/count])
  (:use clojure.test
        midje.sweet)
  (:require [simpledb.wrapper :as couchdb]
            [simpledb.db :as db])
  (:import [java.lang IllegalStateException]
           [java.util Date]
           [java.net ConnectException]))

(def currentNamespace (str *ns*))

(fact "Store default configuration for the historian"
  (couchdb/store currentNamespace :configuration {"username" "Daniil" "password" "password" "server" "pi-connector-test.dsquare.intra"}) => anything
  (provided (db/newDB anything currentNamespace) => "mock")
  (provided
    (db/assoc! "mock" :configuration {"username" "Daniil" "password" "password" "server" "pi-connector-test.dsquare.intra"})
    => {:result {:ok true, :id ":configuration", :rev "2-16870bc1dc06755b895ba715da9041ce"}}))

(fact "Get values for the configuration"
  (couchdb/get-value currentNamespace :configuration ) => {:_id ":configuration"
                                                           :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                           :server "pi-connector-test.dsquare.intra"
                                                           :username "Daniil"
                                                           :password "password"}
  (provided (db/newDB anything currentNamespace) => {:configuration {:_id ":configuration"
                                                                         :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                                         :server "pi-connector-test.dsquare.intra"
                                                                         :username "Daniil"
                                                                         :password "password"}}))

(fact "update configuration"
  (couchdb/update-value currentNamespace :configuration {:username "edu" :password "edu-passowrd" :server "pi-connector-test.dsquare.intra"}) => anything

  (provided (db/newDB anything currentNamespace) => {:configuration {:_id ":configuration"
                                                                         :_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                                                         :server "pi-connector-test.dsquare.intra"
                                                                         :username "Daniil"
                                                                         :password "password"}})
  (provided (db/assoc!
              anything :configuration {:_rev "3-887ec7ea147165566a5ac5948eb7c383"
                                       :server "pi-connector-test.dsquare.intra"
                                       :username "edu", :password "edu-passowrd"}) => anything))

(fact "update configuration when the key does not exist"
  (couchdb/update-value currentNamespace :configuration {:username "edu"}) => anything

  (provided (db/newDB anything currentNamespace) => {})
  (provided (db/assoc!
              anything :configuration {:username "edu"}) => anything))


(def testAtom (atom {:username "Daniil"
                     :password "password"
                     :server "pi-connector-test.dsquare.intra"}))

(def currentNamespace *ns*)
(def db (couchdb/database currentNamespace testAtom))


(fact "if the server is down we just return. Everything is going to be in memory"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "simpledb-wrapper-test") => false))

(fact "If the server is up we check if it's the first time"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "simpledb-wrapper-test") => true)
  (provided (couchdb/first-time? "simpledb-wrapper-test") => false)
  (provided (couchdb/get-value "simpledb-wrapper-test" :configuration )
    => {:_id ":configuration"
        :_rev "3-887ec7ea147165566a5ac5948eb7c383"
        :server "pi-connector-test.dsquare.intra"
        :username "Daniil"
        :password "password"})
  (provided (couchdb/override-reference {:server "pi-connector-test.dsquare.intra"
                                         :username "Daniil"
                                         :password "password"} testAtom) => anything)
  (provided (couchdb/add-configuration-watch currentNamespace testAtom) => anything))

(fact "If it's the first time we story the default values"
  (couchdb/init db) => anything
  (provided (couchdb/server-is-up? "simpledb-wrapper-test") => true)
  (provided (couchdb/first-time? "simpledb-wrapper-test") => true)
  (provided (couchdb/store "simpledb-wrapper-test" :configuration {:username "Daniil" :password "password" :server "pi-connector-test.dsquare.intra"}) => anything)
  (provided (couchdb/get-value "simpledb-wrapper-test" :configuration )
    => {:_id ":configuration", :_rev "3-887ec7ea147165566a5ac5948eb7c383", :server "pi-connector-test.dsquare.intra", :username "Daniil", :password "password"})
  (provided (couchdb/override-reference {:server "pi-connector-test.dsquare.intra"
                                         :username "Daniil"
                                         :password "password"} testAtom) => anything)
  (provided (couchdb/add-configuration-watch currentNamespace testAtom) => anything))

(fact "if the server is up remove the watch when destroying it"
  (couchdb/destroy db) => anything
  (provided (couchdb/server-is-up? "simpledb-wrapper-test") => true)
  (provided (couchdb/remove-configuration-watch testAtom) => anything))
