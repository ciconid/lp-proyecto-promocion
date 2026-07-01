package resumen

import resumen.core.{Entidades, FiltroTema, Resumidor}
import resumen.io.{
  ClienteWikipedia,
  DetectorEntidades,
  EntradaConsola,
  LectorDocumentos,
  SalidaConsola
}
import resumen.modelo.Reporte

object Main {

  def main(args: Array[String]): Unit = {
    val ruta = args.headOption.getOrElse("archivos")
    val tema = EntradaConsola.leerTema()

    val documentos = LectorDocumentos.leerDirectorio(ruta)
    val relevantes = FiltroTema.documentosRelevantes(tema, documentos)

    if (relevantes.isEmpty)
      SalidaConsola.sinDocumentos(tema)
    else {
      val resumen = Resumidor.resumir(relevantes)
      val entidades = Entidades.normalizar(DetectorEntidades.detectar(relevantes))
      val informacion =
        Entidades.personasYOrganizaciones(entidades).flatMap(ClienteWikipedia.describir)
      SalidaConsola.mostrarReporte(Reporte(tema, resumen, entidades, informacion))
    }
  }
}
