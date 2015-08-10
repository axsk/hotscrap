(ns hotscrap.core
	(:require (hotscrap [scrapper :refer [scrapall]])))

;; w warrior s support a assasin c specialist
(def heroclass
  {"Thrall" :a, "Tychus" :a, "Raynor" :a, "Li Li" :s, "Sgt. Hammer" :c, "The Butcher" :a, "Rehgar" :s, "Brightwing" :s, "Malfurion" :s, "Kael'thas" :a, "Illidan" :a, "Nova" :a, "Zagara" :c, "Uther" :s, "Azmodan" :c, "Tassadar" :s, "Zeratul" :a, "Gazlowe" :c, "Arthas" :w, "E.T.C." :w, "Stitches" :w, "Diablo" :w, "Sylvanas" :c, "Sonya" :w, "Tyrael" :w, "Johanna" :w, "Murky" :c, "Valla" :a, "Nazeebo" :c, "Leoric" :w, "Abathur" :c, "Falstad" :a, "Kerrigan" :a, "Muradin" :w, "Chen" :w, "Anub'arak" :w, "Tyrande" :s, "The Lost Vikings" :c, "Jaina" :a})

(defn -main [& args]
  (def data (scrapall))
)  

(defn odds [] (data :odds))
(defn herowr 
  ([mapname] (get-in data [:maps mapname]))
  ([] (herowr :all)))
(defn playerstats [playerid] (get-in data [:player :stats]))

(defn map-factors [mapname]
  (merge-with / (herowr mapname) (herowr)))

(defn player-factors [playerid]
  (merge-with #(/ (:wr %1) %2) (playerstats playerid) (herowr)))

(defn map-values [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn sumodds [heroes]
  (apply merge-with + (vals (select-keys (odds) heroes))))

(defn counter 
  ([{playerid :player mapname :map class :class :or {mapname :all class :all}} heroes]
    (let [heroes (clojure.string/split heroes #" ")]
      (sort-by val >
        (map-values #(/ % (count heroes))
          (apply dissoc (->>
            (sumodds heroes) 
            (merge-with * (map-factors mapname))
            ((if playerid
             (partial merge-with * (player-factors playerid))
             identity))
            (filter #(or (= class :all) (= (heroclass (key %)) class)))
            (into (sorted-map)))
          heroes)))))
  ([heroes]
    (counter {} heroes)))

