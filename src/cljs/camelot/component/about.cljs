(ns camelot.component.about
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.translation.core :as tr])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn open-url
  [url]
  (.open js/window url "_blank" "nodeIntegration=no"))

(defn find-out-more
  []
  (open-url "https://gitlab.com/camelot-project/camelot/blob/master/CONTRIBUTING.md"))

(defn donate
  []
  (open-url "https://www.patreon.com/join/camelot_software"))

(defn blurb
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "about-camelot"}
               (dom/p nil "Wildlife populations are in rapid decline as the environment around them changes at an unprecedented rate. Conservation efforts are critically important to reverse this trend. However these efforts must be supported by the right techniques and technologies in order to keep pace with the rate of change around us.")

               (dom/p nil "Unfortunately, too often software for scientific research is of poor quality or prohibitively expensive. The lack of access to quality software stalls the discovery of solutions to the challenges we all currently face. We need those solutions now.")

               (dom/p nil "This software, Camelot, is part of the Camelot Project, which seeks to ensure that high-quality feature-rich software is readily available for conservation research.")

               (dom/p nil "The Camelot Project is an entirely voluntary initiative. Producing high-quality conservation software requires a significant investment of time and skills, which we could not do without the support of the community.")

               (dom/p nil "You can support us in one of two ways:")

               (dom/ol nil
                       (dom/li nil
                               (dom/strong nil "Contribute to the project") ": "
                               "direct contributions to the project can take many forms, from filing feature requests and bugs, through to documentation, translation and code contributions. "
                               (dom/a #js {:onClick find-out-more}
                                      "Find out more"))
                       (dom/li nil
                               (dom/strong nil "Donate") ": "
                               "with your financial support we will be able to continue expanding the range and capabilities of software and cover server and hosting costs. Organisations may also donate to take advantage of our priority support offering."))

               (dom/div #js {:className "donate-button-container"}
                        (dom/a #js {:onClick donate}
                               (dom/button #js {:className "btn btn-primary"} "Donate now")))

               (dom/p nil "Your support is incredibly valuable to us.")

               (dom/p nil "Regards,")

               (dom/p nil "Chris and Heidi")))))

(defn about-view
  "About Camelot."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil "About Camelot"))
               (dom/div #js {:className "single-section text-section"}
                        (om/build blurb {}))))))
