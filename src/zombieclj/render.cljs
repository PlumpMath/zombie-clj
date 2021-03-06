(ns zombieclj.render
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(q/defcomponent Cell [tile player-moves]
  (d/div {:className "cell"
          :onClick #(put! player-moves (:id tile))}
         (d/div {:className (str "tile"
                                 (when (:revealed? tile) " revealed")
                                 (when (:matched? tile) " match"))}
                (d/div {:className "front"})
                (d/div {:className (str "back "
                                        (when-let [face (:face tile)]
                                          (name face)))}))))

(q/defcomponent Line [tiles player-moves]
  (apply d/div {:className "line"}
         (map #(Cell % player-moves) tiles)))

(q/defcomponent Board [game player-moves]
  (apply d/div {:className (str "board clearfix"
                                (when (:foggy? game)
                                  " foggy"))}
         (map #(Line % player-moves) (partition 4 (:tiles game)))))

(q/defcomponent Sand [sand]
  (d/div {:className (str "sand " (name sand))}))

(q/defcomponent Hourglass [sand]
  (apply d/div {:className "timer"}
         (map Sand sand)))

(q/defcomponent Game [game player-moves]
  (d/div {}
         (Board game player-moves)
         (Hourglass (:sand game))))

(enable-console-print!)

(go
  (let [server-ch (<! (ws-ch "ws://localhost:8666/ws" {:format :edn}))
        container (.getElementById js/document "main")]
    (go-loop []
      (when-let [game (:message (<! server-ch))]
        (cond
         (:dead? game) (set! (.-className (.-body js/document)) "game-over")
         (:safe? game) (set! (.-href js/location) "safe.html")
         :else (do (q/render (Game game server-ch) container)
                   (recur)))))))
