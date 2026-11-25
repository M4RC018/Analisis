# Explicación de la generación de informes PDF

La pestaña **Análisis** permite elegir un deportista y un periodo, y generar un informe desde el botón **Generar informe PDF**. La aplicación reutiliza el objeto `ResultadoAnalisis` producido por `ServicioAnalisis`; el módulo PDF no vuelve a calcular los indicadores.

`GeneradorInformePdf` crea el documento con Apache PDFBox. La portada muestra los datos del deportista, el periodo, la fecha y el logotipo del club. La fotografía es opcional: si no se selecciona o el archivo no puede leerse, aparece una ilustración genérica. Las imágenes se escalan manteniendo su proporción.

El informe contiene el resumen del periodo, la tabla semanal, el histórico de pruebas de 5 km y la interpretación de los indicadores con sus limitaciones. Las tablas tienen cabecera, bordes y valores alineados. Cada página incluye su número y un pie. Si una tabla no cabe, continúa en otra página y repite la cabecera.

`GeneradorGraficosPdf` crea con JFreeChart los gráficos de carga semanal y de evolución de las marcas, los guarda temporalmente como PNG y `GeneradorInformePdf` los inserta en el documento con PDFBox. Los PNG temporales se eliminan al terminar la generación.
