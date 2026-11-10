package br.com.fiap.oficina.execution.funcionario.domain.valueObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CargoFuncionarioTest {

    @Test
    @DisplayName("fromId deve retornar cargo correto para cada id")
    void deveRetornarCargoPorId() {
        assertEquals(CargoFuncionario.MECANICO, CargoFuncionario.fromId(1));
        assertEquals(CargoFuncionario.ATENDENTE, CargoFuncionario.fromId(2));
        assertEquals(CargoFuncionario.ADMINISTRADOR, CargoFuncionario.fromId(3));
    }

    @Test
    @DisplayName("fromId deve lançar exceção para id inválido")
    void deveLancarExcecaoParaIdInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CargoFuncionario.fromId(99));
    }

    @Test
    @DisplayName("Deve retornar id e descrição corretos para cada cargo")
    void deveRetornarIdEDescricao() {
        assertEquals(1, CargoFuncionario.MECANICO.getId());
        assertEquals(2, CargoFuncionario.ATENDENTE.getId());
        assertEquals(3, CargoFuncionario.ADMINISTRADOR.getId());

        assertEquals("Mecânico", CargoFuncionario.MECANICO.getDescricao());
        assertEquals("Atendente", CargoFuncionario.ATENDENTE.getDescricao());
        assertEquals("Administrador", CargoFuncionario.ADMINISTRADOR.getDescricao());
    }
}









