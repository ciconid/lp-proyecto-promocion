package resumen.core

object Tokenizador {

  val stopwords: Set[String] = Set(
    "a", "al", "algo", "algunas", "algunos", "ante", "antes", "como", "con",
    "contra", "cual", "cuando", "de", "del", "desde", "donde", "durante", "e",
    "el", "ella", "ellas", "ellos", "en", "entre", "era", "erais", "eran",
    "eras", "eres", "es", "esa", "esas", "ese", "eso", "esos", "esta", "estaba",
    "estado", "estais", "estamos", "estan", "estar", "estas", "este", "esto",
    "estos", "estoy", "fue", "fueron", "fui", "fuimos", "ha", "habia", "han",
    "hasta", "hay", "la", "las", "le", "les", "lo", "los", "mas", "me", "mi",
    "mis", "mucho", "muy", "nada", "ni", "no", "nos", "nosotros", "nuestra",
    "nuestras", "nuestro", "nuestros", "o", "os", "otra", "otras", "otro",
    "otros", "para", "pero", "poco", "por", "porque", "que", "quien", "quienes",
    "se", "sea", "sean", "segun", "ser", "si", "sin", "sobre", "sois", "somos",
    "son", "su", "sus", "tambien", "tanto", "te", "tiene", "tienen", "todo",
    "todos", "tu", "tus", "un", "una", "uno", "unas", "unos", "y", "ya", "yo"
  )

  def segmentar(texto: String): List[String] =
    texto
      .split("\\r?\\n")
      .flatMap(_.split("(?<=[.!?])\\s+"))
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList

  def tokenizar(texto: String): List[String] =
    texto.toLowerCase
      .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
      .split("\\s+")
      .filter(_.nonEmpty)
      .filterNot(stopwords.contains)
      .toList
}
