package pe.banco.documentos.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
public class Documento extends PanacheEntity {

    @Column(nullable = false)
    public String titulo;

    @Column(name = "contenido_cifrado", nullable = false, columnDefinition = "TEXT")
    public String contenidoCifrado;  // Se guardará CIFRADO

    @Column(name = "fecha_creacion", nullable = false)
    public LocalDateTime fechaCreacion;

    public Documento() {
    }

    public Documento(String titulo, String contenidoCifrado) {
        this.titulo = titulo;
        this.contenidoCifrado = contenidoCifrado;
        this.fechaCreacion = LocalDateTime.now();
    }
}