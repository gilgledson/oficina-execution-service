package br.com.fiap.oficina.execution.funcionario.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Funcionario {
    private UUID id;
    private UUID usuarioId;
    private String nome;
    private String sobrenome;
    private String cpf;
    private String telefone;
    private CargoFuncionario cargo;
    private boolean ativo;
    private LocalDateTime deletadoEm;

    public Funcionario(UUID usuarioId, String nome, String sobrenome, String cpf, String telefone,
            CargoFuncionario cargo) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.cargo = cargo;
        this.ativo = true;
        this.deletadoEm = null;
        validar();
    }

    public void atualizar(Funcionario funcionario) {
        if (!this.ativo) {
            throw new IllegalArgumentException("Não é possível atualizar um funcionário inativo");
        }
        if (funcionario.getNome() != null) {
            this.nome = funcionario.getNome();
        }
        if (funcionario.getSobrenome() != null) {
            this.sobrenome = funcionario.getSobrenome();
        }
        if (funcionario.getCpf() != null) {
            this.cpf = funcionario.getCpf();
        }
        if (funcionario.getTelefone() != null) {
            this.telefone = funcionario.getTelefone();
        }
        if (funcionario.getCargo() != null) {
            this.cargo = funcionario.getCargo();
        }

        validar();
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void ativar() {
        if (this.ativo) {
            throw new IllegalArgumentException("Funcionario ja esta ativo");
        }
        this.ativo = true;
        this.deletadoEm = null;
    }

    public void desativar() {
        if (!this.ativo) {
            throw new IllegalArgumentException("Funcionario ja esta inativo");
        }
        this.ativo = false;
        this.deletadoEm = LocalDateTime.now();
    }

    public boolean isDeletado() {
        return deletadoEm != null;
    }

    public void validar() {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (sobrenome == null || sobrenome.trim().isEmpty()) {
            throw new IllegalArgumentException("Sobrenome é obrigatório");
        }
        if (cpf == null || cpf.trim().isEmpty() || !cpf.matches("\\d{3}\\.\\d{3}\\.\\d{3}\\-\\d{2}")) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (telefone == null || telefone.trim().isEmpty() || !telefone.matches("\\(\\d{2}\\) \\d{4,5}-\\d{4}")) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        if (cargo == null) {
            throw new IllegalArgumentException("Cargo é obrigatório");
        }
    }
}






