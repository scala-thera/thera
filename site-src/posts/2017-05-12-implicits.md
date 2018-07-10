---
title: Implicits mechanism in Scala
keywords: implicit,scala
---

# Motivation
There are three things Scala implicits mechanism is good for:

- Conversion from one type to another
- Method injection
- Dependency injection

**Conversion** from one type to another may be needed when a variable of one type is used where another type is expected. For example, a `java.lang.Integer` is used where `scala.Int` is expected. The conversion is straightforward, but it may be a nuisance to do that by hand. Implicit conversion from `Integer` to `Int` (and vice versa) enables you to use one when the other is expected and have the compiler to do the conversion automatically.

**Method injection** allows you to inject and subsequently call a method on a class that does not have this method defined or inherited. The "injection" is meant in a metaphorical sense - the class definitions are not modified. For example, in Scala you can call the collections methods (`map`, `flatMap` etc) on a `java.lang.String`, which does not define them.

**Dependency injection** is needed in the majority of large applications to glue their modules together. In Java, people use Spring Framework for this, and in Scala the Pie pattern or injection via constructor parameters are popular. One way to think about the implicits is a language-level support for dependency injection. Whenever we define something that relies on something else, we can declare this dependency via implicits.

This may not look like much at a glance. But the implicits mechanism powers a whole programming style oriented on purely functional and type level programming based on category theory.

This article explains the Scala implicits and these three ways to make use of them.

# Example
The best way to understand a concept is to see its concrete application.

Consider a web application in Scala. Its job is to maintain a database of users and expose them as a JSON API. A user has an `Int` ID and a `String` name. Let us see how implicits can come handy in this scenario.

## Architecture
A natural way to model users is to define a `User` case class. Next, we need a way to perform read/write operations on a database of users. Finally, within our web framework we want to register an HTTP request handler to expose the JSON API to the users.

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="Setting"}
```

For simplicity, we do not use any real database or web framework here, we merely mock their functionality:

- JSON and HTTP handlers here are represented in terms of core Scala functionality.
- `serve` method represents a way to register an HTTP handler within your web framework. Since we do not have a framework, we use it for testing the handler by calling it with a test path.
- `UserDB` represents an interface to the database. `DummyUserDB` is a simple implementation of it that uses a `Map` as a persistence backend. By the end of the tutorial we will have two such dummy access objects, so it is useful to abstract this functionality in a separate class.
- `UserDBSql` pretends to use a SQL database as a backend.

## Execution
With that architecture, in order to expose the API, we need to register a handler for the HTTP path of that API within the web framework using the `serve` method.

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E1_1_Force_Conversion"}
```

The above code first writes a sample user to the database. Then it defines a HTTP request handler for the JSON API to the users. We use regex to define a RESTful path of the form `"/api/user/${userId}"`. Inside the handler, we read the user with the requested id from the SQL database, serialize it to JSON and return.

# Implicit Conversions
## Introduction
Notice how we convert the `user` object to `Json` after reading it. This is a technical step that diverts our focus from the task at hand - responding to HTTP request. It is rather obvious too - we need to return `Json`, we have `User` and the conversion process from `User` to `Json` is well defined - a call to `userToJson`.

It turns out Scala compiler can also see that and can wrap the `user` into the conversion method `userToJson` automatically for us. We can instruct it to do so by prefixing the `def userToJson` with the `implicit` keyword:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="userToJson"}
```

Now we can write the handler as:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E1_2_Conversion"}
```

This `implicit def` is an *implicit conversion*. It behaves almost exactly the same way the original `def` does. For instance, you can call it explicitly as an ordinary `def`. The only difference is that whenever the compiler encounters `User` where `Json` is expected, it now has our permission to automatically use that method to convert one to another.

## Mechanics
In the example above, the `ApiHandler` is an alias for `PartialFunction[String, Json]`. This means the compiler knows that if we define a partial function of that type (the `handler`), we must return a value of type `Json` from it. It also sees that we return `User` instead. So the expression of type `User` is used where an expression of type `Json` is expected. At this point, the compiler will start to look for an implicit conversion from `User` to `Json`. If it finds such a conversion, it will use it, otherwise it will fail with an error as we would expect.

