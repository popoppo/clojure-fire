![Clojure CI](https://github.com/popoppo/clojure-fire/workflows/Clojure%20CI/badge.svg)

# clojure-fire

Easy function dispatcher for Clojure, heavily inspired by [python-fire](https://github.com/google/python-fire).

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

how do you call the function with `args`?  
Parsing `args`? or using [tools.cli](https://github.com/clojure/tools.cli)?  
Fine, there's no problem.

With `fire`, you can call the function like this.

``` clojure
(defn -main
  [& args]
  (fire))
```

and run it with the function name on CLI.

```clojure
$ bb -m <path to file> multiply 2 3 ;; or "clj -m <path to file> ..."
6
```

So, all you need to do is just calling `(fire)` and passing a function name and args via CLI.  
`fire` finds the target function and call it with the given arguments.  
`fire` works with `clj`, [lein-exec](https://github.com/kumarshantanu/lein-exec) and [babashka](https://github.com/borkdude/babashka).  
(Please install them if needed.)

## Motivation
When writing CLI tools, I usually use [tools.cli](https://github.com/clojure/tools.cli) to parse arguments. That's great enough.  
However, I sometimes felt that it's too rich to provide internal tools or tools for myself.  
In such cases, as I already know what arguments the target function needs, rich parsers or validations are not needed. I just fix parameters and retry when errors/exceptions happen.  
With `fire`, you can skip the process of writing argument parsers and can focus on realising your idea.  
Of course we Clojurians mainly use REPL to develop tools/apps and have less chance to use CLI,
but this tool would give you another handy option in your development process.  
This tool is designed to cover many common cases, but not all. If you need more rich functionality, it's time to use `tools.cli`, I think.

## Examples

The easiest way is just calling `fire` in your `*.clj` file.  

```clojure
(fire)
```

Now it's ready to call your functions. Just specify a function name via CLI. (and pass args if needed)  
Here we use a built-in sample program `src/fire/examples/basics_bb.clj` with bb.   
`BABASHKA_CLASSPATH=src` should be set before running them.

```bash
$ src/fire/examples/basics_bb.clj hello
Hello

$ src/fire/examples/basics_bb.clj hello-world
Hello World
```

Looks good, but what if no function is specified?

```bash
$ src/fire/examples/basics_bb.clj
hello ([])
No docstrings

hello-world ([& {:or {name "World"}, :keys [name]}])
No docstrings

add ([[x y]] [x y])
No docstrings

multi ([x y & vs])
No docstrings

area ([{:keys [x y]}])
No docstrings

echo ([& opts])
No docstrings

```

`fire` gives you the list of public functions with its arg-list and docstrings. (or "No docstrings" if docstrings not found)  

Next, let's call `add` which has multi-arity.

```bash
$ src/fire/examples/basics_bb.clj add 2 3
5
$ src/fire/examples/basics_bb.clj add "[2 3]"
5
```

Nothing special? Right, but note that the arguments passed to `add` are cast properly.  
All arguments from CLI are parsed as string, so you need to cast the type of the vars if needed.  
With `fire`, as it's done under the food, you no longer need to be bothered by type casting.  
To handle other types (vec, list or map), see the References.

`area` will be another good example to see how to pass a map.

```bash
$ src/fire/examples/basics_bb.clj area --x=2 --y=3
6
$ src/fire/examples/basics_bb.clj area "{:x 2 :y 3}"
6
```

Options with `-` or `--` will be mapped to a `hash-map`.  
For example, when `-x` or `--x` is used, `x` will be the key of an item in a map.
So in the first case of the above example, `--x=2` and `--y=3` are mapped to `{:x 2}` and `{:y 3}` respectively, then merged into a map and the map `{:x 2 :y 3}`is passed to the target function.  
The second case takes a different way. It passes the string "{:x 2 :y 3}" and that is parsed as the map `{:x 2 :y 3}` (not string) and passed to `area`. As a result, it's destructured to 2 and 3.  

As the last example, let's take a look of the case that contains both positional args and options.

```bash
$ src/fire/examples/basics_bb.clj echo 1 "[1 2 3]" "{:x 4 :y 5}" -x=6 --y="[7 8 9]" -z="{:a foo}"
(1 [1 2 3] {:x 4, :y 5} {:x 6, :y [7 8 9], :z {:a foo}})
```

Some edge cases are covered in the following section, plese look over them.

## Usage

`fire` gives you several features.

### Listing callable functions

To get the list of fuctions, just call `fire` witout arguments.

```clojure
(fire)
```

and passes no parameters from CLI, then the list of available functions are shown with arg-list and docstrings (if it exists).

```bash
$ src/fire/examples/basics_bb.clj
hello ([])
No docstrings

hello-world ([& {:or {name "World"}, :keys [name]}])
No docstrings

add ([[x y]] [x y])
No docstrings

multi ([x y & vs])
No docstrings

area ([{:keys [x y]}])
No docstrings

echo ([& opts])
No docstrings

```

### With no args

This is the simplest/esiest way to go. Just call `fire` in your program.

```clojure
(fire)
```

Then specify a target function name and pass arguments from CLI as needed. If no args are given, the list of callable functions will be printed as described above.

### With fn name

If you want to call a specific function with arbitrary arguments, call `fire` with the function name, symbol or keyword.

```clojure
(fire 'echo)
(fire "echo")
(fire :echo)
```

and passes any arguments from CLI according to the signature of the target function.

```bash
$ src/fire/examples/basics_bb.clj foo -x
(foo {:x nil})
```

You can also specify a function name with a map.

```clojure
(fire {:fn 'echo})
```

### Command line arguments

Although `fire` is desinged to cover many common cases, as there are lots of variations, it's difficult to cover all cases.  
Here is a good example of how some basic data types are parsed.
(Assuming `(fire 'echo)` is put in your program)

```bash
$ src/fire/examples/basics_bb.clj 1 "[1 2 3]" "{:x 4 :y 5}" -a --b -x=6 --y="[7 8 9]" -z="{:a foo}"
(1 [1 2 3] {:x 4, :y 5} {:a nil :b nil :x 6, :y [7 8 9], :z {:a foo}})
```

No confusion, isn't it? but I might have to explain a bit about the options.  

All arguments which do not have hyphens are treated as positional arguments. The order of the params are kept and passed to your target function. An exception will be thrown if there are inconsistencies between the number of positional args and the signature/arity of target functions.

As you can see in the above example, you can pass any type of data by wrapping the values with `"..."`. `fire` casts the type appropriately. But how?  
`fire` uses `clojure.edn/read-string` to parse arguments. If the args follow the spec of edn, they are transformed properly, but if it's not edn data (e.g. JSON), it falls back to plain string. For example, `{"foo": 123}` is JSON data and not valid edn data (you'll get an error when you eval it in your repl). `fire` tries to parse it with `clojure.edn/read-string` but fails, then handle it as string. Here are some examples that might cause problems.

```bash
$ src/fire/examples/basics_bb.clj "1,2,3" "\"1,2,3\"" ",,," " " "\" \""
(1 1,2,3 nil nil  )
```

Options with `-` or `--` are used as keys in a map. (The number of hyphens doesn't matter)

```bash
$ src/fire/examples/basics_bb.clj -x --y
{:x nil :y nil}
```

If you want to associate values to the keys, you NEED to use `=` and white spaces ` ` are not allowed . "not allowed" means that it brings unexpected results to you (but works).

```bash
$ src/fire/examples/basics_bb.clj -x=1 --y 2 -z
{2 :x 1 :y nil :z nil}
```

When the same options appear several times, they are merged into the same key. (Again, the number of hyphens doesn't matter)

```bash
$ src/fire/examples/basics_bb.clj -x=1 --y=2 --x=2 -z=foo --x="[9 8 7]"
({:x (1 2 [9 8 7]), :y 2, :z foo})
```

The following example might confuse you a bit.

```bash
$ src/fire/examples/basics_bb.clj "{:x 1 :y 2}" -x=1 --y=2
({:x 1, :y 2} {:x 1, :y 2})

```

The first argument is parsed as a positional arg as it doesn't have hyphens. The second and third args are parsed as `{:x 1}` and `{:y 2}` respectively and merged it into the same one. So the options will be merged into a map but postional args won't. 

### With parsers

All arguments are parsed by using `clojure.edn/read-string` and I believe it works in most of all cases. On the other hand, there would be cases which `clojure.edn/read-string` doesn't fit. For such cases, you can customize the argument parser by yourself.

Before having your own one, let's look at the default parser.

```clojure
[[#".+" #(clojure.edn/read-string %)]]
```

The parsers consists of items (only 1 item is there by default though) and each item must have a regex pattern and a function. `fire` apllies the pattern to all values and call the function if the pattern matches. The function must take 1 argument which is a string that matched the pattern. By default, the pattern matches all values and parsed by `read-string`. If a value cannot be parsed, then it is treated as string. Like this.

```clojure
(try
  (f matched-str) ;; f is the second item in the list. clojure.edn/read-string by default.
  (catch Exception e v))
```

You can customize the parser anyway you want by using `parsers` parameter. The default behavior is equivalent to the following code.

```clojure
(fire {:parsers [[#".+" #(clojure.edn/read-string %)]]})
```

If you want to handle data which contains commas as string, another item should be added before the default one.

```clojure
(fire {:fn 'echo
        :parsers [[#".*,.*" #(identity %)]
                  [#".+" #(clojure.edn/read-string %)]]})
```

The result should be

```bash
$ src/fire/examples/basics_bb.clj "1,2,3" "\"1,2,3\"" ",,," " " "\" \""
(1,2,3 "1,2,3" ,,, nil  )
```

The order of items is important.   
`fire` uses the items in order from top to bottom and once a pattern matches, the following patters are not used. So if your parser is like this

```clojure
[[#".+" #(clojure.edn/read-string %)]
 [#".*,.*" #(identity %)]]
```

the first item will catch all string and no one will be handled with the second item.  
Note that even if a value is invalid for the first item, that will be just treated as string and never be passed to the following items. That is, it's always safer to add your parsers before the default one and keep the default one as the last item so that the default one will work as like `:else` clause.

It's also allowed to have no parsers. (don't pass `nil` but a list or vector)

```clojure
(fire {:parsers []})
```

Then a white space is passed as it is.

```bash
# src/fire/examples/basics_bb.clj echo " "
( )
```

## References

T.B.D

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
