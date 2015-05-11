(defproject simpledb "0.1.9-SNAPSHOT"
  :description "Simple SQL database access"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [honeysql "0.5.2"]
                 [org.clojure/data.json "0.2.4"]
                 [clj-time "0.9.0"]]

  :plugins [[lein-midje "3.1.3"]
            [lein-pprint "1.1.1"]
            [lein-ancient "0.5.5"]]

  :repl-options {:welcome (println "Welcome to the magical world of the repl!")
                 :port 4004}

  :deploy-repositories [["releases" {:url "http://nexus.dsquare.intra/content/repositories/hps-releases"
                                     :sign-releases false}]
                        ["snapshots" {:url "http://nexus.dsquare.intra/content/repositories/hps-snapshots"
                                      :sign-releases false}]]
  :mirrors {"central" {:name "nexus"
                       :url "http://nexus.dsquare.intra/content/groups/public"}}

  :min-lein-version "2.0.0"

  :profiles {:dev {:dependencies [[midje "1.6.3"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha1"]]}}

  :aliases {"dev" ["do" "test"]})
