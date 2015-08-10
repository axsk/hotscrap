(ns hotscrap.scrapper
  (:gen-class)
	(:require [clj-webdriver.taxi :refer :all]))

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

(defn parse-player-stats [playerid]
  (get-url (str "https://www.hotslogs.com/Player/Profile?PlayerID=" playerid))
  (->>
    (text (find-element {:tag :table, :id "ctl00_MainContent_RadGridCharacterStatistics_ctl00"}))
    )
)

(defn scrapall [& args]
  (start-browser)
  (parse-odds)
  (parse-heroes-winrate))
