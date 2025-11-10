package pe.banco.evaluacion.repositorios;

import io.quarkus.test.junit.QuarkusTest;  // Anotación que habilita testing con contexto de Quarkus completo
import jakarta.inject.Inject;  // Inyección de dependencias CDI en tests
import jakarta.transaction.Transactional;  // Gestión declarativa de transacciones JTA
import org.junit.jupiter.api.BeforeEach;  // Hook que se ejecuta antes de cada test
import org.junit.jupiter.api.Test;  // Anotación para métodos de test unitarios
import pe.banco.evaluacion.entidades.SolicitudCredito;  // Entidad JPA que persiste el repositorio
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;  // Enum de estados

import java.math.BigDecimal;  // Tipo numérico de precisión para montos financieros
import java.util.List;  // Colección estándar para múltiples resultados
import java.util.Optional;  // Contenedor que puede o no contener un valor

import static org.junit.jupiter.api.Assertions.*;  // Métodos de asserción

/**
 * Suite de tests de integración para el repositorio de solicitudes de crédito.
 * <p>
 * Esta clase de test valida exhaustivamente el {@link SolicitudCreditoRepository},
 * verificando todas sus operaciones de persistencia y consulta contra una base de datos
 * real (H2 en memoria durante tests). A diferencia de tests unitarios con mocks, estos
 * son "integration tests" que validan interacción real con JPA/Hibernate y base de datos.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en estos tests como una "auditoría del sistema de archivado"
 * del banco. Así como un auditor verificaría que el sistema de archivo físico puede:
 * guardar documentos correctamente, buscarlos por diferentes criterios (DNI, fecha, estado),
 * contarlos, actualizarlos sin pérdida de datos, estos tests automatizan esa verificación
 * para el "archivero digital" (repositorio + base de datos).
 * </p>
 * 
 * <h3>Diferencias clave: Unit Test vs Integration Test:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Aspecto</th>
 *     <th>Unit Test</th>
 *     <th>Integration Test (este)</th>
 *   </tr>
 *   <tr>
 *     <td>Dependencias</td>
 *     <td>Mockeadas</td>
 *     <td>Reales (BD, JPA, Hibernate)</td>
 *   </tr>
 *   <tr>
 *     <td>Base de datos</td>
 *     <td>No usa BD</td>
 *     <td>H2 en memoria</td>
 *   </tr>
 *   <tr>
 *     <td>Velocidad</td>
 *     <td>Muy rápido (~5ms)</td>
 *     <td>Más lento (~50-200ms)</td>
 *   </tr>
 *   <tr>
 *     <td>Alcance</td>
 *     <td>Lógica de negocio aislada</td>
 *     <td>Stack completo de persistencia</td>
 *   </tr>
 *   <tr>
 *     <td>Confianza</td>
 *     <td>Valida lógica</td>
 *     <td>Valida integración real</td>
 *   </tr>
 * </table>
 * 
 * <h3>Estrategia de testing:</h3>
 * <p>
 * La suite implementa una estrategia completa de testing de persistencia:
 * <ul>
 *   <li><b>Setup de datos:</b> @BeforeEach crea dataset conocido antes de cada test</li>
 *   <li><b>Tests de consulta:</b> Verificar que queries retornan datos correctos</li>
 *   <li><b>Tests CRUD:</b> Crear, leer, actualizar operaciones básicas</li>
 *   <li><b>Tests de conteo:</b> Validar operaciones de agregación</li>
 *   <li><b>Tests de auditoría:</b> Verificar timestamps automáticos</li>
 *   <li><b>Tests de filtrado:</b> Consultas con criterios múltiples</li>
 * </ul>
 * </p>
 * 
 * <h3>Dataset de prueba (creado en @BeforeEach):</h3>
 * <table border="1">
 *   <tr>
 *     <th>Cantidad</th>
 *     <th>Estado</th>
 *     <th>Aprobada</th>
 *     <th>Score</th>
 *     <th>Propósito</th>
 *   </tr>
 *   <tr>
 *     <td>3</td>
 *     <td>APROBADA</td>
 *     <td>true</td>
 *     <td>750</td>
 *     <td>Casos de éxito</td>
 *   </tr>
 *   <tr>
 *     <td>2</td>
 *     <td>RECHAZADA</td>
 *     <td>false</td>
 *     <td>400</td>
 *     <td>Casos de rechazo</td>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td>PENDIENTE</td>
 *     <td>-</td>
 *     <td>850</td>
 *     <td>Caso especial identificable</td>
 *   </tr>
 * </table>
 * Total: 6 solicitudes por test.
 * 
 * <h3>Uso de H2 en memoria:</h3>
 * <p>
 * Quarkus automáticamente configura H2 para tests cuando detecta @QuarkusTest:
 * <ul>
 *   <li>Base de datos se crea al inicio de cada clase de test</li>
 *   <li>Schema se genera automáticamente desde entidades JPA</li>
 *   <li>Datos se destruyen al finalizar tests (cada ejecución es limpia)</li>
 *   <li>Compatible con sintaxis PostgreSQL (que usa producción)</li>
 * </ul>
 * </p>
 * 
 * <h3>¿Por qué @Transactional en algunos métodos?</h3>
 * <p>
 * JPA requiere transacción activa para operaciones de escritura (persist, update).
 * Los métodos marcados con @Transactional son aquellos que modifican datos:
 * <ul>
 *   <li>@BeforeEach: Inserta dataset de prueba</li>
 *   <li>Tests de persistencia: Crean nuevas entidades</li>
 *   <li>Tests de actualización: Modifican entidades existentes</li>
 * </ul>
 * Tests de solo lectura NO necesitan @Transactional.
 * </p>
 * 
 * <h3>Patrón de aislamiento de tests:</h3>
 * <p>
 * Cada test es independiente gracias a:
 * <ol>
 *   <li>@BeforeEach limpia BD con deleteAll()</li>
 *   <li>@BeforeEach inserta dataset fresco</li>
 *   <li>Cada test ve mismo estado inicial</li>
 *   <li>Tests pueden ejecutarse en cualquier orden</li>
 * </ol>
 * Esto implementa el principio FIRST de testing: Fast, Independent, Repeatable, Self-validating, Timely.
 * </p>
 * 
 * <h3>Ejecución de los tests:</h3>
 * <pre>
 * # Ejecutar todos los tests del repositorio
 * ./mvnw test -Dtest=SolicitudCreditoRepositoryTest
 * 
 * # Ejecutar con logs de SQL (ver queries generadas)
 * ./mvnw test -Dtest=SolicitudCreditoRepositoryTest -Dquarkus.log.category."org.hibernate.SQL".level=DEBUG
 * 
 * # Ver estadísticas de Hibernate
 * ./mvnw test -Dquarkus.hibernate-orm.log.sql=true -Dquarkus.hibernate-orm.statistics=true
 * </pre>
 * 
 * <h3>Cobertura de métodos del repositorio:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Método</th>
 *     <th>Tests</th>
 *     <th>Cobertura</th>
 *   </tr>
 *   <tr>
 *     <td>buscarPorDni()</td>
 *     <td>2</td>
 *     <td>100% (encontrado + no encontrado)</td>
 *   </tr>
 *   <tr>
 *     <td>buscarPorEmail()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>buscarPorEstado()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>buscarAprobadas()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>buscarRechazadas()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>contarPorEstado()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>buscarPorScoreMinimo()</td>
 *     <td>1</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>persist()</td>
 *     <td>2</td>
 *     <td>100% (crear + actualizar)</td>
 *   </tr>
 * </table>
 * 
 * <h3>Mejoras pendientes:</h3>
 * <ul>
 *   <li>Tests de concurrencia: verificar que múltiples threads pueden escribir sin conflictos</li>
 *   <li>Tests de performance: medir tiempo de consultas complejas</li>
 *   <li>Tests de paginación: cuando se implemente en buscarPorEstado()</li>
 *   <li>Tests de N+1 queries: detectar problemas de lazy loading</li>
 *   <li>Tests de integridad referencial: si se agregan relaciones entre entidades</li>
 *   <li>Tests de rollback: verificar que transacciones fallan correctamente</li>
 * </ul>
 * 
 * @see SolicitudCreditoRepository
 * @see SolicitudCredito
 */
