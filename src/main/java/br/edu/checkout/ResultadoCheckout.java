package br.edu.checkout;

import java.math.BigDecimal;

public record ResultadoCheckout(
        String numeroPedido,
        BigDecimal total,
        boolean notificacaoEnviada) {
}
