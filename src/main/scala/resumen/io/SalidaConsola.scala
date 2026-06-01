package resumen.io

import resumen.modelo.OracionPuntuada

object SalidaConsola {

  def mostrarResumen(oraciones: List[OracionPuntuada]): Unit =
    if (oraciones.isEmpty)
      println("No se encontraron oraciones para resumir.")
    else {
      println(s"Resumen extractivo (${oraciones.length} oraciones):")
      println()
      oraciones.zipWithIndex.foreach { case (op, i) =>
        println(
          f"${i + 1}%2d. [${op.puntaje}%.4f] (${op.oracion.documento}) ${op.oracion.texto}"
        )
      }
    }
}
