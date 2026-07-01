package resumen.core

import resumen.modelo.{Oracion, OracionPuntuada, OracionTokenizada}

object Resumidor {

  val maxOraciones: Int = 10

  def resumir(documentos: List[(String, String)]): List[OracionPuntuada] = {
    val tokenizadas = segmentar(documentos)
      .map(o => OracionTokenizada(o, Tokenizador.tokenizar(o.texto)))
      .filter(_.tokens.nonEmpty)

    val idf = TfIdf.idf(tokenizadas.map(_.tokens))

    val puntuadas = tokenizadas.map { ot =>
      OracionPuntuada(ot.oracion, TfIdf.puntuar(ot.tokens, idf))
    }

    seleccionar(puntuadas)
  }

  private def segmentar(documentos: List[(String, String)]): List[Oracion] =
    documentos.flatMap { case (documento, contenido) =>
      Tokenizador.segmentar(contenido).map(texto => Oracion(texto, documento))
    }

  private def seleccionar(puntuadas: List[OracionPuntuada]): List[OracionPuntuada] =
    puntuadas.sortBy(-_.puntaje).take(maxOraciones)
}
