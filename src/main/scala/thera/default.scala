package thera

import better.files._, File._

/**
 * Reasonable defaults for various configuration options.
 */
 object default {
  val src      = file"src"
  val compiled = file"_site"

  def templateResolver(name: String): File = src/s"templates/$name.html"
  def fragmentResolver(name: String): File = src/s"fragments/$name.html"
 }