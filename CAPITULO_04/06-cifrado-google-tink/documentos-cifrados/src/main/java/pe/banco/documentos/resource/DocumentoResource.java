package pe.banco.documentos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.documentos.dto.DocumentoRequest;
import pe.banco.documentos.entity.Documento;
import pe.banco.documentos.repository.DocumentoRepository;
import pe.banco.documentos.service.CryptoService;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/v1/documentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DocumentoResource {

    @Inject
    DocumentoRepository repository;

    @Inject
    CryptoService cryptoService;

    @POST
    @Transactional
    public Response crear(DocumentoRequest request) {
        try {
            // Cifrar el contenido ANTES de persistir
            String contenidoCifrado = cryptoService.cifrar(request.contenido);
            
            Documento documento = new Documento(request.titulo, contenidoCifrado);
            repository.persist(documento);
            
            return Response.created(URI.create("/api/v1/documentos/" + documento.id))
                    .entity(buildResponse(documento))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al cifrar: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarTodos() {
        try {
            List<Documento> documentos = repository.listAll();
            List<Map<String, Object>> response = documentos.stream()
                    .map(this::buildResponse)
                    .collect(Collectors.toList());
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al descifrar: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        try {
            Documento documento = repository.findById(id);
            if (documento == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(buildResponse(documento)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al descifrar: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Endpoint especial para ver el contenido CIFRADO tal como está en la BD.
     * Útil para propósitos pedagógicos y debugging.
     * ⚠️ NO descifra el contenido - lo devuelve en su forma cifrada (Base64).
     */
    @GET
    @Path("/raw/{id}")
    public Response buscarCifrado(@PathParam("id") Long id) {
        Documento documento = repository.findById(id);
        if (documento == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", documento.id);
        response.put("titulo", documento.titulo);
        response.put("contenido_cifrado", documento.contenidoCifrado); // ¡SIN descifrar!
        response.put("fechaCreacion", documento.fechaCreacion);
        response.put("advertencia", "Este contenido está CIFRADO - es exactamente como se almacena en PostgreSQL");
        
        return Response.ok(response).build();
    }

    private Map<String, Object> buildResponse(Documento doc) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("id", doc.id);
            response.put("titulo", doc.titulo);
            response.put("contenido", cryptoService.descifrar(doc.contenidoCifrado)); // Descifrar al mostrar
            response.put("fechaCreacion", doc.fechaCreacion);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar documento", e);
        }
    }
}