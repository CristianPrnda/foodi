package com.university.demo.presentation.exception;

/** Se lanza cuando un id no existe. El GlobalExceptionHandler la convierte en HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
