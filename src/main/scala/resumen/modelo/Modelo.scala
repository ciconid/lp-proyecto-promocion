package resumen.modelo

case class Oracion(texto: String, documento: String)

case class OracionTokenizada(oracion: Oracion, tokens: List[String])

case class OracionPuntuada(oracion: Oracion, puntaje: Double)

case class Entidad(texto: String, tipo: String)

case class InfoAdicional(entidad: Entidad, descripcion: String)

case class Reporte(
    tema: String,
    resumen: List[OracionPuntuada],
    entidades: List[Entidad],
    informacion: List[InfoAdicional]
)
