# Análisis: consigna de la Etapa 2 vs. implementación

Este documento relaciona **lo que pide el enunciado** (`enunciados/etapa2.md`,
Requisitos de Implementación) con **lo que hace el código** actual, módulo por
módulo, indicando responsabilidades.

---

## 1. Qué pide la consigna (Requisitos de Implementación)

| # | Requisito del enunciado | Resumen |
|---|--------------------------|---------|
| 1 | App de consola que lea todos los `.txt` de un directorio y produzca un **resumen extractivo**. **La lógica de procesamiento debe estar separada de la entrada/salida.** | Lectura masiva + separación lógica / E-S |
| 2 | El resumen se construye con un algoritmo basado en **TF-IDF** | Ver pasos abajo |
| 2a | Reunir todos los documentos y **segmentar en oraciones** | Corpus = todas las oraciones |
| 2b | Tokenizar: **minúsculas, sin puntuación, sin stopwords** | Normalización |
| 2c | `IDF(t) = log(N / (1 + df(t)))`, `N` = total de oraciones, `df(t)` = oraciones que contienen `t` | IDF a nivel oración |
| 2d | `puntaje(s) = Σ TF(t,s) · IDF(t)`, `TF(t,s)` = apariciones de `t` en `s` / total de tokens de `s` | Puntaje por oración |
| 2e | Seleccionar las oraciones de **mayor puntaje** (máximo **10**) | Top-N |
| 3 | El resultado se **muestra por consola** | Salida |
| 4 | Código de honor + registrar prompts de IA en el informe | Entrega/documentación |

> Restricción de diseño adicional (en `CLAUDE.md`): **el código no lleva
> comentarios** y debe seguir convenciones idiomáticas de Scala.

---

## 2. Arquitectura y mapeo a la consigna

