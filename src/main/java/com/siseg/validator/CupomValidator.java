package com.siseg.validator;

import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cupom;
import com.siseg.repository.CupomRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class CupomValidator {

    private final CupomRepository cupomRepository;

    public CupomValidator(CupomRepository cupomRepository) {
        this.cupomRepository = cupomRepository;
    }

    public void validateCodigoUnico(String codigo) {
        if (cupomRepository.findByCodigo(codigo).isPresent()) {
            throw new IllegalArgumentException("Já existe um cupom com este código");
        }
    }

    public void validateCupomValido(Cupom cupom) {
        if (cupom == null) {
            throw new ResourceNotFoundException("Cupom não encontrado");
        }
        if (!cupom.getAtivo()) {
            throw new IllegalArgumentException("Cupom está desativado");
        }
    }

    public void validateCupomAplicavel(Cupom cupom, BigDecimal valorMinimo) {
        validateCupomValido(cupom);
        validateCupomNaoExpirado(cupom);
        
        if (cupom.getUsosAtuais() >= cupom.getUsosMaximos()) {
            throw new IllegalArgumentException("Cupom atingiu o limite de usos");
        }
        
        if (valorMinimo.compareTo(cupom.getValorMinimo()) < 0) {
            throw new IllegalArgumentException("Valor mínimo do pedido não atende ao requisito do cupom");
        }
    }

    public void validateCupomNaoExpirado(Cupom cupom) {
        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(cupom.getDataInicio()) || hoje.isAfter(cupom.getDataFim())) {
            throw new IllegalArgumentException("Cupom fora do período de validade");
        }
    }
}
    

