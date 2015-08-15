(ns hotscrap.scrapper
	(:require [clj-webdriver.taxi :refer :all]))

(defn start-browser
  ([] (start-browser :chrome))
  ([driver] (set-driver! {:browser driver})))

(def browser :chrome)

(defn map-values [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(def allheroes '("Abathur" "Anub'arak" "Arthas" "Azmodan" "Brightwing" "Chen" "Diablo" "E.T.C." "Falstad" "Gazlowe" "Illidan" "Jaina" "Johanna" "Kael'thas" "Kerrigan" "Leoric" "Li Li" "Malfurion" "Muradin" "Murky" "Nazeebo" "Nova" "Raynor" "Rehgar" "Sgt. Hammer" "Sonya" "Stitches" "Sylvanas" "Tassadar" "The Butcher" "The Lost Vikings" "Thrall" "Tychus" "Tyrael" "Tyrande" "Uther" "Valla" "Zagara" "Zeratul"))

(defn format-parsed [re-res]
  (->>
   re-res
   (map #(drop 1 %))
   (flatten)
   (apply hash-map)
   (map-values read-string)))

(defn normalize-values
  "[70 55] -> [0.3 0.45]"
  [m]
  (map-values #(double (- 1 (/ (bigdec %) 100))) m))

(defn parse-hero-odds
  "compute the winrates of all other heroes against the given hero"
  [hero]
  (let [heroselect (str "option[value*=\"" hero "\"]")
        vsotherbtn "a[href*=VsOther]"
        oddtable "table#DataTables_Table_0"]
    (println  "Scrapping" hero)
    (click heroselect)
    (Thread/sleep 500)
    (click vsotherbtn)
    (->>
     (text oddtable)
     (re-seq #"\n(.*) \d+ (\d\d\.\d)")
     (format-parsed)
     normalize-values)))

(defn scrap-odds "get the odds of all heroes vs all others" []
  (with-driver {:browser browser}
	  (get-url "https://www.hotslogs.com/Sitewide/HeroDetails")
    (zipmap allheroes (map parse-hero-odds allheroes))))

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

(defn parse-heroes-map [Map]
  (->>
    (text (str "#" (maptable Map)))
    (re-seq #"\n(.*) \d+ .*% (\d\d\.\d)")
    (format-parsed)))

(defn scrap-heroes "get heroes winrates on all maps" []
  "Scrapping hero winrates on all maps"
  (with-driver {:browser browser}
    (get-url "https://www.hotslogs.com/sitewide/heroandmapstatistics")
    (let [allmaps (keys maptable)]
      (zipmap allmaps (map parse-map-winrate allmaps)))))

;; parse winrates with each hero of a player

(defn scrap-player-stats [playerid]
  (with-driver {:browser browser}
    (get-url (str "https://www.hotslogs.com/Player/Profile?PlayerID=" playerid))
    (->>
     (text "#ctl00_MainContent_RadGridCharacterStatistics_ctl00")
     (re-seq #"\n(.*) \d+ (\d+) .* (\d\d.\d)")
     (map #(let [[_ hero games wr] %] {hero {:games (read-string games) :wr (read-string wr)}}))
     (apply merge)
     (hash-map playerid))))

;; parse all played games of a player

(defn wait-loaded []
  (let [loading-panel "div[id*=LoadingPanel1ctl]"]
    (wait-until #(exists? loading-panel) 20000)
    (wait-until #(not(exists? loading-panel)) 20000)))

(defn expand [n]
  (click (nth (elements ".rgRow button[value=Expand]") n))
  (wait-loaded))

(defn collapse []
  (click "button[value=Collapse]")
  (wait-loaded))

(defn parse-player-ids []
  (for [a (elements "table[id*=Detail] a[href^='/Player/Profile?']")]
    (->>
     (attribute a :href)
     (re-find #"PlayerID=(\d+)")
     (last)
     (read-string))))

(defn parse-player-heroes []
  (for [[_ h w]
        (->>
         (text "table[id*=Detail]>tbody")
         (re-seq #"\n[\s]*[^\s]+ (.*) \d+ \d+ (-?)\d+"))]
    {:hero h, :win (= w "")}))

(defn parse-map-and-time [n]
  (->>
   (text (str "[id$=__" n "]"))
   (re-find #"(.*?) \d.*(\d+/\d+/\d{4} \d+:\d+:\d+)")
   (rest)))

(defn parse-game [n]
  (let [[mapname time] (parse-map-and-time n)]
    (expand n)
    (let [game {:map mapname, :time time
                :players (doall (map #(assoc %1 :pid %2)
                                     (parse-player-heroes)
                                     (parse-player-ids)))}]
      (collapse)
      game)))

(defn number-of-games [] (count (elements ".rgRow button[value=Expand]")))

(defn next-page []
  (try
    (do
      (click ".rgCurrentPage+a")
      (wait-loaded)
      true)
    (catch org.openqa.selenium.NoSuchElementException e
      false)))

(defn scrap-player-games [playerid maxgames]
  (println "Scrapping games of " playerid)
  (get-url (str "https://www.hotslogs.com/Player/MatchHistory?PlayerID=" playerid))
  (loop [n 0
         games []]
    (if (= maxgames (count games))
      games
      (if (< n (number-of-games))
        (recur (inc n) (conj games (parse-game n)))
        (if (next-page)
          (recur 0 games)
          (games))))))

(defn scrap-games [playerids maxgames]
  (apply concat
    (pmap (fn [pid]
            (with-driver {:browser :chrome}
            (scrap-player-games pid maxgames)))
        playerids)))

;; convenience functions for testing

(defn scrap-all []
  {:odds (scrap-odds)
   :heroes (scrap-heroes)
   :games (scrap-games [1220651] 10)
   :players (scrap-player-stats 1220651)})