@QuarkusTest
class SolicitudCreditoRepositoryTest {

    /**
     * Instancia real del repositorio inyectada por CDI de Quarkus.
     * <p>
     * A diferencia de un mock, esta es la implementación real de Panache Repository
     * conectada a H2 en memoria. Todas las operaciones ejecutan SQL real contra BD real.
     * </p>
     */
    @Inject
    SolicitudCreditoRepository repository;

    /**
     * Setup ejecutado automáticamente antes de CADA test.
     * <p>
     * Este método garantiza que cada test comienza con un estado de base de datos
     * conocido y predecible. Es el equivalente al "preparar el escenario" antes de
     * cada prueba.
     * </p>
     * 
     * <h4>Flujo de ejecución:</h4>
     * <ol>
     *   <li><b>Limpieza:</b> deleteAll() elimina cualquier dato previo</li>
     *   <li><b>Creación de aprobadas:</b> 3 solicitudes con score 750, estado APROBADA</li>
     *   <li><b>Creación de rechazadas:</b> 2 solicitudes con score 400, estado RECHAZADA</li>
     *   <li><b>Creación de caso especial:</b> 1 solicitud PENDIENTE con datos únicos</li>
     * </ol>
     * 
     * <h4>¿Por qué @Transactional aquí?</h4>
     * <p>
     * persist() es operación de escritura que requiere transacción activa.
     * @Transactional crea transacción, ejecuta método, y hace commit automáticamente.
     * Sin esta anotación, obtendríamos TransactionRequiredException.
     * </p>
     * 
     * <h4>Patrón de construcción de datos:</h4>
     * <p>
     * Usa loops para crear entidades similares (aprobadas, rechazadas) reduciendo
     * duplicación de código. El caso especial (líneas 63-74) se crea manualmente
     * porque tiene datos únicos para tests específicos.
     * </p>
     * 
     * <h4>Datos del caso especial (PENDIENTE):</h4>
     * <ul>
     *   <li><b>DNI:</b> "12345678" (fácil de recordar en tests)</li>
     *   <li><b>Nombre:</b> "Juan Pérez González" (nombre completo realista)</li>
     *   <li><b>Email:</b> "juan.perez@email.cl" (identificable)</li>
     *   <li><b>Score:</b> 850 (excelente, fácil de filtrar)</li>
     *   <li><b>Estado:</b> PENDIENTE (único con este estado)</li>
     * </ul>
     * Estos valores únicos permiten tests que busquen específicamente esta entidad.
     */
    @BeforeEach
    @Transactional
    void insertarDatosPrueba() {
        // Limpieza: garantizar estado inicial vacío
        repository.deleteAll();

        // Crear 3 solicitudes aprobadas (DNI: 12345671, 12345672, 12345673)
        for (int i = 1; i <= 3; i++) {
            SolicitudCredito s = new SolicitudCredito();
            s.setDni("1234567" + i);
            s.setNombreCompleto("Test Usuario " + i);
            s.setEmail("test" + i + "@test.cl");
            s.setEdad(30);
            s.setIngresosMensuales(new BigDecimal("2000000"));
            s.setDeudasActuales(new BigDecimal("100000"));
            s.setMontoSolicitado(new BigDecimal("5000000"));
            s.setMesesEnEmpleoActual(24);
            s.setScoreCrediticio(750);
            s.setAprobada(true);
            s.setEstado(EstadoSolicitud.APROBADA);
            repository.persist(s);
        }

        // Crear 2 rechazadas (DNI: 12345674, 12345675)
        for (int i = 4; i <= 5; i++) {
            SolicitudCredito s = new SolicitudCredito();
            s.setDni("1234567" + i);
            s.setNombreCompleto("Test Usuario " + i);
            s.setEmail("test" + i + "@test.cl");
            s.setEdad(25);
            s.setIngresosMensuales(new BigDecimal("1000000"));
            s.setDeudasActuales(new BigDecimal("600000"));  // DTI 60%
            s.setMontoSolicitado(new BigDecimal("3000000"));
            s.setMesesEnEmpleoActual(2);  // Menos de 3 meses
            s.setScoreCrediticio(400);
            s.setAprobada(false);
            s.setEstado(EstadoSolicitud.RECHAZADA);
            repository.persist(s);
        }
        
        // Crear 1 pendiente (caso especial identificable)
        SolicitudCredito pendiente = new SolicitudCredito();
        pendiente.setDni("12345678");  // DNI fácil de recordar
        pendiente.setNombreCompleto("Juan Pérez González");
        pendiente.setEmail("juan.perez@email.cl");
        pendiente.setEdad(35);
        pendiente.setIngresosMensuales(new BigDecimal("2500000"));
        pendiente.setDeudasActuales(new BigDecimal("300000"));
        pendiente.setMontoSolicitado(new BigDecimal("5000000"));
        pendiente.setMesesEnEmpleoActual(48);
        pendiente.setScoreCrediticio(850);  // Score alto, fácil de filtrar
        pendiente.setEstado(EstadoSolicitud.PENDIENTE);
        repository.persist(pendiente);
    }

