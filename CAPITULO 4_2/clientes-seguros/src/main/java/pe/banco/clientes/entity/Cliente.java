package pe.banco.clientes.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
public class Cliente extends PanacheEntity {

    @Column(nullable = false)
    public String nombre;

    @Column(name = "numero_tarjeta", nullable = false)
    public String numeroTarjeta;  // Esta columna será CIFRADA

    @Column(nullable = false)
    public String email;  // Esta columna será CIFRADA

    @Column(nullable = false)
    public String telefono;  // Sin cifrar

    public Cliente() {
    }

    public Cliente(String nombre, String numeroTarjeta, String email, String telefono) {
        this.nombre = nombre;
        this.numeroTarjeta = numeroTarjeta;
        this.email = email;
        this.telefono = telefono;
    }
}
