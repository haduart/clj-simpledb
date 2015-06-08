(defproject simpledb "0.1.15"
  :description "Simple SQL database access"
  :url "https://github.com/haduart/clj-simpledb"
  :license {:name "BSD"
            :url "http://www.opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [honeysql "0.5.2"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.9.0"]]

  :plugins [[lein-midje "3.1.3"]
            [lein-pprint "1.1.1"]
            [lein-ancient "0.5.5"]]

  :repl-options {:welcome (println "Welcome to the magical world of the repl!")
                 :port 4004}

  :min-lein-version "2.0.0"

  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [mysql/mysql-connector-java "5.1.25"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha1"]]}}

  :aliases {"dev" ["do" "test"]})
