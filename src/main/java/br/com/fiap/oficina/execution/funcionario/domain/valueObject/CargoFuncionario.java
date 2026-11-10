package br.com.fiap.oficina.execution.funcionario.domain.valueObject;

public enum CargoFuncionario {
    MECANICO(1, "Mecânico"),
    ATENDENTE(2, "Atendente"),
    ADMINISTRADOR(3, "Administrador");

    private final int id;
    private final String descricao;

    CargoFuncionario(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public static CargoFuncionario fromId(int id) {
        for (CargoFuncionario c : values()) {
            if (c.id == id) {
                return c;
            }
        }
        throw new IllegalArgumentException("ID de cargo inválido: " + id);
    }
}