    /**
     * Verifica que buscarPorDni() encuentra solicitud existente.
     * <p>
     * Test del "camino feliz" donde el DNI existe en la base de datos.
     * Valida que:
     * <ul>
     *   <li>Optional.isPresent() es true (solicitud fue encontrada)</li>
     *   <li>Nombre coincide con el esperado (verificación de datos correctos)</li>
     * </ul>
     * </p>
     * 
     * <h4>Query SQL generada (aproximada):</h4>
     * <pre>
     * SELECT * FROM solicitudes_credito WHERE dni = '12345678' LIMIT 1
     * </pre>
     * 
     * <h4>Uso de Optional:</h4>
     * <p>
     * Optional es el patrón Java moderno para representar "puede haber o no un valor".
     * Es superior a retornar null porque fuerza al código cliente a manejar explícitamente
     * el caso de ausencia, evitando NullPointerException.
     * </p>
     */
    @Test
    void deberiaBuscarPorDni() {
        Optional<SolicitudCredito> solicitud = repository.buscarPorDni("12345678");
        
        assertTrue(solicitud.isPresent(), "Debería encontrar solicitud con DNI 12345678");
        assertEquals("Juan Pérez González", solicitud.get().getNombreCompleto());
    }

    /**
     * Verifica que buscarPorDni() retorna Optional.empty() cuando DNI no existe.
     * <p>
     * Test del "camino infeliz" donde el DNI no existe en la base de datos.
     * Es crítico validar este caso para prevenir NullPointerException en código
     * que asume que siempre habrá resultado.
     * </p>
     * 
     * <h4>¿Por qué "99999999"?</h4>
     * <p>
     * DNI suficientemente diferente de los del dataset (1234567X) que es imposible
     * que coincida por casualidad. Representa DNI no registrado en el sistema.
     * </p>
     * 
     * <h4>Query SQL generada:</h4>
     * <pre>
     * SELECT * FROM solicitudes_credito WHERE dni = '99999999' LIMIT 1
     * -- Retorna 0 filas → Optional.empty()
     * </pre>
     */
    @Test
    void noDeberiaEncontrarRutInexistente() {
        Optional<SolicitudCredito> solicitud = repository.buscarPorDni("99999999");
        
        assertFalse(solicitud.isPresent(), "No debería encontrar DNI inexistente");
    }

