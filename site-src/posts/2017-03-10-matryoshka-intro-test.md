---
template: post
variables:
  title: Introduction to Recursion Schemes with Matryoshka
  categories: [blog]
  description: Motivation for the recursion schemes based on fixed-point types and their implementation in Matryoshka.
  keywords: [scala,programming,functional programming,category theory,recursion schemes,matryoshka,catamorphism,recursion]
filters: [post]
---

> Recursion is the GOTO of functional programming - Erik Meijer[^1]

[^1]: [https://twitter.com/headinthebox/status/384105824315928577?lang=en](https://twitter.com/headinthebox/status/384105824315928577?lang=en)

# Recursive data structures
In our daily programming life, we encounter recursive data structures on a regular basis. The best-known examples include linked lists and trees. Often working with such data structures we have a need to evaluate (collapse) them to a value. For example: 

- Given a list of integers, say 1, 2 and 3, one may want to find their sum 6.
- Given a parser of arithmetic expressions, such as `2 * 3 + 3`, we can expect it to produce a tree out of that expression - `Add(Mult(Num(2), Num(3)), Num(3))`. Such trees often need to be evaluated by actually performing these mathematical operations.
- A more abstract example: natural numbers. Given the number zero and an ability to construct a successor of any natural number, you can construct all the natural numbers. If `Zero` is such a zero number, and `Succ(x)` constructs a natural number following `x`, `Succ(Succ(Succ(Zero)))` can represent `3`. This is also a recursive structure, and the simplest operation you want to do on it is to actually evaluate it to an `Int`: `Nat => Int`.

In this article, we shall see how all of these examples involve recursion. [Don't Repeat Yourself (DRY)](https://en.wikipedia.org/wiki/Don't_repeat_yourself) is one of the fundamental principles of programming - so, if we repeat recursion from example to example, we should abstract it away. We shall see how to do that.

But first, let us set the foundation by doing all of the above examples in code.

## Natural Numbers
Here is how an implementation of natural numbers might look like:


And here is a visualization of the number 3 represented this way:

```{.graphviz width=100% #nat_diagram}
digraph G { label=" Structure " rankdir=LR
  S3 [label=" Succ "]

  subgraph cluster1 { label=" Substructure " graph[style=solid]
    S2 [label=" Succ "]

    subgraph cluster2 {
      S1 [label=" Succ "]
      
      subgraph cluster3 {
        "Zero"
      }
    }
  }

  S3 -> S2 -> S1 -> Zero [label=" previous "]
} 
```
