package resumen.io

import java.io.File
import scala.io.Source
import scala.util.Using

object LectorDocumentos {

  def leerDirectorio(ruta: String): List[(String, String)] = {
    val dir = new File(ruta)
    if (!dir.isDirectory) List.empty
    else
      buscarArchivos(dir)
        .sortBy(archivo => rutaRelativa(dir, archivo))
        .map(archivo => rutaRelativa(dir, archivo) -> leerArchivo(archivo))
  }

  private def buscarArchivos(dir: File): List[File] =
    Option(dir.listFiles())
      .map(_.toList)
      .getOrElse(List.empty)
      .flatMap { archivo =>
        if (archivo.isDirectory) buscarArchivos(archivo)
        else if (archivo.getName.toLowerCase.endsWith(".txt")) List(archivo)
        else List.empty
      }

  private def rutaRelativa(base: File, archivo: File): String =
    base.toPath.relativize(archivo.toPath).toString.replace('\\', '/')

  private def leerArchivo(archivo: File): String =
    Using.resource(Source.fromFile(archivo, "UTF-8"))(_.mkString)
}
