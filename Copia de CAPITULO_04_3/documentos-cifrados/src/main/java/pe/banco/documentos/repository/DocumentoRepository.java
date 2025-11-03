package pe.banco.documentos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.documentos.entity.Documento;

@ApplicationScoped
public class DocumentoRepository implements PanacheRepositoryBase<Documento, Long> {
    
    // Métodos personalizados si los necesitas
}