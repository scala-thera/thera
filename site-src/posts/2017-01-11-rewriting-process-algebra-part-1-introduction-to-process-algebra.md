---
layout: post
title: Rewriting Process Algebra, Part 1&#58; Introduction to Process Algebra
categories:
- blog
description: A rewriting-based process algebra implementation in Scala.
keywords: process algebra,scala,subscript,free object,functional programming,algebra of communicating processes,acp,category theory,concurrent programming,concurrency,reactive programming
---
This is the first part of a progress report on my attempt to model a process algebra as an expression rewriting machine. The process algebra in question is [SubScript](http://subscript-lang.org/from-acp-and-scala-to-subscript/)[^1][^2], which is an extension of [ACP](https://en.wikipedia.org/wiki/Algebra_of_Communicating_Processes). It is recommended to familiarize yourself with ACP and [process algebras](https://en.wikipedia.org/wiki/Process_calculus) before reading this article further.

This part covers the idea of process algebra and why it can be useful. It is a bird's eye glance of how a process algebra engine can help you in writing programs. You can have a look at the work in progress at the [FreeACP](https://github.com/anatoliykmetyuk/free-acp) repository.

Other parts of this report:

- [Rewriting Process Algebra, Part 2: Engine Theory](/blog/2017/01/12/rewriting-process-algebra-part-2-engine-theory.html)
- [Rewriting Process Algebra, Part 3: FreeACP Implementation](/blog/2017/01/13/rewriting-process-algebra-part-3-freeacp-implementation.html)

**Disclaimer:** Note that the theory and implementation in this series is a work in progress. Although their aim is to implement the features of SubScript in the long run, in its current state this work is far from its goal. Hence, the theory used in this series should be considered only as a language-independent foundation to the implementation of FreeACP to date. The series should not be considered a tutorial on ACP or any other process algebra. The terminology used in the series has a meaning as defined in the article and may not comply to that of ACP or any other process algebra.

[^1]: [http://subscript-lang.org/papers/subscript-white-paper/](http://subscript-lang.org/papers/subscript-white-paper/)
[^2]: [https://arxiv.org/abs/1504.03719](https://arxiv.org/abs/1504.03719)

# Process algebra expressions

## Theory
A **process algebra (PA) expression** consists of atomic actions (AAs), special operands and operators. It describes some process with the help of these elements.

An **AA** specifies an executable action. During its execution, side effects can occur and the result of such an execution is either a **success** (denoted by **ε**) if the computation proceeded as planned, or a **failure (δ)** if an error occurred (in JVM languages, a thrown exception) during the execution.

ε and δ are two fundamental **special operands**.

**Operators** define in which order AAs are to be executed and how their outcomes influence one another. The behavior of an operator is its **semantics**.

The idea of a **PA engine** is to execute a PA expression according to the rules of the particular PA in question. The main job of such an engine is to implement the semantics of the PA entities — what an operator should do, how to execute an AA, etc.

## Example
Here is an example of how this theory can be used in the context of a Scala GUI application controller. This example will be used throughout the series, so make sure you remember where to find it.

Consider a GUI application with two buttons, `first` and `second`, as well as a text field `textField`. If the `first` button was pressed, you want to set `textField`'s text to "Hello World", if the `second` was pressed - to "Something Else".

This can be described by the following PA expression:

```scala
button(first ) * setText(textField, "Hello World"   ) +
button(second) * setText(textField, "Something Else")
```

Let `button(btn)` be an atomic action that happens when the button `btn` is pressed. The action `button(btn)` performs is a wait upon `btn`. When `btn` is pressed, the action finishes successfully.

Let `setText(textField, string)` specify an AA, the action of which sets a text of a `textField` to `string`.

`*` is a sequential operator, specifying that its operands should execute one after another.

`+` is a choice operator, its precedence is lower than that of `*`. A choice is one of the fundamental operators of the ACP, along with `*`. It selects one of its operands to execute and discards the others. The mechanism of such a choice is not specified and depends on implementation. For the sake of this example, assume that this particular implementation is as follows: Given two arbitrary PA expressions as its operands, the first one to start (have an AA successfully evaluate to ε in it) will be chosen for full execution and the other one will be discarded.

Hence, in the example above, if the button `first` is pressed, the following will happen:

1. `button(first)` will evaluate to `ε`.
2. Since one of its operands has started, `+` will discard the `button(second) * setText(textField, "Something Else")` - now, even if the user presses the `second` button, `setText(textField, "Something Else")` has no chance of being executed.
3. According to `*` in `button(first) * setText(textField, "Hello World")`, its second operand must be executed after the successful execution of the first one. Since this has happened, `setText(textField, "Hello World")` will be executed, setting the text of `textField` to "Hello World".

### Intuition
You can check your intuition regarding the semantics of the example above by having a look at how the same behavior can be expressed in plain Scala.

In the code below, consider `eventSystem` to be some object you can register and deregister callbacks of the form `Event => Unit` with. It is guaranteed that these callbacks will be called on a button click - a typical scenario in a Scala GUI application.

Then, the above example would be expressed as follows:

```scala
// button(first) * setText(textField, "Hello World")
val firstButtonCallback: Event => Unit = e => {
  if (e.button == first) {  // button(first) action: `first` was clicked
    // Effects of the "+" operator: the choice has been made, so deregister all the callbacks
    eventSystem.deregister(firstButtonCallback )
    eventSystem.deregister(secondButtonCallback)

    // Proceed with reaction to `first` click
    atGuiThread { textField.text = "Hello World" }  // setText(textField, "Hello World") action: set the text for the text area
  }
}

// button(second) * setText(textField, "Something Else")
val secondButtonCallback: Event => Unit = e => {
  if (e.button == second) {  // button(second) action: `second` was clicked
    // Effects of the "+" operator: the choice has been made, so deregister all the callbacks
    eventSystem.deregister(firstButtonCallback )
    eventSystem.deregister(secondButtonCallback)

    // Proceed with reaction to `second` click
    atGuiThread { textField.text = "Something Else" }  // setText(textField, "Hello World") action: set the text for the text area
  }
}

// The callbacks are set up to discard one another on corresponding event
// The code below the comment adds them both to the event system, emulating the `+` choice operator like this:
// button(first) * setText(textField, "Hello World") + button(second) * setText(textField, "Something Else")
eventSystem.register(firstButtonCallback )
eventSystem.register(secondButtonCallback)
```

# Conclusion
This first part of the progress report introduced the concept of process algebra and demonstrated how it can be used in practice. In the [second part](/blog/2017/01/12/rewriting-process-algebra-part-2-engine-theory.html), we will have a look at the standard implementation of an engine for SubScript that is able to execute PA expressions. Also, we will introduce a possibility of an alternative implementation of the engine, FreeACP.