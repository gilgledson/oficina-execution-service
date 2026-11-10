package br.com.fiap.oficina.execution.funcionario.domain.entity;

import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioTest {

    @Test
    @DisplayName("Deve criar funcionário com dados válidos")
    void deveCriarFuncionarioValido() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        assertNotNull(funcionario.getId());
        assertTrue(funcionario.isAtivo());
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF inválido")
    void deveValidarCpf() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "12345678900", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para Telefone inválido")
    void deveValidarTelefone() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "11999999999", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para Nome vazio ou apenas espaços")
    void deveValidarNome() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "   ", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para Sobrenome vazio ou apenas espaços")
    void deveValidarSobrenome() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "   ", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF nulo ou vazio")
    void deveValidarCpfNullEmpty() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", null, "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "", "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para Telefone nulo ou vazio")
    void deveValidarTelefoneNullEmpty() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", null, CargoFuncionario.MECANICO)
        );
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "", CargoFuncionario.MECANICO)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção para Cargo nulo")
    void deveValidarCargoNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", null)
        );
    }

    @Test
    @DisplayName("Deve atualizar dados do funcionário")
    void deveAtualizarFuncionario() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        
        Funcionario novosDados = Funcionario.builder()
                .nome("José")
                .sobrenome("Santos")
                .cpf("987.654.321-11")
                .telefone("(11) 88888-8888")
                .cargo(CargoFuncionario.ADMINISTRADOR)
                .build();
        
        funcionario.atualizar(novosDados);
        
        assertEquals("José", funcionario.getNome());
        assertEquals("Santos", funcionario.getSobrenome());
        assertEquals("987.654.321-11", funcionario.getCpf());
        assertEquals("(11) 88888-8888", funcionario.getTelefone());
        assertEquals(CargoFuncionario.ADMINISTRADOR, funcionario.getCargo());
    }

    @Test
    @DisplayName("Deve atualizar dados do funcionário com campos nulos")
    void deveAtualizarFuncionarioComCamposNulos() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        
        Funcionario novosDados = Funcionario.builder().build();
        
        funcionario.atualizar(novosDados);
        
        assertEquals("João", funcionario.getNome());
        assertEquals("Silva", funcionario.getSobrenome());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar funcionário inativo")
    void deveLancarExcecaoAoAtualizarInativo() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        funcionario.desativar();
        
        assertThrows(IllegalArgumentException.class, () -> funcionario.atualizar(funcionario));
    }

    @Test
    @DisplayName("Deve ativar e desativar funcionário")
    void deveAtivarEDesativar() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        
        funcionario.desativar();
        assertFalse(funcionario.isAtivo());
        assertTrue(funcionario.isDeletado());
        
        funcionario.ativar();
        assertTrue(funcionario.isAtivo());
        assertFalse(funcionario.isDeletado());
    }

    @Test
    @DisplayName("Deve lançar exceção ao ativar já ativo")
    void deveLancarExcecaoAoAtivarJaAtivo() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        assertThrows(IllegalArgumentException.class, funcionario::ativar);
    }

    @Test
    @DisplayName("Deve lançar exceção ao desativar já inativo")
    void deveLancarExcecaoAoDesativarJaInativo() {
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00", "(11) 99999-9999", CargoFuncionario.MECANICO);
        funcionario.desativar();
        assertThrows(IllegalArgumentException.class, funcionario::desativar);
    }
}
