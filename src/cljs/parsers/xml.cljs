(ns cljs-parsers.parsers.xml
  (:use-macros
    [purnam.core :only [? ! !> obj]]
    [cljs.core.match.macros :only [match]]
    [cljs.core.async.macros :only [go go-loop]])
  (:require
    [cljs.core.async :refer [>! <! put! chan]]
    [cljs.core.match]
    [clojure.set :refer [rename-keys]]
    [cljs-parsers.utils.collections :refer [flatten-keys]]
    [cljs-parsers.utils.file-reader :as reader]))

(defn select-elements [xml elements]
  ;Return a function that for a given map:
  ;1. Flattens the keys
  ;2. Selects only the keys in elements
  ;3. Rename the selected keys to the names in elements
  (if elements
    (map (comp #(rename-keys % elements) #(select-keys % (keys elements)) flatten-keys) xml)
    xml))

(defn select-path [xml tag-path]
  (if tag-path
    (get-in xml tag-path)
    xml))

(defn parse-xml [xml {:keys [tag-path elements-map]}]
  ;Receive a prased string and select the required tags
  (->
    (js->clj xml :keywordize-keys true)
    (select-path tag-path)
    (select-elements elements-map)))

;Safely parse the xml text using the xmlToJSON library
(defn safe-parse-string [s]
  (try
    [:ok (js/xmlToJSON.parseString s (obj :childrenAsArray false
                                            :attrsAsObject false
                                            :attrKey ""))]
    (catch :default e
      [:error e])))

;parse the data read from the file.
(defn parse-string [s opts]
  (match (safe-parse-string s)
    [:error error] [:error :invalid-xml]
    [:ok data] [:ok (parse-xml data opts)]))

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

(defn _xml?
  "Ensure the requested file is a xml, by checking if the file name ends with .xml"
  [file]
  (->
    (re-pattern "^.+\\.(xml)$")
    (re-find (? file.name))))

(defn parse-file [file opts]
  (go
    (if (_xml? file)
      (<! (_read-file file opts))
      [:error :not-xml])))
