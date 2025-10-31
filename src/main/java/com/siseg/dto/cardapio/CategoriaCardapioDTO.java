package com.siseg.dto.cardapio;

import com.siseg.model.enumerations.CategoriaMenu;
import lombok.Data;

import java.util.List;

@Data
public class CategoriaCardapioDTO {
    private CategoriaMenu categoria;
    private List<PratoCardapioDTO> pratos;
}
