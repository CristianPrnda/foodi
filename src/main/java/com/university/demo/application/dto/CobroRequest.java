package com.university.demo.application.dto;

/** clienteId opcional: si viene, la factura se emite a nombre de ese cliente. */
public record CobroRequest(Long clienteId) {
}
