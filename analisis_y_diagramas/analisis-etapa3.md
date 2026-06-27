# Análisis: consigna de la Etapa 3 vs. implementación

Este documento relaciona **lo que pide el enunciado** (`enunciados/etapa3.md`) con
**lo que hace el código**, módulo por módulo, indicando responsabilidades. La
Etapa 3 amplía la Etapa 2: ahora el sistema responde a una **consulta por un tema
de interés** y produce un **reporte** con tres partes (Resumen, Entidades e
Información adicional), integrando un script externo de Python (spaCy) y la API
REST de Wikipedia.

---

## 1. Qué pide la consigna

| # | Requisito del enunciado | Resumen |
|---|--------------------------|---------|
| 3 | App de consola que lea todos los `.txt` de un directorio y produzca un **reporte** asociado al **tema consultado por el usuario**. Lógica separada de E/S. | CLI + separación lógica/E-S |
| 3 | Contemplar **tema sin documentos disponibles** → mensaje adecuado. | Caso borde |
| 3.1.1 | **Resumen**: extractivo con el **TF-IDF de la Etapa 2**, seleccionando las oraciones de mayor puntaje. | Reutiliza Etapa 2 |
| 3.1.2 | **Entidades**: extraer entidades nombradas (personas, organizaciones, lugares) integrando el **script Python provisto** (`ner_detector`, spaCy). | NER vía proceso externo |
| 3.1.3 | **Información adicional**: para **personas y organizaciones**, obtener un texto descriptivo desde la **API REST de Wikipedia** (`GET .../page/summary/{entidad}`), extrayendo el campo `extract`. | Enriquecimiento HTTP |
| 3.1.3 | **HTTP nativo** + deserialización JSON con el ecosistema del lenguaje. | java.net.http + ujson |
| 3.1.3 | **User-Agent** con número de comisión en cada petición. | Regla de Wikimedia |
| 3.1.3 | **Manejo robusto de errores**: 404 (u otros) → omitir esa entidad sin interrumpir la ejecución. | Tolerancia a fallos |
| 4 | Entrega: en la raíz `build.sbt`, `Dockerfile` y `ner_detector.py`; **excluir** `target/`, `venv/`. Evaluación vía Docker. | Empaquetado |

> Restricción heredada (`CLAUDE.md`): el código **no lleva comentarios** y sigue
> convenciones idiomáticas de Scala.

---

## 2. Arquitectura y mapeo a la consigna

Se mantiene la separación estricta **procesamiento puro** (`core/`, `modelo/`) vs
**entrada/salida** (`io/`), conectados por `Main`. La Etapa 3 agrega módulos sin
romper esa separación.

```
src/main/scala/resumen/
  Main.scala                  -> orquestación (wiring)
  modelo/Modelo.scala         -> case classes (+ Entidad, InfoAdicional, Reporte)
  core/Tokenizador.scala      -> segmentar + tokenizar            (PURO, Etapa 2)
  core/TfIdf.scala            -> idf, tf, puntuar                 (PURO, Etapa 2)
  core/Resumidor.scala        -> pipeline del resumen            (PURO, Etapa 2)
  core/FiltroTema.scala       -> selección de docs por tema      (PURO, NUEVO)
  core/Entidades.scala        -> normalizar / clasificar PER-ORG (PURO, NUEVO)
  io/LectorDocumentos.scala   -> leer .txt del directorio        (E/S, Etapa 2)
  io/EntradaConsola.scala     -> leer el tema por stdin          (E/S, NUEVO)
  io/DetectorEntidades.scala  -> invocar ner_detector_v2.py      (E/S, NUEVO)
  io/ClienteWikipedia.scala   -> HTTP GET + JSON a Wikipedia     (E/S, NUEVO)
  io/SalidaConsola.scala      -> imprimir el reporte completo    (E/S, ampliado)
```

Archivos de integración en la raíz (provistos por la cátedra):
`ner_detector_v2.py` (spaCy) y `Dockerfile`.

### Flujo de datos

```
          tema (stdin)                directorio
               │                          │
   EntradaConsola.leerTema     LectorDocumentos.leerDirectorio   (E/S)
               │                          │
               └────────────┬─────────────┘
                            ▼
             FiltroTema.documentosRelevantes              (PURO)
                            │
              ¿lista vacía? ─── sí ──► SalidaConsola.sinDocumentos   (E/S)
                            │ no
        ┌───────────────────┼──────────────────────────┐
        ▼                   ▼                           ▼
 Resumidor.resumir   DetectorEntidades.detectar   (luego, sobre entidades)
   (PURO, TF-IDF)        (E/S, Python/spaCy)               │
        │                   │                              │
        │          Entidades.normalizar (PURO)             │
        │                   │                              │
        │     Entidades.personasYOrganizaciones (PURO)     │
        │                   └──────────► ClienteWikipedia.describir (E/S, HTTP)
        ▼                                                  ▼
                 Reporte(tema, resumen, entidades, informacion)
                            │
                 SalidaConsola.mostrarReporte                (E/S)
```

