package thera.reporting

sealed trait ErrorType {
  def toErrorMessage: String
}

sealed trait ParserErrorType extends ErrorType

case object YamlError extends ParserErrorType {
  override def toErrorMessage: String = "Syntax error in YAML header"
}

case object FastparseError extends ParserErrorType {
  override def toErrorMessage: String = "TODO"
}

sealed trait EvaluationErrorType extends ErrorType

// TODO