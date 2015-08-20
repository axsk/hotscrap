(ns hotscrap.scrapper
	(:require [clj-webdriver.taxi :refer :all]
            [clojure.set]))

(if-not (resolve 'data)
  (def data {}))
(def MAXBROWSERS 4)
(def browser :chrome)

(defn start-browser
  ([] (start-browser :chrome))
  ([driver] (set-driver! {:browser driver})))

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
    (Thread/sleep 1000)
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

(defn parse-map [Map]
  (->>
    (text (str "#" (maptable Map)))
    (re-seq #"\n(.*) \d+ .*% (\d\d\.\d)")
    (format-parsed)))

(defn scrap-heroes "get heroes winrates on all maps" []
  "Scrapping hero winrates on all maps"
  (with-driver {:browser browser}
    (get-url "https://www.hotslogs.com/sitewide/heroandmapstatistics")
    (let [allmaps (keys maptable)]
      (zipmap allmaps (map parse-map allmaps)))))

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
    (wait-until #(not(exists? loading-panel)) 20000))
  (Thread/sleep 50))

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

(defn knowngames []
  (set
   (for [{:keys [map time]} (data :games)]
     [map time])))

(defn parse-game [n]
  (let [[mapname time] (parse-map-and-time n)]
    (if (contains? (knowngames) [mapname time])
      nil
      (do
        (expand n)
        (let [game {:map mapname, :time time
                    :players (doall (map #(assoc %1 :pid %2)
                                         (parse-player-heroes)
                                         (parse-player-ids)))}]
          (collapse)
          game)))))

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
  (try
    (get-url (str "https://www.hotslogs.com/Player/MatchHistory?PlayerID=" playerid))
    (set
     (loop [n 0
            games []]
       (if (= maxgames (count games))
         games
         (if (< n (number-of-games))
           (recur (inc n) (conj games (parse-game n)))
           (if (next-page)
             (recur 0 games)
             games)))))
    (catch Exception e
      (prn "Caught "e)
      {})))

(defn par-scrap-games [playerids maxgames]
  (apply clojure.set/union
         (pmap (fn [pid]
                 (with-driver {:browser browser}
                   (scrap-player-games pid maxgames)))
               playerids)))

(defn scrap-games [playerids maxgames]
  (loop [results #{}
         pidpart (partition MAXBROWSERS MAXBROWSERS nil playerids)]
    (if (empty? pidpart)
      results
      (recur
       (clojure.set/union results
                          (par-scrap-games (first pidpart) maxgames))
       (rest pidpart)))))

(defn all-players [games]
  (distinct
   (for [{ps :players} games
         {p  :pid}     ps]
     p)))

;; PUBLIC API
;; top-level functions to actually parse the data

(defn update-static [data]
  (->
   data
   (assoc :odds (scrap-odds))
   (assoc :heroes (scrap-heroes))))

(defn update-games
  "scrap maxgames games of the given playerids collection and update them into data"
  [data playerids maxgames]
  (assoc data :games
         (clojure.set/union
          (data :games)
          (scrap-games playerids maxgames))))

(defn auto-update-games
  "randomly choose np known players and update the latest ng games of them into data"
  [data np ng]
  (update-games data
                (repeatedly np
                            #(rand-nth (all-players (data :games))))
                ng))

(defn update-players [data players]
  (->>
   (apply merge  (map scrap-player-stats players))
   (merge (data :players))
   (assoc data :players)))
