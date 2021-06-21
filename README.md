![Clojure CI](https://github.com/popoppo/clojure-fire/workflows/Clojure%20CI/badge.svg)

# clojure-fire

This makes it easy to create and run CLI tools in Clojure without any configuration.  
The idea and name are coming from [python-fire](https://github.com/google/python-fire).

## What's that?

Let's say you have a function to run via `-main`,

``` clojure
(defn multiply
  [x y & more]
  (apply * (concat [x y] more))

(defn -main
  [& args]
  ;; call multiply
)
```

How do you handle `args`?  
Just parsing it by hand? or using [tools.cli](https://github.com/clojure/tools.cli)?  
Fine, there's no problem.

With `fire`, you can call the function like this.

``` clojure
(defn -main
  [& args]
  (fire))
```

and run it on a command line.

```clojure
$ bb -m <path to file> multiply 2 3 ;; or "clj -m <path to file> ..."
6
```

So, all you need to do is just calling `(fire)` in your program and run it with a function name and args on a command line. Then `fire` finds the target function and calls it with the given arguments. You don't need any configuration.  

`fire` works with `clj`, [lein-exec](https://github.com/kumarshantanu/lein-exec) and [babashka](https://github.com/borkdude/babashka).  

Tested on Mac, should work on Linux.  
Sorry, but not sure with Windows.

## License

Copyright © 2021 Koji Takahashi

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
