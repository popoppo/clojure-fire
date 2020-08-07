(ns shoot.core
  (:require
   [clojure.edn :as edn]
   [flatland.useful.cli :refer [parse-opts]]))

(def default-parsers
  [[#".+" #(clojure.edn/read-string %)]])

(defn apply-func
  [func-name positional-args options]
  (let [funcs-map (dissoc (ns-publics *ns*) 'shoot)
        func-names (keep #(let [m (meta (second %))]
                            (when (:arglists m)
                              (:name m)))
                         funcs-map)
        args (if (seq options)
               (concat positional-args
                       (list options))
               positional-args)]
    (if (some #{func-name} func-names)
      (apply (funcs-map func-name) args)
      (println func-name "not found. expected one of" func-names))))

(defn transform
  [pattern-list v]
  (loop [lst pattern-list]
    (cond
      (not (seq lst)) v ;; return string
      :else (let [[ptrn f] (first lst)]
              (if (re-matches ptrn v)
                (try
                  (f v) ;; transform
                  (catch Exception ;; e (prn (.getMessage e))
                    v)) ;; return str if transform fails
                (recur (rest lst)))))))

(defn parse-command-line-args
  [args-has-func-name? & {:keys [custom-pattern-list]
                          :or {custom-pattern-list default-parsers}}]
  (let [opts (parse-opts :pos (rest *command-line-args*)) ;; drop cmd
        args (:pos opts)
        f-symbol (when args-has-func-name?
                   (symbol (first args)))
        tf #(transform custom-pattern-list %)
        tf-for-map #(let [[k vs] %]
                      (hash-map k (map (fn [v] (tf v)) vs)))
        positional-args (map tf (if args-has-func-name?
                                  (rest args)
                                  args))
        options (map tf-for-map (dissoc opts :pos))]
    {:func f-symbol
     :positional-args positional-args
     :options options}))

(defn shoot
  "This function is the main entrypoint."
  ([]
   (let [{:keys [func positional-args options]} (parse-command-line-args true)]
     (apply-func func positional-args options)))
  ([args]
   (condp #(%1 %2) args
     map? (let [custom-pattern-list (or (:parsers args) default-parsers)
                cl-has-func-name? (if (:func args) false true)
                parsed (parse-command-line-args cl-has-func-name?
                                                :custom-pattern-list custom-pattern-list)
                {:keys [func positional-args options]} parsed
                func-name (if func func (symbol (:func args)))]
            (if (not func-name)
              (println "function-name must be specified")
              (apply-func func-name positional-args options)))
     (let [{:keys [positional-args options]} (parse-command-line-args false)]
       (apply-func (symbol args) positional-args options))))
  ([func-name parsers]
   (shoot {:func func-name :parsers parsers})))
