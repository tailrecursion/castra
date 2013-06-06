(ns wigwam-clj.exception)

(declare ex-classes)

(defmulti ex-status   identity)
(defmulti ex-message  identity)
(defmulti ex-severity identity)

(defmacro defstatus [type code & {:keys [severity message]}]
  (let [class* (if (integer? code) (ex-classes (int (/ code 100))) code)
        code*  (if (integer? code) code (ex-status code))
        msg*   (or message (ex-message code))
        svr*   (or severity (ex-severity code))]
    `(do
       (derive ~type ~class*)
       (defmethod ex-status ~type [_#] ~code*)
       (defmethod ex-message ~type [_#] ~msg*)
       (defmethod ex-severity ~type [_#] ~svr*))))

(def ex-classes {1 ::informational
                 2 ::successful
                 3 ::redirection
                 4 ::client-error
                 5 ::server-error})

(derive ::informational ::exception)
(derive ::successful    ::exception)
(derive ::redirection   ::exception)
(derive ::client-error  ::exception)
(derive ::server-error  ::exception)

(defstatus ::ok                     200 :severity :ignore :message "OK")
(defstatus ::bad-request            400 :severity :error  :message "Bad Request")
(defstatus ::unauthorized           401 :severity :error  :message "Unauthorized")
(defstatus ::forbidden              403 :severity :error  :message "Forbidden")
(defstatus ::not-found              404 :severity :error  :message "Not Found")
(defstatus ::method-not-allowed     405 :severity :error  :message "Method Not Allowed")
(defstatus ::not-acceptable         406 :severity :error  :message "Not Acceptable")
(defstatus ::unsupported-media-type 415 :severity :error  :message "Unsupported Media Type")
(defstatus ::internal-server-error  500 :severity :error  :message "Internal Server Error")
(defstatus ::not-implemented        501 :severity :error  :message "Method Not Implemented")
(defstatus ::bad-gateway            502 :severity :error  :message "Bad Gateway")
(defstatus ::service-unavailable    503 :severity :error  :message "Service Unavailable")
(defstatus ::gateway-timeout        504 :severity :error  :message "Gateway Timeout")

(defstatus ::csrf     ::forbidden)
(defstatus ::login    ::forbidden)
(defstatus ::auth     ::forbidden)
(defstatus ::ignore   ::internal-server-error :severity :ignore)
(defstatus ::debug    ::internal-server-error :severity :debug)
(defstatus ::info     ::internal-server-error :severity :info)
(defstatus ::notice   ::internal-server-error :severity :notice)
(defstatus ::warning  ::internal-server-error :severity :warning)
(defstatus ::error    ::internal-server-error :severity :error)
(defstatus ::fatal    ::internal-server-error :severity :fatal)

(defn ex
  "Create new wigwam exception."
  [type & {:keys [message data headers body cause]}]
  (let [m (or message (ex-message type))
        d {:type      type
           :data      data
           :headers   headers
           :body      body}]
    (ex-info m d cause)))

(defn ex->clj
  "Get exception properties and data as a clj map."
  [e]
  (let [e (if (isa? (:type (ex-data e)) ::exception) e (ex ::fatal :cause e))
        p (ex-data e)
        t (:type p)
        m (.getMessage e)
        s (ex-severity t)
        d (:data p)]
    {:type t :message m :severity s :data d}))

;; exceptions

(defn ignore [& [data & [cause]]]
  (ex ::ignore :data data :cause cause))

(defn csrf []
  (ex ::csrf :message "Bad CSRF token."))

(defn login [& [message & [data & [cause]]]]
  (ex ::login :message (or message "Wrong username or password.") :data data))

(defn error [& [message & [data & [cause]]]]
  (ex ::error :message message :data data :cause cause))

