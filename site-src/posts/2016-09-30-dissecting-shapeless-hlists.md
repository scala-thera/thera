---
template: post
filters: [post]
variables:
  title: Dissecting Shapeless&#58; HLists
  redirect_from: /blog/2016/09/30/learning-shapeless-hlists.html
  description: An overview of the architecture of Shapeless' heterogenous lists (HList).
  keywords: [scala,shapeless,functional programming,category theory,heterogenous list,hlist,software architecture]
---
Shapeless is a Scala library that aims to make programming more type-safe. This means that you let the compiler know as much as possible about your program, so that if something is wrong with it, it is more likely to be caught during compile time.

Since a lot of the information about a program is represented by types, Shapeless utilises them heavily to achieve its goal.

In this post, I will cover the basics of Shapeless' `HList`s.

# Introduction
An `HList` is a heterogenous list. It is a generalisation of Scala's tuples. While ordinary Scala `List`s can contain only elements of one type (`List[Int]` for `Int`s, `List[String]` for `String`s etc), `HLists` can contain many. This is done in a type-safe way: The compiler knows about the types of the elements in the list.

Make sure to `import shapeless._` before running the examples. Here is a simple `HList`:

```scala
scala> val hlist = 1 :: 2 :: "foo" :: 3.5 :: HNil
hlist: shapeless.::[Int,shapeless.::[Int,shapeless.::[String,shapeless.::[Double,shapeless.HNil]]]] = 1 :: 2 :: foo :: 3.5 :: HNil
```

## The head of an empty list
Consider an example where we want to get the head of a list. Some lists are empty and this edge case should be handled somehow.

```scala
scala> List(1).head
res0: Int = 1

scala> Nil.head
java.util.NoSuchElementException: head of empty list
  at scala.collection.immutable.Nil$.head(List.scala:420)
  ... 42 elided

scala> (1 :: HNil).head
res2: Int = 1

scala> HNil.head
<console>:15: error: could not find implicit value for parameter c: shapeless.ops.hlist.IsHCons[shapeless.HNil.type]
       HNil.head
```

A normal Scala `List` throws an exception during runtime, but in case of `HNil`, a compilation error happens. Hence, with a `List` you won't know about the error until you run the program and this code line gets executed. In case of `HList`, the error will be identified during the compilation, allowing you to fix it right away and reduce the risk of bugs.

## HList architecture
Consider our previous example:

```scala
scala> val hlist = 1 :: 2 :: "foo" :: 3.5 :: HNil
hlist: shapeless.::[Int,shapeless.::[Int,shapeless.::[String,shapeless.::[Double,shapeless.HNil]]]] = 1 :: 2 :: foo :: 3.5 :: HNil
```

The most interesting thing here is the type of this object and how it is constructed. It is `shapeless.::`, recursive in its right-hand side argument. This type is defined as follows:

```scala
sealed trait HList extends Product with Serializable

final case class ::[+H, +T <: HList](head : H, tail : T) extends HList
```

There are two methods to help you construct `HList`s: one for `HNil`, which is an empty list, and one for `HList`, which is an arbitrary list:

```scala
trait HNil extends HList {
  def ::[H](h : H) = shapeless.::(h, this)
}

obejct HNil extends HNil

trait HListOps {
  def ::[H](h : H) : H :: L = shapeless.::(h, l)
}
```
With `::` it is possible to construct lists by induction:

- Given an arbitrary `H` and `T <: HList`, we can construct `H :: T`.
- An instance of an empty list `HNil` exists.

This pattern of building types inductively is observed in many parts of Shapeless. It allows to use simple primitives and inductive definitions to compose types.

# HList operations

## Architecture
To learn about what you can do with `HList`s, you will want to have a look at HList's [syntax](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/syntax/hlists.scala) and [operations](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/ops/hlists.scala) sources. Since there are a lot of operations defined, it makes sense to understand the general architecture and the patterns that are used.

### Syntax
The [syntax](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/syntax/hlists.scala) file features a `HListOps` class, to which `HLists` are implicitly converted. This class contains methods similar to the ones you can find in an ordinary Scala `List`.

The operations defined in `HListOps` follow a common pattern. For example:

```scala
def head(implicit c : IsHCons[L]) : c.H = c.head(l)
```

This method returns a first element of a list. Some observations to make here:

- The `IsHCons` implicit is a *type class* that encapsulates the required behaviour, and all the work is done by it. In this case, `IsHCons[L]` defines how to split a `L <: HList` into a head and a tail. All the operations on `HList`s are defined by type classes in a similar manner.
- This type class is parameterised by `L` which is the type of the `HList` the operation is called on. This is how Shapeless provides type safety: If there is no appropriate type class for `L` in scope, the operation is impossible and a compile-time error will happen.
- The return type `c.H` of the method is defined by the type class. Hence the logic responsible for `implicit` resolution is also responsible for the output type computation. The more complex this logic is, the more complex computations with types are possible during compile time.

### Operations
The [operations](https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/ops/hlists.scala) file contains all the type classes required by the `syntax`: their `trait`s and `implicit def`s to resolve them.

