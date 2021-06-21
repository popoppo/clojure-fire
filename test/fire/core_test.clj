(ns fire.core-test
  (:require
    [clojure.edn :as edn]
    [clojure.test :refer [are deftest testing]]
    [fire.core :as fire :refer [fire]]))

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

(deftest fire-test
  (binding [*ns* 'fire.core-test]
    (with-redefs [fire/cl-args-without-file-name (fn [] *command-line-args*)]
      (testing "With no args"
        (are [orig args] (= orig
                            (do
                              (set! *command-line-args* args)
                              (fire)))
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
                                   (fire func)))
          (hello) 'hello [] ;; (fire 'hello)
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
                                   (fire opts)))
          (hello) {:fn 'hello} [] ;; (fire 'hello)
          (hello-world) {:fn 'hello-world} []
          (hello-world :name "Clojure") {:fn 'hello-world} [":name" "Clojure"]
          (add 1 2) {:fn 'add} ["1" "2"]
          (add 1 2 3 4 5) {:fn 'add} ["1" "2" "3" "4" "5"]
          (volume {:x 2 :y 3 :z 4}) {:fn 'volume} ["{:x 2 :y 3 :z 4}"]
          (is-that-true? true) {:fn 'is-that-true?} ["true"]
          (is-that-true? false) {:fn 'is-that-true?} ["false"]
          (is-that-true? true) {:fn 'is-that-true?} ["False"]))

      (testing "With custom parsers"
        (let [enhanced-parsers (concat
                                 [[#"(?i)false" (fn [_] false)]]
                                 fire/default-parsers)
              wrong-order-parsers (concat
                                    fire/default-parsers ;; this will catch all
                                    [[#"(?i)false" (fn [_] false)]])
              no-parsers []]
          (testing "via args"
            (are [orig parsers args] (= orig
                                        (do
                                          (set! *command-line-args* args)
                                          (fire 'is-that-true? parsers)))
              (is-that-true? false) enhanced-parsers ["False"]
              (is-that-true? true) wrong-order-parsers ["False"]
              ;; "false" is passed as string without parsers.
              (is-that-true? true) no-parsers ["false"]))
          (testing "via opts"
            (are [orig parsers args] (= orig
                                        (do
                                          (set! *command-line-args* args)
                                          (fire {:fn 'is-that-true?
                                                 :parsers parsers})))
              (is-that-true? false) enhanced-parsers ["False"]
              (is-that-true? true) wrong-order-parsers ["False"]
              (is-that-true? true) no-parsers ["false"]))))

      (testing "Additional combinations"
        (are [expected opts args] (= expected
                                     (do
                                       (set! *command-line-args* args)
                                       (fire opts)))
          ;; With no parsers, :name is passed as plain string, not keyword.
          "Hello World" {:parsers []} ["hello-world" ":name" "{:k 1}"]
          ;; Only #":.+" is parsed as keyword, so "{...}" is handled as string
          "Hello {:k 1}" {:parsers [[#":.+" #(edn/read-string %)]]} ["hello-world" ":name" "{:k 1}"])))))

(deftest parse-arg-list-test
  (testing "Positional args"
    (are [expected args] (= expected (fire/parse-arg-list args))
      {} []
      {:* [" "]} [" "] ;; " "
      {:* ["1"]} ["1"]
      {:* ["-1" "--1" "---1"]} ["-1" "--1" "---1"]
      {:* ["\"abc\"" "def"]} ["\"abc\"" "def"]
      {:* ["-_" "-?" "=" "==" "-="]} ["-_" "-?" "=" "==" "-="]
      {:* ["\"abc\""]} ["\"abc\""] ;; "\"abc\"" or '"abc"'
      ))
  (testing "Options without values"
    (are [expected args] (= expected (fire/parse-arg-list args))
      {:a []} ["-a"]
      {:a []} ["--a"]
      {:a [] :b []} ["-a" "-b"]
      {:a [] :b []} ["-a" "-b" "-a"]))
  (testing "Options with values"
    (are [expected args] (= expected (fire/parse-arg-list args))
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
    (are [expected args] (= expected (fire/parse-arg-list args))
      {:* ["-1"] :a ["1"]} ["-1" "-a=1"]
      {:* ["--1" "-1"] :a ["1"]} ["--1" "-a=1" "-1"])))

(deftest parse-command-line-args-test
  (testing "Cl-args has fn name"
    (with-redefs [fire/cl-args-without-file-name (fn [] *command-line-args*)]
      (are [expected args] (= expected (do
                                         (set! *command-line-args* args)
                                         (fire/parse-command-line-args false)))
        {:fn nil :positional-args '() :options {}} []
        {:fn nil :positional-args '(nil nil) :options {}} [" " "   "] ;; white spaces will be nill
        {:fn nil :positional-args '("   ") :options {}} ["\"   \""] ;; use '"' to pass spaces
        {:fn nil :positional-args '(a) :options {}} ["a"] ;; args will be symbol
        {:fn nil :positional-args '(abc "abc") :options {}} ["abc" "\"abc\""] ;; use '"' for str
        {:fn nil :positional-args '(-1 --1 ---1) :options {}} ["-1" "--1" "---1"]
        {:fn nil :positional-args '(:foo bar) :options {}} [":foo" "bar"]
        {:fn nil :positional-args '({:a 1} [1 2 3]) :options {}} ["{:a 1}" "[1 2 3]"]
        {:fn nil :positional-args '({:a {:b {:c 1}}}) :options {}} ["{:a {:b {:c 1}}}"]
        {:fn nil :positional-args '("},@_@,{") :options {}} ["},@_@,{"]
        {:fn nil :positional-args '(---c) :options {:a nil :b nil}} ["-a" "--b" "---c"]
        {:fn nil :positional-args '() :options {:a 1 :b '(1 2 3) :c '(4 5 6)}} ["-a=1" "-b=[1 2 3]" "-c=(4 5 6)"]
        {:fn nil :positional-args '() :options {:a nil :b " "}} ["-a= " "--b=\" \""]
        {:fn nil :positional-args '() :options {:a '(1 3) :b '(2 [4 5 6])}} ["-a=1" "--b=2" "--a=3" "-b=[4 5 6]"]))))

