package br.edu.checkout;

import java.math.BigDecimal;

public record PedidoConfirmado(
        String clienteId,
        String email,
        String sku,
        int quantidade,
        BigDecimal total,
        String transacaoId) {
}
