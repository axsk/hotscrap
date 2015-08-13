(ns hotscrap.core
	(:require (hotscrap [scrapper :refer [scrapall]])))

;; w warrior s support a assasin c specialist
(def heroclass
  {"Thrall" :a, "Tychus" :a, "Raynor" :a, "Li Li" :s, "Sgt. Hammer" :c, "The Butcher" :a, "Rehgar" :s, "Brightwing" :s, "Malfurion" :s, "Kael'thas" :a, "Illidan" :a, "Nova" :a, "Zagara" :c, "Uther" :s, "Azmodan" :c, "Tassadar" :s, "Zeratul" :a, "Gazlowe" :c, "Arthas" :w, "E.T.C." :w, "Stitches" :w, "Diablo" :w, "Sylvanas" :c, "Sonya" :w, "Tyrael" :w, "Johanna" :w, "Murky" :c, "Valla" :a, "Nazeebo" :c, "Leoric" :w, "Abathur" :c, "Falstad" :a, "Kerrigan" :a, "Muradin" :w, "Chen" :w, "Anub'arak" :w, "Tyrande" :s, "The Lost Vikings" :c, "Jaina" :a})

(def myheroes
  #{"Thrall" "Nazeebo" "Zagara" "Tyrael" "Anub'arak" "Zeratul" "Rehgar" "Kerrigan" "Raynor" "Brightwing" "Arthas" "Tyrande" "The Lost Vikings" "Tychus" "Illidan" "Sonya" "Nova" "Tassadar" "Diablo" "E.T.C." "Stitches"})

(defn loaddata []
  (def data (read-string (slurp "data.edn"))))

(defn savedata []
  (spit "data.edn" (pr-str data)))

(defn -main [& args]
  (loaddata))

(defn odds [] (data :odds))

(defn herowr
  ([mapname] (get-in data [:stats mapname]))
  ([] (herowr :all)))
(defn playerstats [playerid] (get-in data [:player :stats]))

(defn map-factors [mapname]
  (merge-with / (herowr mapname) (herowr)))

(defn map-factors-sorted [mapname]
  (sort-by val > (map-factors mapname)))

(defn player-factors [playerid]
  (merge-with #(/ (:wr %1) %2) (playerstats playerid) (herowr)))

(defn map-values [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn averageodds [heroes]
  (map-values #(/ % (count heroes))
    (apply merge-with +
      (vals (select-keys (odds) heroes)))))

(defn counter
  ([{playerid :player mapname :map class :class :or {mapname :all class :all}} heroes]
      (let [heroes (clojure.string/split heroes #" ")]
      (sort-by val >
        (apply dissoc (->>
          (averageodds heroes)
          (merge-with * (map-factors mapname))
          ((if playerid
           (partial merge-with * (player-factors playerid))
           identity))
          (filter #(or (= class :all) (= (heroclass (key %)) class)))
          (filter #(contains? myheroes (key %)))
          (into (sorted-map)))
        heroes))))
  ([heroes]
    (counter {} heroes)))


(defn hero-strengths [hero]
  (let [allmaps [:boe :bb :ch :ds :got :hm :st :tsq]]
    (zipmap allmaps (map #(% hero) (map map-factors allmaps)))))

(defn winners [game] (game true))
(defn losers  [game] (game false))
(defn tuples [players]
  (for [a (range 5)
        b (range (inc a) 5)]
    #{((vec players) a) ((vec players) b)}))

(defn herotuples [games mingames]
  (let [wins (frequencies (mapcat (comp tuples winners) games))
        loss (frequencies (mapcat (comp tuples losers) games))
        sums (merge-with + wins loss)]
    (into {}
      (for [[tuple sum] sums :when (> sum mingames)]
        [tuple (/ (if (contains? wins tuple) (wins tuple) 0) sum)]))))

(defn print-herostrengths []
  (clojure.pprint/print-table
    [:name :boe :bb :ch :ds :got :hm :st :tsq]
    (map
      #(assoc
         (map-values
           (fn [n] (format "%.3f" n))
           (hero-strengths %))
         :name %)
      (keys heroclass))))

;; TODO check stats vs maps
;; uniform percentage handling
(defn rel-odds [hero]
  (dissoc
    (merge-with #(* 100 (/ %1 %2))
                (get-in data [:odds hero])
                (get-in data [:stats :all]))
    hero))

(defn show [res]
  (let [sorted (sort-by val > res)]
    clojure.pprint/pprint [(take 10 sorted) (take-last 10 sorted)]))

(defn table [method subj]
  (clojure.pprint/print-table subj (map method subj)))
