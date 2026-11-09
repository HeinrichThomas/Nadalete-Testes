package br.edu.checkout;

public class FalhaNotificacaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FalhaNotificacaoException(String mensagem) {
        super(mensagem);
    }
}