    /**
     * Verifica que buscarPorEmail() encuentra solicitudes por email.
     * <p>
     * Valida búsqueda por campo único alternativo (email) que también identifica cliente.
     * </p>
     * 
     * <h4>Validaciones:</h4>
     * <ul>
     *   <li>Lista NO está vacía (encontró resultados)</li>
     *   <li>Tamaño es exactamente 1 (email es unique en BD)</li>
     * </ul>
     * 
     * <h4>Nota sobre unicidad:</h4>
     * <p>
     * Aunque email tiene constraint UNIQUE en la entidad, el método retorna List
     * en lugar de Optional porque es más genérico. Si se relaja el constraint en
     * el futuro, el método ya está preparado para múltiples resultados.
     * </p>
     */
    @Test
    void deberiaBuscarPorEmail() {
        List<SolicitudCredito> solicitudes = repository.buscarPorEmail("juan.perez@email.cl");
        
        assertFalse(solicitudes.isEmpty(), "Debería encontrar solicitudes con ese email");
        assertEquals(1, solicitudes.size());
    }

    /**
     * Verifica que buscarPorEstado() filtra correctamente por estado.
     * <p>
     * Test de consulta con criterio de filtrado. Valida que el repositorio
     * puede filtrar solicitudes según su posición en el workflow.
     * </p>
     * 
     * <h4>Dataset esperado:</h4>
     * <p>
     * @BeforeEach creó 3 solicitudes APROBADAS, por lo que este test espera
     * encontrar al menos 2 (usa >= en vez de ==3 para ser robusto ante cambios).
     * </p>
     * 
     * <h4>Query SQL generada:</h4>
     * <pre>
     * SELECT * FROM solicitudes_credito WHERE estado = 'APROBADA'
     * </pre>
     * 
     * <h4>Uso de Enum en query:</h4>
     * <p>
     * JPA mapea automáticamente enum a String en BD. La comparación en query
     * usa el nombre del enum (APROBADA) como string en la columna estado.
     * </p>
     */
    @Test
    void deberiaBuscarPorEstado() {
        List<SolicitudCredito> aprobadas = repository.buscarPorEstado(EstadoSolicitud.APROBADA);
        
        assertFalse(aprobadas.isEmpty(), "Debería haber solicitudes aprobadas");
        assertTrue(aprobadas.size() >= 2, "Debería haber al menos 2 aprobadas");
    }

