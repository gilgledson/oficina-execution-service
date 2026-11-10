package br.com.fiap.oficina.execution.funcionario.application.mapper;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;

public class FuncionarioOutputMapper {
    public static FuncionarioOutput toOutput(Funcionario funcionario) {
        return new FuncionarioOutput(
            funcionario.getId(),
            funcionario.getUsuarioId(),
            funcionario.getNome(),
            funcionario.getSobrenome(),
            funcionario.getCpf(),
            funcionario.getTelefone(),
            funcionario.getCargo(),
            funcionario.isAtivo(),
            funcionario.getDeletadoEm()
        );
    }
}






