(ns cljs-parsers.utils.file-reader
  (:use-macros
    [purnam.core :only [? ! !>]])
  (:require
    [cljs.core.async :refer [put! chan]]))

(defn callback [c event result]
  (put! c [event result]))

(defn read-file [{:keys [file type c]}]
  (let [reader (js/FileReader.)]
    (! reader.onload (partial callback c :loaded))
    (! reader.onloadstart (partial callback c :load-start))
    (! reader.onloadend (partial callback c :load-end))
    (! reader.onprogress (partial callback c :progress))
    (! reader.onabort (partial callback c :abort))
    (! reader.onerror (partial callback c :error))
    (case type
      :txt (!> reader.readAsText file)
      :arr (!> reader.readAsArrayBuffer file)
      :bin (!> reader.readAsBinaryString file)
      :data (!> reader.readAsDataURL file))
    c))