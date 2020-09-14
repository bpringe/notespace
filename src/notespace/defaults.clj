(ns notespace.defaults
  (:require [notespace.kinds :as kinds]))

(def initial-state
  {;; global configuration:
   :config                {:target-base-path "doc"}
   ;; namespace specific configuration:
   :ns->config            {}
   ;; one way to write a form is using one of several note symbols,
   ;; that have corresponding note kinds.
   ;; E.g., a form of the form (note-md ...) is a note form of kind :md.
   ;; This matching note-symbol->kind is configurable:
   :note-symbol->kind     {}
   ;; A note's kind determintes controls various parameters of its evaluation and rendering.
   :kind->behaviour       kinds/kind->behaviour
   ;; a catalogue of notes, holding a vector of notes per namespace:
   :ns->notes             {}
   ;; for every line of code,
   ;; the index of the corresponding note in the sequence of notes,
   ;; if that line happens to lie inside a note:
   :ns->line->index       {}
   ;; the indices of every note's label appearances in the sequence:
   :ns->label->indices    {}
   ;; the last notespace handled:
   :last-ns-handled       nil
   ;; We keep track of changes in source files corresponding to namespaces.
   :ns->last-modification {}
   ;; We keep track of UI inputs per namespace.
   :inputs {}})