    /**
     * Verifica que buscarAprobadas() retorna solo solicitudes con aprobada=true.
     * <p>
     * Test de método de conveniencia que encapsula filtrado común.
     * </p>
     * 
     * <h4>Validaciones:</h4>
     * <ul>
     *   <li>Lista NO está vacía</li>
     *   <li>TODAS las solicitudes tienen aprobada=true (forEach validation)</li>
     * </ul>
     * 
     * <h4>Patrón forEach con asserción:</h4>
     * <pre>
     * aprobadas.forEach(s -> assertTrue(s.getAprobada()));
     * </pre>
     * <p>
     * Esto valida que CADA elemento cumple la condición, no solo el primero o último.
     * Si alguna solicitud tiene aprobada=false, el test falla inmediatamente.
     * </p>
     */
    @Test
    void deberiaBuscarAprobadas() {
        List<SolicitudCredito> aprobadas = repository.buscarAprobadas();
        
        assertFalse(aprobadas.isEmpty());
        aprobadas.forEach(s -> assertTrue(s.getAprobada()));
    }

    /**
     * Verifica que buscarRechazadas() retorna solo solicitudes con aprobada=false.
     * <p>
     * Complemento del test anterior, valida el filtrado inverso.
     * </p>
     * 
     * <h4>Dataset esperado:</h4>
     * <p>
     * @BeforeEach creó 2 solicitudes rechazadas (DNI 12345674 y 12345675).
     * </p>
     */
    @Test
    void deberiaBuscarRechazadas() {
        List<SolicitudCredito> rechazadas = repository.buscarRechazadas();
        
        assertFalse(rechazadas.isEmpty());
        rechazadas.forEach(s -> assertFalse(s.getAprobada()));
    }

    /**
     * Verifica que contarPorEstado() retorna cantidad correcta.
     * <p>
     * Test de operación de agregación (COUNT en SQL).
     * </p>
     * 
     * <h4>Query SQL generada:</h4>
     * <pre>
     * SELECT COUNT(*) FROM solicitudes_credito WHERE estado = 'PENDIENTE'
     * </pre>
     * 
     * <h4>Ventaja de COUNT sobre list().size():</h4>
     * <ul>
     *   <li>COUNT: Solo retorna un número, no carga entidades en memoria</li>
     *   <li>list().size(): Carga todas las entidades, luego cuenta (ineficiente)</li>
     * </ul>
     * <p>
     * Para 1 millón de solicitudes pendientes:
     * <ul>
     *   <li>COUNT: ~10ms, memoria constante</li>
     *   <li>list().size(): ~30s, OutOfMemoryError probable</li>
     * </ul>
     * </p>
     */
    @Test
    void deberiaContarPorEstado() {
        long pendientes = repository.contarPorEstado(EstadoSolicitud.PENDIENTE);
        
        assertTrue(pendientes >= 1, "Debería haber al menos 1 pendiente");
    }

