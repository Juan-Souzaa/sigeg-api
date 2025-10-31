package com.siseg.dto.cardapio;

import lombok.Data;

import java.util.List;

@Data
public class CardapioResponseDTO {
    private Long restauranteId;
    private String restauranteNome;
    private List<CategoriaCardapioDTO> categorias;
}
