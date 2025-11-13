package com.siseg.model;

import com.siseg.model.enumerations.TipoEndereco;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "enderecos")
@Getter
@Setter
@NoArgsConstructor
public class Endereco {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id")
    private Restaurante restaurante;
    
    @Column(nullable = false, length = 200)
    private String logradouro;
    
    @Column(nullable = false, length = 10)
    private String numero;
    
    @Column(length = 50)
    private String complemento;
    
    @Column(nullable = false, length = 100)
    private String bairro;
    
    @Column(nullable = false, length = 100)
    private String cidade;
    
    @Column(nullable = false, length = 2)
    private String estado;
    
    @Column(nullable = false, length = 8)
    private String cep;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEndereco tipo = TipoEndereco.OUTRO;
    
    @Column(nullable = false)
    private Boolean principal = false;
    
    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    
    /**
     * Formata endereço completo para geocoding
     * Exemplo: "Rua das Flores, 123, Centro, São Paulo, SP, 01310-100, Brasil"
     */
    public String toGeocodingString() {
        StringBuilder sb = new StringBuilder();
        sb.append(logradouro).append(", ");
        sb.append(numero);
        
        if (complemento != null && !complemento.trim().isEmpty()) {
            sb.append(", ").append(complemento);
        }
        
        sb.append(", ").append(bairro);
        sb.append(", ").append(cidade);
        sb.append(", ").append(estado);
        sb.append(", ").append(formatarCep());
        sb.append(", Brasil");
        
        return sb.toString();
    }
    
    /**
     * Formata CEP com hífen: 12345-678
     */
    public String formatarCep() {
        if (cep == null || cep.length() != 8) {
            return cep;
        }
        return cep.substring(0, 5) + "-" + cep.substring(5);
    }
    
    /**
     * Valida se o endereço está completo
     */
    public boolean isCompleto() {
        return logradouro != null && !logradouro.trim().isEmpty() &&
               numero != null && !numero.trim().isEmpty() &&
               bairro != null && !bairro.trim().isEmpty() &&
               cidade != null && !cidade.trim().isEmpty() &&
               estado != null && estado.length() == 2 &&
               cep != null && cep.matches("\\d{8}");
    }
}

