package com.siseg.dto.prato;

import com.siseg.model.enumerations.CategoriaMenu;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class PratoRequestDTO {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    private String descricao;
    
    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    private BigDecimal preco;
    
    @NotNull(message = "Categoria é obrigatória")
    private CategoriaMenu categoria;
    
    private Boolean disponivel = true;
    
    private MultipartFile foto;
}
