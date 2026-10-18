package pe.tecniya.auth.exception;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String email) {
        super("Ya existe un usuario con el correo " + email);
    }
}
