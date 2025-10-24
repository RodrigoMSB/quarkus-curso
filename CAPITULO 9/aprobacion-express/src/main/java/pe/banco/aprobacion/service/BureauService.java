package pe.banco.aprobacion.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Random;
import java.util.Set;

/**
 * Servicio que simula la consulta a un Bureau de Crédito externo.
 * En producción, esto sería una llamada REST a Equifax, Sentinel, Infocorp, etc.
 * 
 * Analogía: Es como llamar al historial médico de un paciente antes de operarlo.
 * Necesitas saber su historia antes de tomar decisiones importantes.
 */
@ApplicationScoped
public class BureauService {

    private static final Logger LOG = Logger.getLogger(BureauService.class);

    // Simulación: Lista negra de documentos con mal historial crediticio
    private static final Set<String> LISTA_NEGRA_SIMULADA = Set.of(
            "12345678",  // DNI con mal historial
            "87654321",  // DNI con deudas impagas
            "11111111",  // DNI bloqueado
            "99999999"   // DNI en cobranza judicial
    );

    // Simulación: Documentos con historial regular (requieren más análisis)
    private static final Set<String> HISTORIAL_REGULAR = Set.of(
            "22222222",
            "33333333",
            "44444444"
    );

    private final Random random = new Random();

    /**
     * Consulta el historial crediticio de un cliente.
     * 
     * @param numeroDocumento Documento del cliente
     * @return true si está en lista negra, false si está limpio
     */
    public boolean estaEnListaNegra(String numeroDocumento) {
        LOG.infof("🔍 Consultando bureau crediticio para documento: %s", numeroDocumento);

        // Simular latencia de red (5-50ms)
        simularLatencia();

        boolean enListaNegra = LISTA_NEGRA_SIMULADA.contains(numeroDocumento);
        
        if (enListaNegra) {
            LOG.warnf("⚠️ Cliente %s encontrado en LISTA NEGRA del bureau", numeroDocumento);
        } else {
            LOG.infof("✅ Cliente %s tiene historial limpio en el bureau", numeroDocumento);
        }

        return enListaNegra;
    }

    /**
     * Consulta el score histórico del cliente en el bureau.
     * Retorna un score entre 300 y 850 (estándar FICO).
     * 
     * @param numeroDocumento Documento del cliente
     * @return Score histórico del bureau (300-850)
     */
    public int consultarScoreHistorico(String numeroDocumento) {
        LOG.infof("📊 Consultando score histórico para documento: %s", numeroDocumento);

        // Simular latencia de red
        simularLatencia();

        // Si está en lista negra → score bajo
        if (LISTA_NEGRA_SIMULADA.contains(numeroDocumento)) {
            int scoreBajo = 300 + random.nextInt(150); // 300-449
            LOG.warnf("Score histórico bajo: %d (cliente en lista negra)", scoreBajo);
            return scoreBajo;
        }

        // Si tiene historial regular → score medio
        if (HISTORIAL_REGULAR.contains(numeroDocumento)) {
            int scoreMedio = 550 + random.nextInt(100); // 550-649
            LOG.infof("Score histórico medio: %d (historial regular)", scoreMedio);
            return scoreMedio;
        }

        // Por defecto: score bueno (simulación de clientes nuevos/buenos)
        int scoreBueno = 650 + random.nextInt(200); // 650-849
        LOG.infof("Score histórico bueno: %d", scoreBueno);
        return scoreBueno;
    }

    /**
     * Consulta el número de productos crediticios activos del cliente.
     * 
     * @param numeroDocumento Documento del cliente
     * @return Número de créditos activos
     */
    public int consultarCreditosActivos(String numeroDocumento) {
        LOG.infof("🏦 Consultando créditos activos para documento: %s", numeroDocumento);

        simularLatencia();

        // Clientes en lista negra tienen muchos créditos
        if (LISTA_NEGRA_SIMULADA.contains(numeroDocumento)) {
            int muchoCreditos = 5 + random.nextInt(6); // 5-10 créditos
            LOG.warnf("Cliente tiene %d créditos activos (alto riesgo)", muchoCreditos);
            return muchoCreditos;
        }

        // Historial regular
        if (HISTORIAL_REGULAR.contains(numeroDocumento)) {
            int creditosRegulares = 2 + random.nextInt(3); // 2-4 créditos
            LOG.infof("Cliente tiene %d créditos activos (aceptable)", creditosRegulares);
            return creditosRegulares;
        }

        // Por defecto: pocos créditos (saludable)
        int pocosCreditos = random.nextInt(3); // 0-2 créditos
        LOG.infof("Cliente tiene %d créditos activos (saludable)", pocosCreditos);
        return pocosCreditos;
    }

    /**
     * Verifica si el cliente tiene morosidad en los últimos 12 meses.
     * 
     * @param numeroDocumento Documento del cliente
     * @return true si tiene morosidad, false si está al día
     */
    public boolean tieneMorosidadReciente(String numeroDocumento) {
        LOG.infof("📅 Verificando morosidad reciente para documento: %s", numeroDocumento);

        simularLatencia();

        // Lista negra siempre tiene morosidad
        if (LISTA_NEGRA_SIMULADA.contains(numeroDocumento)) {
            LOG.warnf("⚠️ Cliente tiene MOROSIDAD reciente");
            return true;
        }

        // Historial regular: 40% de probabilidad de morosidad leve
        if (HISTORIAL_REGULAR.contains(numeroDocumento)) {
            boolean morosidad = random.nextDouble() < 0.4;
            if (morosidad) {
                LOG.warnf("Cliente tiene morosidad leve en los últimos 12 meses");
            }
            return morosidad;
        }

        // Por defecto: sin morosidad
        LOG.infof("✅ Cliente sin morosidad en los últimos 12 meses");
        return false;
    }

    /**
     * Simula la latencia de red de una consulta REST real.
     * En producción, esto sería el tiempo real de respuesta del servicio externo.
     */
    private void simularLatencia() {
        try {
            // Simular entre 5 y 50 milisegundos
            int latencia = 5 + random.nextInt(45);
            Thread.sleep(latencia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Simulación de latencia interrumpida", e);
        }
    }

    /**
     * Método auxiliar para testing: permite agregar documentos a la lista negra.
     * En producción, esto no existiría.
     */
    public boolean esDocumentoConocido(String numeroDocumento) {
        return LISTA_NEGRA_SIMULADA.contains(numeroDocumento) 
            || HISTORIAL_REGULAR.contains(numeroDocumento);
    }
}