---

## 3. Responsabilidad de cada módulo

### `modelo/Modelo.scala` — Dominio (ampliado)
A las case classes de la Etapa 2 (`Oracion`, `OracionTokenizada`,
`OracionPuntuada`) se suman:
- `Entidad(texto, tipo)` — una entidad nombrada y su tipo (`PER`/`ORG`/`LOC`/`MISC`).
- `InfoAdicional(entidad, descripcion)` — la entidad + su `extract` de Wikipedia.
- `Reporte(tema, resumen, entidades, informacion)` — el resultado consolidado que
  agrupa las tres partes exigidas por la consigna.

### `core/FiltroTema.scala` — Selección por tema (PURO, NUEVO)
Resuelve "reporte asociado al tema consultado" y el caso "sin documentos".
- `documentosRelevantes(tema, documentos)`: tokeniza el tema con el mismo
  `Tokenizador` (minúsculas, sin puntuación, sin stopwords) y conserva los
  documentos cuyo texto contiene **al menos uno** de esos términos. Si el tema no
  deja términos útiles (todo stopwords/vacío), devuelve todos los documentos.
- Si el resultado es vacío, `Main` emite el mensaje de "tema sin documentos".

### `core/Entidades.scala` — Clasificación de entidades (PURO, NUEVO)
- `normalizar(entidades)`: recorta, descarta vacías, **deduplica** (la misma
  entidad puede aparecer en varios documentos) y ordena por tipo
  (`PER`→`ORG`→`LOC`→`MISC`) y luego alfabéticamente.
- `personasYOrganizaciones(entidades)`: filtra solo `PER` y `ORG`, que son las que
  según el requisito 3.1.3 se enriquecen con Wikipedia.

### `core/Tokenizador.scala`, `core/TfIdf.scala`, `core/Resumidor.scala`
Sin cambios respecto de la Etapa 2. `Resumidor.resumir` se reutiliza tal cual para
el **Resumen** (requisito 3.1.1), ahora aplicado a los documentos relevantes.

### `io/EntradaConsola.scala` — Entrada del tema (E/S, NUEVO)
- `leerTema()`: imprime el prompt y lee una línea de `stdin`. Devuelve `""` si no
  hay entrada (`null`), lo que activa el comportamiento de fallback de `FiltroTema`.
  Se eligió `stdin` porque el `Dockerfile` ejecuta `CMD ["sbt", "run"]` **sin
  argumentos**: el tema se ingresa interactivamente o por tubería (`echo ... |`).

### `io/DetectorEntidades.scala` — Integración con Python/spaCy (E/S, NUEVO)
Implementa el requisito 3.1.2 respetando el **protocolo del script provisto**:
- Construye el JSON de entrada `{ "doc": "texto", ... }` con `ujson`.
- Lanza `python3 ner_detector_v2.py` con `scala.sys.process` y un `ProcessIO`
  que escribe ese JSON en el `stdin` del proceso y captura su `stdout` en UTF-8.
- Parsea la salida `{ "doc": { "entities": [ {text,label} ] } }` a `List[Entidad]`.
- **Tolerancia a fallos**: todo el proceso va dentro de `Try`; si Python no está,
  el script falla o el código de salida es ≠ 0, devuelve `List.empty` y el reporte
  continúa (resumen + resto), sin interrumpir la ejecución.

### `io/ClienteWikipedia.scala` — Enriquecimiento HTTP (E/S, NUEVO)
Implementa el requisito 3.1.3 con las consideraciones técnicas exigidas:
- **HTTP nativo**: `java.net.http.HttpClient` (incluido en el JDK, sin dependencias
  externas de red) para el `GET` a `…/page/summary/{entidad}`.
- **User-Agent**: cabecera `ProyectoLenguajes/1.0 (Comision 12)` en cada petición.
- **JSON con el ecosistema del lenguaje**: `ujson` para leer la respuesta y extraer
  el campo `extract`.
- **Manejo robusto de errores**: si el código no es `200` (típicamente `404`)
  devuelve `None`; además todo va dentro de `Try`, de modo que cualquier excepción
  de red también resulta en `None`. La entidad simplemente se omite de la sección
  de información adicional.
- El título se normaliza (espacios → `_`) y se codifica con `URLEncoder`
  (UTF-8) para soportar acentos y caracteres especiales.

### `io/SalidaConsola.scala` — Salida del reporte (E/S, ampliado)
- `mostrarReporte(reporte)`: imprime el encabezado con el tema y las tres
  secciones: **RESUMEN** (oraciones con su puntaje y documento), **ENTIDADES**
  (agrupadas por tipo con etiquetas legibles) e **INFORMACIÓN ADICIONAL**
  (entidad + descripción de Wikipedia). Cada sección informa cuando está vacía.
- `sinDocumentos(tema)`: mensaje para el caso "tema sin documentos".

