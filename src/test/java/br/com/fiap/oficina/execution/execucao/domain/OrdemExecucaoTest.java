package br.com.fiap.oficina.execution.execucao.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrdemExecucaoTest {

    private static final Instant T0 = Instant.parse("2026-01-10T12:00:00Z");
    private final UUID osId = UUID.randomUUID();
    private final UUID mecanico = UUID.randomUUID();
    private final UUID servicoA = UUID.randomUUID();
    private final UUID servicoB = UUID.randomUUID();

    private List<ServicoExecucao> doisServicos() {
        return List.of(new ServicoExecucao(servicoA, UUID.randomUUID(), "Troca de óleo"),
                new ServicoExecucao(servicoB, UUID.randomUUID(), "Alinhamento"));
    }

    private OrdemExecucao aguardandoAprovacao() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, T0);
        execucao.iniciarDiagnostico(mecanico, T0.plusSeconds(60));
        execucao.concluirDiagnostico(mecanico, T0.plusSeconds(120));
        return execucao;
    }

    private OrdemExecucao emExecucao() {
        OrdemExecucao execucao = aguardandoAprovacao();
        execucao.enfileirarParaExecucao(doisServicos(), T0.plusSeconds(180));
        execucao.iniciarExecucao(mecanico, T0.plusSeconds(240));
        return execucao;
    }

    private static List<String> eventos(OrdemExecucao execucao) {
        return execucao.getEventosPendentes().stream().map(EventoPendente::routingKey).toList();
    }

    @Test
    @DisplayName("OS aberta entra na fila de diagnóstico, sem eventos")
    void deveEntrarNaFilaDeDiagnostico() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, T0);

        assertEquals(FaseExecucao.FILA_DIAGNOSTICO, execucao.getFase());
        assertEquals(T0, execucao.getEntradaNaFila());
        assertEquals(0, execucao.getVersao());
        assertEquals(List.of(new RegistroFase(FaseExecucao.FILA_DIAGNOSTICO, T0, null)), execucao.getHistorico());
        assertTrue(execucao.getEventosPendentes().isEmpty());
    }

    @Test
    @DisplayName("Não deve criar execução sem OS")
    void deveExigirOrdemDeServico() {
        assertThrows(IllegalArgumentException.class, () -> OrdemExecucao.novaNaFila(null, T0));
    }

    @Test
    @DisplayName("Diagnóstico: inicia, conclui e gera os dois eventos")
    void deveFazerDiagnostico() {
        OrdemExecucao execucao = aguardandoAprovacao();

        assertEquals(FaseExecucao.AGUARDANDO_APROVACAO, execucao.getFase());
        assertEquals(T0.plusSeconds(60), execucao.getInicioDiagnostico());
        assertEquals(T0.plusSeconds(120), execucao.getFimDiagnostico());
        assertEquals(List.of(EventoPendente.DIAGNOSTICO_INICIADO, EventoPendente.DIAGNOSTICO_CONCLUIDO),
                eventos(execucao));
        EventoPendente evento = execucao.getEventosPendentes().get(0);
        assertEquals(osId.toString(), evento.dados().get("ordemServicoId"));
        assertNull(evento.correlationId());
        assertEquals(mecanico, execucao.getHistorico().get(1).usuarioId());
    }

    @Test
    @DisplayName("Não deve iniciar diagnóstico fora da fila de diagnóstico")
    void naoDeveIniciarDiagnosticoForaDaFila() {
        OrdemExecucao execucao = aguardandoAprovacao();

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> execucao.iniciarDiagnostico(mecanico, T0));
        assertTrue(erro.getMessage().contains("AGUARDANDO_APROVACAO"));
    }

    @Test
    @DisplayName("Não deve concluir diagnóstico que não foi iniciado")
    void naoDeveConcluirDiagnosticoNaoIniciado() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, T0);

        assertThrows(IllegalStateException.class, () -> execucao.concluirDiagnostico(mecanico, T0));
    }

    @Test
    @DisplayName("Aprovação coloca a OS na fila de execução com os serviços aceitos")
    void deveEntrarNaFilaDeExecucao() {
        OrdemExecucao execucao = aguardandoAprovacao();

        boolean enfileirou = execucao.enfileirarParaExecucao(doisServicos(), T0.plusSeconds(180));

        assertTrue(enfileirou);
        assertEquals(FaseExecucao.FILA_EXECUCAO, execucao.getFase());
        assertEquals(T0.plusSeconds(180), execucao.getEntradaNaFila());
        assertEquals(2, execucao.getServicos().size());
        assertEquals(StatusServico.PENDENTE, execucao.getServicos().get(0).getStatus());
    }

    @Test
    @DisplayName("Aprovação repetida é ignorada e não reinicia os serviços")
    void deveIgnorarAprovacaoRepetida() {
        OrdemExecucao execucao = emExecucao();
        execucao.iniciarServico(servicoA, mecanico, T0.plusSeconds(300));

        boolean enfileirou = execucao.enfileirarParaExecucao(doisServicos(), T0.plusSeconds(400));

        assertFalse(enfileirou);
        assertEquals(FaseExecucao.EM_EXECUCAO, execucao.getFase());
        assertEquals(StatusServico.EM_EXECUCAO, execucao.getServicos().get(0).getStatus());
    }

    @Test
    @DisplayName("Não deve aprovar sem serviços nem antes do diagnóstico")
    void deveValidarAprovacao() {
        OrdemExecucao aguardando = aguardandoAprovacao();
        OrdemExecucao naFila = OrdemExecucao.novaNaFila(osId, T0);

        assertThrows(IllegalArgumentException.class, () -> aguardando.enfileirarParaExecucao(List.of(), T0));
        assertThrows(IllegalArgumentException.class, () -> aguardando.enfileirarParaExecucao(null, T0));
        assertThrows(IllegalStateException.class, () -> naFila.enfileirarParaExecucao(doisServicos(), T0));
    }

    @Test
    @DisplayName("Execução completa: serviços iniciados e finalizados, OS concluída")
    void deveExecutarEConcluir() {
        OrdemExecucao execucao = emExecucao();

        execucao.iniciarServico(servicoA, mecanico, T0.plusSeconds(300));
        execucao.finalizarServico(servicoA, T0.plusSeconds(300 + 1800));
        execucao.iniciarServico(servicoB, mecanico, T0.plusSeconds(2200));
        execucao.finalizarServico(servicoB, T0.plusSeconds(2200 + 600));
        execucao.concluirExecucao(mecanico, T0.plusSeconds(3000));

        assertEquals(FaseExecucao.CONCLUIDA, execucao.getFase());
        assertTrue(execucao.getFase().isEncerrada());
        assertEquals(T0.plusSeconds(240), execucao.getInicioExecucao());
        assertEquals(T0.plusSeconds(3000), execucao.getFimExecucao());
        ServicoExecucao primeiro = execucao.getServicos().get(0);
        assertEquals(mecanico, primeiro.getExecutorId());
        assertEquals(Duration.ofMinutes(30), primeiro.duracao());
        assertEquals(List.of(EventoPendente.DIAGNOSTICO_INICIADO, EventoPendente.DIAGNOSTICO_CONCLUIDO,
                EventoPendente.EXECUCAO_INICIADA, EventoPendente.SERVICO_INICIADO, EventoPendente.SERVICO_FINALIZADO,
                EventoPendente.SERVICO_INICIADO, EventoPendente.SERVICO_FINALIZADO,
                EventoPendente.EXECUCAO_CONCLUIDA), eventos(execucao));
        EventoPendente servicoIniciado = execucao.getEventosPendentes().get(3);
        assertEquals(servicoA.toString(), servicoIniciado.dados().get("servicoOsId"));
        assertEquals(mecanico.toString(), servicoIniciado.dados().get("usuarioExecutorId"));
    }

    @Test
    @DisplayName("Não deve concluir a execução com serviço pendente")
    void naoDeveConcluirComServicoPendente() {
        OrdemExecucao execucao = emExecucao();
        execucao.iniciarServico(servicoA, mecanico, T0.plusSeconds(300));
        execucao.finalizarServico(servicoA, T0.plusSeconds(600));

        assertThrows(IllegalStateException.class, () -> execucao.concluirExecucao(mecanico, T0.plusSeconds(700)));
        assertEquals(FaseExecucao.EM_EXECUCAO, execucao.getFase());
    }

    @Test
    @DisplayName("Regras dos serviços: fase, existência e ordem iniciar/finalizar")
    void deveValidarServicos() {
        OrdemExecucao naFila = aguardandoAprovacao();
        naFila.enfileirarParaExecucao(doisServicos(), T0);
        OrdemExecucao execucao = emExecucao();

        assertThrows(IllegalStateException.class, () -> naFila.iniciarServico(servicoA, mecanico, T0));
        assertThrows(IllegalStateException.class, () -> naFila.finalizarServico(servicoA, T0));
        assertThrows(IllegalArgumentException.class, () -> execucao.iniciarServico(UUID.randomUUID(), mecanico, T0));
        assertThrows(IllegalStateException.class, () -> execucao.finalizarServico(servicoA, T0));
        execucao.iniciarServico(servicoA, null, T0);
        assertThrows(IllegalStateException.class, () -> execucao.iniciarServico(servicoA, mecanico, T0));
        assertNull(execucao.getEventosPendentes().get(3).dados().get("usuarioExecutorId"));
    }

    @Test
    @DisplayName("Não deve iniciar execução fora da fila de execução")
    void naoDeveIniciarExecucaoForaDaFila() {
        OrdemExecucao execucao = aguardandoAprovacao();

        assertThrows(IllegalStateException.class, () -> execucao.iniciarExecucao(mecanico, T0));
    }

    @Test
    @DisplayName("Falha reportada encerra a OS e gera o evento de compensação")
    void deveReportarFalha() {
        OrdemExecucao execucao = emExecucao();

        execucao.reportarFalha("Peça indisponível", mecanico, T0.plusSeconds(500));

        assertEquals(FaseExecucao.FALHOU, execucao.getFase());
        assertEquals("Peça indisponível", execucao.getMotivoEncerramento());
        EventoPendente evento = execucao.getEventosPendentes().get(execucao.getEventosPendentes().size() - 1);
        assertEquals(EventoPendente.EXECUCAO_FALHOU, evento.routingKey());
        assertEquals("Peça indisponível", evento.dados().get("motivo"));
    }

    @Test
    @DisplayName("Falha exige motivo e ordem ainda ativa")
    void deveValidarFalha() {
        OrdemExecucao execucao = emExecucao();

        assertThrows(IllegalArgumentException.class, () -> execucao.reportarFalha(" ", mecanico, T0));
        assertThrows(IllegalArgumentException.class, () -> execucao.reportarFalha(null, mecanico, T0));
        execucao.reportarFalha("Motivo", mecanico, T0);
        assertThrows(IllegalStateException.class, () -> execucao.reportarFalha("De novo", mecanico, T0));
    }

    @Test
    @DisplayName("Cancelamento (compensação) tira a OS da fila sem gerar evento e é idempotente")
    void deveCancelar() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, T0);

        assertTrue(execucao.cancelar("OS cancelada", T0.plusSeconds(10)));
        assertFalse(execucao.cancelar("OS cancelada", T0.plusSeconds(20)));

        assertEquals(FaseExecucao.CANCELADA, execucao.getFase());
        assertEquals("OS cancelada", execucao.getMotivoEncerramento());
        assertTrue(execucao.getEventosPendentes().isEmpty());
        assertEquals(2, execucao.getHistorico().size());
    }

    @Test
    @DisplayName("CorrelationId é atribuído só aos eventos que ainda não têm um")
    void deveAtribuirCorrelationId() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, T0);
        execucao.iniciarDiagnostico(mecanico, T0);
        execucao.atribuirCorrelationId("corr-1");
        execucao.concluirDiagnostico(mecanico, T0);
        execucao.atribuirCorrelationId(null);
        execucao.atribuirCorrelationId("corr-2");

        assertEquals("corr-1", execucao.getEventosPendentes().get(0).correlationId());
        assertEquals("corr-2", execucao.getEventosPendentes().get(1).correlationId());
    }

    @Test
    @DisplayName("Serviço não finalizado tem duração zero")
    void duracaoDeServicoNaoFinalizado() {
        ServicoExecucao servico = new ServicoExecucao(servicoA, UUID.randomUUID(), "Troca de óleo");

        assertEquals(Duration.ZERO, servico.duracao());
        assertFalse(servico.isFinalizado());
    }

    @Test
    @DisplayName("Reconstrução a partir do banco preserva o estado e aceita listas nulas")
    void deveReconstruir() {
        OrdemExecucao execucao = OrdemExecucao.builder()
                .ordemServicoId(osId)
                .fase(FaseExecucao.EM_DIAGNOSTICO)
                .versao(3)
                .build();

        assertEquals(3, execucao.getVersao());
        assertTrue(execucao.getServicos().isEmpty());
        assertTrue(execucao.getHistorico().isEmpty());
        execucao.concluirDiagnostico(mecanico, T0);
        assertEquals(FaseExecucao.AGUARDANDO_APROVACAO, execucao.getFase());
    }
}
