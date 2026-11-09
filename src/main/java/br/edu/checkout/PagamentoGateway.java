package br.edu.checkout;

import java.math.BigDecimal;

public interface PagamentoGateway {

    Pagamento cobrar(String clienteId, BigDecimal total);
}
