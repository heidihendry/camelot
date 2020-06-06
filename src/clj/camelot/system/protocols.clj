(ns camelot.system.protocols)

(defprotocol BackupManager
  (backup [this x]
    "Take a backup of the given thing."))

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

(defprotocol Contextual
  (set-context [this ctx k]
    "Set the given context")
  (context [this k]
    "Get the given context."))

(defprotocol Reloadable
  (reload [this]
    "Reload."))
