package resumen.io

import java.io.File
import scala.io.Source
import scala.util.Using

object LectorDocumentos {

  def leerDirectorio(ruta: String): List[(String, String)] = {
    val dir = new File(ruta)
    if (!dir.isDirectory) List.empty
    else
      Option(dir.listFiles((_, nombre) => nombre.toLowerCase.endsWith(".txt")))
        .map(_.toList)
        .getOrElse(List.empty)
        .sortBy(_.getName)
        .map(archivo => archivo.getName -> leerArchivo(archivo))
  }

  private def leerArchivo(archivo: File): String =
    Using.resource(Source.fromFile(archivo, "UTF-8"))(_.mkString)
}
