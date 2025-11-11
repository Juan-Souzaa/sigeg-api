package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.configuracao.ConfiguracaoTaxaRequestDTO;
import com.siseg.dto.configuracao.ConfiguracaoTaxaResponseDTO;
import com.siseg.dto.ganhos.RelatorioDistribuicaoDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.Periodo;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.ConfiguracaoTaxaService;
import com.siseg.service.GanhosService;
import com.siseg.service.PedidoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private GanhosService ganhosService;

    @MockBean
    private ConfiguracaoTaxaService configuracaoTaxaService;

    @MockBean
    private PedidoRepository pedidoRepository;

    private String adminToken;
    private String clienteToken;
    private Pedido pedido;
    private PedidoResponseDTO pedidoResponseDTO;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = testJwtUtil.generateTokenForUser("admin", ERole.ROLE_ADMIN);
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.PREPARING);

        pedidoResponseDTO = new PedidoResponseDTO();
        pedidoResponseDTO.setId(1L);
        pedidoResponseDTO.setStatus(StatusPedido.PREPARING);
    }

    @Test
    void deveListarPedidosAndamentoComoAdmin() throws Exception {
        when(pedidoRepository.findByStatus(StatusPedido.PREPARING)).thenReturn(List.of(pedido));
        when(pedidoService.buscarPorId(1L)).thenReturn(pedidoResponseDTO);

        mockMvc.perform(get("/api/admin/pedidos/andamento")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deveNegarAcessoAoListarPedidosAndamentoComoCliente() throws Exception {
        mockMvc.perform(get("/api/admin/pedidos/andamento")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveObterRelatorioVendas() throws Exception {
        when(pedidoRepository.findByStatus(StatusPedido.DELIVERED)).thenReturn(List.of(pedido));
        when(pedidoService.buscarPorId(1L)).thenReturn(pedidoResponseDTO);

        mockMvc.perform(get("/api/admin/relatorios/vendas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deveObterRelatorioDistribuicao() throws Exception {
        RelatorioDistribuicaoDTO relatorio = new RelatorioDistribuicaoDTO();
        relatorio.setVolumeTotal(new BigDecimal("10000.00"));
        relatorio.setDistribuicaoRestaurantes(new BigDecimal("8500.00"));
        relatorio.setPeriodo("MES");

        when(ganhosService.gerarRelatorioDistribuicao(any(Periodo.class))).thenReturn(relatorio);

        mockMvc.perform(get("/api/admin/relatorios/distribuicao")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodo", "MES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volumeTotal").exists());
    }

    @Test
    void deveCriarConfiguracaoTaxa() throws Exception {
        ConfiguracaoTaxaRequestDTO requestDTO = new ConfiguracaoTaxaRequestDTO();
        requestDTO.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        requestDTO.setPercentual(new BigDecimal("5.00"));

        ConfiguracaoTaxaResponseDTO responseDTO = new ConfiguracaoTaxaResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        responseDTO.setPercentual(new BigDecimal("5.00"));

        when(configuracaoTaxaService.criarConfiguracaoTaxa(any(ConfiguracaoTaxaRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/admin/configuracoes/taxas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoTaxa").value("TAXA_RESTAURANTE"));
    }

    @Test
    void deveListarHistoricoTaxas() throws Exception {
        ConfiguracaoTaxaResponseDTO configDTO = new ConfiguracaoTaxaResponseDTO();
        configDTO.setId(1L);
        configDTO.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);

        when(configuracaoTaxaService.listarHistoricoTaxas(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(List.of(configDTO));

        mockMvc.perform(get("/api/admin/configuracoes/taxas")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("tipoTaxa", "TAXA_RESTAURANTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tipoTaxa").value("TAXA_RESTAURANTE"));
    }
}

