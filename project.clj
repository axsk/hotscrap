(defproject hotscrap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-webdriver "0.7.2"]
                 [org.seleniumhq.selenium/selenium-java "2.47.1"]
                 [com.codeborne/phantomjsdriver "1.2.1" :exclusions [org.seleniumhq.selenium/selenium-remote-driver]]]
  :plugins [[cider/cider-nrepl "0.9.1"]]
  :main ^:skip-aot hotscrap.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
