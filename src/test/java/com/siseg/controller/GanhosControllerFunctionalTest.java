package com.siseg.controller;

import com.siseg.dto.ganhos.GanhosEntregadorDTO;
import com.siseg.dto.ganhos.GanhosPorEntregaDTO;
import com.siseg.dto.ganhos.GanhosRestauranteDTO;
import com.siseg.model.Entregador;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.Periodo;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.GanhosService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GanhosControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @MockBean
    private GanhosService ganhosService;

    @MockBean
    private RestauranteRepository restauranteRepository;

    @MockBean
    private EntregadorRepository entregadorRepository;

    private String restauranteToken;
    private String entregadorToken;
    private Restaurante restaurante;
    private Entregador entregador;

    @BeforeEach
    void setUp() throws Exception {
        User restauranteUser = testJwtUtil.getOrCreateUser("restaurante", ERole.ROLE_RESTAURANTE);
        restauranteToken = testJwtUtil.generateTokenForUser("restaurante", ERole.ROLE_RESTAURANTE);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setUser(restauranteUser);

        User entregadorUser = testJwtUtil.getOrCreateUser("entregador", ERole.ROLE_ENTREGADOR);
        entregadorToken = testJwtUtil.generateTokenForUser("entregador", ERole.ROLE_ENTREGADOR);

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador Teste");
        entregador.setUser(entregadorUser);

        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(entregadorRepository.findById(1L)).thenReturn(Optional.of(entregador));
    }

    @Test
    void deveObterGanhosRestauranteComoOwner() throws Exception {
        GanhosRestauranteDTO ganhosDTO = new GanhosRestauranteDTO();
        ganhosDTO.setValorBruto(new BigDecimal("1000.00"));
        ganhosDTO.setValorLiquido(new BigDecimal("950.00"));
        ganhosDTO.setTotalPedidos(10L);
        ganhosDTO.setPeriodo("MES");

        when(ganhosService.calcularGanhosRestaurante(eq(1L), any(Periodo.class))).thenReturn(ganhosDTO);

        mockMvc.perform(get("/api/restaurantes/1/ganhos")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .param("periodo", "MES")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorBruto").exists());
    }

    @Test
    void deveNegarAcessoAoObterGanhosRestauranteDeOutro() throws Exception {
        User outroUser = new User();
        outroUser.setId(999L);
        
        Restaurante outroRestaurante = new Restaurante();
        outroRestaurante.setId(2L);
        outroRestaurante.setUser(outroUser);

        when(restauranteRepository.findById(2L)).thenReturn(Optional.of(outroRestaurante));

        mockMvc.perform(get("/api/restaurantes/2/ganhos")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveObterGanhosEntregadorComoOwner() throws Exception {
        GanhosEntregadorDTO ganhosDTO = new GanhosEntregadorDTO();
        ganhosDTO.setValorBruto(new BigDecimal("500.00"));
        ganhosDTO.setValorLiquido(new BigDecimal("475.00"));
        ganhosDTO.setTotalEntregas(5L);
        ganhosDTO.setPeriodo("SEMANA");

        when(ganhosService.calcularGanhosEntregador(eq(1L), any(Periodo.class))).thenReturn(ganhosDTO);

        mockMvc.perform(get("/api/entregadores/1/ganhos")
                        .header("Authorization", "Bearer " + entregadorToken)
                        .param("periodo", "SEMANA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorBruto").exists());
    }

    @Test
    void deveListarGanhosPorEntrega() throws Exception {
        GanhosPorEntregaDTO ganhoEntrega = new GanhosPorEntregaDTO();
        ganhoEntrega.setPedidoId(1L);
        ganhoEntrega.setTaxaEntrega(new BigDecimal("5.00"));
        ganhoEntrega.setValorLiquido(new BigDecimal("4.75"));

        when(ganhosService.listarGanhosPorEntrega(eq(1L), any(Periodo.class)))
                .thenReturn(List.of(ganhoEntrega));

        mockMvc.perform(get("/api/entregadores/1/ganhos/entregas")
                        .header("Authorization", "Bearer " + entregadorToken)
                        .param("periodo", "HOJE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].pedidoId").value(1L));
    }
}

