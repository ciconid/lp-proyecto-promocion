package resumen.io

import resumen.modelo.Entidad
import java.io.{InputStream, OutputStream}
import scala.sys.process.{Process, ProcessIO}
import scala.util.Try

object DetectorEntidades {

  val script: String = "ner_detector_v2.py"

  private val comando: Seq[String] = Seq("python3", script)

  def detectar(documentos: List[(String, String)]): List[Entidad] =
    if (documentos.isEmpty) List.empty
    else
      Try(ejecutar(construirEntrada(documentos)))
        .toOption
        .map(parsear)
        .getOrElse(List.empty)

  private def construirEntrada(documentos: List[(String, String)]): String =
    ujson.write(
      ujson.Obj.from(documentos.map { case (id, texto) => id -> ujson.Str(texto) })
    )

  private def ejecutar(entrada: String): String = {
    val salida = new StringBuilder
    val io = new ProcessIO(
      (in: OutputStream) => { in.write(entrada.getBytes("UTF-8")); in.close() },
      (out: InputStream) => {
        salida.append(scala.io.Source.fromInputStream(out, "UTF-8").mkString)
        out.close()
      },
      (err: InputStream) => {
        scala.io.Source.fromInputStream(err, "UTF-8").mkString
        err.close()
      }
    )
    val proceso = Process(comando).run(io)
    if (proceso.exitValue() != 0)
      throw new RuntimeException(s"ner_detector finalizó con código distinto de cero")
    salida.toString
  }

  private def parsear(salida: String): List[Entidad] =
    ujson.read(salida).obj.values.toList.flatMap { documento =>
      documento("entities").arr.toList.map { entidad =>
        Entidad(entidad("text").str, entidad("label").str)
      }
    }
}
