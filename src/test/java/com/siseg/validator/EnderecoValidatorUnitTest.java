package com.siseg.validator;

import com.siseg.model.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EnderecoValidatorUnitTest {

    private EnderecoValidator enderecoValidator;
    private Endereco endereco;

    @BeforeEach
    void setUp() {
        enderecoValidator = new EnderecoValidator();
        
        endereco = new Endereco();
        endereco.setLogradouro("Rua Teste");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310100");
    }

    @Test
    void deveValidarEnderecoCompleto() {
        assertDoesNotThrow(() -> enderecoValidator.validate(endereco));
    }

    @Test
    void deveLancarExcecaoQuandoEnderecoNulo() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(null));
        assertEquals("Endereço não pode ser nulo", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoLogradouroVazio() {
        endereco.setLogradouro(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoNumeroVazio() {
        endereco.setNumero(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoBairroVazio() {
        endereco.setBairro(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCidadeVazia() {
        endereco.setCidade(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEstadoInvalido() {
        endereco.setEstado("SPA"); // Mais de 2 caracteres
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Estado inválido. Deve ser sigla de 2 letras maiúsculas (ex: SP).", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEstadoMinusculo() {
        endereco.setEstado("sp"); // Minúsculas
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("Estado inválido. Deve ser sigla de 2 letras maiúsculas (ex: SP).", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCepInvalido() {
        endereco.setCep("12345"); // Menos de 8 dígitos
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("CEP inválido. Deve conter 8 dígitos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCepComLetras() {
        endereco.setCep("12345-67a"); // Contém letra
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("CEP inválido. Deve conter 8 dígitos.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoCepNulo() {
        endereco.setCep(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> enderecoValidator.validate(endereco));
        assertEquals("CEP inválido. Deve conter 8 dígitos.", exception.getMessage());
    }
}

