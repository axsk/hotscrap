(ns hotscrap.core
  (:gen-class)
	(:require [clj-webdriver.taxi :refer :all]))

;; w warrior s support a assasin c specialist
(def heroclass
  {"Thrall" :a, "Tychus" :a, "Raynor" :a, "Li Li" :s, "Sgt. Hammer" :c, "The Butcher" :a, "Rehgar" :s, "Brightwing" :s, "Malfurion" :s, "Kael'thas" :a, "Illidan" :a, "Nova" :a, "Zagara" :c, "Uther" :s, "Azmodan" :c, "Tassadar" :s, "Zeratul" :a, "Gazlowe" :c, "Arthas" :w, "E.T.C." :w, "Stitches" :w, "Diablo" :w, "Sylvanas" :c, "Sonya" :w, "Tyrael" :w, "Johanna" :w, "Murky" :c, "Valla" :a, "Nazeebo" :c, "Leoric" :w, "Abathur" :c, "Falstad" :a, "Kerrigan" :a, "Muradin" :w, "Chen" :w, "Anub'arak" :w, "Tyrande" :s, "The Lost Vikings" :c, "Jaina" :a})

(def maptable 
  {:all "DataTables_Table_0"
   :boe "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl06_Detail10"
   :bb  "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl09_Detail20"
   :ch  "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl12_Detail30"
   :ds  "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl15_Detail40"
   :got "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl18_Detail50"
   :hm  "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl21_Detail60" 
   :st  "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl24_Detail70"
   :tsq "ctl00_MainContent_RadGridMapStatistics_ctl00_ctl27_Detail80"})

(defn start-browser []
	(set-driver! {:browser :chrome}))

(defn stringvals->float [hashmap]
  (zipmap (keys hashmap) (map read-string (vals hashmap))))

(defn parse-hero-odds [hero]
  (click (str "option[value*=\"" hero "\"]"))
  (Thread/sleep 1000)
  (click "a[href*=VsOther")
  (->>
    (text (find-element {:tag :table, :id "DataTables_Table_0"}))
    (re-seq #"\n(.*) \d+ (\d\d\.\d)")
    (map (fn [x] (drop 1 x)))
    (flatten)
    (apply hash-map)
    (stringvals->float)))

(defn parse-odds []
	(get-url "https://www.hotslogs.com/Sitewide/HeroDetails")
  (def allheroes (conj (map first (parse-hero-odds "Jaina")) "Jaina"))
  (def odds (zipmap allheroes (map parse-hero-odds allheroes))))

(defn parse-map-winrate [Map]
  (->>
    (text (find-element {:tag :table, :id (maptable Map)}))
    (re-seq #"\n(.*) \d+ .*% (\d\d\.\d)")
    (map #(drop 1 %))
    (flatten)
    (apply hash-map)
    (stringvals->float)))

(defn parse-heroes-winrate []
  (get-url "https://www.hotslogs.com/sitewide/heroandmapstatistics")
  (def map-winrates (zipmap (keys maptable) (map parse-map-winrate (keys maptable)))))
  

(defn map-factors [Map]
  (merge-with / (map-winrates Map) (map-winrates :all)))

(defn counter 
  ([Map heroes class]
    (sort-by val < 
      (filter #(if (= class :all) true (= (heroclass (key %)) class))
        (merge-with * (map-factors Map)
          (apply merge-with + 
            (vals (select-keys odds heroes)))))))
  ([Map heroes]
   (counter Map heroes :all))

  ([heroes]
    (counter :all heroes :all)))

(defn -main [& args]
  (start-browser)
  (parse-odds)
  (parse-heroes-winrate))

