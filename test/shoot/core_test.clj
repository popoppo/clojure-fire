(ns shoot.core-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [are deftest testing]]
   [shoot.core :refer [shoot default-parsers parse-arg-list parse-command-line-args] :as shoot]))

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
  (binding [*ns* 'shoot.core-test]
    (with-redefs [shoot.core/cl-args-without-file-name (fn [] *command-line-args*)]
      (testing "With no args"
        (are [orig args] (= orig
                            (do
                              (set! *command-line-args* args)
                              (shoot)))
          (hello) ["hello"]
          (hello-world) ["hello-world"]
          (hello-world :name "Clojure") ["hello-world" ":name" "Clojure"]
          (add 1 2) ["add" "1" "2"]
          (add 1 2 3 4 5) ["add" "1" "2" "3" "4" "5"]
          (volume {:x 2 :y 3 :z 4}) ["volume" "{:x 2 :y 3 :z 4}"]
          (is-that-true? true) ["is-that-true?" "true"]
          (is-that-true? false) ["is-that-true?" "false"]
          ;; edn/read-string is used to parse args and
          ;; it parses "False" as string.
          ;; As a result, (boolean "False") returns true.
          (is-that-true? true) ["is-that-true?" "False"]))

      (testing "With function name"
        (are [orig func args] (= orig
                                 (do
                                   (set! *command-line-args* args)
                                   (shoot func)))
          (hello) 'hello [] ;; (shoot 'hello)
          (hello-world) "hello-world" [] ;; str and keyword are also allowed
          (hello-world :name "Clojure") :hello-world [":name" "Clojure"]
          (add 1 2) 'add ["1" "2"]
          (add 1 2 3 4 5) 'add ["1" "2" "3" "4" "5"]
          (volume {:x 2 :y 3 :z 4}) 'volume ["{:x 2 :y 3 :z 4}"]
          (is-that-true? true) 'is-that-true? ["true"]
          (is-that-true? false) 'is-that-true? ["false"]
          (is-that-true? true) 'is-that-true? ["False"]))

      (testing "With options"
        (are [orig opts args] (= orig
                                 (do
                                   (set! *command-line-args* args)
                                   (shoot opts)))
          (hello) {:func 'hello} [] ;; (shoot 'hello)
          (hello-world) {:func 'hello-world} []
          (hello-world :name "Clojure") {:func 'hello-world} [":name" "Clojure"]
          (add 1 2) {:func 'add} ["1" "2"]
          (add 1 2 3 4 5) {:func 'add} ["1" "2" "3" "4" "5"]
          (volume {:x 2 :y 3 :z 4}) {:func 'volume} ["{:x 2 :y 3 :z 4}"]
          (is-that-true? true) {:func 'is-that-true?} ["true"]
          (is-that-true? false) {:func 'is-that-true?} ["false"]
          (is-that-true? true) {:func 'is-that-true?} ["False"]))

      (testing "With custom parsers"
        (let [enhanced-parsers (concat
                                [[#"(?i)false" (fn [_] false)]]
                                default-parsers)
              wrong-order-parsers (concat
                                   default-parsers ;; this will catch all
                                   [[#"(?i)false" (fn [_] false)]])
              no-parsers []]
          (testing "via args"
            (are [orig parsers args] (= orig
                                        (do
                                          (set! *command-line-args* args)
                                          (shoot 'is-that-true? parsers)))
              (is-that-true? false) enhanced-parsers ["False"]
              (is-that-true? true) wrong-order-parsers ["False"]
              ;; "false" is passed as string without parsers.
              (is-that-true? true) no-parsers ["false"]))
          (testing "via opts"
            (are [orig parsers args] (= orig
                                        (do
                                          (set! *command-line-args* args)
                                          (shoot {:func 'is-that-true?
                                                  :parsers parsers})))
              (is-that-true? false) enhanced-parsers ["False"]
              (is-that-true? true) wrong-order-parsers ["False"]
              (is-that-true? true) no-parsers ["false"]))))

      (testing "Additional combinations"
        (are [expected opts args] (= expected
                                     (do
                                       (set! *command-line-args* args)
                                       (shoot opts)))
          ;; With no parsers, :name is passed as plain string, not keyword.
          "Hello World" {:parsers []} ["hello-world" ":name" "{:k 1}"]
          ;; Only #":.+" is parsed as keyword, so "{...}" is handled as string
          "Hello {:k 1}" {:parsers [[#":.+" #(edn/read-string %)]]} ["hello-world" ":name" "{:k 1}"])))))

(deftest parse-arg-list-test
  (testing "Positional args"
    (are [expected args] (= expected (parse-arg-list args))
      {} []
      {:* [" "]} [" "] ;; " "
      {:* ["1"]} ["1"]
      {:* ["-1" "--1" "---1"]} ["-1" "--1" "---1"]
      {:* ["\"abc\"" "def"]} ["\"abc\"" "def"]
      {:* ["-_" "-?" "=" "==" "-="]} ["-_" "-?" "=" "==" "-="]
      {:* ["\"abc\""]} ["\"abc\""] ;; "\"abc\"" or '"abc"'
      ))
  (testing "Options without values"
    (are [expected args] (= expected (parse-arg-list args))
      {:a []} ["-a"]
      {:a []} ["--a"]
      {:a [] :b []} ["-a" "-b"]
      {:a [] :b []} ["-a" "-b" "-a"]))
  (testing "Options with values"
    (are [expected args] (= expected (parse-arg-list args))
      {:a ["1"]} ["-a=1"]
      {:a ["--a"]} ["--a=--a"]
      {:a ["=="]} ["--a==="]
      {:a ["-a=-a"]} ["-a=-a=-a"]
      {:a ["1"] :b ["2"]} ["-a=1" "-b=2"]
      {:a ["1" "3"] :b ["2"]} ["-a=1" "-b=2" "-a=3"]
      {:A ["1,2,3"]} ["--A=1,2,3"] ;; will be string
      {:A ["a b c"]} ["--A=a b c"] ;; --A="a b c" will be string
      {:A ["[1 2 3]"]} ["--A=[1 2 3]"] ;; --A="[1 2 3]" will be vec
      {:A ["(1 2 3)"]} ["--A=(1 2 3)"] ;; --A="(1 2 3)" will be list
      {:A [","] :B ["1,"] :C [",,,"]} ["--A=," "--B=1," "--C=,,,"]
      {:a ["   "]} ["--a=   "] ;; --a='   '
      ))
  (testing "Combined"
    (are [expected args] (= expected (parse-arg-list args))
      {:* ["-1"] :a ["1"]} ["-1" "-a=1"]
      {:* ["--1" "-1"] :a ["1"]} ["--1" "-a=1" "-1"])))


(deftest parse-command-line-args-test
  (testing "Cl-args has fn name"
    (with-redefs [shoot/cl-args-without-file-name (fn [] *command-line-args*)]
     (are [expected args] (= expected (do
                                        (set! *command-line-args* args)
                                        (parse-command-line-args false)))
       {:func nil :positional-args '() :options {}} []
       {:func nil :positional-args '(nil nil) :options {}} [" " "   "] ;; white spaces will be nill
       {:func nil :positional-args '("   ") :options {}} ["\"   \""] ;; use '"' to pass spaces
       {:func nil :positional-args '(a) :options {}} ["a"] ;; args will be symbol
       {:func nil :positional-args '(abc "abc") :options {}} ["abc" "\"abc\""] ;; use '"' for str
       {:func nil :positional-args '(-1 --1 ---1) :options {}} ["-1" "--1" "---1"]
       {:func nil :positional-args '(:foo bar) :options {}} [":foo" "bar"]
       {:func nil :positional-args '({:a 1} [1 2 3]) :options {}} ["{:a 1}" "[1 2 3]"]
       {:func nil :positional-args '({:a {:b {:c 1}}}) :options {}} ["{:a {:b {:c 1}}}"]
       {:func nil :positional-args '("},@_@,{") :options {}} ["},@_@,{"]
       {:func nil :positional-args '(---c) :options {:a [] :b []}} ["-a" "--b" "---c"]
       {:func nil :positional-args '() :options {:a 1 :b '(1 2 3) :c '(4 5 6)}} ["-a=1" "-b=[1 2 3]" "-c=(4 5 6)"]
       {:func nil :positional-args '() :options {:a nil :b " "}} ["-a= " "--b=\" \""]
       {:func nil :positional-args '() :options {:a '(1 3) :b '(2 [4 5 6])}} ["-a=1" "--b=2" "--a=3" "-b=[4 5 6]"]))))

(deftest simulating-commands-test
  (testing "simulate bb")
  (testing "simulate clj")
  (testing "simulate lein-exec"))
