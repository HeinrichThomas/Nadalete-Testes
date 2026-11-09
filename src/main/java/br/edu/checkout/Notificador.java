package br.edu.checkout;

import java.math.BigDecimal;

public interface Notificador {

    void enviarConfirmacao(String email, String numeroPedido, BigDecimal total);
}
