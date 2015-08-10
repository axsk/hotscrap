(ns hotscrap.scrapper
	(:require [clj-webdriver.taxi :refer :all]))

(defn start-browser []
	(set-driver! {:browser :chrome}))

(defn map-values [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn stringvals->float [hashmap]
  (zipmap (keys hashmap) (map read-string (vals hashmap))))

(defn format-parsed [re-res]
  (->>
    re-res
    (map #(drop 1 %))
    (flatten)
    (apply hash-map)
    (map-values read-string)))

;; parse hero odds, i.e. winrate of one hero vs another

(defn normalize-values [m]
  (map-values #(double (- 1 (/ (bigdec %) 100))) m))

(defn parse-hero-odds [hero]
  (println "parsing" hero)
  (click (str "option[value*=\"" hero "\"]"))
  (Thread/sleep 1000)
  (click "a[href*=VsOther")
  (->>
    (text (find-element {:tag :table, :id "DataTables_Table_0"}))
    (re-seq #"\n(.*) \d+ (\d\d\.\d)")
    (format-parsed)
    normalize-values))

(defn parse-odds []
	(get-url "https://www.hotslogs.com/Sitewide/HeroDetails")
  (def allheroes (conj (map first (parse-hero-odds "Jaina")) "Jaina"))
  (zipmap allheroes (map parse-hero-odds allheroes)))

;; parse hero winrates for each map

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
    (format-parsed)))

(defn parse-heroes-winrate []
  (get-url "https://www.hotslogs.com/sitewide/heroandmapstatistics")
  (let [allmaps (keys maptable)]
    (zipmap allmaps (map parse-map-winrate allmaps))))

;; parse winrates with each hero of a player 

(defn parse-player-stats [playerid]
  (get-url (str "https://www.hotslogs.com/Player/Profile?PlayerID=" playerid))
  (->>
    (text (find-element {:tag :table, :id "ctl00_MainContent_RadGridCharacterStatistics_ctl00"}))
    (re-seq #"\n(.*) \d+ (\d+) .* (\d\d.\d)")
    (map #(let [[_ hero games wr] %] {hero {:games (read-string games) :wr (read-string wr)}}))
    (apply merge)))

;; parse all played games of a player

(defn parse-player-games [playerid]
  (get-url (str "https://www.hotslogs.com/Player/MatchHistory?PlayerID=" playerid))
  (->>
    (click (find-element {:tag :button, :value "Expand"}))
  )
)

(defn scrapall []
  (start-browser)
  {:odds (parse-odds),
   :maps (parse-heroes-winrate) })

