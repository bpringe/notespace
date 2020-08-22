(ns notespace.events
  (:require [cljfx.api :as fx]))

(defmulti handle :event/type)

(defmethod handle :default [event]
  (throw (ex-info "Unrecognized event"
                  {:event event})))

(defmethod handle ::reset [{:keys [fx/context initial-state]}]
  {:context (fx/reset-context
             context
             initial-state)})

(defmethod handle ::on-result [{:keys [fx/context idx value]}]
  {:context (fx/swap-context
             context assoc-in
             [:notes idx :value] value)})

(defmethod handle ::file-modified [{:keys [fx/context namespace modification-time]}]
  {:context (fx/swap-context
             context
             #(-> %
                  (assoc-in
                   [:ns->last-modification namespace] modification-time)
                  (assoc :last-ns-handled namespace)))})

(defmethod handle ::assoc-notes [{:keys [fx/context namespace notes line->index label->indices]}]
  {:context (fx/swap-context
             context
             #(-> %
                  (assoc-in [:ns->notes namespace] notes)
                  (assoc-in [:ns->line->index namespace] line->index)
                  (assoc-in [:ns->label->indices namespace] label->indices)
                  (assoc :last-ns-handled namespace)))})

(defmethod handle ::update-note [{:keys [fx/context namespace idx f]}]
  {:context (fx/swap-context
             context
             #(-> %
                  (update-in [:ns->notes namespace idx]
                             f)
                  (assoc :last-ns-handled namespace)))})

(defmethod handle ::realize-note [{:keys [fx/context idx]}]
  {:context     (fx/swap-context
                 context update-in
                 [:notes idx]
                 #(assoc-in % [:status :stage] :started-realizing))
   :realization {:idx          idx
                 :note         (fx/sub context
                                       (fn [ctx]
                                         (-> ctx
                                             (fx/sub :notes)
                                             (get idx))))
                 :on-result    {:event/type ::on-result}
                 :on-exception {:event/type ::on-exception}}})
