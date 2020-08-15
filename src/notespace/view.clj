(ns notespace.view
  (:require [clojure.string :as string]
            [zprint.core :as zprint]
            [clojure.pprint :as pp]
            [hiccup.core :as hiccup]
            [markdown.core :refer [md-to-html-string]]
            [notespace.util :refer [careful-zprint fresh?]]
            [notespace.repo :as repo]
            [notespace.check :as check]
            [cambium.core :as log]
            [notespace.context :as ctx]))

(defn htmlify-whitespace [s]
  (-> s
      (string/replace #"\n" "</br>")
      (string/replace #" " "&nbsp;")))

(defn code->hiccup [code & {:keys [remove-label?]}]
  [:code {:class "prettyprint lang-clj"}
   (-> code
       (zprint/zprint 72 {:parse-string? true})
       with-out-str
       htmlify-whitespace)])

(defn form->hiccup [form print-fn]
  [:code {:class "prettyprint lang-clj"}
   (-> form
       print-fn
       with-out-str
       (string/split #"\n")
       (->> (filter (complement (partial re-find #"nRepl-session")))
            (string/join "\n"))
       htmlify-whitespace)])

(defn value->hiccup [v]
  [:div {:class "nspout"}
   (cond (fn? v)         ""
         (sequential? v) (case (first v)
                           :hiccup (hiccup/html v)
                           (form->hiccup v pp/pprint))
         :else           (form->hiccup v pp/pprint))])

(defn md->hiccup [md]
  [:div
   (md-to-html-string md)])

(defn mds->hiccup [mds]
  (->> mds
       (map md-to-html-string)
       (into [:div])))

(defn label->anchor-id [label]
  (->> label name))

(defn label->anchor [label]
  [:a  {;; :style "border: 2px solid green;"
        :id (label->anchor-id label)}
   " "])


;; We can render the notes of a namespace to the file.
(defn note-and-state->hiccup [{:keys [forms label kind]
                               :as   anote}
                              {:keys [value rendered]
                               :as   note-state}]
  [:div {:class "nspbox"}
   (when label
     (label->anchor label))
   (when (ctx/sub-get-in :kind->behaviour kind :render-src?)
     (->> (or [(some-> anote
                       :metadata
                       :source
                       (code->hiccup
                        :remove-label? label))]
              (->> (if label
                     (rest forms)
                     forms)
                   (map (fn [form]
                          [:div
                           (-> form
                               (form->hiccup #(careful-zprint % 80)))]))))
          (into [:div {:class "nspin"}])))
   (if (fresh? value)
     rendered
     [:div
      "--"
      [:img {:src "./waiting.gif"}]])])

(defn ->reference [namespace]
  [:div
   [:i
    [:small
     (if-let [url (repo/ns-url namespace)]
       [:a {:href url} namespace]
       namespace)
     " - created by " [:a {:href "https://github.com/scicloj/notespace"}
                       "notespace"] ", " (java.util.Date.) "."]]
   [:hr]])


(defn toc [notes]
  (when-let [labels (->> notes
                         (map :label)
                         (filter some?)
                         seq)]
    [:div
     "Table of contents"
     (->> labels
          (map (fn [label]
                 [:li [:a {:href (->> label
                                      label->anchor-id
                                      (str "#"))}
                       (name label)]]))
          (into [:ul]))
     [:hr]]))

(defn notes-and-states->hiccup [namespace notes note-states]
  (let [checks-freqs   (check/->checks-freqs notes)
        checks-summary (check/->checks-summary checks-freqs)
        reference      (->reference namespace)]
    (when checks-freqs
      (log/info [::checks checks-freqs]))
    [:div
     [:h1 (str namespace)]
     reference
     checks-summary
     (toc notes)
     (map note-and-state->hiccup
          notes
          note-states)
     [:hr]
     checks-summary
     reference]))