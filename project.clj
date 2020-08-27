(defproject shoot "0.0.2-SNAPSHOT"
  :description "Easy function dipatcher"
  :url "https://github.com/popoppo/shoot"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.1"]]

  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                  :username :env/clojars_username
                                  :password :env/clojars_password
                                  :sign-releases false}]]

  :repl-options {:init-ns shoot.core}

  :plugins [[cider/cider-nrepl "0.25.2"]
            [lein-exec "0.3.7"]
            [lein-pprint "1.1.1"]
            [refactor-nrepl "2.5.0"]]

  :src-paths ["src"]
  :test-paths ["test"])
