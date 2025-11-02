package pe.banco;

public class ValidacionResponse {
    
    private boolean valido;
    private String numeroCuenta;
    private String mensaje;
    
    public ValidacionResponse() {
    }
    
    public ValidacionResponse(boolean valido, String numeroCuenta, String mensaje) {
        this.valido = valido;
        this.numeroCuenta = numeroCuenta;
        this.mensaje = mensaje;
    }
    
    public boolean isValido() {
        return valido;
    }
    
    public void setValido(boolean valido) {
        this.valido = valido;
    }
    
    public String getNumeroCuenta() {
        return numeroCuenta;
    }
    
    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}