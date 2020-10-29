package thera.reporting

import sourcecode.File

case class FileInfo(file: sourcecode.File, isExternal: Boolean)
object FileInfo {
  def apply(implicit file: File): FileInfo = new FileInfo(file, isExternal = false)
}
