package resumen.io

import scala.io.StdIn

object EntradaConsola {

  def leerTema(): String = {
    print("Ingrese el tema de interés: ")
    Option(StdIn.readLine()).map(_.trim).getOrElse("")
  }
}
