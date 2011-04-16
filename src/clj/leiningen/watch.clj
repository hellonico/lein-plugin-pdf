(ns leiningen.watch
  "Watch a folder and run funktion when needed"
  (:import [java.io File]))

(def monitoring-frequency 500)
(def watched (ref {}))

(defn collect-files
  "Make a list of files that are to be monitored for timestamp"
  [root-folder]
  (dosync (ref-set watched {}))
  (doseq [file  (.listFiles root-folder)]  
      (dosync 
          (alter watched conj 
          {(.getName file) (.lastModified file)}))))

(defn check-changed-files
  "Check for timestamps in children of root-folder. If any change, run <funktion>"
  [root-folder funktion]
  (let [files (.listFiles root-folder)]
    (loop [afiles files]
      (if (not (empty? afiles))
      (do
      (let[ 
           ; the first file of the list
           f (first afiles)  
           ; the name of the first file of the list
           namae (.getName f)  
           ; the equivalent file that should be in the list
           mem (get @watched namae)  
           ; the last date modification of that file
           current (.lastModified f) ] 
      ; check the timestamps
      (if (not (= mem current )) 
        (do (collect-files root-folder)  (funktion)) 
        (recur (rest afiles))))))))) 

(defn watch-and-play
    [input funktion]
    (let
    [   ; we take a directory, or the parent directory of the file
        root-folder (if (.isDirectory input) 
                       input
                       (.getParentFile (File. (.getAbsolutePath input))))]
    ; make a list of the files to monitor
    (collect-files root-folder)
    ; let the user know
    (println "Monitoring..." (.getPath root-folder))
    (loop []
        (do
        ; sleep a little bit
        (Thread/sleep monitoring-frequency)
        ; check for changes
        (check-changed-files root-folder funktion))
        ; keep on going
        (recur))))
    