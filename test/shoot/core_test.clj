(ns shoot.core-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [are deftest testing]]
   [shoot.core :refer [shoot default-parsers]]))

(def ^:private this-ns *ns*)

(defn hello
  []
  "Hello")

(defn hello-world
  [& {:keys [name]
      :or {name "World"}}]
  (str "Hello " name))

(defn add
  [x y & vs]
  (apply + (concat [x y] vs)))

(defn volume
  [{:keys [x y z]}]
  (* x y z))

(defn is-that-true?
  [v]
  (boolean v))

(deftest shoot-test
  (binding [*ns* this-ns]
    (testing "With no args"
      (are [orig args] (= orig
                          (do
                            (set! *command-line-args* args)
                            (shoot)))
        (hello) ["dummy-cmd" "hello"]
        (hello-world) ["dummy-cmd" "hello-world"]
        (hello-world :name "Clojure") ["dummy-cmd" "hello-world" ":name" "Clojure"]
        (add 1 2) ["dummy-cmd" "add" "1" "2"]
        (add 1 2 3 4 5) ["dummy-cmd" "add" "1" "2" "3" "4" "5"]
        (volume {:x 2 :y 3 :z 4}) ["dummy-cmd" "volume" "{:x 2 :y 3 :z 4}"]
        (is-that-true? true) ["dummy-cmd" "is-that-true?" "true"]
        (is-that-true? false) ["dummy-cmd" "is-that-true?" "false"]
        ;; edn/read-string is used to parse args and
        ;; it parses "False" as string.
        ;; As a result, (boolean "False") returns true.
        (is-that-true? true) ["dummy-cmd" "is-that-true?" "False"]))

    (testing "With function name"
      (are [orig func args] (= orig
                               (do
                                 (set! *command-line-args* args)
                                 (shoot func)))
        (hello) 'hello ["dummy-cmd"]  ;; (shoot 'hello)
        (hello-world) "hello-world" ["dummy-cmd"] ;; str and keyword are also allowed
        (hello-world :name "Clojure") :hello-world ["dummy-cmd" ":name" "Clojure"]
        (add 1 2) 'add ["dummy-cmd" "1" "2"]
        (add 1 2 3 4 5) 'add ["dummy-cmd" "1" "2" "3" "4" "5"]
        (volume {:x 2 :y 3 :z 4}) 'volume ["dummy-cmd" "{:x 2 :y 3 :z 4}"]
        (is-that-true? true) 'is-that-true? ["dummy-cmd" "true"]
        (is-that-true? false) 'is-that-true? ["dummy-cmd" "false"]
        (is-that-true? true) 'is-that-true? ["dummy-cmd" "False"]))

    (testing "With options"
      (are [orig opts args] (= orig
                               (do
                                 (set! *command-line-args* args)
                                 (shoot opts)))
        (hello) {:func 'hello} ["dummy-cmd"]  ;; (shoot 'hello)
        (hello-world) {:func 'hello-world} ["dummy-cmd"]
        (hello-world :name "Clojure") {:func 'hello-world} ["dummy-cmd" ":name" "Clojure"]
        (add 1 2) {:func 'add} ["dummy-cmd" "1" "2"]
        (add 1 2 3 4 5) {:func 'add} ["dummy-cmd" "1" "2" "3" "4" "5"]
        (volume {:x 2 :y 3 :z 4}) {:func 'volume} ["dummy-cmd" "{:x 2 :y 3 :z 4}"]
        (is-that-true? true) {:func 'is-that-true?} ["dummy-cmd" "true"]
        (is-that-true? false) {:func 'is-that-true?} ["dummy-cmd" "false"]
        (is-that-true? true) {:func 'is-that-true?} ["dummy-cmd" "False"]))

    (testing "With custom parsers"
      (let [enhanced-parsers (concat
                              [[#"(?i)false" (fn [_] false)]]
                              default-parsers)
            wrong-order-parsers (concat
                                 default-parsers  ;; this will catch all
                                 [[#"(?i)false" (fn [_] false)]])
            no-parsers []]
        (testing "via args"
          (are [orig parsers args] (= orig
                                      (do
                                        (set! *command-line-args* args)
                                        (shoot 'is-that-true? parsers)))
            (is-that-true? false) enhanced-parsers ["dummy-cmd" "False"]
            (is-that-true? true) wrong-order-parsers ["dummy-cmd" "False"]
            ;; "false" is passed as string without parsers.
            (is-that-true? true) no-parsers ["dummy-cmd" "false"]))
        (testing "via opts"
          (are [orig parsers args] (= orig
                                      (do
                                        (set! *command-line-args* args)
                                        (shoot {:func 'is-that-true?
                                                :parsers parsers})))
            (is-that-true? false) enhanced-parsers ["dummy-cmd" "False"]
            (is-that-true? true) wrong-order-parsers ["dummy-cmd" "False"]
            (is-that-true? true) no-parsers ["dummy-cmd" "false"]))))

    (testing "Additional combinations"
      (are [orig opts args] (= orig
                               (do
                                 (set! *command-line-args* args)
                                 (shoot opts)))
        ;; With no parsers, :name is passed as plain string, not keyword.
        "Hello World" {:parsers []} ["dummy-cmd" "hello-world" ":name" "{:k 1}"]
        ;; Only #":.+" is parsed as keyword, so "{...}" is handled as string
        "Hello {:k 1}" {:parsers [[#":.+" #(edn/read-string %)]]} ["dummy-cmd" "hello-world" ":name" "{:k 1}"]))))
