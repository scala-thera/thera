# Thera - the templating engine for Scala
![CI](https://github.com/anatoliykmetyuk/thera/workflows/CI/badge.svg) [![Gitter](https://badges.gitter.im/akmetiuk/thera.svg)](https://gitter.im/akmetiuk/thera?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

```scala
import thera._

val template =
"""
---
system:
  name: Solar System
  centralBody: Sun
  planets:
    - { name: "Mercury", mass: "3.30 * 10^23" }
    - { name: "Mars", mass: " 6.42 * 10^23" }
    - { name: "Venus", mass: "4.87 * 10^24" }
    - { name: "Earth", mass: "5.97 * 10^24" }
    - { name: "Uranus", mass: " 8.68 * 10^25" }
    - { name: "Neptune", mass: "1.02 * 10^26" }
    - { name: "Saturn", mass: " 5.68 * 10^26" }
    - { name: "Jupiter", mass: "1.90 * 10^27" }
---
Hello! We are located at the ${system.name}!
The central body here is ${system.centralBody}.
The planets and their masses are as follows:

${foreach: ${system.planets}, ${planet => \
  - ${planet.name} - ${planet.mass}
}}
"""

println(Thera(template).mkString)

// Hello! We are located at the Solar System!
// The central body here is Sun.
// The planets and their masses are as follows:

//   - Mercury - 3.30 * 10^23
//   - Mars -  6.42 * 10^23
//   - Venus - 4.87 * 10^24
//   - Earth - 5.97 * 10^24
//   - Uranus -  8.68 * 10^25
//   - Neptune - 1.02 * 10^26
//   - Saturn -  5.68 * 10^26
//   - Jupiter - 1.90 * 10^27
```

Thera is a template engine for Scala. It is intended to help people build static websites (such as ones deployed to [GitHub Pages](https://pages.github.com/)) in Scala.

- [Getting started](#getting-started)
- [Templates](#templates)
- [ValueHierarchy](#valuehierarchy)
- [Creating and using ValueHierarchies in templates](#creating-and-using-valuehierarchies-in-templates)
- [Functions](#functions)
- [Predefined functions](#predefined-functions)
- [Lambdas](#lambdas)
- [Syntactic rules](#syntactic-rules)
  * [Escapes](#escapes)
  * [Whitespace parsing](#whitespace-parsing)
- [Philosophy](#philosophy)
- [Roadmap](#roadmap)
- [Contributions](#contributions)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

## Getting started
Requires Scala 2.13. Add the following dependency to your SBT project:

```scala
libraryDependencies += "com.akmetiuk" %% "thera" % "0.2.0-M2"
```

Or in Mill:

```scala
def ivyDeps = Agg(ivy"com.akmetiuk::thera:0.2.0-M2")
```

Or in Ammonite:

```scala
import $ivy.`com.akmetiuk::thera:0.2.0-M2`
```

## Templates
A template consists of two parts – header and body. They are delimited by `---`. A header is formatted as [Yaml](https://yaml.org/) and defines the variables accessible to the template body. The template body can access these variables via `${path.to.variable}` syntax. You can process the template via `Thera(templateString).mkString` syntax.

```scala

val person =
"""
---
person:
  name: Tom
  age: 40
---
${person.name} is aged ${person.age}
"""

println(Thera(person).mkString)  // Tom is aged 40

```

## ValueHierarchy
A template context is the hierarchy of variables accessible to the template when it is processed. Yaml header is parsed to such a hierarchy.

Internally the hierarchy is represented as a `ValueHierarchy`. Given `h: ValueHierarchy`, you can query the member variables programmatically from Scala via `h("path.to.variable")` syntax. This call will return a `Value`. A Value can be one of the following:

- `Str(value: String)` – a String
- `Arr(value: List[Value])` - a collection of values
- `Function(f: (List[Value]) => Str)` - a function – such as `foreach` function in the example at the beginning of this document
- `ValueHierarchy` – a nested value hierarchy
- Throws a RuntimeException - if the queried path doesn't point to a variable

## Creating and using ValueHierarchies in templates
You can create a `ValueHierarchy` from Yaml, or a Scala `Map` using methods defined in its companion object. If you defined a value hierarchy as an implicit value, the `mkString` method of a template will implicitly pick it up and add to the template context:

```scala
val book =
"""
---
books:
  masteringScala:
    title: Mastering Scala
---
${books.masteringScala.title} costs \$${books.masteringScala.price}.
It was released in ${books.masteringScala.year}
"""

implicit val ctx = ValueHierarchy.names(
  "books" -> ValueHierarchy.names(
    "masteringScala" -> ValueHierarchy.names(
      "price" -> Str("20"),
      "year" -> Str("2015")
    )
  )
)

println(Thera(book).mkString)

// Mastering Scala costs $20.
// It was released in 2015
```

## Functions
You can define functions, put them in the template context and call them from a template. You can do so via methods in the `Function` companion object. For example:

```scala
val hiTml =
"""
${sayHi: World}
"""

implicit val ctx = ValueHierarchy.names(
  "sayHi" -> Function.function[Str] { name: Str =>
    Str(s"Hello ${name.value}")
  }
)

println(Thera(hiTml).mkString)

// Hello World
```

Templates can also be functions. The argument are specified in square brackets at the top of the header. You can use them as ordinary Scala functions as follows:

```scala
val hiTml =
"""
---
[title, name]
city: Lausanne
---
Welcome to $city, $title $name!
"""

val hi: List[Value] => String = Thera(hiTml).mkFunction
println(hi(Str("Mr") :: Str("Jack") :: Nil))

// Welcome to Lausanne, Mr Jack!
```

Or you can make a Thera `Function` out of them and pass them to other templates:

```scala
val hiTml =
"""
---
[title, name]
city: Lausanne
---
Welcome to $city, $title $name!
"""

val wrapperTml =
"""
${greetingsFun: Mr, Jack}
"""

val hi: Function = Thera(hiTml).mkValue.asFunction
implicit val ctx = ValueHierarchy.names(
  "greetingsFun" -> hi
)

println(Thera(wrapperTml).mkString)

// Welcome to Lausanne, Mr Jack!
```

## Predefined functions
Currently the following functions are available out of the box in Thera:

- `id: Str => Str` – identity, evaluates to its input.
- `foreachSep: (arr: Arr, sep: Str, f: Function) => Str` - applies `f` to every element of `arr`. Then concatenates the results while separating them with `sep`.
- `foreach: (arr: Arr, f: Function) => Str` – like `foreachSep` where `sep` is an empty string.
- `if: (cond: Str, ifTrue: Str, ifFalse: Str) => Str` - if `cond` is `true`, evaluates to `ifTrue`, otherwise – to `ifFalse`.
- `outdent: (size: Str, text: Str) => Str` – outdents every line of `text` by `size`. Useful when working with lambdas.

## Lambdas
If a function you are calling accepts another function as an argument, you can define this other function inline using a lambda syntax: `${ arg1, arg2, ... => body }`. For example:


```scala
val article =
"""
---
tags: [scala, functional, programming]
---
Tags are: ${foreachSep: $tags, \, , ${x => Tag $x}}
"""

println(Thera(article).mkString)

// Tags are: Tag scala, Tag functional, Tag programming
```

## Syntactic rules
### Escapes
Symbols `$`, `{` and `}` are significant symbols for the template. If you want to use them as plain text, you need to escape them with `\`, e.g. `\$`.

### Whitespace parsing
In function calls and lambdas, we need to decide when to parse the whitespaces and when to drop them for ergonomics reasons. For example: `${foreachSep: $tags, \, , ${x => Tag $x}}` – here, the whitespace before `$tags` and `\,` is for convenience of reading rather than for the output. Hence, in arguments to the function calls, we always drop initial whitespaces and start the argument parsing from the first non-whitespace character.

You can modify this behavior by escaping the whitespace: `${foreachSep: $tags,\ \, , ${x => Tag $x}}` – here, the separator will be `" , "` instead of `", "`.

In the bodies of lambdas, the story is similar. To start parsing the whitespaces, you need to escape them, e.g.:

```scala
${foreach: ${system.planets}, ${planet => \
  - ${planet.name} - ${planet.mass}
}}
```

## Philosophy
This project started as a static website generator because there wasn't one for Scala and I needed one to generate my blog. Since then, however, I realised that Scala doesn't need a static website generator. It has a powerful enough ecosystem for a user to effortlessly unroll their own logic for generating a website using existing libraries. For instance, my [blog](https://akmetiuk.com/) uses [Ammonite](https://ammonite.io/) and [os-lib](https://github.com/lihaoyi/os-lib) in conjunction with [Pandoc](https://pandoc.org/), a [Docker](https://www.docker.com/) image that defines the environment with Pandoc in it and [GitHub Actions](https://github.com/features/actions) that runs the Docker and deploys the website to [GitHub Pages](https://pages.github.com/). You can have a look at the sources of the blog [here](https://github.com/anatoliykmetyuk/anatoliykmetyuk.github.io).

The only missing piece in the ecosystem is a good templating engine. Thera attempts to provide such an engine for Scala. It doesn't aim to be a markdown processor or a website generator since these tasks can already be easily done using other tools.

## Roadmap
The main thing that Thera currently lacks is a good error reporting mechanism.

## Contributions
If you would like to collaborate on this project, do not hesitate to contact me about it!
