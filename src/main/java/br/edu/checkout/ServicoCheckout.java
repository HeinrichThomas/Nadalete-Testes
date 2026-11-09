package br.edu.checkout;

import java.math.BigDecimal;
import java.util.Objects;

public final class ServicoCheckout {

    private final EstoqueGateway estoqueGateway;
    private final PagamentoGateway pagamentoGateway;
    private final PedidoRepository pedidoRepository;
    private final Notificador notificador;

    public ServicoCheckout(
            EstoqueGateway estoqueGateway,
            PagamentoGateway pagamentoGateway,
            PedidoRepository pedidoRepository,
            Notificador notificador) {
        this.estoqueGateway = Objects.requireNonNull(estoqueGateway);
        this.pagamentoGateway = Objects.requireNonNull(pagamentoGateway);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.notificador = Objects.requireNonNull(notificador);
    }

    public ResultadoCheckout finalizar(SolicitacaoCheckout solicitacao) {
        if (solicitacao == null
                || solicitacao.clienteId() == null || solicitacao.clienteId().isBlank()
                || solicitacao.email() == null || solicitacao.email().isBlank()
                || solicitacao.sku() == null || solicitacao.sku().isBlank()
                || solicitacao.quantidade() <= 0
                || solicitacao.precoUnitario() == null
                || solicitacao.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }

        String clienteId = solicitacao.clienteId();
        String email = solicitacao.email();
        String sku = solicitacao.sku();
        int quantidade = solicitacao.quantidade();
        BigDecimal total = solicitacao.precoUnitario().multiply(BigDecimal.valueOf(quantidade));

        if (!estoqueGateway.reservar(sku, quantidade)) {
            throw new EstoqueInsuficienteException();
        }

        try {
            Pagamento pagamento = pagamentoGateway.cobrar(clienteId, total);
            if (pagamento == null || !pagamento.aprovado()) {
                estoqueGateway.liberar(sku, quantidade);
                throw new PagamentoRecusadoException();
            }

            PedidoConfirmado pedido = new PedidoConfirmado(
                    clienteId, email, sku, quantidade, total, pagamento.transacaoId());
            String numeroPedido = pedidoRepository.salvar(pedido);

            try {
                notificador.enviarConfirmacao(email, numeroPedido, total);
                return new ResultadoCheckout(numeroPedido, total, true);
            } catch (FalhaNotificacaoException e) {
                return new ResultadoCheckout(numeroPedido, total, false);
            }
        } catch (FalhaPagamentoException e) {
            estoqueGateway.liberar(sku, quantidade);
            throw e;
        }
    }
}