### `Main.scala` — Orquestación (wiring)
Conecta todo sin contener lógica de negocio:
1. ruta del directorio (arg opcional, por defecto `archivos`) y tema (stdin),
2. lee documentos → filtra por tema,
3. si no hay relevantes → `sinDocumentos`,
4. si los hay → resumen (puro) + NER (E/S) → normaliza/clasifica (puro) →
   Wikipedia (E/S) → arma `Reporte` → `mostrarReporte`.

Es la única pieza que combina `core` y `io`, manteniendo la separación pedida.

---

## 4. Dependencias y empaquetado

- `build.sbt`: Scala 3.3.4, `mainClass = resumen.Main`, dependencia
  `com.lihaoyi::ujson` (JSON idiomático de Scala) y `run / fork := true` +
  `run / connectInput := true` para que `StdIn` funcione bajo `sbt run` (la lectura
  de stdin no se conecta al proceso hijo sin estos ajustes).
- `Dockerfile` (provisto): Java 17 + sbt 1.9.9 + venv con spaCy y
  `es_core_news_sm`; `sbt compile` en build y `sbt run` como `CMD`.
- `.gitignore`: excluye `target/`, `venv/`, `env/`, `__pycache__/` y artefactos de
  compilación, cumpliendo "archivos a excluir" de la entrega.
- HTTP resuelto con `java.net.http` (sin dependencia externa de red).

---

## 5. Estado de cumplimiento

| Requisito | Estado | Dónde |
|-----------|--------|-------|
| Leer todos los `.txt` + separar lógica/E-S | ✅ | `LectorDocumentos` + `core`/`io` |
| Reporte asociado al **tema** consultado | ✅ | `EntradaConsola` + `FiltroTema` |
| **Tema sin documentos** → mensaje | ✅ | `Main` + `SalidaConsola.sinDocumentos` |
| 3.1.1 Resumen TF-IDF (Etapa 2) | ✅ | `Resumidor` (reutilizado) |
| 3.1.2 Entidades vía script Python/spaCy | ✅ | `DetectorEntidades` (protocolo del script) |
| 3.1.3 Información adicional desde Wikipedia | ✅ | `ClienteWikipedia` |
| HTTP nativo + JSON del ecosistema | ✅ | `java.net.http` + `ujson` |
| User-Agent con comisión | ✅ | `ClienteWikipedia` (Comision 12) |
| Manejo robusto de 404 / errores | ✅ | `Try` + chequeo de `statusCode` |
| Raíz con `build.sbt` / `Dockerfile` / script | ✅ | raíz del repo |
| Excluir `target/`, `venv/` | ✅ | `.gitignore` |
| Honor / uso de IA documentado | ⏳ | tarea de entrega/informe |

---

## 6. Decisiones de diseño relevantes para la defensa

- **Tema → selección de documentos.** La relevancia se decide por coincidencia de
  términos del tema (tokenizados igual que el corpus). Es simple, explicable y
  resuelve naturalmente el caso "sin documentos". Si el tema queda vacío de
  términos, se reportan todos los documentos en lugar de fallar.
- **El tema se lee por `stdin`.** El `Dockerfile` corre `sbt run` sin argumentos;
  por eso la consulta llega por entrada estándar (interactiva o por tubería), y se
  habilitan `fork`/`connectInput` para que `sbt run` conecte el stdin al programa.
- **Integración por procesos, no reimplementación.** El NER se delega al script
  provisto vía `stdin/stdout` con contrato JSON; Scala solo orquesta y parsea.
  Esto respeta "integrar el script provisto" y mantiene el NER fuera de la lógica
  pura de Scala (queda en la capa de E/S).
- **Tolerancia a fallos en ambas integraciones.** Tanto el NER (proceso externo)
  como Wikipedia (red) están envueltos en `Try`/chequeo de estado: una falla de
  Python o un `404`/timeout degrada la sección correspondiente pero **no
  interrumpe** el programa, tal como exige el requisito 3.1.3.
- **HTTP nativo del JDK.** `java.net.http.HttpClient` evita dependencias de red
  externas y es parte del ecosistema del lenguaje; solo se agrega `ujson` para el
  JSON, que es la pieza idiomática para (de)serializar en Scala.
- **Separación lógica/E-S preservada.** `FiltroTema` y `Entidades` son funciones
  puras y testeables; toda interacción con disco, consola, proceso Python y red
  vive en `io/`; `Main` es el único punto de wiring.

---

## 7. Nota sobre el nombre del script

El enunciado (Sección 4) menciona `ner_detector.py`, pero la cátedra entregó el
archivo como **`ner_detector_v2.py`**. La implementación invoca el archivo
realmente provisto (`DetectorEntidades.script = "ner_detector_v2.py"`). Si el
entorno de corrección esperara exactamente `ner_detector.py`, basta con renombrar
el archivo y actualizar esa constante (o agregar una copia con ese nombre).
