package com.emir.analyzer.annotations;

import java.lang.annotation.*;

/**
 * Marca interacciones COSMIC (READ/WRITE) que ocurren dentro de expresiones Lambda
 * y que el analizador estático no puede detectar automáticamente debido a invokedynamic.
 *
 * <h2>PROBLEMA TÉCNICO:</h2>
 * Cuando una llamada a una frontera externa (RestTemplate, repository.save(), Files.write())
 * está encapsulada dentro del cuerpo de un Lambda, el compilador Java genera bytecode usando
 * la instrucción INVOKEDYNAMIC, la cual difiere el enlace del código hasta tiempo de ejecución
 * mediante LambdaMetafactory. Esto hace que el contenido del Lambda sea una "caja negra"
 * para analizadores estáticos basados en ASM.
 *
 * <h2>EJEMPLOS DE USO:</h2>
 *
 * <h3>Ejemplo 1: Llamada HTTP GET dentro de Lambda (READ)</h3>
 * <pre>
 * &#64;GetMapping("/products")
 * &#64;CosmicLambda(value = MovementType.READ,
 *              description = "HTTP GET to inventory service inside Lambda")
 * public List&lt;ProductDTO&gt; getProducts(List&lt;Long&gt; ids) {
 *     return ids.stream()
 *         .map(id -&gt; restTemplate.getForObject("http://api/products/" + id, ProductDTO.class))
 *         .collect(Collectors.toList());
 * }
 * </pre>
 *
 * <h3>Ejemplo 2: Persistencia dentro de Lambda (WRITE)</h3>
 * <pre>
 * &#64;PostMapping("/orders")
 * &#64;CosmicLambda(MovementType.WRITE)
 * public void saveOrders(List&lt;Order&gt; orders) {
 *     orders.forEach(order -&gt; orderRepository.save(order));
 * }
 * </pre>
 *
 * <h3>Ejemplo 3: Múltiples movimientos (READ + WRITE)</h3>
 * <pre>
 * &#64;GetMapping("/process")
 * &#64;CosmicLambda(value = {MovementType.READ, MovementType.WRITE},
 *              count = 1,
 *              description = "Lambda: DB read + HTTP POST + DB write")
 * public List&lt;OrderDTO&gt; processOrders(List&lt;Long&gt; ids) {
 *     return ids.stream()
 *         .map(id -&gt; {
 *             Order order = orderRepository.findById(id).get();        // READ
 *             restTemplate.postForObject(url, order, Void.class);      // WRITE (HTTP POST)
 *             order.setStatus("PROCESSED");
 *             orderRepository.save(order);                             // WRITE
 *             return new OrderDTO(order);
 *         })
 *         .collect(Collectors.toList());
 * }
 * </pre>
 *
 * <h2>CLASIFICACIÓN SEGÚN COSMIC ISO/IEC 19761:</h2>
 * <ul>
 *     <li><b>READ:</b> Incluye lectura de BD, llamadas HTTP GET, lectura de archivos</li>
 *     <li><b>WRITE:</b> Incluye escritura a BD, llamadas HTTP POST/PUT/DELETE, escritura de archivos</li>
 * </ul>
 *
 * <h2>REGLAS DE USO:</h2>
 * <ul>
 *     <li><b>count = 1 (por defecto):</b> Si el Lambda se ejecuta N veces pero la interacción
 *         es la MISMA cada vez, usar count = 1 (COSMIC cuenta la operación una sola vez)</li>
 *     <li><b>count &gt; 1:</b> Solo si hay MÚLTIPLES interacciones DISTINTAS dentro del Lambda</li>
 *     <li><b>Múltiples tipos:</b> Usar array: {@code @CosmicLambda({MovementType.READ, MovementType.WRITE})}</li>
 * </ul>
 *
 * <h2>INTEGRACIÓN EN PROYECTOS:</h2>
 * Agregar como dependencia en pom.xml:
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;com.emir.analyzer&lt;/groupId&gt;
 *     &lt;artifactId&gt;cosmic-annotations&lt;/artifactId&gt;
 *     &lt;version&gt;1.0.0&lt;/version&gt;
 *     &lt;scope&gt;provided&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * @author Sistema de Análisis COSMIC
 * @version 1.0.0
 * @since 2025-01-01
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface CosmicLambda {

    /**
     * Tipos de movimientos COSMIC que ocurren dentro del Lambda.
     *
     * @return Array de tipos de movimiento
     */
    MovementType[] value();

    /**
     * Número de veces que ocurre cada movimiento dentro del Lambda.
     *
     * @return Número de movimientos (por defecto 1)
     */
    int count() default 1;

    /**
     * Descripción opcional para documentación y debugging.
     *
     * @return Descripción legible por humanos
     */
    String description() default "";

    /**
     * Tipos de movimientos COSMIC soportados (ISO/IEC 19761).
     */
    enum MovementType {
        /**
         * Lectura de datos desde almacenamiento persistente.
         *
         * <p><b>Incluye:</b></p>
         * <ul>
         *     <li>Lectura de base de datos: repository.findById(), query.getResultList()</li>
         *     <li>Llamadas HTTP GET: restTemplate.getForObject(), webClient.get()</li>
         *     <li>Lectura de archivos: Files.readAllBytes(), FileInputStream.read()</li>
         * </ul>
         */
        READ,

        /**
         * Escritura de datos hacia almacenamiento persistente.
         *
         * <p><b>Incluye:</b></p>
         * <ul>
         *     <li>Escritura a base de datos: repository.save(), query.executeUpdate()</li>
         *     <li>Llamadas HTTP POST/PUT/DELETE: restTemplate.postForObject(), webClient.post()</li>
         *     <li>Escritura de archivos: Files.write(), FileOutputStream.write()</li>
         * </ul>
         */
        WRITE
    }
}