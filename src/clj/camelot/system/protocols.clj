(ns camelot.system.protocols)

(defprotocol Migratable
  (migrate [this x]
    "Migrate `x`.")
  (rollback [this x]
    "Rollback `x`."))

(defprotocol Connectable
  (connect [this x]
    "Connect to `x`.")
  (disconnect [this x]
    "Disconnect from `x`."))

(defprotocol Inspectable
  (inspect [this]
    "Return the state."))
