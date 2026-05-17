# Relatório Semanal de Desenvolvimento - Sprint 1

**Responsável Principal:** Vinícius Baggio  
**Período Avaliado:** 22/03/2026 a 28/03/2026  
**Status do Ciclo:** Concluído  

---

## 1. Resumo Executivo das Avaliações
Durante esta primeira semana, direcionei meu foco integralmente na estruturação funcional da aplicação corporativa. Como Desenvolvedor Solitário em um formato *All-in-One* (Back, Front, Gerência), meu foco principal consistiu em extrair as demandas brutas, construir Diagramas ER para infraestrutura, e injetar o purismo teórico em um desenho arquitetural de uso sólido via UML de alto escalão. 

## 2. Diário Histórico de Execuções e Decisões Técnicas

| Data de Registro | Evento Executado / Veredito Arquitetural |
|------------------|------------------------------------------|
| **23/03/2026** | **Análise da Demanda e Esboço DER:** Identifiquei a normalização essencial de "Cargos" dos componentes de Equipes e parametrização universal de Gestores de Processo em um diagrama base de relacionalidade impecável. |
| **24/03/2026** | **[DECISÃO TÉCNICA] Trocando JDBC Nativo por JPA/ORM:** Como arquiteto do meu próprio app, hesitei antes de engatar o Driver nativo (que me obrigaria a mapear dezenas de ID's sujos nas classes finais). O aval deliberado que eu mesmo dei consistiu na modernização por injeções do Framework **Hibernate**. Eu abri mão de sintaxe primária em prol do Mapeamento Robusto da Composição Direta `private Perfil cargo;` e evazão limpa das listas de memória do Java. |
| **25/03/2026** | **Geração de POJOs / Tabelas Cruas:** O esqueleto DDL (formato `.sql`) e a primeira interface do modelo (Enumerações base e classes de entidades) foram codados sem interdependências brutas de projeto. |
| **26/03/2026** | **Engenharia Reversa Assistida e Aprovada:** Utilizei a suíte da engine local _Astah UML_ para rastrear a leitura literal das compilações Java. Meu objetivo (Composição 1:N / N:N perfeita) encadeou todas as instâncias em Setas Orientadas exatatamente como idealizei para as avaliações teóricas. |
| **26/03/2026** | **Vínculo Oficial de Versões em Nuvem:** Subi formalmente meus manuais no Github atrelando minha assinatura na aba primária utilizando regras severas de `.gitignore`. |

## 3. Registros de Desafios Enfrentados
O desafio mais relevante nesta semana 1 focava-se no problema da escalabilidade na **"Impedância Crônico-Relacional"**.  Mapear interconexões de Magnitude Alta/Alta nas Tabelas `equipe_projeto` para gerenciar Equipes com inumeráveis Usuários requereriam construções "braçais" severas e Tabelas Transitórias de Banco Inativas nas esferas de POO. 

* **Minha Resolução de Software:** Deleguei ao Framework de Intercomunicação (Hibernate via `@ManyToMany`). Com essa adoção, a construção transiente (exemplo: Salvar dados em tabela Associative de N:M) será executada no meu escopo interno de Build - alocando em mim somente as rotinas nativas primárias (ex: chamar `equipe.addProj(xyz)`). 

## 4. Artefatos Entregues

| Artefato | Caminho | Operação |
|----------|---------|----------|
| Diagrama Entidade-Relacionamento (DER) | `docs/assets/a3-der.svg` / `docs/assets/a3.drawio` | Criado |
| Diagrama de Classes UML | `docs/assets/a3-uml.svg` / `docs/assets/a3.asta` | Criado |
| Schema DDL inicial | `src/main/resources/db/migration/V1__Create_schema.sql` | Criado |
| Entidades de domínio (POJOs iniciais) | `src/main/java/.../model/entity/` | Criado |
| Configuração do repositório e `.gitignore` | `.gitignore` | Criado |
