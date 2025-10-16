package pe.banco.evaluacion.repositorios;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.evaluacion.entidades.SolicitudCredito;
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SolicitudCreditoRepository implements PanacheRepository<SolicitudCredito> {

    public Optional<SolicitudCredito> buscarPorDni(String dni) {
        return find("dni", dni).firstResultOptional();
    }

    public List<SolicitudCredito> buscarPorEmail(String email) {
        return list("email", email);
    }

    public List<SolicitudCredito> buscarPorEstado(EstadoSolicitud estado) {
        return list("estado", estado);
    }

    public List<SolicitudCredito> buscarAprobadas() {
        return list("aprobada", true);
    }

    public List<SolicitudCredito> buscarRechazadas() {
        return list("aprobada", false);
    }

    public long contarPorEstado(EstadoSolicitud estado) {
        return count("estado", estado);
    }

    public List<SolicitudCredito> buscarPorScoreMinimo(int scoreMinimo) {
        return list("scoreCrediticio >= ?1", scoreMinimo);
    }
}
