(ns mogul.server
  (:use [hiccup core page-helpers])
  (:require (noir [core :as noir]
                  [server :as server])
            (hiccup [core :as hiccup]
                    [page-helpers :as helper])))

(noir/defpage "/" []
  (helper/html5
   [:head
    [:title "Browser REPL"]
    (helper/include-css "/main.css")]
   [:body
    [:div#content
     (helper/include-js "/js/base.js")
     (helper/include-js "/js/alea.js")
     (helper/include-js "/js/main.js")
     [:script {:type "text/javascript"} "goog.require('mogul.repl')"]]
    [:canvas#foo]]))

(defn server []
  (server/start 8080))

(defn -main [& args]
  (server))
