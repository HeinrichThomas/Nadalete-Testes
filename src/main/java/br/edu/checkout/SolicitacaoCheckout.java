package br.edu.checkout;

import java.math.BigDecimal;

public record SolicitacaoCheckout(
        String clienteId,
        String email,
        String sku,
        int quantidade,
        BigDecimal precoUnitario) {
}
