(ns camelot.translations.en)

(def t-en
  {:problems
   {:error "[ERROR] "
    :warn "[WARNING] "
    :info "[INFO] "
    :ignore "[IGNORE] "
    :okay "[OK] "
    :datetime "Date/time consistency issues detected on photos. Check date/time on camera."
    :rename-field-not-found "Unable to resolve the following field selectors during photo renaming: '%s'"
    :rename-existing-conflict "%s: Not renaming as doing so may overwrite existing file '%s'"
    :rename-conflict "%s: Not performing renames, as not all results would be unique. A file with this named would be the result of renaming the following files:"
    }
   :status
   {:rename-photos "Renaming photos in album: '%s'"
    :apply-rename "Renaming '%s' to '%s'..."}
   :missing  "|Missing translation: [%1$s %2$s %3$s]|"})
