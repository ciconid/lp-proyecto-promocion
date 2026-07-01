package resumen.core

object FiltroTema {

  def documentosRelevantes(
      tema: String,
      documentos: List[(String, String)]
  ): List[(String, String)] = {
    val terminos = Tokenizador.tokenizar(tema).toSet
    if (terminos.isEmpty) documentos
    else
      documentos.filter { case (_, contenido) =>
        val tokens = Tokenizador.tokenizar(contenido).toSet
        terminos.exists(tokens.contains)
      }
  }
}
