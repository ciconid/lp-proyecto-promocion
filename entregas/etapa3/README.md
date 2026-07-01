# Cómo compilar y ejecutar la aplicación

Guía rápida para alguien que no conoce el proyecto. Son dos pasos: primero
**construir** (una sola vez) y después **ejecutar** (las veces que quieras).

La forma recomendada es **Docker**, porque trae todo lo necesario ya instalado
(Scala, sbt, Python, spaCy). Es además el entorno con el que la cátedra corrige.

**Requisitos previos:**
- Tener **Docker Desktop instalado y abierto** (en Windows debe estar corriendo
  antes de ejecutar los comandos).
- Conexión a Internet (la primera construcción descarga dependencias; al ejecutar,
  se usa para consultar Wikipedia).

> Los comandos se escriben en una terminal (**PowerShell**, **CMD** o **Git Bash**)
> ubicada en la carpeta raíz del proyecto (donde está el `Dockerfile`).

---

## 1. Compilar / Construir

Construye la imagen y compila el código. **Se hace una sola vez** (y otra vez solo
si cambiás el código). La primera vez tarda varios minutos.

```bash
docker build -t lp-etapa3 .
```

Cuando termina sin errores, la aplicación está lista para ejecutarse.

---

## 2. Ejecutar

```bash
docker run -i --rm lp-etapa3
```

Al iniciar, el programa muestra:

```
Ingrese el tema de interés:
```

Escribís el tema (por ejemplo, `Energías renovables`), presionás **Enter**, y se
imprime el reporte (Resumen, Entidades e Información adicional).

> El flag `-i` es **obligatorio**: permite escribir el tema por teclado.
> `--rm` borra el contenedor al terminar.