El proyecto separa estrictamente **procesamiento puro** (`core/`, `modelo/`) de
**entrada/salida** (`io/`), conectados por `Main`. Esto satisface directamente la
exigencia del requisito 1 ("la lógica de procesamiento debe estar separada de la
entrada/salida").

```
src/main/scala/resumen/
  Main.scala                -> orquestación (wiring): leer -> resumir -> mostrar
  modelo/Modelo.scala       -> case classes del dominio
  core/Tokenizador.scala    -> segmentar + tokenizar       (PURO)
  core/TfIdf.scala          -> idf, tf, puntuar            (PURO)
  core/Resumidor.scala      -> pipeline del algoritmo       (PURO)
  io/LectorDocumentos.scala -> leer .txt del directorio     (E/S)
  io/SalidaConsola.scala    -> imprimir el resumen          (E/S)
```

Flujo de datos:

```
directorio
   │  LectorDocumentos.leerDirectorio        (E/S — lee disco)
   ▼
List[(nombreArchivo, contenido)]
   │  Resumidor.resumir                       (PURO — orquesta el algoritmo)
   │     ├─ Tokenizador.segmentar  → Oracion
   │     ├─ Tokenizador.tokenizar  → OracionTokenizada
   │     ├─ TfIdf.idf / tf / puntuar → OracionPuntuada
   │     └─ seleccionar (top 10)
   ▼
List[OracionPuntuada]
   │  SalidaConsola.mostrarResumen            (E/S — imprime)
   ▼
consola
```

---

## 3. Responsabilidad de cada módulo

### `modelo/Modelo.scala` — Dominio
Tres `case class` inmutables que dan nombre a los datos que viajan por el pipeline:
- `Oracion(texto, documento)` — una oración cruda y de qué archivo provino.
- `OracionTokenizada(oracion, tokens)` — la oración + su lista de tokens limpios.
- `OracionPuntuada(oracion, puntaje)` — la oración + su puntaje TF-IDF.

Sin lógica; solo estructura. Permite que las firmas del `core` sean expresivas.

### `core/Tokenizador.scala` — Normalización de texto (PURO)
Cubre los requisitos **2a** (segmentación) y **2b** (tokenización).
- `stopwords`: `Set[String]` de stopwords en español (descarte en 2b).
- `segmentar(texto)`: parte por saltos de línea (`\r?\n`, captura títulos estilo
  wiki) y luego por fin de oración (`.!?` seguido de espacio). Limpia y descarta
  vacíos. → **requisito 2a**.
- `tokenizar(texto)`: `toLowerCase` → reemplaza todo lo que no sea letra Unicode
  (`\p{L}`, conserva acentos/ñ) o dígito (`\p{Nd}`) por espacio → divide → filtra
  vacíos → filtra stopwords. → **requisito 2b**.

### `core/TfIdf.scala` — Núcleo matemático (PURO)
Cubre los requisitos **2c** y **2d**.
- `idf(oraciones)`: `N` = cantidad de oraciones; `df` se calcula con
  `distinct` por oración (cada token cuenta una sola vez por oración) y luego
  `groupBy`. Devuelve `Map[token → log(N / (1 + df))]`. → **requisito 2c**, fórmula
  exacta del enunciado.
- `tf(tokens)`: frecuencia relativa = apariciones / total de tokens de la oración.
  → mitad de **2d**.
- `puntuar(tokens, idf)`: `Σ TF(t,s) · IDF(t)`. Usa `getOrElse(token, 0.0)` por
  seguridad. → **requisito 2d** completo.

### `core/Resumidor.scala` — Pipeline del algoritmo (PURO)
Orquesta el algoritmo completo y aplica el **requisito 2e**.
- `maxOraciones = 10` → tope del enunciado.
- `resumir(documentos)`: segmenta todos los documentos en un **corpus único de
  oraciones** (requisito 2a a nivel global), las tokeniza, **descarta oraciones
  sin tokens** (evita división por cero en TF), calcula el IDF sobre ese corpus,
  puntúa cada oración y selecciona.
- `segmentar` (privado): aplana `List[(documento, contenido)]` a `List[Oracion]`,
  preservando el archivo de origen.
- `seleccionar` (privado): ordena por puntaje descendente y toma 10. → **2e**.

### `io/LectorDocumentos.scala` — Entrada (E/S)
Cubre la parte de lectura del **requisito 1**.
- `leerDirectorio(ruta)`: valida que sea directorio (si no, `List.empty`), lista
  solo archivos `.txt` (filtro por extensión, case-insensitive), los ordena por
  nombre y los lee. Devuelve `List[(nombreArchivo, contenido)]`.
- `leerArchivo`: lee en UTF-8 con `Using.resource` (cierre seguro del recurso).
- Uso idiomático de `Option(...)` para envolver el posible `null` de `listFiles`.

### `io/SalidaConsola.scala` — Salida (E/S)
Cubre el **requisito 3**.
- `mostrarResumen(oraciones)`: si está vacío, avisa; si no, imprime un encabezado
  y cada oración numerada con su puntaje (`%.4f`) y su documento de origen,
  ordenadas como vienen (ya ordenadas por el `Resumidor`).

### `Main.scala` — Orquestación (wiring)
Conecta todo sin contener lógica de negocio ni de E/S detallada:
1. toma la ruta de `args` (o `"archivos"` por defecto),
2. `LectorDocumentos.leerDirectorio`,
3. si no hay documentos, avisa; si los hay, `Resumidor.resumir` →
   `SalidaConsola.mostrarResumen`.

Es la única pieza que toca a la vez E/S y `core`, cumpliendo el rol de
composición.

---

## 4. Estado de cumplimiento

| Requisito | Estado | Dónde |
|-----------|--------|-------|
| 1 — Leer todos los `.txt` + separar lógica/E-S | ✅ | `LectorDocumentos` + separación `core`/`io` |
| 2a — Segmentar en oraciones | ✅ | `Tokenizador.segmentar`, corpus en `Resumidor` |
| 2b — Minúsculas / sin puntuación / sin stopwords | ✅ | `Tokenizador.tokenizar` |
| 2c — `IDF = log(N/(1+df))` | ✅ | `TfIdf.idf` |
| 2d — `Σ TF·IDF`, TF relativo | ✅ | `TfIdf.tf` + `TfIdf.puntuar` |
| 2e — Top-N (máx. 10) | ✅ | `Resumidor.seleccionar` (`maxOraciones = 10`) |
| 3 — Salida por consola | ✅ | `SalidaConsola` |
| Sin comentarios + estilo idiomático | ✅ | Todos los módulos |
| 4 — Honor / prompts en el informe | ⏳ | Tarea de entrega, no de código |
| Verificación de compilación/ejecución | ⏳ | Requiere toolchain Scala (ver `RunProjectCommands.md`) |

---

## 5. Decisiones de diseño relevantes para el informe

- **IDF a nivel oración (no documento):** el "corpus" son **todas las oraciones**
  de todos los documentos; `N` y `df` se miden sobre oraciones. Es lo que el
  enunciado describe en 2c ("N = total de oraciones").
- **`distinct` en el cálculo de `df`:** un token suma 1 a `df` por oración aunque
  aparezca varias veces en ella (definición correcta de *document frequency*).
- **Oraciones sin tokens descartadas:** evita división por cero en TF y oraciones
  vacías de contenido (títulos solo con stopwords, etc.).
- **Segmentación doble (saltos de línea + `.!?`):** los `.txt` estilo Wikipedia
  tienen títulos sin punto final; partir por línea los captura como oraciones.
- **Inmutabilidad y colecciones funcionales** (`map`/`filter`/`groupBy`/`sortBy`)
  en todo el `core`: módulos puros, testeables de forma aislada — refuerza el
  argumento de separación lógica/E-S exigido por el requisito 1.
