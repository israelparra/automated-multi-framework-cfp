/**
 * Anotaciones personalizadas para análisis COSMIC Function Point.
 *
 * <p>Este paquete contiene anotaciones que permiten a los desarrolladores marcar
 * manualmente interacciones COSMIC que ocurren dentro de expresiones Lambda y que
 * el analizador estático no puede detectar automáticamente debido a la instrucción
 * INVOKEDYNAMIC del bytecode Java.</p>
 *
 * <p><b>Uso principal:</b> Anotar métodos de endpoints REST que contienen Lambdas
 * con llamadas a fronteras externas (base de datos, archivos, HTTP, etc.).</p>
 *
 * @see com.emir.analyzer.annotations.CosmicLambda
 * @version 1.0.0
 * @since 2026-01-04
 */
package com.emir.analyzer.annotations;