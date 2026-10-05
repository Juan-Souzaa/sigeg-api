# SIGEG API

API REST de uma plataforma de delivery de comida, feita em Java com Spring Boot. Atende clientes, restaurantes, entregadores e administradores, e é consumida pelo aplicativo [ifeats](https://github.com/Juan-Souzaa/ifeats). O pagamento fica em um microsserviço separado, o [sigeg-pagamento-service](https://github.com/Juan-Souzaa/sigeg-pagamento-service).

<p>
  <img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger">
</p>

## Funcionalidades

- **Autenticação** com JWT assinado por par de chaves RSA e cinco papéis: usuário, cliente, restaurante, entregador e administrador
- **Restaurantes e cardápio**: cadastro com aprovação pelo administrador, pratos por categoria com upload de foto e raio de entrega
- **Carrinho e cupons** de desconto
- **Pedidos** com os status criado, confirmado, em preparo, saiu para entrega e entregue
- **Pagamento** em dinheiro, PIX ou cartão de crédito, delegado ao microsserviço de pagamento
- **Entrega**: geocodificação de endereços (ViaCEP e LocationIQ), cálculo de rota (OSRM), taxa de entrega e rastreamento do entregador
- **Ganhos e relatórios** de restaurantes e entregadores, com taxas da plataforma configuráveis
- **Avaliações** de restaurante e entregador
- **Tickets de suporte** com comentários

## Arquitetura

```
src/main/java/com/siseg/
├── controller/    Endpoints REST
├── service/       Regras de negócio
├── repository/    Acesso a dados com Spring Data JPA
├── model/         Entidades e enumerações
├── dto/           Objetos de entrada e saída
├── mapper/        Conversão entre entidade e DTO
├── validator/     Validações de negócio
├── integration/   Cliente do microsserviço de pagamento
├── exception/     Exceções e tratamento global de erros
└── config/        Segurança, Swagger e carga inicial de dados
```

A API e o serviço de pagamento conversam por HTTP e se autenticam com uma chave de serviço no cabeçalho `X-Service-Key`.

## Como rodar com Docker

Pré-requisitos: Docker e um MySQL acessível a partir do computador (por padrão, na porta 3306 da própria máquina).

O `docker-compose.yml` sobe a API e o serviço de pagamento juntos, e constrói o segundo a partir da pasta vizinha `../sigeg-pagamento-service`. Por isso os dois repositórios precisam estar clonados lado a lado:

```bash
git clone https://github.com/Juan-Souzaa/sigeg-api.git
git clone https://github.com/Juan-Souzaa/sigeg-pagamento-service.git
cd sigeg-api
cp .env.example .env
docker compose up --build
```

As chaves RSA do JWT são geradas automaticamente na primeira subida e compartilhadas entre os dois serviços.

| Serviço | Endereço |
|---|---|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Pagamento | http://localhost:8081 |

Na primeira execução a API cria um usuário administrador de desenvolvimento (`admin` / `admin123`). Troque a senha antes de usar fora do ambiente local.

## Variáveis de ambiente

As principais, todas com exemplo no `.env.example`:

| Variável | Para que serve |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexão com o MySQL da API |
| `PAYMENT_SPRING_DATASOURCE_URL` | Banco do serviço de pagamento |
| `PAYMENT_SERVICE_KEY`, `PEDIDO_SERVICE_KEY` | Chave de serviço entre API e pagamento |
| `ASAAS_API_KEY`, `ASAAS_WEBHOOK_SECRET` | Gateway de pagamento Asaas (sandbox) |
| `LOCATIONIQ_API_KEY` | Geocodificação de endereços |
| `JWT_EXPIRATION_SECONDS` | Validade do token |

## Testes

```bash
./mvnw test
```

Os testes usam banco H2 em memória.

## Postman

A coleção `SIGEG_API_Postman_Collection.json` e o ambiente `SIGEG_API_Environment.json`, na raiz do repositório, trazem as requisições prontas para importar.
