(ns infograph.canvas)

(defmacro with-stroke
  "Wraps body in boilerplate code for strokes on canvas and executes."
  [ctx & body]
  `(do
     (.beginPath ~ctx)
     ~@body
     (.stroke ~ctx)
     (.closePath ~ctx)))

(defmacro with-style
  "Saves current global draw state, sets up global draw state according to
  style, executes body, then restores global draw state as if this never
  happened.
  Very impure function that lets the rest of the program be a big more pure."
  ;; TODO: Make it do what it promises.
  ;; This may not need to be a macro.
  [ctx style & body]
  `(do
     ;; noop
     ~@body))

