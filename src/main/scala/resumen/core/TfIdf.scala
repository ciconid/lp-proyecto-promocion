package resumen.core

import scala.math.log

object TfIdf {

  def idf(oraciones: List[List[String]]): Map[String, Double] = {
    val n = oraciones.length
    val df = oraciones
      .flatMap(_.distinct)
      .groupBy(identity)
      .view
      .mapValues(_.length)
      .toMap
    df.map { case (token, frecuencia) =>
      token -> log(n.toDouble / (1 + frecuencia))
    }
  }

  def tf(tokens: List[String]): Map[String, Double] = {
    val total = tokens.length
    tokens
      .groupBy(identity)
      .view
      .mapValues(_.length.toDouble / total)
      .toMap
  }

  def puntuar(tokens: List[String], idf: Map[String, Double]): Double =
    tf(tokens)
      .map { case (token, frecuencia) => frecuencia * idf.getOrElse(token, 0.0) }
      .sum
}
