package pe.banco.clientes.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.clientes.entity.Cliente;

import java.util.List;

@ApplicationScoped
public class ClienteRepository implements PanacheRepositoryBase<Cliente, Long> {

    public List<Cliente> buscarPorTarjeta(String numeroTarjeta) {
        return list("numeroTarjeta", numeroTarjeta);
    }
}
