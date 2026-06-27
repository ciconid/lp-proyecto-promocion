package resumen.io

import resumen.modelo.{Entidad, InfoAdicional}
import java.net.URI
import java.net.URLEncoder
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import scala.util.Try

object ClienteWikipedia {

  private val baseUrl = "https://es.wikipedia.org/api/rest_v1/page/summary/"
  private val userAgent = "ProyectoLenguajes/1.0 (Comision 12)"
  private val cliente = HttpClient.newHttpClient()

  def describir(entidad: Entidad): Option[InfoAdicional] =
    Try(consultar(entidad)).toOption.flatten

  private def consultar(entidad: Entidad): Option[InfoAdicional] = {
    val titulo = URLEncoder.encode(entidad.texto.replace(" ", "_"), StandardCharsets.UTF_8)
    val peticion = HttpRequest
      .newBuilder()
      .uri(URI.create(baseUrl + titulo))
      .header("User-Agent", userAgent)
      .header("Accept", "application/json")
      .GET()
      .build()
    val respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    if (respuesta.statusCode() == 200) extraer(entidad, respuesta.body())
    else None
  }

  private def extraer(entidad: Entidad, cuerpo: String): Option[InfoAdicional] =
    ujson
      .read(cuerpo)
      .obj
      .get("extract")
      .map(_.str)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(texto => InfoAdicional(entidad, texto))
}
