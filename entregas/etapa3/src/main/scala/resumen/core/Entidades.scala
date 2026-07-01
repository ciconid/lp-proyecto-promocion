package resumen.core

import resumen.modelo.Entidad

object Entidades {

  private val orden: List[String] = List("PER", "ORG", "LOC", "MISC")

  def normalizar(entidades: List[Entidad]): List[Entidad] =
    entidades
      .map(e => e.copy(texto = e.texto.trim))
      .filter(_.texto.nonEmpty)
      .distinct
      .sortBy(e => (indice(e.tipo), e.texto.toLowerCase))

  def personasYOrganizaciones(entidades: List[Entidad]): List[Entidad] =
    entidades.filter(e => e.tipo == "PER" || e.tipo == "ORG")

  private def indice(tipo: String): Int =
    orden.indexOf(tipo) match {
      case -1 => orden.length
      case i  => i
    }
}