In order to find a conversion, the compiler looks through the *implicit scope* of the site where the conversion needs to be applied (in our case, the place where we returned `user`). There are complicated [rules](http://stackoverflow.com/a/5598107/3895471) to which implicit methods end up in the scope. But as a rule of thumb, if you can call an `implicit def` without using its fully qualified path, you have it in scope. The most common scenario is when you import the conversion or define it locally.

To find the appropriate conversion, the compiler looks at the signatures of the implicit methods in scope. It knows that `PartialFunction[String, Json]` expects a `Json` to be returned, but `User` was returned. So, the required conversion must be a function that takes `User` as an argument and has `Json` as its return type. `userToJson` is an implicit def, is in scope and has the correct signature - so the compiler uses it.

# Method Injection with Rich Wrappers
## Theory
Implicit conversions can be used to augment existing types with new methods. For example, instead of explicitly calling the database each time you want to perform a read or write operation, you can augment the model objects with corresponding methods.

Whenever we call a method on an object that does not have this method defined, the compiler tries to implicitly convert it to a class on which you can call this method. If there is no such conversion in scope, it fails as expected.

Hence the Rich Wrapper pattern. A rich wrapper to augment some type `T` with a method `f` includes:

- A wrapper class that has:
  - A reference to the object to be augmented
  - The `f` method we want to augment `T` with
- An implicit conversion from `T` to the wrapper class.

This way, whenever we call the method `f` on an object of type `T` that does not have it defined, the compiler will implicitly convert `T` to the wrapper and call `f` on the wrapper. Due to the fact that the wrapper has a reference to the wrapped object (the one being converted), we can access this object from `f`.

## Example
Let us try to augment the `User` class with a `write()` method and the `String` with `readUser()` method. This way, we can call `u.write()` instead of `UserDBSql.write(u)` and `id.toInt.readUser()` instead of `UserDBSql.read(id.toInt)`. Notice how the read operation on an `Int` can be ambiguous: we can have more models than just `User` that we might want to read this way. So we should use non-ambiguous name for the method.

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E2_Wrapper_conversions"}
```

After we have these conversions in scope, we can rewrite our example as follows:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E2_Wrapper_example"}
```

The compiler will encounter the `u.write()` and `id.toInt.readUser()`, but will not find the `write()` and `readUser()` methods in `User` and `Int` correspondingly. It will then look at the types of the objects the methods are called on and will try to find the conversions from these types to the ones that have the required methods. In our case, these objects will be converted to the rich wrappers we have defined.

# Dependency Injection with Implicit arguments
## Theory
A `def` may have its last argument group defined as `implicit`. This means that we we can ignore this entire group. When the method is called, the compiler will look for the missing arguments in the implicit scope for us.

It works similarly to how the compiler looks for implicit conversions: just that in case of the conversions it looks for methods with a particular signature (`TargetType => RequiredType`) and in case of implicit arguments it looks for `val`s or `def`s (yes, it is possible to put `val`s to the implicit scope too) with the required return type.

This can be used to declare dependencies for method calls or object constructions (a class constructor can also have an implicit argument group). Then, to inject the dependency, we just need to make sure it is on the implicit scope when we call the method or the constructor.

## Example
In our example, the rich wrappers depend on the database access object. Let us see what happens if we want to support multiple database backends - for instance, a MongoDB backend:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="MongoDB"}
```

Then, whenever we want to change the backend we use, we will need to change every occurrence of it in the code. That's not very DRY, so normally you would assign the backend you want to use to a variable and reference it every time you need the backend:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E3_1_Force_Context_conversions"}
```

This introduces a dependency on a global variable, however. `RichUser`, `RichId` and any other code that needs a backend must know exactly where this variable is located. This can greatly reduce flexibility in case of large code bases: every time you work with the backend, you are forced to think globally, and this reduces focus on the current task. Modular, purely local solutions where you need to think only about the local piece of code you are currently writing, are preferred. Hence the need for a dependency injection mechanism.

With the implicit arguments, we can perform the dependency injection as follows:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E3_Context_conversions"}
```

Notice how the `write()` and `readUser()` methods no longer depend on anything outside the scope of their parent classes. Next, notice how the `db` backend is passed as an implicit argument to the implicit conversion methods. When the compiler needs to call these conversions, it will look up these implicit arguments and inject them into the original `implicit def`s:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E3_Context_example"}
```

We still have the backend stored in one variable, but this time no code that requires it references it directly. Instead, the implicits mechanism acts as a dependency injection framework. All we need to do before calling a method with implicit dependencies is to place these dependencies at the implicit scope before the call is performed.

# `implicit class` pattern
The rich wrapper pattern discussed above is so commonly used that there is a language level support for it in Scala. It is possible to declare an `implicit class` that has exactly one constructor parameter (not counting a possible implicit parameter group). This will declare a class as usual, but also an implicit conversion from its constructor's argument type to that class:

```{.scala include="code/implicits/src/main/scala/implicitconversions/Main.scala" snippet="E4_WrapperShorthand_conversions"}
```

Notice how class constructors can also have an implicit argument group.

# A word of caution
There is another rather technical conversion in our code that potentially can be simplified away with implicit conversions - it is `id.toInt` in `UserDBSql.read(id.toInt)`.

`case usersApiPath(id)` matches the `String` fed into the handler against the regex we defined and captures the text group with the id of the requested user. The problem is, our ids are `Int`s, but the regex captures everything as a `String`. So in order to call the `read` method, we first need to convert a `String` to an `Int` - hence `id.toInt`.

At a glance, we may think it is a good idea to do this via an implicit conversion:

```scala
implicit def strToInt(s: String): Int = Integer.parseInt(s)
```

But what happens if someone glances at `UserDBSql.read()` and thinks it queries the database by username, not an id. Not an unlikely scenario: in a reasonable implementation of the database schema will, every user can be uniquely identified by both its id and the username. So without looking at the `read()`'s signature, one may assume it accepts a username of type`String` and proceed to call `UserDBSql.read("Bob")`.

First consider the scenario without an implicit conversion. The compiler will see that we have made a mistake by looking at the types (`Int` expected but `String` found), and will emit an error.

But what if we have an implicit conversion from `String` to `Int` in scope? In this case, the compiler will not fail with an error and try to use the implicit conversion instead. On runtime, this conversion is bound to fail with an exception, since `"Bob"` is not a valid number. But on compile time, the compiler has no way of knowing this and will successfully compile the program.

So the error that normally would have been discovered on compile time remains undetected until the runtime. And it is much harder to detect and debug runtime errors.

Scala's strong type system aims to safeguard against errors and bugs, catching as much of them as possible on compile time. The implicits mechanism is a powerful feature of the compiler. However with great power comes great responsibility. If you overuse it, you may introduce bugs at runtime, harm the robustness of your program and make it hard to understand (situation known as "implicits hell").

So when do you use them and when - not? When you bring implicit conversions in scope you instruct the compiler to make certain assumptions about your program. It assumes you want it to perform the conversion whenever applicable without asking you. In each individual case, you should consider the consequences of this additional freedom you give to the compiler.
