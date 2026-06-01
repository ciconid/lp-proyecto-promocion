# Funcionamiento del Resumen Extractivo

Aplicación de consola en Scala que lee todos los `.txt` de un directorio y
produce un resumen extractivo con un algoritmo basado en TF-IDF. El diseño
separa de forma estricta la **lógica de procesamiento** (paquetes `core` y
`modelo`, funciones puras) de la **entrada/salida** (paquete `io` y `Main`).

## Mapa de archivos

```
src/main/scala/resumen/
  Main.scala                  -> orquestación / punto de entrada
  modelo/Modelo.scala         -> tipos de datos (case classes)
  core/Tokenizador.scala      -> segmentar + tokenizar        (PURO)
  core/TfIdf.scala            -> idf, tf, puntuar             (PURO)
  core/Resumidor.scala        -> pipeline del algoritmo       (PURO)
  io/LectorDocumentos.scala   -> lectura de archivos .txt     (E/S)
  io/SalidaConsola.scala      -> impresión del resumen        (E/S)
```

---

## Las partes

### 1. `modelo/Modelo.scala` — el lenguaje común

Define los tipos que viajan entre las capas. Son `case class` inmutables, sin
comportamiento, que actúan como contrato compartido:

- `Oracion(texto, documento)`: una oración y el archivo del que proviene.
- `OracionTokenizada(oracion, tokens)`: la oración junto a su lista de tokens.
- `OracionPuntuada(oracion, puntaje)`: la oración junto a su puntaje TF-IDF.

Todas las demás partes hablan en términos de estos tipos; ninguna de ellas
expone estructuras propias hacia afuera.

### 2. `core/Tokenizador.scala` — texto a tokens (puro)

Dos funciones puras sobre `String`, sin acceso a disco ni consola:

- `segmentar(texto)`: parte el texto en oraciones. Corta primero por saltos de
  línea (para capturar los títulos estilo Wikipedia) y luego por puntuación de
  fin de oración (`.`, `!`, `?`). Devuelve `List[String]`.
- `tokenizar(texto)`: pasa a minúsculas, elimina la puntuación (conserva letras
  Unicode con acentos/ñ y dígitos), separa por espacios y descarta las
  `stopwords` en español. Devuelve `List[String]`.

### 3. `core/TfIdf.scala` — la matemática del algoritmo (puro)

Implementa las fórmulas, operando sólo sobre colecciones de tokens:

- `idf(oraciones: List[List[String]])`: recibe todas las oraciones ya
  tokenizadas (el corpus). Calcula `df(t)` (cantidad de oraciones que contienen
  cada token) y devuelve `Map[token -> IDF]` con `IDF(t) = log(N / (1 + df(t)))`.
- `tf(tokens)`: frecuencia relativa de cada token dentro de una oración
  (apariciones / total de tokens de la oración).
- `puntuar(tokens, idf)`: suma `TF(t,s) · IDF(t)` sobre los tokens de la
  oración; devuelve el puntaje (`Double`).

### 4. `core/Resumidor.scala` — el pipeline (puro)

Coordina el algoritmo completo sin tocar la E/S. Su función `resumir` recibe los
documentos ya leídos (pares `nombre -> contenido`) y ejecuta:

1. Segmenta cada documento en `Oracion` (vía `Tokenizador.segmentar`).
2. Tokeniza cada oración (`Tokenizador.tokenizar`) y descarta las que quedan sin
   tokens (evita división por cero en TF).
3. Calcula el `IDF` global sobre todas las oraciones (`TfIdf.idf`).
4. Puntúa cada oración (`TfIdf.puntuar`).
5. Selecciona las de mayor puntaje, con un máximo de 10
   (`sortBy(-puntaje).take(10)`).

Devuelve `List[OracionPuntuada]`.

### 5. `io/LectorDocumentos.scala` — entrada (E/S)

`leerDirectorio(ruta)` abre el directorio, filtra los archivos `.txt`, los ordena
por nombre y lee cada uno en UTF-8 (con `Using` para cerrar el recurso).
Devuelve `List[(String, String)]` = pares `(nombreDeArchivo, contenido)`. Es el
único punto que lee del disco.

### 6. `io/SalidaConsola.scala` — salida (E/S)

`mostrarResumen(oraciones)` recibe la lista de `OracionPuntuada` y la imprime
numerada, mostrando puntaje, documento de origen y texto. Es el único punto que
escribe en consola.

### 7. `Main.scala` — orquestación

Conecta las capas y no contiene lógica del algoritmo:

1. Lee la ruta de `args` (por defecto `archivos`).
2. Pide los documentos a `LectorDocumentos`.
3. Si no hay documentos, avisa; si los hay, llama a `Resumidor.resumir` y pasa el
   resultado a `SalidaConsola.mostrarResumen`.

---

## Cómo se comunican las partes

El flujo es lineal y unidireccional; los datos cruzan las capas siempre mediante
los tipos del `modelo`:

```
                 archivos .txt
                      |
                      v
   [io] LectorDocumentos.leerDirectorio
                      |   List[(nombre, contenido)]
                      v
   [Main] orquesta y delega
                      |   List[(nombre, contenido)]
                      v
   [core] Resumidor.resumir
        |  segmentar ->  List[Oracion]            (Tokenizador)
        |  tokenizar ->  List[OracionTokenizada]  (Tokenizador)
        |  idf       ->  Map[token, Double]        (TfIdf)
        |  puntuar   ->  List[OracionPuntuada]     (TfIdf)
        |  seleccionar -> top 10 por puntaje
                      |   List[OracionPuntuada]
                      v
   [io] SalidaConsola.mostrarResumen
                      |
                      v
                   consola
```

Claves del diseño:

- **`core` no conoce a `io`.** Las funciones de `core` reciben y devuelven datos
  puros (Strings, colecciones, `case class` del `modelo`); nunca leen disco ni
  imprimen. Esto las hace deterministas y testeables de forma aislada.
- **`io` no conoce el algoritmo.** Sólo sabe leer archivos y mostrar resultados.
- **`Main` es el único que conoce a ambos** y los conecta. La dependencia va de
  afuera (E/S) hacia adentro (procesamiento), nunca al revés.
- **El `modelo` es el contrato compartido**: el medio neutro por el que viajan
  los datos entre capas, sin que ninguna dependa de la implementación de la otra.

## Ejecución

Requiere Scala/sbt o scala-cli ya instalados:

- `sbt run`  (usa `archivos/` por defecto)  ó  `sbt "run <directorio>"`
- `scala-cli run src/main/scala`
