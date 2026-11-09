package br.edu.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ServicoCheckoutTest {

    @Mock
    private EstoqueGateway estoqueGateway;

    @Mock
    private PagamentoGateway pagamentoGateway;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private Notificador notificador;

    private ServicoCheckout servico;

    @BeforeEach
    void criarServicoReal() {
        servico = new ServicoCheckout(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveRejeitarSolicitacaoNulaSemConsultarColaboradores() {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(null));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @ParameterizedTest
    @MethodSource("solicitacoesInvalidas")
    void deveRejeitarSolicitacoesInvalidas(
            String descricao, SolicitacaoCheckout solicitacao) {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(solicitacao));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveAceitarQuantidadeMinimaValida() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-1", 1,
                new BigDecimal("15.50"));
        when(estoqueGateway.reservar("SKU-1", 1)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("15.50")))
                .thenReturn(new Pagamento(true, "txn-001"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-1");

        ResultadoCheckout resultado = servico.finalizar(solicitacao);

        assertEquals("PED-1", resultado.numeroPedido());
        assertEquals(new BigDecimal("15.50"), resultado.total());
        assertEquals(true, resultado.notificacaoEnviada());
    }

    @Test
    void deveCalcularTotalPadrao() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-1", 3,
                new BigDecimal("19.90"));
        when(estoqueGateway.reservar("SKU-1", 3)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("59.70")))
                .thenReturn(new Pagamento(true, "txn-abc"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-100");

        servico.finalizar(solicitacao);

        verify(pagamentoGateway).cobrar("cliente-1", new BigDecimal("59.70"));
    }

    @Test
    void deveManterPrecisaoEmValoresDecimais() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-2", 3,
                new BigDecimal("0.005"));
        when(estoqueGateway.reservar("SKU-2", 3)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("0.015")))
                .thenReturn(new Pagamento(true, "txn-precise"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-PRECISO");

        servico.finalizar(solicitacao);

        verify(pagamentoGateway).cobrar("cliente-1", new BigDecimal("0.015"));
    }

    @Test
    void deveLancarExcecaoQuandoEstoqueForInsuficiente() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-9", 2,
                new BigDecimal("25.00"));
        when(estoqueGateway.reservar("SKU-9", 2)).thenReturn(false);

        assertThrows(EstoqueInsuficienteException.class, () -> servico.finalizar(solicitacao));

        verify(estoqueGateway).reservar("SKU-9", 2);
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
        verifyNoInteractions(pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveLiberarReservaQuandoPagamentoForRecusado() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-7", 2,
                new BigDecimal("40.00"));
        when(estoqueGateway.reservar("SKU-7", 2)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("80.00")))
                .thenReturn(new Pagamento(false, null));

        assertThrows(PagamentoRecusadoException.class, () -> servico.finalizar(solicitacao));

        InOrder ordem = inOrder(estoqueGateway, pagamentoGateway);
        ordem.verify(estoqueGateway).reservar("SKU-7", 2);
        ordem.verify(pagamentoGateway).cobrar("cliente-1", new BigDecimal("80.00"));
        ordem.verify(estoqueGateway).liberar("SKU-7", 2);
        verify(pedidoRepository, never()).salvar(any(PedidoConfirmado.class));
        verify(notificador, never()).enviarConfirmacao(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void devePropagarFalhaTecnicaDoPagamentoELiberarReserva() {
        FalhaPagamentoException falha = new FalhaPagamentoException("gateway caiu");
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-6", 2,
                new BigDecimal("10.00"));
        when(estoqueGateway.reservar("SKU-6", 2)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("20.00"))).thenThrow(falha);

        FalhaPagamentoException capturada = assertThrows(FalhaPagamentoException.class,
                () -> servico.finalizar(solicitacao));

        assertSame(falha, capturada);
        verify(estoqueGateway).liberar("SKU-6", 2);
        verify(pedidoRepository, never()).salvar(any(PedidoConfirmado.class));
        verify(notificador, never()).enviarConfirmacao(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void deveSalvarPedidoCorretamente() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-404", 3,
                new BigDecimal("12.50"));
        when(estoqueGateway.reservar("SKU-404", 3)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("37.50")))
                .thenReturn(new Pagamento(true, "txn-123"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-42");

        servico.finalizar(solicitacao);

        ArgumentCaptor<PedidoConfirmado> captor = ArgumentCaptor.forClass(PedidoConfirmado.class);
        verify(pedidoRepository).salvar(captor.capture());

        PedidoConfirmado pedido = captor.getValue();
        assertNotNull(pedido);
        assertEquals("cliente-1", pedido.clienteId());
        assertEquals("cliente@email.com", pedido.email());
        assertEquals("SKU-404", pedido.sku());
        assertEquals(3, pedido.quantidade());
        assertEquals(new BigDecimal("37.50"), pedido.total());
        assertEquals("txn-123", pedido.transacaoId());
    }

    @Test
    void deveRegistrarPedidoEEnviarConfirmacaoEmOrdemCorreta() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-8", 2,
                new BigDecimal("13.00"));
        when(estoqueGateway.reservar("SKU-8", 2)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("26.00")))
                .thenReturn(new Pagamento(true, "txn-456"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-77");

        ResultadoCheckout resultado = servico.finalizar(solicitacao);

        assertEquals("PED-77", resultado.numeroPedido());
        assertEquals(new BigDecimal("26.00"), resultado.total());
        assertEquals(true, resultado.notificacaoEnviada());
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
        verify(notificador).enviarConfirmacao("cliente@email.com", "PED-77", new BigDecimal("26.00"));

        InOrder ordem = inOrder(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
        ordem.verify(estoqueGateway).reservar("SKU-8", 2);
        ordem.verify(pagamentoGateway).cobrar("cliente-1", new BigDecimal("26.00"));
        ordem.verify(pedidoRepository).salvar(any(PedidoConfirmado.class));
        ordem.verify(notificador).enviarConfirmacao("cliente@email.com", "PED-77", new BigDecimal("26.00"));
    }

    @Test
    void deveRetornarResultadoMesmoQuandoNotificacaoFalhar() {
        SolicitacaoCheckout solicitacao = solicitacaoValida("cliente-1", "cliente@email.com", "SKU-3", 2,
                new BigDecimal("10.00"));
        when(estoqueGateway.reservar("SKU-3", 2)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente-1", new BigDecimal("20.00")))
                .thenReturn(new Pagamento(true, "txn-999"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("PED-88");
        doThrow(new FalhaNotificacaoException("falha de envio"))
                .when(notificador).enviarConfirmacao("cliente@email.com", "PED-88", new BigDecimal("20.00"));

        ResultadoCheckout resultado = servico.finalizar(solicitacao);

        assertEquals("PED-88", resultado.numeroPedido());
        assertEquals(new BigDecimal("20.00"), resultado.total());
        assertEquals(false, resultado.notificacaoEnviada());
        verify(pagamentoGateway, times(1)).cobrar("cliente-1", new BigDecimal("20.00"));
        verify(pedidoRepository, times(1)).salvar(any(PedidoConfirmado.class));
        verify(notificador, times(1)).enviarConfirmacao("cliente@email.com", "PED-88", new BigDecimal("20.00"));
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
    }

    private static Stream<Arguments> solicitacoesInvalidas() {
        return Stream.of(
                Arguments.of("clienteId nulo", nullSolicitacao(null, "a@a.com", "SKU", 1, new BigDecimal("10"))),
                Arguments.of("clienteId vazio", nullSolicitacao("", "a@a.com", "SKU", 1, new BigDecimal("10"))),
                Arguments.of("clienteId em branco", nullSolicitacao("   ", "a@a.com", "SKU", 1, new BigDecimal("10"))),
                Arguments.of("email nulo", nullSolicitacao("c1", null, "SKU", 1, new BigDecimal("10"))),
                Arguments.of("email vazio", nullSolicitacao("c1", "", "SKU", 1, new BigDecimal("10"))),
                Arguments.of("email em branco", nullSolicitacao("c1", "   ", "SKU", 1, new BigDecimal("10"))),
                Arguments.of("sku nulo", nullSolicitacao("c1", "a@a.com", null, 1, new BigDecimal("10"))),
                Arguments.of("sku vazio", nullSolicitacao("c1", "a@a.com", "", 1, new BigDecimal("10"))),
                Arguments.of("sku em branco", nullSolicitacao("c1", "a@a.com", "   ", 1, new BigDecimal("10"))),
                Arguments.of("quantidade zero", nullSolicitacao("c1", "a@a.com", "SKU", 0, new BigDecimal("10"))),
                Arguments.of("quantidade negativa", nullSolicitacao("c1", "a@a.com", "SKU", -1, new BigDecimal("10"))),
                Arguments.of("preco nulo", nullSolicitacao("c1", "a@a.com", "SKU", 1, null)),
                Arguments.of("preco zero", nullSolicitacao("c1", "a@a.com", "SKU", 1, BigDecimal.ZERO)),
                Arguments.of("preco negativo", nullSolicitacao("c1", "a@a.com", "SKU", 1, new BigDecimal("-1.00"))),
                Arguments.of("solicitacao nula", null, null));
    }

    private static SolicitacaoCheckout nullSolicitacao(
            String clienteId,
            String email,
            String sku,
            int quantidade,
            BigDecimal precoUnitario) {
        return new SolicitacaoCheckout(clienteId, email, sku, quantidade, precoUnitario);
    }

    private static SolicitacaoCheckout solicitacaoValida(
            String clienteId,
            String email,
            String sku,
            int quantidade,
            BigDecimal precoUnitario) {
        return new SolicitacaoCheckout(clienteId, email, sku, quantidade, precoUnitario);
    }
}
