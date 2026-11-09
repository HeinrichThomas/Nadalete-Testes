package br.edu.checkout;

public class FalhaPagamentoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaPagamentoException(String mensagem) {
        super(mensagem);
    }
}
