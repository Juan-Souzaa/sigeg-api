package com.siseg.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Endereco> enderecos;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    
    /**
     * Retorna o endereço principal do cliente
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
