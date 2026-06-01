# Resumen Extractivo (Etapa 2 — requisito de implementación)

Aplicación de consola en **Scala** que lee todos los `.txt` de un directorio
(`archivos/` por defecto) y produce un resumen extractivo basado en TF-IDF.

## Objetivo y restricciones

- La **lógica de procesamiento** debe estar separada de la **entrada/salida**.
- El código no lleva comentarios.
- Resultado mostrado por consola.
- Convenciones idiomáticas de Scala (paquetes, `object`, `trait`, `case class`,
  inmutabilidad, colecciones funcionales, `Option`/`Using`).

## Algoritmo (TF-IDF extractivo)

1. Reunir todos los documentos y segmentar el texto en oraciones (corpus = todas
   las oraciones de todos los documentos). `N` = total de oraciones.
2. Tokenizar cada oración: minúsculas, eliminar puntuación, descartar stopwords.
3. `IDF(t) = log(N / (1 + df(t)))`, con `df(t)` = oraciones que contienen `t`.
4. `puntaje(s) = Σ TF(t,s) · IDF(t)`, con `TF(t,s)` = frecuencia relativa de `t`
   en `s` (apariciones de `t` en `s` / total de tokens de `s`).
5. Seleccionar las oraciones de mayor puntaje (máximo 10).

## Arquitectura (separación procesamiento / E-S)

```
build.sbt
project/build.properties
src/main/scala/resumen/
  Main.scala                  -> orquestación (wiring)
  modelo/Modelo.scala         -> case classes (Oracion, OracionTokenizada, OracionPuntuada)
  core/Tokenizador.scala      -> segmentar + tokenizar      (PURO, sin E/S)
  core/TfIdf.scala            -> idf, tf, puntuar           (PURO, sin E/S)
  core/Resumidor.scala        -> pipeline del algoritmo     (PURO, sin E/S)
  io/LectorDocumentos.scala   -> leer .txt del directorio   (E/S)
  io/SalidaConsola.scala      -> imprimir resumen           (E/S)
```

- `core/*` y `modelo/*` son funciones puras sobre `String`/colecciones: no tocan
  disco ni consola. Testeables de forma aislada.
- `io/*` concentra todo el acceso a archivos y la impresión.
- `Main` sólo conecta: lee -> resume -> muestra.

## Decisiones de diseño

- Segmentación: por saltos de línea (captura títulos estilo wiki) y por
  puntuación de fin de oración `.!?`.
- Tokenización: `toLowerCase`, se conservan letras Unicode (acentos/ñ) y dígitos,
  el resto se descarta; luego se filtran stopwords en español.
- Oraciones sin tokens se descartan (evita división por cero en TF).
- La salida lista las oraciones ordenadas por puntaje descendente, con su valor.

## Cómo ejecutar

Requiere una instalación de Scala/sbt o scala-cli (no presente en este equipo,
sólo hay JDK). Opciones:

- `sbt run`  (usa `archivos/` por defecto)  ó  `sbt "run <directorio>"`
- `scala-cli run src/main/scala`  (alternativa sin sbt)

## Estado

- [x] Plan en CLAUDE.md
- [x] Estructura del proyecto sbt
- [x] Modelo + núcleo (Tokenizador, TfIdf, Resumidor)
- [x] Capa de E/S (Lector, Salida) + Main
- [ ] Verificación de compilación/ejecución (bloqueada: falta toolchain Scala)
