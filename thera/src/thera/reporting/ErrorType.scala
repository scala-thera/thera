package thera.reporting

sealed trait ErrorType {
  def toErrorMessage: String
}

// Parser errors

sealed trait ParserErrorType extends ErrorType

case object YamlError extends ParserErrorType {
  override def toErrorMessage: String = "Syntax error in YAML header"
}

case class NonExistentTopLevelVariableError(variable: String) extends ParserErrorType {
  override def toErrorMessage: String = f"Error in the variable name: non-existent top-level variable $variable"
}

case class NonExistentNonTopLevelVariableError(variable: String) extends ParserErrorType {
  override def toErrorMessage: String = f"Error in the variable name: non-existent non-top-level variable $variable"
}

case class NonExistentFunctionError(name: String) extends ParserErrorType {
  override def toErrorMessage: String = f"Error in the function name: non-existent function $name"
}

case object SyntaxError extends ParserErrorType {
  override def toErrorMessage: String = "Invalid syntax"
}

// Evaluation errors

sealed trait EvaluationErrorType extends ErrorType

case class WrongArgumentTypeError(expected: String, found: String) extends EvaluationErrorType {
  override def toErrorMessage: String = f"Incorrect argument type. Expected: $expected, found: $found"
}

case class WrongNumberOfArgumentsError(expected: Int, found: Int) extends EvaluationErrorType {
  override def toErrorMessage: String = f"Wrong number of arguments. Expected: $expected, found: $found"
}

case class InvalidFunctionUsageError(name: String) extends EvaluationErrorType {
  override def toErrorMessage: String = f"Invalid usage of a function $name. Functions can only be used as arguments to function calls."
}

case object InvalidLambdaUsageError extends EvaluationErrorType {
  override def toErrorMessage: String = f"Invalid usage of a lambda. Lambdas can only be used as arguments to function calls."
}