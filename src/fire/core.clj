(ns fire.core
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def default-parsers
  [[#".+" #(edn/read-string %)]])

(defn- transform
  [pattern-list v]
  (loop [lst pattern-list]
    (cond
      (not (seq lst)) v ;; return string
      :else (let [[ptrn f] (first lst)]
              (if (re-matches ptrn v)
                (try
                  (f v) ;; transform
                  (catch Exception e
                    (comment (println (.getMessage e)))
                    v)) ;; return str if transform fails
                (recur (rest lst)))))))

(defn cl-args-without-file-name
  "As 'lein exec' keeps target file name in *command-line-args*,
  first item in *command-line-args* has to be dropped"
  []
  (if (System/getenv "LEIN_JAVA_CMD")
    (rest *command-line-args*)  ;; lein
    *command-line-args*))

(defn parse-value
  [s]
  {:pre [(string? s)]}
  (condp #(re-matches %1 %2) s
    #"-([a-zA-Z])" :>> #(hash-map (-> % second keyword) [])
    #"--?([a-zA-Z][-\w]*)=(.+)" :>> #(hash-map (-> % second keyword) [(nth % 2)])
    #"--?([a-zA-Z][-\w]*)" :>> #(hash-map (-> % second keyword) [])
    {:* [s]}))

(defn parse-arg-list
  [arg-list]
  (reduce
   (fn [acc v]
     (->> (parse-value v)
          (merge-with into acc)))
   {}
   arg-list))

(defn parse-command-line-args
  [args-has-func-name? & {:keys [custom-parsers]
                          :or {custom-parsers default-parsers}}]
  (let [cl-args (cl-args-without-file-name)
        opts (parse-arg-list (if args-has-func-name?
                               (rest cl-args)
                               cl-args))
        fn-symbol (when args-has-func-name?
                    (symbol (first cl-args)))
        tf #(transform custom-parsers %)
        tf-for-map #(let [[k vs] %
                          new-vs (map (fn [v] (tf v)) vs)]
                      (hash-map k (condp = (count new-vs)
                                    0 nil
                                    1 (first new-vs)
                                    new-vs)))
        positional-args (map tf (:* opts))
        options (->> (dissoc opts :*)
                     (map tf-for-map)
                     (apply merge {}))] ;; {} is to avoid nil
    {:fn fn-symbol
     :positional-args positional-args
     :options options}))

(defn get-publics
  "Returns map of public vars.
  With bb, use babashka.main (-m/--main optsion) or use *ns* (-f options).
  With clj, parse command line args.
  WIth lein-exec, just use *ns*"
  []
  (let [bb-main (System/getProperty "babashka.main")
        cmd (System/getProperty "sun.java.command")
        cmd-list (when cmd (str/split cmd #" +"))
        target-info (cond
                      ;; bb -m
                      bb-main {:ns (symbol bb-main)
                               :main true}

                      ;; bb -f or lein exec
                      (or
                       (System/getProperty "babashka.version")
                       (System/getenv "LEIN_JAVA_CMD"))
                      {:ns *ns* ;; lein
                       :main false}

                      ;; clj -m/--main
                      :else
                      (let [i (count *command-line-args*)
                            run-cmd (drop-last i cmd-list)
                            idx (.indexOf run-cmd (some #{"-m" "--main"} run-cmd))]
                        {:ns (symbol (nth run-cmd (inc idx)))
                         :main true}))]
    (-> (:ns target-info)
        ns-publics
        (dissoc 'fire)
        (cond->
         (:main target-info) (dissoc '-main)))))

(defn print-func-names
  "Print list of fn names and its doc"
  []
  (let [fns-map (get-publics)
        fns-info (keep #(let [m (-> % second meta)]
                          (when (:arglists m)
                            (format "%s %s\n%s"
                                    (:name m)
                                    (:arglists m)
                                    (if-let [d (:doc m)]
                                      (str d "\n")
                                      ""))))
                       fns-map)]
    (println (str/join "\n" fns-info))))

(defn apply-func
  [func-name positional-args options]
  (let [funcs-map (get-publics)
        func-names (keep #(let [m (meta (second %))]
                            (when (:arglists m)
                              (:name m)))
                         funcs-map)
        args (if (seq options)
               (concat positional-args (list options))
               positional-args)]
    (if (some #{func-name} func-names)
      (apply (funcs-map func-name) args)
      (println func-name "not found. Speccify one of" func-names))))

(defn fire*
  "Called by 'fire'"
  ([]
   (let [{:keys [fn positional-args options]} (parse-command-line-args true)]
     (if (every? nil? [positional-args options])
       (println "function name must be specified")
       (apply-func fn positional-args options))))
  ([arg]
   (condp #(%1 %2) arg
     map? (let [parsers (:parsers arg default-parsers)
                cl-has-func-name? (if (:fn arg) false true)
                parsed (parse-command-line-args cl-has-func-name?
                                                :custom-parsers parsers)
                {:keys [fn positional-args options]} parsed
                func-name (if fn fn (symbol (:fn arg)))]
            (if (nil? func-name)
              (println "function name must be specified")
              (apply-func func-name positional-args options)))
     ;; else: arg should be function name
     (let [{:keys [positional-args options]} (parse-command-line-args false)]
       (apply-func (symbol arg) positional-args options))))
  ([func-name conf-map]
   (let [arg-map (-> {:fn func-name}
                     (assoc :parsers (:parsers conf-map default-parsers)))]
     (fire* arg-map))))

(defn fire
  "Entry point"
  [& args]
  (if (every? empty? [args (cl-args-without-file-name)])
    (print-func-names)
    (if (seq args)
      (apply fire* args)
      (fire*))))
