package resumen.io

import resumen.modelo.{Entidad, InfoAdicional, OracionPuntuada, Reporte}

object SalidaConsola {

  private val etiquetas: Map[String, String] = Map(
    "PER"  -> "Personas",
    "ORG"  -> "Organizaciones",
    "LOC"  -> "Lugares",
    "MISC" -> "Otras entidades"
  )

  def sinDocumentos(tema: String): Unit =
    println(s"No se encontraron documentos para el tema: $tema")

  def mostrarReporte(reporte: Reporte): Unit = {
    println("=" * 60)
    println(s"REPORTE: ${reporte.tema}")
    println("=" * 60)
    mostrarResumen(reporte.resumen)
    mostrarEntidades(reporte.entidades)
    mostrarInformacion(reporte.informacion)
  }

  private def mostrarResumen(oraciones: List[OracionPuntuada]): Unit = {
    println()
    println("-- RESUMEN --")
    if (oraciones.isEmpty)
      println("No se encontraron oraciones para resumir.")
    else
      oraciones.zipWithIndex.foreach { case (op, i) =>
        println(
          f"${i + 1}%2d. [${op.puntaje}%.4f] (${op.oracion.documento}) ${op.oracion.texto}"
        )
      }
  }

  private def mostrarEntidades(entidades: List[Entidad]): Unit = {
    println()
    println("-- ENTIDADES --")
    if (entidades.isEmpty)
      println("No se identificaron entidades.")
    else
      entidades.groupBy(_.tipo).toList.sortBy(grupo => orden(grupo._1)).foreach {
        case (tipo, lista) =>
          println(s"${etiquetas.getOrElse(tipo, tipo)}:")
          lista.foreach(e => println(s"  - ${e.texto}"))
      }
  }

  private def mostrarInformacion(informacion: List[InfoAdicional]): Unit = {
    println()
    println("-- INFORMACIÓN ADICIONAL --")
    if (informacion.isEmpty)
      println("No se obtuvo información adicional.")
    else
      informacion.foreach { info =>
        val etiqueta = etiquetas.getOrElse(info.entidad.tipo, info.entidad.tipo)
        println(s"${info.entidad.texto} ($etiqueta):")
        println(s"  ${info.descripcion}")
        println()
      }
  }

  private def orden(tipo: String): Int =
    List("PER", "ORG", "LOC", "MISC").indexOf(tipo) match {
      case -1 => Int.MaxValue
      case i  => i
    }
}
