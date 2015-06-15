(ns cljs-parsers.utils.collections
  (:require [clojure.set]
            [clojure.string :as string]))

(defn flatten-keys* [a ks m]
  (if (map? m)
    (if (seq m)
      (reduce into (map (fn [[k v]] (flatten-keys* a (conj ks k) v)) (seq m)))
      {})
    (assoc a ks m)))

(defn flatten-keys "Thanks to [Jay Fields](http://blog.jayfields.com/2010/09/clojure-flatten-keys.html)"
  [m] (flatten-keys* {} [] m))