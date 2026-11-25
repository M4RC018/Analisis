# Análisis de rendimiento deportivo — Parte 1

## Qué incluye

La aplicación Java Swing permite registrar deportistas, entrenamientos y pruebas de 5 km. La pestaña **Análisis** calcula indicadores para un atleta y un intervalo de fechas; también compara deportistas y exporta un libro `.xlsx`.

El código se organiza en tres paquetes:

- `modelo`: ficha del deportista, sesión y prueba.
- `servicio`: validación y almacenamiento temporal en memoria, cálculos e informe Excel.
- `vistas`: ventana principal y pestañas de entrada y análisis.

## Criterios de cálculo

- **Carga de sesión:** duración en minutos × RPE; se expresa en unidades arbitrarias (UA).
- **Semanas:** de lunes a domingo. Una semana solo se considera completa si los siete días están dentro del intervalo. Las semanas parciales se muestran, pero no se usan para monotonía, tensión ni medias que requieren una semana completa.
- **Media móvil:** promedio de la carga de tres semanas completas consecutivas.
- **Monotonía:** promedio de la carga diaria de los siete días dividido entre la desviación típica poblacional de esas cargas. **Tensión:** carga semanal × monotonía. Si la desviación es cero, el resultado queda sin definir.
- **Estadística descriptiva:** media, mediana, mínimo, máximo, desviación típica poblacional y coeficiente de variación. La carga se resume por semana completa y la distancia por sesión.
- **Regresión:** mínimos cuadrados entre el tiempo de 5 km en segundos y las semanas transcurridas desde la primera prueba. Se muestra pendiente, R² y una extrapolación a la semana siguiente; no es una garantía de rendimiento.
- **Correlación:** carga semanal completa frente a la última marca de 5 km registrada en esa semana. Con menos de dos pares o sin variación en los datos, no se calcula.
- Una asociación estadística no demuestra que la carga haya causado el cambio de marca.

Los registros se guardan en memoria mientras la aplicación está abierta. El libro Excel exportado sí conserva los datos incluidos en ese archivo.

## Ejemplo manual para revisar los resultados

Registra un deportista (por ejemplo, ID `1`, nombre `Ana`, edad `20`, categoría `Absoluta`, objetivo `5 km` y marca inicial `1200` segundos). Usa el mismo tipo de sesión y pulso medio `150` para las siguientes seis sesiones:

| Fecha | Duración | Distancia | RPE | Carga esperada |
|---|---:|---:|---:|---:|
| 2026-02-23 | 50 min | 10 km | 10 | 500 UA |
| 2026-02-25 | 50 min | 10 km | 10 | 500 UA |
| 2026-03-02 | 60 min | 12 km | 10 | 600 UA |
| 2026-03-04 | 60 min | 12 km | 10 | 600 UA |
| 2026-03-09 | 70 min | 14 km | 10 | 700 UA |
| 2026-03-11 | 70 min | 14 km | 10 | 700 UA |

Registra también estas pruebas de 5 km:

| Fecha | Marca |
|---|---:|
| 2026-03-01 | 1200 s (20:00) |
| 2026-03-08 | 1180 s (19:40) |
| 2026-03-15 | 1160 s (19:20) |

En **Análisis**, selecciona el intervalo `2026-02-23` a `2026-03-15`. Los resultados principales que puedes contrastar son:

- Cargas semanales: `1000`, `1200` y `1400` UA.
- Media móvil de tres semanas en la tercera semana: `1200` UA.
- Regresión: pendiente `-20 s/semana`, R² `1` y estimación siguiente `1140 s` (`19:00`).
- Correlación carga-marca: `-1` con tres pares.
- Media, mediana, mínimo y máximo de carga semanal: `1200`, `1200`, `1000` y `1400` UA.

## Cómo usarlo en NetBeans

1. Abre el proyecto Maven `Analisis` y deja que NetBeans cargue las dependencias de Apache POI indicadas en `pom.xml`.
2. Ejecuta `group.analisis.Analisis`.
3. Registra primero al deportista; después añade sus entrenamientos y pruebas en las pestañas correspondientes.
4. En **Análisis**, escribe las fechas en formato `AAAA-MM-DD` y pulsa **Calcular análisis** o **Comparar deportistas**.
5. Pulsa **Exportar libro Excel** y elige dónde guardar el `.xlsx`. Al abrirlo en Excel, este recalcula las fórmulas del resumen.

La exportación crea las hojas `Resumen`, `Deportistas`, `Entrenamientos`, `Análisis` y `Rendimiento`. Los listados históricos incluyen todos los registros; el intervalo elegido se aplica a los análisis y a las gráficas del deportista seleccionado.

## Evidencias

Si la entrega pide capturas, hazlas después de ejecutar el programa: una de la ficha de deportista, otra con entrenamientos/pruebas y otra del análisis o del libro Excel abierto. Las capturas tienen que mostrar datos introducidos en tu ejecución.
