(ns cljs-parsers.parsers.csv
  (:use-macros
    [purnam.core :only [? ! !>]]
    [cljs.core.match.macros :only [match]]
    [cljs.core.async.macros :only [go go-loop]])
  (:require
    [cljs.core.async :refer [>! <! put! chan]]
    [cljs.core.match]
    [clojure.set :refer [rename-keys]]
    [cljs-parsers.utils.file-reader :as reader]
    [cljsjs.csv]))

(defn _parse-data-with-headers [[headers & rows] {:keys [headers-map]}]
  (if headers-map
    ;build maps of the rows, with the headers in the headers-map
    (let [zm (partial zipmap headers) ;build a map with headers as keys and row as values
          ks (keys headers-map)
          sk #(select-keys % ks) ;select only the keys that appear in header-map
          rk #(rename-keys % headers-map) ;rename the keys of the map to those in the headers-map
          c (comp rk sk zm)]
      (map c rows))
    (map (partial zipmap headers) rows)))

(defn _set-parser-option [option value]
  (! js/CSV.|option| value))

(defn _build-parser-options [m [option value]]
  (let [current-value (? js/CSV.|option|)]
    (_set-parser-option option value)
    (assoc m option current-value)))

(defn _safe-parse-string [s]
  (try
    [:ok (js/CSV.parse s)]
    (catch :default e
      [:error e])))

(defn _parse-with-options [s {:keys [parser-options]}]
  ;We set the requested parser options, but remeber the defaults so we can reset them after the parsing
  (let [defaults (reduce _build-parser-options {} parser-options)
       [status data] (_safe-parse-string s)]
    (doseq [d defaults] _set-parser-option)
    [status data]))

(defn parse-string
  "parse the data read from the file.
  The parsed data will be in a nested vector [[row 1][row 2]...[row n]]"
  [s {:keys [headers?] :as opts}]
  (match (_parse-with-options s opts)
    [:error error] [:error :invalid-csv]
    [:ok data]
      (if headers?
        [:ok (_parse-data-with-headers data opts)]
        [:ok data])))

(defn _read-file
  "Call the file reader to read the data from the file"
  [file opts]
  (go
    (let [transducer (filter (comp #{:loaded :error :abort} first))
          in-chan (chan 1 transducer)]
      (reader/read-file {:file file :type :txt :c in-chan})
      (match (<! in-chan)
        [:loaded result] (parse-string (? result.target.result) opts)
        [:error error] [:error error]
        [:abort reason] [:abort reason]))))

(defn _csv?
  "Ensure the requested file is a csv, by checking if the file name ends with .csv"
  [file]
  (->
    (re-pattern "^.+\\.(csv)$")
    (re-find (? file.name))))

(defn parse-file [file opts]
  (let [c (chan)]
    (if (_csv? file)
      (put! c (<! (_read-file file opts)))
      (put! c [:error :not-csv]))
    c))