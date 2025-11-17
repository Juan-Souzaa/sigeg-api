package com.siseg.model;

import com.siseg.model.enumerations.StatusRestaurante;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "restaurantes")
@Getter
@Setter
@NoArgsConstructor
public class Restaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @OneToMany(mappedBy = "restaurante", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Endereco> enderecos;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRestaurante status = StatusRestaurante.PENDING_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    
    /**
     * Retorna o endereço principal do restaurante
     */
    public Optional<Endereco> getEnderecoPrincipal() {
        if (enderecos == null) {
            return Optional.empty();
        }
        return enderecos.stream()
                .filter(e -> Boolean.TRUE.equals(e.getPrincipal()))
                .findFirst();
    }
}
