---
layout: post
title: Thread safety of SubScript values (and other scoped references)
categories:
- blog
---

In [SubScript](https://github.com/scala-subscript/subscript), we have a possibility to use `val`s and `var`s in scripts. They are basically containers for data, just like Scala's `val`s and `var`s, but they store their data in SubScript VM data structures.

Script itself is a tree of nodes defining its execution flow. Some nodes contain maps from names to values where `val`s and `var`s store their data. When a `val` is initialized, it writes to this map. When a `val` is used from a script, it requests a node where it was called for a value of a certain name. If it was not found, the parent of this node is requested, and so on, until the data is found.

In other words, evaluation of `val`s and `var`s requires a scope of the node where their value is written.

For example, the following script:

```scala
script live =
  val a = 3
  println(a)
```

will consist of a sequence operator node with two children. The first child, `val a = 3`, will write to the sequence's map that `a` has a value of `3`. The second child, `println(a)`, will request this data and output it.

This approach, however, is not thread safe. The following `live` script, for example, will fail:

```scala
script live =
  val a = 3
  f(println(a))
  {..}

def f(task: => Unit) = new Thread (new Runnable {override def run {
  Thread.sleep(1000)
  task
}}).start()
```

When the new thread created by `f` calls `task` (which is `println(a)`), an attempt to resolve `a` is made. Since its value is stored in the sequential operator's node (the topmost one in the tree), we need an access to it. But on the resolution time the link between the `f(println(a))` child and the sequential parent node is broken, since this child was deactivated after successful execution.

The trickiness of the issue is mainly caused by the fact that SubScript is supposed to take care of all the threading and concurrency. You won't normally encounter any such problems if you operate solely in SubScript. But in some corner cases, you need to pass data to some foreign threads - for example, when working with frameworks.

Concretely, in ScalaFX the GUI-related calls should be done from GUI thread via `Platform.runLater()` method. It accepts a by-name argument and executes it from the GUI thread. If you try to pass there a SubScript value, you're likely to run into the problem described above.

The workaround is rather simple: we need to evaluate the reference that requires the scope while still in scope, and then pass its value outside the scope. In SubScript case, the solution is as follows:

```scala
script live =
    val a = 3
    {!val _a = a; f(println(_a))!}
    {..}

def f(task: => Unit) = new Thread (new Runnable {override def run {
  Thread.sleep(1000)
  task
}}).start()
```