# bankapp_microservices
banking-discovery-server
  → Eureka service registry
  → Every service registers here on startup
  → No business logic, no DB
  → Port: 8761

banking-common
  → Shared library (not a service, no port)
  → JwtService, JwtFilter
  → Common exceptions, DTOs
  → Every service depends on this

banking-auth-service
  → Register new user
  → Login, return JWT
  → Own DB: banking_auth_db (users table)
  → Port: 8081

banking-account-service
  → Create account
  → Deposit, withdraw, transfer
  → Block, unblock account
  → Own DB: banking_accounts_db (accounts table)
  → Publishes events to Kafka
  → Calls transaction-service via Feign
  → Port: 8082

banking-transaction-service
  → Record every transaction
  → Serve transaction history
  → Own DB: banking_transactions_db (transactions table)
  → Consumes events from Kafka
  → Port: 8083

banking-notification-service
  → Send SMS / email
  → Stateless, no DB
  → Consumes events from Kafka
  → Port: 8084

banking-api-gateway
  → Single entry point for all clients
  → JWT validation on every request
  → Routes to correct service
  → Rate limiting
  → No DB
  → Port: 8080