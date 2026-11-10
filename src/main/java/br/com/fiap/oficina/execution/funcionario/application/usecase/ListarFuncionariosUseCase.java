package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.shared.domain.Pagina;

public interface ListarFuncionariosUseCase {
    Pagina<FuncionarioOutput> executar(int pagina, int tamanhoLista, boolean incluirInativos);
}






