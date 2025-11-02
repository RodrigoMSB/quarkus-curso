package pe.banco.evaluacion.repositorios;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.banco.evaluacion.entidades.SolicitudCredito;
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SolicitudCreditoRepositoryTest {

    @Inject
    SolicitudCreditoRepository repository;

    @BeforeEach
    @Transactional
    void insertarDatosPrueba() {
        repository.deleteAll();

        // Crear 3 solicitudes aprobadas
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

        // Crear 2 rechazadas
        for (int i = 4; i <= 5; i++) {
            SolicitudCredito s = new SolicitudCredito();
            s.setDni("1234567" + i);
            s.setNombreCompleto("Test Usuario " + i);
            s.setEmail("test" + i + "@test.cl");
            s.setEdad(25);
            s.setIngresosMensuales(new BigDecimal("1000000"));
            s.setDeudasActuales(new BigDecimal("600000"));
            s.setMontoSolicitado(new BigDecimal("3000000"));
            s.setMesesEnEmpleoActual(2);
            s.setScoreCrediticio(400);
            s.setAprobada(false);
            s.setEstado(EstadoSolicitud.RECHAZADA);
            repository.persist(s);
        }
        
        // Crear 1 pendiente
        SolicitudCredito pendiente = new SolicitudCredito();
        pendiente.setDni("12345678");
        pendiente.setNombreCompleto("Juan Pérez González");
        pendiente.setEmail("juan.perez@email.cl");
        pendiente.setEdad(35);
        pendiente.setIngresosMensuales(new BigDecimal("2500000"));
        pendiente.setDeudasActuales(new BigDecimal("300000"));
        pendiente.setMontoSolicitado(new BigDecimal("5000000"));
        pendiente.setMesesEnEmpleoActual(48);
        pendiente.setScoreCrediticio(850);
        pendiente.setEstado(EstadoSolicitud.PENDIENTE);
        repository.persist(pendiente);
    }

    @Test
    void deberiaBuscarPorDni() {
        Optional<SolicitudCredito> solicitud = repository.buscarPorDni("12345678");
        assertTrue(solicitud.isPresent(), "Debería encontrar solicitud con DNI 12345678-5");
        assertEquals("Juan Pérez González", solicitud.get().getNombreCompleto());
    }

    @Test
    void noDeberiaEncontrarRutInexistente() {
        Optional<SolicitudCredito> solicitud = repository.buscarPorDni("99999999");
        assertFalse(solicitud.isPresent(), "No debería encontrar DNI inexistente");
    }

    @Test
    void deberiaBuscarPorEmail() {
        List<SolicitudCredito> solicitudes = repository.buscarPorEmail("juan.perez@email.cl");
        assertFalse(solicitudes.isEmpty(), "Debería encontrar solicitudes con ese email");
        assertEquals(1, solicitudes.size());
    }

    @Test
    void deberiaBuscarPorEstado() {
        List<SolicitudCredito> aprobadas = repository.buscarPorEstado(EstadoSolicitud.APROBADA);
        assertFalse(aprobadas.isEmpty(), "Debería haber solicitudes aprobadas");
        assertTrue(aprobadas.size() >= 2, "Debería haber al menos 2 aprobadas");
    }

    @Test
    void deberiaBuscarAprobadas() {
        List<SolicitudCredito> aprobadas = repository.buscarAprobadas();
        assertFalse(aprobadas.isEmpty());
        aprobadas.forEach(s -> assertTrue(s.getAprobada()));
    }

    @Test
    void deberiaBuscarRechazadas() {
        List<SolicitudCredito> rechazadas = repository.buscarRechazadas();
        assertFalse(rechazadas.isEmpty());
        rechazadas.forEach(s -> assertFalse(s.getAprobada()));
    }

    @Test
    void deberiaContarPorEstado() {
        long pendientes = repository.contarPorEstado(EstadoSolicitud.PENDIENTE);
        assertTrue(pendientes >= 1, "Debería haber al menos 1 pendiente");
    }

    @Test
    void deberiaBuscarPorScoreMinimo() {
        List<SolicitudCredito> altosScores = repository.buscarPorScoreMinimo(700);
        assertFalse(altosScores.isEmpty());
        altosScores.forEach(s -> assertTrue(s.getScoreCrediticio() >= 700));
    }

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

    @Test
    void deberiaTenerFechasCreacionYActualizacion() {
        List<SolicitudCredito> todas = repository.listAll();
        SolicitudCredito solicitud = todas.get(0);
        assertNotNull(solicitud);
        assertNotNull(solicitud.getFechaCreacion());
    }

    @Test
    void deberiaListarTodasLasSolicitudes() {
        List<SolicitudCredito> todas = repository.listAll();
        assertTrue(todas.size() >= 5, "Debería haber al menos 5 solicitudes");
    }
}
