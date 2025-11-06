package pe.banco.documentos.dto;

public class DocumentoRequest {
    public String titulo;
    public String contenido;  // Este llegará en texto plano y se cifrará

    public DocumentoRequest() {
    }
}