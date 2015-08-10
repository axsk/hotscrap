(ns hotscrap.core
  (:gen-class)
	(:require ([clj-webdriver.taxi :refer :all])
    (hotscrap [scrapper :refer :all] )))

;; w warrior s support a assasin c specialist
(def heroclass
  {"Thrall" :a, "Tychus" :a, "Raynor" :a, "Li Li" :s, "Sgt. Hammer" :c, "The Butcher" :a, "Rehgar" :s, "Brightwing" :s, "Malfurion" :s, "Kael'thas" :a, "Illidan" :a, "Nova" :a, "Zagara" :c, "Uther" :s, "Azmodan" :c, "Tassadar" :s, "Zeratul" :a, "Gazlowe" :c, "Arthas" :w, "E.T.C." :w, "Stitches" :w, "Diablo" :w, "Sylvanas" :c, "Sonya" :w, "Tyrael" :w, "Johanna" :w, "Murky" :c, "Valla" :a, "Nazeebo" :c, "Leoric" :w, "Abathur" :c, "Falstad" :a, "Kerrigan" :a, "Muradin" :w, "Chen" :w, "Anub'arak" :w, "Tyrande" :s, "The Lost Vikings" :c, "Jaina" :a})

(defn -main [& args]
  (def data (scrapall))
)  

(defn map-factors [mapdata Map]
  (merge-with / (mapdata Map) (mapdata :all)))

(defn player-factors [player-stats hero-stats]
  (merge-with #(/ (:wr %1) %2) player-stats hero-stats)
)
 
(defn map-values [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn counter 
  ([Map heroes class]
    (let [heroes (clojure.string/split heroes #" ")]
      (sort-by val >
        (map-values (apply dissoc 
          (->>
            (vals (select-keys (data :odds) heroes))
            (apply merge-with +)
            (merge-with * (map-factors (data :maps) Map))
            (filter #(or (= class :all) (= (heroclass (key %)) class)))
            (into (sorted-map)))
          heroes)
          #(- 100 (/ % (count heroes)))))))
  ([Map heroes]
   (counter Map heroes :all))

  ([heroes]
    (counter :all heroes :all)))

