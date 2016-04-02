(ns camelot.translations.en)

(def t-en
  {:problems {:error "[ERROR] "
              :warn "[WARNING] "
              :info "[INFO] "
              :ignore "[IGNORE] "
              :okay "[OK] "
              :rename-field-not-found "Unable to resolve the following field selectors during photo renaming: '%s'"
              :rename-existing-conflict "%s: Not renaming as doing so may overwrite existing file '%s'"
              :rename-conflict "%s: Not performing renames, as not all results would be unique. A file with this named would be the result of renaming the following files:"
              :species-quantity-missing "%s: has species without a quantity"
              :species-name-missing "%s: has quantity without a species"
              :default-config-exists "A default configuration file already exists. Please delete '%s' and try again."
              :config-not-found "A configuration file was not found.  Please run camelot with the 'init' option and adjust the configuration file created."
              }

   :status {:rename-photos "Renaming photos in album: '%s'"
            :apply-rename "Renaming '%s' to '%s'..."}

   :checks {:starting "Running consistency checks..."
            :failure-notice "FAIL: %s:"
            :photo-stddev "The timestamp on some photos significantly exceeds the standard deviation"
            :project-dates "The timestamp on some photos lies outside of the project start and stop times"
            :time-light-sanity "The camera settings do not coincide with the timestamp on numerous photos"
            :camera-checks "The photos do not have a camera check at the beginning and end"
            :headline-consistency "The headline of the photos differs across photos"
            :required-fields "One or more required fields are missing from one or more photos"
            :album-has-data "Album must not be empty"
            :sighting-consistency "Mismatch found in sighting data: species without quantity or quantity without species"
            :surveyed-species "A species was identified which is not known to the survey"
            }

   :default-config-created "A default configuration has been created in '%s'"

   :missing  "|Missing translation: [%1$s %2$s %3$s]|"})
