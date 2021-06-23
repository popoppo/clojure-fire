(defproject org.clojars.popoppo/clojure-fire "0.0.6"
  :description "Making it easier to run Clojure program"
  :url "https://github.com/popoppo/clojure-fire"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.1"]]

  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]

  :plugins [[lein-exec "0.3.7"]]

  :src-paths ["src"]
  :test-paths ["test"])
