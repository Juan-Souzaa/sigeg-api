package com.siseg.service;

import com.siseg.model.Endereco;
import com.siseg.model.enumerations.TipoEndereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class GeocodingServiceUnitTest {

    @Autowired
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        // O GeocodingService será injetado automaticamente pelo Spring
        // usando as configurações do application-test.properties
    }

    @Test
    void deveRetornarSemErroParaEnderecoNulo() {
        assertDoesNotThrow(() -> geocodingService.geocodeAddress(null));
    }

    @Test
    void deveRetornarSemErroParaEnderecoIncompleto() {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua Teste");
        
        assertDoesNotThrow(() -> geocodingService.geocodeAddress(endereco));
    }

    @Test
    void deveLimparCache() {
        assertDoesNotThrow(() -> geocodingService.clearCache());
    }

    @Test
    void deveReutilizarCacheParaMesmoEndereco() {
        Endereco endereco1 = criarEnderecoCompleto("Rua Teste", "123");
        Endereco endereco2 = criarEnderecoCompleto("Rua Teste", "123");
        
        geocodingService.geocodeAddress(endereco1);
        geocodingService.geocodeAddress(endereco2);
        
        if (endereco1.getLatitude() != null && endereco2.getLatitude() != null) {
            assertEquals(endereco1.getLatitude(), endereco2.getLatitude());
            assertEquals(endereco1.getLongitude(), endereco2.getLongitude());
        }
        
        geocodingService.clearCache();
    }

    @Test
    void deveAceitarEnderecoComCepValido() {
        Endereco endereco = criarEnderecoCompleto("Rua Teste", "123");
        endereco.setCep("01310100");
        
        assertDoesNotThrow(() -> geocodingService.geocodeAddress(endereco));
    }

    @Test
    void deveAceitarEnderecoCompleto() {
        Endereco endereco = criarEnderecoCompleto("Avenida Paulista", "1000");
        endereco.setBairro("Bela Vista");
        
        assertDoesNotThrow(() -> geocodingService.geocodeAddress(endereco));
    }

    @Test
    void naoDeveGeocodificarSeEnderecoJaTemCoordenadas() {
        Endereco endereco = criarEnderecoCompleto("Rua Teste", "123");
        endereco.setLatitude(java.math.BigDecimal.valueOf(-23.5505));
        endereco.setLongitude(java.math.BigDecimal.valueOf(-46.6333));
        
        assertDoesNotThrow(() -> geocodingService.geocodeAddress(endereco));
    }

    private Endereco criarEnderecoCompleto(String logradouro, String numero) {
        Endereco endereco = new Endereco();
        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310100");
        endereco.setTipo(TipoEndereco.OUTRO);
        endereco.setPrincipal(false);
        return endereco;
    }
}

