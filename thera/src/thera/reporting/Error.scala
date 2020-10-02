package thera.reporting

sealed abstract class Error(path: String, line: Int, column: Int, codeSnippet: String, errorMessage: String) extends Exception {

  override def toString: String = {
    val cursor = " " * (column - 1) + "^"
    val prefixingWhitespaces = " " * (Math.floor(Math.log10(line)).toInt + 1)

    f"""\n-- Error: $path:$line:$column -------
       #$line | $codeSnippet
       #$prefixingWhitespaces | $cursor
       #$prefixingWhitespaces | $errorMessage
    """.stripMargin('#')
  }
}

case class ParserError(path: String, line: Int, column: Int, codeSnippet: String, errorType: ParserErrorType) extends
  Error(path, line, column, codeSnippet, errorType.toErrorMessage)

case class EvaluationError(path: String, line: Int, column: Int, codeSnippet: String, errorType: EvaluationErrorType) extends
  Error(path, line, column, codeSnippet, errorType.toErrorMessage)
