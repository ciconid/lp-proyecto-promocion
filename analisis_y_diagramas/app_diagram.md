# Diagrama de la aplicación

Cómo se comunican los módulos al generar un reporte. Las cajas marcadas `E/S`
tocan el exterior (consola, disco, proceso Python, red); las marcadas `PURO` son
funciones sin efectos, testeables de forma aislada. `Main` es el único punto de
conexión (wiring) entre ambas capas.

```
                          ┌──────────────────────────┐
                          │           Main           │  (wiring)
                          │   orquesta todo el flujo  │
                          └────────────┬─────────────┘
            tema (teclado)             │             directorio (arg)
                  │                    │                    │
                  ▼                    │                    ▼
        ┌───────────────────┐         │        ┌───────────────────────┐
        │  EntradaConsola   │ E/S     │   E/S  │   LectorDocumentos    │
        │  leerTema()       │         │        │   leerDirectorio()    │
        └─────────┬─────────┘         │        └───────────┬───────────┘
                  │ tema              │   List[(archivo, contenido)]   │
                  └──────────┬────────┴────────────────────┬──────────┘
                             ▼                              │
                   ┌───────────────────┐                   │
                   │    FiltroTema     │ PURO              │
                   │ documentosRelev.  │◄──────────────────┘
                   └─────────┬─────────┘
                             │ documentos relevantes
              ┌──────────────┤
        vacío │              │ no vacío
              ▼              ├───────────────────────┬───────────────────────┐
   ┌───────────────────┐     ▼                       ▼                        │
   │   SalidaConsola   │  ┌────────────┐   ┌───────────────────────┐          │
   │   sinDocumentos() │  │ Resumidor  │   │  DetectorEntidades    │ E/S      │
   └───────────────────┘  │  resumir() │   │  detectar()           │          │
        (E/S, fin)        │   PURO     │   │  python3 ner_detector │          │
                          └─────┬──────┘   └───────────┬───────────┘          │
                                │                      │ entidades crudas     │
                       usa: Tokenizador,               ▼                      │
                            TfIdf            ┌───────────────────────┐        │
                            (PURO)           │      Entidades        │ PURO   │
                                │            │ normalizar / filtrar  │        │
                                │            │ personasYOrganizac.   │        │
                                │            └───────────┬───────────┘        │
                                │                        │ PER + ORG          │
                                │                        ▼                    │
                                │            ┌───────────────────────┐        │
                                │            │   ClienteWikipedia    │ E/S    │
                                │            │   describir() (HTTP)  │        │
                                │            └───────────┬───────────┘        │
                                │                        │ extract (Wikipedia)│
                                ▼                        ▼                    │
                          ┌──────────────────────────────────────────┐       │
                          │   Reporte(tema, resumen, entidades, info) │◄──────┘
                          │            (modelo, datos)                │
                          └────────────────────┬─────────────────────┘
                                               ▼
                                   ┌───────────────────────┐
                                   │     SalidaConsola     │ E/S
                                   │   mostrarReporte()    │
                                   └───────────────────────┘
                                          consola
```

Dependencias externas que cruzan la frontera `E/S`:

```
EntradaConsola / SalidaConsola ──► consola (stdin / stdout)
LectorDocumentos               ──► disco (archivos .txt de archivos/)
DetectorEntidades              ──► proceso  python3 ner_detector_v2.py  (spaCy)
ClienteWikipedia               ──► red      https://es.wikipedia.org/.../summary/
```

---

## Módulos y responsabilidades

### Orquestación
- **`Main`** — Wiring. Lee el tema y el directorio, encadena: filtrar → (si vacío)
  mensaje / (si no) resumir + detectar entidades + enriquecer + mostrar. Es la
  única pieza que combina lógica pura y E/S.

### Modelo (datos)
- **`modelo/Modelo`** — Case classes inmutables: `Oracion`, `OracionTokenizada`,
  `OracionPuntuada`, `Entidad`, `InfoAdicional` y `Reporte` (resultado consolidado).

### Núcleo — lógica pura (`core/`, sin E/S)
- **`Tokenizador`** — Segmenta el texto en oraciones y las tokeniza (minúsculas,
  sin puntuación, sin stopwords).
- **`TfIdf`** — Cálculos del algoritmo: `idf`, `tf` y `puntuar` (Σ TF·IDF).
- **`Resumidor`** — Pipeline del resumen extractivo: segmenta, puntúa y selecciona
  las 10 oraciones de mayor puntaje. Reutilizado de la Etapa 2.
- **`FiltroTema`** — Selecciona los documentos relevantes al tema consultado; si
  no hay coincidencias, habilita el caso "sin documentos".
- **`Entidades`** — Normaliza (recorta, deduplica, ordena por tipo) y filtra las
  entidades de tipo persona (`PER`) y organización (`ORG`) para enriquecer.

### Entrada/Salida (`io/`, frontera con el exterior)
- **`EntradaConsola`** — Lee el tema de interés desde el teclado (stdin).
- **`LectorDocumentos`** — Lee todos los `.txt` del directorio en UTF-8.
- **`DetectorEntidades`** — Invoca el script `ner_detector_v2.py` (spaCy) por
  proceso externo, con protocolo JSON por stdin/stdout. Tolerante a fallos
  (devuelve vacío si Python falla).
- **`ClienteWikipedia`** — Petición HTTP GET nativa (`java.net.http`) a la API de
  Wikipedia, con User-Agent de la comisión; extrae el campo `extract` del JSON.
  Maneja 404/errores omitiendo la entidad sin interrumpir el programa.
- **`SalidaConsola`** — Imprime el reporte (Resumen, Entidades, Información
  adicional) o el mensaje de "tema sin documentos".

### Integración externa (en la raíz del proyecto)
- **`ner_detector_v2.py`** — Script Python provisto por la cátedra; reconoce
  entidades nombradas con spaCy (`es_core_news_sm`).
- **`Dockerfile`** — Imagen de evaluación: Java 17 + sbt + Python/spaCy; compila y
  ejecuta el sistema en un entorno aislado.