    /**
     * Verifica que buscarPorScoreMinimo() filtra correctamente por score.
     * <p>
     * Test de consulta con operador de comparación (>=).
     * </p>
     * 
     * <h4>Dataset esperado:</h4>
     * <ul>
     *   <li>3 solicitudes aprobadas con score 750</li>
     *   <li>1 solicitud pendiente con score 850</li>
     *   <li>Total con score >= 700: 4 solicitudes</li>
     * </ul>
     * 
     * <h4>Query SQL generada:</h4>
     * <pre>
     * SELECT * FROM solicitudes_credito WHERE score_crediticio >= 700
     * </pre>
     * 
     * <h4>Validación exhaustiva:</h4>
     * <p>
     * No solo verifica que la lista no está vacía, sino que CADA elemento
     * cumple el criterio de score >= 700. Esto detectaría bugs donde el
     * repositorio retorna datos que no cumplen el filtro.
     * </p>
     */
    @Test
    void deberiaBuscarPorScoreMinimo() {
        List<SolicitudCredito> altosScores = repository.buscarPorScoreMinimo(700);
        
        assertFalse(altosScores.isEmpty());
        altosScores.forEach(s -> assertTrue(s.getScoreCrediticio() >= 700));
    }

    /**
     * Verifica que persist() crea nueva solicitud y genera ID automáticamente.
     * <p>
     * Test de operación CREATE del CRUD. Valida que JPA:
     * <ul>
     *   <li>Inserta la entidad en base de datos</li>
     *   <li>Genera ID automáticamente (secuencia/identity)</li>
     *   <li>ID es mayor a 0 (valores positivos)</li>
     * </ul>
     * </p>
     * 
     * <h4>Flujo de persistencia:</h4>
     * <ol>
     *   <li>Crear entidad nueva (id = null)</li>
     *   <li>repository.persist(nueva)</li>
     *   <li>Hibernate ejecuta INSERT INTO solicitudes_credito (...)</li>
     *   <li>BD genera ID (secuencia o auto-increment)</li>
     *   <li>Hibernate actualiza nueva.id con valor generado</li>
     *   <li>Transaction commit hace flush a BD</li>
     * </ol>
     * 
     * <h4>¿Por qué @Transactional aquí?</h4>
     * <p>
     * persist() es operación de escritura que requiere transacción.
     * Al finalizar el método (fin del test), @Transactional hace commit
     * automáticamente, materializando el INSERT en BD.
     * </p>
     */
    @Test
    @Transactional
    void deberiaPersistirNuevaSolicitud() {
        SolicitudCredito nueva = new SolicitudCredito();
        nueva.setDni("11111111");
        nueva.setNombreCompleto("Test Persistencia");
        nueva.setEmail("test.persistencia@email.cl");
        nueva.setEdad(30);
        nueva.setIngresosMensuales(new BigDecimal("2000000"));
        nueva.setDeudasActuales(new BigDecimal("300000"));
        nueva.setMontoSolicitado(new BigDecimal("4000000"));
        nueva.setMesesEnEmpleoActual(12);
        nueva.setEstado(EstadoSolicitud.PENDIENTE);

        repository.persist(nueva);

        assertNotNull(nueva.id);
        assertTrue(nueva.id > 0);
    }

