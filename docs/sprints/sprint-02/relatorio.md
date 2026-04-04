# Relatório Semanal de Desenvolvimento - Sprint 2

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 29/03/2026 a 04/04/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações

Durante esta segunda semana, o foco foi a construção da infraestrutura de persistência da aplicação. Partindo das entidades de domínio criadas na Sprint 1 (POJOs puros), evoluí o projeto para um ecossistema completo de banco de dados: containerização do PostgreSQL via Docker, mapeamento objeto-relacional com anotações JPA/Hibernate e controle de versionamento de schema com Flyway. A migração `V1__Create_schema.sql` foi aplicada com sucesso, criando as 6 tabelas relacionais no banco.

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **04/04/2026** | **Criação do `pom.xml`:** Estruturei o gerenciador de dependências Maven com as três dependências nucleares da Sprint: `hibernate-core 6.4.4.Final`, `postgresql 42.7.3` e `flyway-core 10.10.0`. Configurado `maven-compiler-plugin` para Java 21 e `flyway-maven-plugin` para execução via `mvn flyway:migrate`. |
| **04/04/2026** | **Containerização com Docker:** Criado `docker-compose.yml` com serviço `postgres:16-alpine`, volume persistente `postgres_data` e banco `projectapp` na porta 5432. Container nomeado `projectapp-db` para rastreabilidade. |
| **04/04/2026** | **Anotações JPA nas Entidades:** Evoluí as 4 entidades de POJOs para classes JPA completas. Decisões técnicas: `@GeneratedValue(UUID)` para IDs, `FetchType.LAZY` nos relacionamentos `@ManyToOne`, `@ManyToMany` com `@JoinTable` explícito para as tabelas associativas `equipe_membro` e `equipe_projeto`. |
| **04/04/2026** | **`persistence.xml` com `hbm2ddl=validate`:** Configuração deliberada para que o Hibernate apenas valide o schema existente, sem criar ou alterar tabelas. O controle do DDL é responsabilidade exclusiva do Flyway — separação de responsabilidades arquitetural. |
| **04/04/2026** | **Flyway `V1__Create_schema.sql` e Migração:** Script DDL com 6 tabelas (`cargo`, `usuario`, `projeto`, `equipe`, `equipe_membro`, `equipe_projeto`), chaves primárias UUID via `gen_random_uuid()` e integridade referencial via `FOREIGN KEY`. Migração aplicada com sucesso: `Successfully applied 1 migration to schema "public", now at version v1`. |

## 3. Registros de Desafios Enfrentados

O principal desafio foi a instalação e configuração do ambiente de build a partir do zero. O Maven não estava disponível no PATH do sistema, exigindo instalação via `dnf`. Adicionalmente, o usuário não pertencia ao grupo `docker`, bloqueando o acesso ao socket do daemon — resolvido com `usermod -aG docker`.

* **Resolução:** Ambiente normalizado com Maven instalado via gerenciador de pacotes do sistema e permissões Docker ajustadas. O projeto agora compila, resolve dependências e executa migrações de banco de forma totalmente automatizada via CLI Maven.

## 4. Artefatos Entregues

| Artefato | Caminho |
|----------|---------|
| Definição de Dependências | `pom.xml` |
| Infraestrutura de Container | `docker-compose.yml` |
| Entidades JPA | `src/main/java/.../model/entity/*.java` |
| Configuração de Persistência | `src/main/resources/META-INF/persistence.xml` |
| Script de Migração Flyway | `src/main/resources/db/migration/V1__Create_schema.sql` |
