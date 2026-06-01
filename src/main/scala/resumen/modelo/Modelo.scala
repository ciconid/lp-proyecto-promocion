package resumen.modelo

case class Oracion(texto: String, documento: String)

case class OracionTokenizada(oracion: Oracion, tokens: List[String])

case class OracionPuntuada(oracion: Oracion, puntaje: Double)
