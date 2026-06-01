package resumen

import resumen.core.Resumidor
import resumen.io.{LectorDocumentos, SalidaConsola}

object Main {

  def main(args: Array[String]): Unit = {
    val ruta = args.headOption.getOrElse("archivos")
    val documentos = LectorDocumentos.leerDirectorio(ruta)

    if (documentos.isEmpty)
      println(s"No se encontraron archivos .txt en el directorio: $ruta")
    else
      SalidaConsola.mostrarResumen(Resumidor.resumir(documentos))
  }
}
