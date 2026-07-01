package resumen.io

import scala.io.StdIn

object EntradaConsola {

  def leerTema(): String = {
    println(" ")
    println("Ingrese el tema de interés: ")
    Option(StdIn.readLine()).map(_.trim).getOrElse("")
  }
}
