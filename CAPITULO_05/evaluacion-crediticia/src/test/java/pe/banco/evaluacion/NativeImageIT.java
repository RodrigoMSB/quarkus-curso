package pe.banco.evaluacion;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import pe.banco.evaluacion.recursos.CreditoRecursoTest;

/**
 * Suite de tests de integración para verificar compilación nativa con GraalVM.
 * <p>
 * Esta clase extiende {@link CreditoRecursoTest} heredando todos sus tests,
 * pero ejecutándolos contra una imagen nativa compilada con GraalVM en lugar
 * de ejecutar en JVM tradicional.
 * </p>
 * 
 * <h3>¿Qué valida este test?</h3>
 * <p>
 * Verifica que la aplicación funciona correctamente cuando se compila a código nativo.
 * GraalVM Native Image tiene restricciones que no existen en JVM tradicional:
 * </p>
 * <ul>
 *   <li>Reflection debe registrarse explícitamente</li>
 *   <li>No se puede cargar clases dinámicamente en runtime</li>
 *   <li>Proxies dinámicos deben generarse en build-time</li>
 * </ul>
 * 
 * <h3>Ventajas de Native Image:</h3>
 * <ul>
 *   <li><b>Startup:</b> 0.024s vs 2.5s en JVM (100x más rápido)</li>
 *   <li><b>Memoria:</b> 50-100 MB vs 200-500 MB en JVM (4-5x menos)</li>
 *   <li><b>Ejecutable:</b> Binario standalone, no requiere JVM instalada</li>
 * </ul>
 * 
 * <h3>Ejecución:</h3>
 * <p>
 * Para compilar y ejecutar este test:
 * </p>
 * <code>./mvnw verify -Pnative</code>
 * <p>
 * Si no tienes GraalVM instalado, usa Docker:
 * </p>
 * <code>./mvnw verify -Pnative -Dquarkus.native.container-build=true</code>
 * 
 * <p>
 * <b>Nota:</b> La compilación nativa toma 2-10 minutos dependiendo del hardware.
 * Se recomienda ejecutar solo en CI/CD, no en desarrollo local.
 * </p>
 * 
 * @see CreditoRecursoTest
 * @see QuarkusIntegrationTest
 */
@QuarkusIntegrationTest
public class NativeImageIT extends CreditoRecursoTest {
    // No hay código aquí - todos los tests son heredados de CreditoRecursoTest
    // Esta clase vacía es suficiente para que Quarkus ejecute los tests heredados
    // contra la imagen nativa en lugar de la JVM
}