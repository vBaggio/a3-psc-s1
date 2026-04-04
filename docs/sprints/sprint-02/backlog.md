# Sprint Backlog - Sprint 2

**Período Inicial/Final:** 29/03/2026 a 04/04/2026  
**Responsável do Projeto:** Vinícius Baggio  
**Objetivo da Sprint:** Estabelecer a infraestrutura de banco de dados e persistência da aplicação, conectando as entidades de domínio ao PostgreSQL via JPA/Hibernate com controle de schema gerenciado pelo Flyway.

---

## Tarefas Catalogadas (Backlog Items)

| ID | Descrição da Tarefa | Status |
|----|----------------------|--------|
| **TSK-01** | Criar `pom.xml` com dependências Maven (Hibernate, PostgreSQL Driver, Flyway). | ✅ Concluído |
| **TSK-02** | Configurar ambiente de banco de dados com Docker e `docker-compose.yml` (PostgreSQL 16). | ✅ Concluído |
| **TSK-03** | Adicionar anotações JPA nas entidades de domínio (`Cargo`, `Usuario`, `Projeto`, `Equipe`). | ✅ Concluído |
| **TSK-04** | Criar `persistence.xml` com configuração da unidade de persistência JPA/Hibernate. | ✅ Concluído |
| **TSK-05** | Criar script Flyway `V1__Create_schema.sql` com DDL completo do schema relacional. | ✅ Concluído |
| **TSK-06** | Executar migração Flyway e validar criação das tabelas no banco. | ✅ Concluído |

---

## Ferramentas Adotadas na Sprint

- **Gerenciador de Dependências:** Apache Maven 3.x
- **ORM / Persistência:** Hibernate 6.4.4 (JPA 3.0)
- **Banco de Dados:** PostgreSQL 16 (Alpine)
- **Containerização:** Docker + Docker Compose
- **Controle de Schema:** Flyway 10.10.0
- **Editor de Texto:** VS Code
