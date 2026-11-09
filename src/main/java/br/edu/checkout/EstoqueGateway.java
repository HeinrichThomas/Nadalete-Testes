package br.edu.checkout;

public interface EstoqueGateway {

    boolean reservar(String sku, int quantidade);

    void liberar(String sku, int quantidade);
}
