package br.com.fiap.oficina.execution.funcionario.application.repository;

import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.shared.domain.Pagina;

import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository {
    Optional<Funcionario> buscarPorId(UUID id);

    Optional<Funcionario> buscarPorCpf(String cpf);

    Optional<Funcionario> buscarPorUsuarioId(UUID usuarioId);

    Funcionario salvar(Funcionario funcionario);

    Funcionario atualizar(Funcionario funcionario);

    void deletar(UUID id);

    Pagina<Funcionario> listarTodos(int pagina, int tamanhoLista, boolean incluirInativos);
}