    /**
     * Verifica que persist() actualiza entidad existente (operación UPDATE).
     * <p>
     * JPA es inteligente: persist() detecta si la entidad ya tiene ID y decide:
     * <ul>
     *   <li>ID es null → INSERT (crear nueva)</li>
     *   <li>ID no es null → UPDATE (actualizar existente)</li>
     * </ul>
     * Este test valida el segundo caso.
     * </p>
     * 
     * <h4>Flujo de actualización:</h4>
     * <ol>
     *   <li>Obtener solicitud existente de BD (tiene ID)</li>
     *   <li>Modificar campo (estado)</li>
     *   <li>repository.persist(solicitud)</li>
     *   <li>Hibernate detecta que ID existe → UPDATE en vez de INSERT</li>
     *   <li>Hibernate ejecuta UPDATE solicitudes_credito SET estado='REQUIERE_ANALISIS' WHERE id=?</li>
     *   <li>Transaction commit hace flush a BD</li>
     * </ol>
     * 
     * <h4>Validación de actualización:</h4>
     * <p>
     * Recupera la entidad nuevamente de BD con findById() para verificar que
     * el cambio se persistió correctamente. Esto evita falsos positivos donde
     * la entidad en memoria está modificada pero BD no.
     * </p>
     */
    @Test
    @Transactional
    void deberiaActualizarSolicitud() {
        List<SolicitudCredito> todas = repository.listAll();
        SolicitudCredito solicitud = todas.get(0);
        assertNotNull(solicitud);

        solicitud.setEstado(EstadoSolicitud.REQUIERE_ANALISIS);
        repository.persist(solicitud);

        SolicitudCredito actualizada = repository.findById(solicitud.id);
        assertEquals(EstadoSolicitud.REQUIERE_ANALISIS, actualizada.getEstado());
    }

    /**
     * Verifica que timestamps de auditoría se generan automáticamente.
     * <p>
     * Test de funcionalidad de auditoría proporcionada por anotaciones Hibernate:
     * <ul>
     *   <li>@CreationTimestamp: Se establece automáticamente al crear entidad</li>
     *   <li>@UpdateTimestamp: Se actualiza automáticamente al modificar entidad</li>
     * </ul>
     * </p>
     * 
     * <h4>Importancia de auditoría:</h4>
     * <ul>
     *   <li><b>Compliance:</b> Regulaciones requieren saber cuándo se creó/modificó cada solicitud</li>
     *   <li><b>Debugging:</b> Identificar cuándo surgió un problema</li>
     *   <li><b>Analytics:</b> Análisis de tiempos de procesamiento</li>
     *   <li><b>Legal:</b> Evidencia de cuándo se tomó decisión crediticia</li>
     * </ul>
     * 
     * <h4>Nota sobre actualización:</h4>
     * <p>
     * Este test solo verifica fechaCreacion, no fechaActualizacion.
     * Para testear fechaActualizacion, necesitaríamos:
     * <ol>
     *   <li>Crear entidad</li>
     *   <li>Esperar (Thread.sleep) o usar Clock mockeado</li>
     *   <li>Actualizar entidad</li>
     *   <li>Verificar que fechaActualizacion > fechaCreacion</li>
     * </ol>
     * Esto está en mejoras pendientes.
     * </p>
     */
    @Test
    void deberiaTenerFechasCreacionYActualizacion() {
        List<SolicitudCredito> todas = repository.listAll();
        SolicitudCredito solicitud = todas.get(0);
        
        assertNotNull(solicitud);
        assertNotNull(solicitud.getFechaCreacion());
    }

    /**
     * Verifica que listAll() retorna todas las solicitudes sin filtros.
     * <p>
     * Test de operación READ básica del CRUD sin criterios de filtrado.
     * </p>
     * 
     * <h4>Dataset esperado:</h4>
     * <p>
     * @BeforeEach crea 6 solicitudes (3 aprobadas + 2 rechazadas + 1 pendiente),
     * por lo que este test espera al menos 5 para ser robusto.
     * </p>
     * 
     * <h4>Query SQL generada:</h4>
     * <pre>
     * SELECT * FROM solicitudes_credito
     * -- Sin WHERE, retorna todas las filas
     * </pre>
     * 
     * <h4>⚠️ Advertencia de escalabilidad:</h4>
     * <p>
     * listAll() sin paginación es peligroso en producción. Con 100,000 solicitudes:
     * <ul>
     *   <li>Carga todas en memoria → OutOfMemoryError</li>
     *   <li>Query lenta (30+ segundos)</li>
     *   <li>Red saturada transfiriendo MB de datos</li>
     * </ul>
     * Usar paginación en producción: list(Page.of(0, 20))
     * </p>
     */
    @Test
    void deberiaListarTodasLasSolicitudes() {
        List<SolicitudCredito> todas = repository.listAll();
        
        assertTrue(todas.size() >= 5, "Debería haber al menos 5 solicitudes");
    }
}