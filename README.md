# Sistema de Gestão de Projetos e Equipes

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Hibernate](https://img.shields.io/badge/Hibernate-JPA-gray?style=for-the-badge)
![Maven](https://img.shields.io/badge/Apache_Maven-C71A36?style=for-the-badge&logo=apachemaven)
![Swing](https://img.shields.io/badge/Java_Swing-Desktop-lightgrey?style=for-the-badge)

## 📌 Visão Geral do Projeto
Este projeto é um sistema desktop desenvolvido em Java para o gerenciamento corporativo de projetos, tarefas, equipes e controle de prazos. O foco da aplicação é entregar uma solução baseada nas melhores práticas de **Engenharia de Software**, garantindo manutenibilidade através de uma arquitetura limpa (MVC), banco de dados relacional e purismo na **Programação Orientada a Objetos**.

O sistema atende a necessidades táticas (Gerentes de Projeto) e operacionais (Desenvolvedores/Analistas), substituindo controles descentralizados em planilhas por uma plataforma unificada de visibilidade de entregáveis e alocação de recursos.

## 🏗️ Modelagem e Arquitetura

### Diagrama Entidade-Relacionamento (DER)
A modelagem de dados foi desenhada focando em normalização estrutural, contendo Entidades e Tabelas Associativas para controle de integridade referencial.
![DER do Banco de Dados](docs/assets/a3-der.svg)

### Diagrama de Classes UML
A arquitetura de software utiliza a **Composição de Objetos** para garantir um mapeamento rico. As referências do relacional (chaves estrangeiras estáticas) são representadas no código por instâncias nativas do Java (ex: `Equipe` possui explicitamente uma `List<Usuario>`), todas controladas pelos algoritmos do JPA.
![Diagrama de Classes UML](docs/assets/a3-uml.svg)

## ⚙️ Tecnologias Utilizadas
- **Linguagem Base:** Java 21 (LTS)
- **Persistência de Dados:** JPA / Hibernate ORM
- **Versionamento de Banco de Dados:** Flyway
- **Banco de Dados:** PostgreSQL
- **Gerenciador de Dependências e Build:** Apache Maven
- **Interface Gráfica (GUI):** Java Swing
- **Padrão Arquitetural:** MVC (Model-View-Controller)
- **Modelagem Visual:** Astah UML & Draw.io

## 📂 Documentação e Evolução do Projeto
O desenvolvimento arquitetural e o processo avaliativo do sistema iteram em processos de *Sprints Semanais*. Abaixo encontra-se o índice centralizador dos meus levantamentos, decisões técnicas pessoais e relatórios métricos.

### Sprints de Avaliação e Entregas

- **Sprint 1 (Fundações e Arquitetura):**
  - 👉 [Sprint Backlog Geral](docs/sprints/sprint-01/backlog.md)
  - 👉 [Relatório de Desenvolvimento Tático](docs/sprints/sprint-01/relatorio.md)