To understand their architecture, let us look at an example of `IsHCons` type class. This is a type class required implicitly by the `head` method discussed above, and it defines how to split an `HList` `L` into a head `H` and a tail `T <: HList`:

```scala
  trait IsHCons[L <: HList] extends Serializable {
    type H
    type T <: HList

    def head(l : L) : H
    def tail(l : L) : T
  }
  
  object IsHCons {
    def apply[L <: HList](implicit isHCons: IsHCons[L]): Aux[L, isHCons.H, isHCons.T] = isHCons

    type Aux[L <: HList, H0, T0 <: HList] = IsHCons[L] { type H = H0; type T = T0 }
    implicit def hlistIsHCons[H0, T0 <: HList]: Aux[H0 :: T0, H0, T0] =
      new IsHCons[H0 :: T0] {
        type H = H0
        type T = T0

        def head(l : H0 :: T0) : H = l.head
        def tail(l : H0 :: T0) : T = l.tail
      }
  }
```

Some observations to make here:

- A type class in Shapeless usually is a `trait`, type-parameterised by the types of its input.
- Output types are usually defined via the `type` keyword.
- type classes have companion objects. They usually consist of the following:
  - An `Aux` type definition to easily define the output types of the type classes.
  - An `apply` method to conveniently resolve a type class instance.
  - One or more `implicit def` that output the type class instances.

## The head of an empty list example internals
Consider our example with a head of an empty list discussed above. How exactly does the compiler verify that an operation is possible for `Int :: HNil`, but is not possible in case of `HNil`?

First, the target `HList` is converted implicitly to `HListOps`, which has the method `head`. This method can be called only if the scope defines the required type class.

There is one `implicit def` in the companion object of `IsHCons`. It has the following signature:

```scala
implicit def hlistIsHCons[H0, T0 <: HList]: IsHCons.Aux[H0 :: T0, H0, T0]
```

In case of `Int :: HNil`, the required type is `IsHCons[Int :: HNil]` and `IsHCons.Aux[H0 :: T0, H0, T0]` conforms to it because this type is an alias for `IsHCons[H0 :: T0]` with `H0` and `T0` output types (one for the head and one for the tail). So the implicit is found and the compilation succeeds.

In case of `HNil`, however, the required type class has the type of `IsHCons[HNil]`. Since `IsHCons.Aux[H0 :: T0, H0, T0]` does not conform to this type (`HNil` cannot be represented as `H0 :: T0`), the compiler cannot find the type class and the compilation fails.

## Length of `HList`s and type-level natural numbers
Recall that the output types of the `HList` operations are often defined by the type classes, which are resolved implicitly. Hence, one can argue that the output types are computed by the same capabilities that resolve the implicits, and the complexity of the computations you can perform with the types in such a manner is proportional to the complexity of the implicits' resolution mechanics.

In Shapeless, a common pattern is to define implicit `def`s that themselves require some implicit type classes. For example, the following implicits are available for the `Length` type class, which computes the length of a given `HList`:

```scala
implicit def hnilLength[L <: HNil]: Length.Aux[L, _0]

implicit def hlistLength[H, T <: HList, N <: Nat](implicit lt : Length.Aux[T, N], sn : Witness.Aux[Succ[N]]): Length.Aux[H :: T, Succ[N]]
```

A key thing to notice here is that `implicit def hlistLength` itself requires implicit arguments. Moreover, the output type of one of these arguments is the same as the one of the `implicit def hlistLength` method itself. This triggers inductive search for implicits during compile time, where in order to resolve `Length[H :: T]` type class, we need to resolve `Length[T]` first.

We also have a `def` for `HNil` in scope, which doesn't take any implicit arguments and represents the base case of the induction. These two methods give us an inductive system:

- A length of a list is the length of its tail plus one.
- A length of an empty list is 0.

Hence, for a list of length `n`, `n + 1` `Length` type classes are needed to be resolved. For example, `Length[Int :: Int :: HNil]` requires `Length[Int :: HNil]`, which requires `Length[HNil]`, which is `hnilLength`.

The recursive nature of the resolution of implicits and the fact that the computations on types can be carried out using the same mechanics give rise to the type-level representation of natural numbers. `Nat` is their common trait, `_0 <: Nat` is `0`, and `Succ[N <: Nat] <: Nat` represents the successor of `N`.

Now, the length of `HNil` is `_0` in `Length.Aux[L <: HNil, _0]`. The input type is `L` and the output is `_0`. The length of `H :: T` given `T <: HList` is `Succ[N]`, where `N` is bound by `Length.Aux[T, N]`.

# Conclusion
This concludes a brief overview of Shapeless' `HList`s. Main points covered:

- Shapeless makes programs more type-safe, hence decreasing the risk of bugs.
- Syntax of the `HList`'s operations is defined in the `HListOps` trait, and each operation method is implemented via a type class.
- Type classes' instances are available via `implicit def`s of the companion objects of their traits.
- The result type of the operations of a type class resolved in this manner is computed during compile time.
- Often `implicit def`s themselves require other implicits, and the resolution passes several levels. This allows inductive definitions of both runtime value computations and compile time type computations.

In the next article, I will cover Shapeless polymorphic functions, their architecture and how they can be used with `HList`s.