package com.siseg.integration.pagamento;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.ClienteInfoDTO;
import com.siseg.dto.pagamento.CriarPagamentoCompletoRequestDTO;
import com.siseg.dto.pagamento.CriarPagamentoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import org.springframework.stereotype.Component;

@Component
public class PagamentoServiceAdapter {
    
    public CriarPagamentoCompletoRequestDTO adaptarPedidoParaCriacaoPagamento(
            Pedido pedido, 
            CartaoCreditoRequestDTO cartaoDTO) {
        
        CriarPagamentoCompletoRequestDTO request = new CriarPagamentoCompletoRequestDTO();
        
        CriarPagamentoRequestDTO pagamentoRequest = new CriarPagamentoRequestDTO();
        pagamentoRequest.setPedidoId(pedido.getId());
        pagamentoRequest.setMetodoPagamento(pedido.getMetodoPagamento());
        pagamentoRequest.setValor(pedido.getTotal());
        pagamentoRequest.setTroco(pedido.getTroco());
        pagamentoRequest.setCartaoCredito(cartaoDTO);
        request.setPagamento(pagamentoRequest);
        
        Cliente cliente = pedido.getCliente();
        ClienteInfoDTO clienteInfo = adaptarClienteParaDTO(cliente);
        request.setCliente(clienteInfo);
        
        return request;
    }
    
    private ClienteInfoDTO adaptarClienteParaDTO(Cliente cliente) {
        ClienteInfoDTO clienteInfo = new ClienteInfoDTO();
        clienteInfo.setId(cliente.getId());
        clienteInfo.setNome(cliente.getNome());
        clienteInfo.setEmail(cliente.getEmail());
        clienteInfo.setTelefone(cliente.getTelefone());
        clienteInfo.setCpfCnpj(obterCpfCnpjDoCliente(cliente));
        clienteInfo.setCep(obterCepDoCliente(cliente));
        clienteInfo.setAddressNumber(obterNumeroEnderecoDoCliente(cliente));
        clienteInfo.setAddressComplement(obterComplementoEnderecoDoCliente(cliente));
        return clienteInfo;
    }
    
    private String obterCpfCnpjDoCliente(Cliente cliente) {
        
        return "24971563792";
    }
    
    private String obterCepDoCliente(Cliente cliente) {
        return cliente.getEnderecoPrincipal()
                .map(endereco -> endereco.getCep())
                .orElse(null);
    }
    
    private String obterNumeroEnderecoDoCliente(Cliente cliente) {
        return cliente.getEnderecoPrincipal()
                .map(endereco -> endereco.getNumero())
                .orElse(null);
    }
    
    private String obterComplementoEnderecoDoCliente(Cliente cliente) {
        return cliente.getEnderecoPrincipal()
                .map(endereco -> endereco.getComplemento())
                .orElse(null);
    }
    
}


