package object thera {
  def tpl(src: String)(implicit line: sourcecode.Line): TemplateSource = TemplateSource(src, line)
}
