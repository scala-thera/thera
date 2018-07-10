---
layout: post
title: Rewriting Process Algebra, Part 3&#58; FreeACP Implementation
categories:
- blog
description: A rewriting-based process algebra implementation in Scala.
keywords: process algebra,scala,subscript,free object,functional programming,algebra of communicating processes,acp,category theory,concurrent programming,concurrency,reactive programming
---
This is the third part of my progress report on a rewriting-based implementation of [SubScript](https://github.com/scala-subscript/subscript), [FreeACP](https://github.com/anatoliykmetyuk/free-acp). This part covers the architecture of FreeACP I came up with so far while implementing the rewriting engine for SubScript.

If you have not read the previous parts of this report, you are advised to do so before reading this one:

- [Rewriting Process Algebra, Part 1: Introduction to Process Algebra](/blog/2017/01/11/rewriting-process-algebra-part-1-introduction-to-process-algebra.html)
- [Rewriting Process Algebra, Part 2: Engine Theory](/blog/2017/01/12/rewriting-process-algebra-part-2-engine-theory.html)


# Tree
Process algebra expressions are modeled as ordinary [Tree](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L6)s. A `Tree` has a higher-kinded type argument to it, `S[_]`. `S` is the boxed type of a suspended computation's result, for example `Future[_]`.

There are the following notorious subclasses of `Tree`:

- Operators: [Sequence](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L85) and [Choice](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L86).
- Terminal cases: [Result](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L88) and its subclasses: `Success` and `Failure`, representing `ε` and `δ` respectively.
- [Suspend](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L83) - carries a suspended computation.

A suspended computation is carried by `Suspend` nodes as `S[Tree[S]]` and should be interpreted as follows: There is some computation running under `S` and the result of this computation is another process algebra expression `Tree[S]`.

For example, an ordinary AA's evaluation can be represented as `S[Result[S]]` and should be read as follows: There is some action running under `S`, and if it is successful (no exception happens), the result is `ε`, otherwise `δ`.

# Axioms
Sets of [rewriting](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L12) and [suspension](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L44) axioms are defined as partial functions and are straightforward to read.

Some points to note about them:

- Rewriting axioms take a `Tree[S]` and return a `Tree[S]` as a result - they just rewrite a given tree.
- Suspension axioms take a `Tree[S]` and return a `List[S[Tree[S]]]`. `List` reflects that there may be a need to make a choice between several trees. `S[Tree[S]]` means that the trees which the current one should be rewritten to are not readily available and are computed in `S`.

# Execution
These axioms are applied in a [loop](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L67) until a terminal case is reached, as described in the theory in [Part 2](/blog/2017/01/12/rewriting-process-algebra-part-2-engine-theory.html).

# Suspension type as a free object
If one has a `Tree[S]`, they do not have much choice but to execute it under `S`. This may not always be desirable: For instance, one may have a `Tree[`[Eval](https://static.javadoc.io/org.typelevel/cats-core_2.12/0.8.1/cats/Eval$.html)`]`, but want to execute it in parallel via `Future`.

Alternatively, `S` may stay constant, but the way of execution under `S` may not. A good example of this is the `setText(textField, string)` AA from our GUI example: We agreed that it sets the text of `textField` to `string` under a particular GUI framework we are working under. But what if we want our program to work under several GUI frameworks? Each of them will have its own implementation of `textField` and the way to set its text will differ between the frameworks.

For this reason, the function that runs the trees, [runM](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Tree.scala#L65), can take a natural transformation, `S ~> G`, using which one can specify how to map a suspended computation `S` to a suspended computation `G`.

Further increasing flexibility, we may even have a default implementation of `S` as a [free object](https://en.wikipedia.org/wiki/Free_object), in style of the [free monad](http://typelevel.org/cats/datatypes/freemonad.html).

## `LanguageT` as a free `S`
The pattern is as follows: All the expressions for FreeACP are written with suspension type `S` equal to [LanguageT](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Language.scala#L5) by default. These expressions are of type `Tree[LanguageT]`.

`LanguageT` is a free object - it does not do anything by itself, but remembers the operations you tried to perform on it. It does this by [reifying](https://en.wikipedia.org/wiki/Reification_(computer_science)) all the operations done on it into case classes. For example, `t.map(f) == MapLanguage(t, f)`.

Next, the user can select whichever `S` they want to execute their program under, define a natural transformation `LanguageT ~> S` (roughly, `LanguageT[Tree[LanguageT]] => S[Tree[LanguageT]]`) and use it in the `runM` method to execute the `LanguageT` instances. This natural transformation is called the **compiler**, it compiles your program written under `LanguageT` to a concrete suspension type `S`.

In our example of the `setText` AA, one may define the following class to represent its action:

```scala
case class SetText[TF](textField: TF, string: String) extends LanguageT[Result[LanguageT]]
```

It contains all the data necessary to set the text of the text field, but does not say anything about *how* to do it. Then, one can define a different natural transformation `LanguageT ~> S` for each GUI framework they are working under, each specifying the way this particular framework performs this action. This way, a GUI controller can be written once and executed on many GUI frameworks.

For example, such a natural transformation under Swing may look like:

```scala
new (LanguageT ~> Future) {
  override def apply[A](t: LanguageT[A]): Future[A] =
    t match {
      case SetText[TextField](textField, string) =>
        Future {
          Swing.onEDTWait { textField.text = string }
          ε
        }
    }
}
```

The point to notice here is that all the things specific to the GUI framework are encapsulated in this compiler, and hence one program can be executed under many GUI frameworks, provided one has proper compilers for these frameworks.

## Compilers for `LanguageT`

### Default compiler
There is a number of default subclasses of `LanguageT` that reify operations that are used in the suspension axioms (recall that the suspension axioms rely on `map: (A => B) => (S[A] => S[B])` and `suspend: A => S[A]`) and hence are necessary for the `LanguageT` to be used as a suspension type.

Hence, there is also a [default compiler](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Language.scala#L38) for such subclasses.

Internally, the compiler is a partial function, specifying how to translate various subclasses of `LanguageT` to `F`. For example, `case MapLanguage(t1, f)   => F.map(mainCompiler(t1))(f)` is the line handling `MapLanguage`, a reification of the `map` method called on `LanguageT`. All it does is mapping `t1` by `f` using a `F.map` where variable `F` is of type `Functor[F]`. In essence, the compiler describes how to *delegate* the `map` to the functor of `F`, whatever this `F` is.

In its definition, the default compiler declares some implicit arguments: `implicit F: Functor[F], S: Suspended[F]`. This means that internally it delegates some operations to a `Functor` and a `Suspend` type classes and hence depends on them. So if they are not in scope, there's nothing to delegate to and hence one can't instantiate the compiler for `F`. This also works another way around: whatever your `F`, if you have a `Suspended` and a `Functor` for it, you will be able to compile your program under that `F`.

*One can get the default compiler for `F` if and only if one has `Functor[F]` and `Suspended[F]`.*

### Compiler framework
It is possible to compose compilers for different subclasses of `LanguageT`. One can define their own subclass of `LanguageT` and add a compiler for that subclass, this way extending the expressive power they have.

By default, one can only use an [atomic action](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Language.scala#L15) to create a suspended computation, but one can define more primitives to write process algebra expressions with, as in the `setText()` example above.

The simplest example is a [say](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/component/Say.scala#L9) primitive that just prints something to the console. The pattern for `say` is as follows: first, we define a [subclass](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/component/Say.scala#L8) of `LanguageT` - wherever we use it, we want it to mean "print the payload to the console". Next, we define a [compiler](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/component/Say.scala#L11) capable of executing that subclass under some `F`. This compiler has a very similar structure to the default one, except that it can only handle [one case](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/component/Say.scala#L13) of the language: `SayContainer`.

Once defined, all the compilers can be composed to form a single compiler to be used in `runM`. This is done via the [compiler](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Language.scala#L34) function. Since all the "small" compilers return an `Option[F[Tree[LanguageT]]]`, the "large" compiler iterates through the list of the "small" ones until a `Some(_)` is returned.

Also, the "small" compilers, or [PartialCompiler](https://github.com/anatoliykmetyuk/free-acp/blob/0932ccde36b0efa83dd01b25ca1fee393154d987/core/src/main/scala/freeacp/Language.scala#L32)s are of type `Compiler[F] => LanguageT ~> OptionK[F, ?]` (`Compiler[F]` is `LanguageT ~> F`, and `OptionK[F[_], A]` is `Option[F[A]]`). This means that we are able to use the "large" compiler from the "small" ones, so that we can invoke it recursively.

# Conclusion
FreeACP is still a work in progress. The theory and architecture are far from perfect and hopefully will endure substantial changes in future. However, the expressive power it grants is promising and worth exploring.
