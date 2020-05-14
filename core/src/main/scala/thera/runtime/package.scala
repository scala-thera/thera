package thera

package object runtime {
  type Args = List[Runtime]


  def function[R1 <: Runtime](f: (R1) => Runtime)(implicit ctx: Context) = Function {
    case (r1: R1 @unchecked) :: Nil => f(r1)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 1-ary function")
  }

  def function[R1 <: Runtime, R2 <: Runtime](f: (R1, R2) => Runtime)(implicit ctx: Context) = Function {
    case (r1: R1 @unchecked) :: (r2: R2 @unchecked) :: Nil => f(r1, r2)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 2-ary function")
  }

  def function[R1 <: Runtime, R2 <: Runtime, R3 <: Runtime](f: (R1, R2, R3)(implicit ctx: Context) => Runtime) = Function {
    case (r1: R1 @unchecked) :: (r2: R2 @unchecked) :: (r3: R3 @unchecked) :: Nil => f(r1, r2, r3)
    case x => throw new RuntimeException(s"Argument list $x is inapplicable to 3-ary function")
  }
}
