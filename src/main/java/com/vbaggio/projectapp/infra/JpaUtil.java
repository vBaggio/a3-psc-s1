package com.vbaggio.projectapp.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utilitário central de infraestrutura JPA.
 *
 * <p>Implementa o padrão Singleton para garantir que apenas uma
 * {@link EntityManagerFactory} seja criada durante toda a execução
 * da aplicação (custo alto de inicialização do Hibernate).</p>
 *
 * <p>As credenciais de conexão são carregadas de {@code db.properties}
 * (classpath), nunca hardcoded nesta classe.</p>
 *
 * <p>Antes de abrir a fábrica JPA, executa as migrações Flyway para
 * assegurar que o schema do banco esteja atualizado.</p>
 *
 * <p>Uso típico em um Repository:</p>
 * <pre>{@code
 *   EntityManager em = JpaUtil.getEntityManager();
 *   try {
 *       em.getTransaction().begin();
 *       // ... operações JPA ...
 *       em.getTransaction().commit();
 *   } catch (Exception e) {
 *       em.getTransaction().rollback();
 *       throw e;
 *   } finally {
 *       em.close();
 *   }
 * }</pre>
 */
public class JpaUtil {

    private static final String CONFIG_FILE = "db.properties";

    private static EntityManagerFactory emf;
    private static Properties dbProps;

    /** Construtor privado — classe utilitária, não deve ser instanciada. */
    private JpaUtil() {}

    // -----------------------------------------------------------------------
    // API pública
    // -----------------------------------------------------------------------

    /**
     * Retorna a instância única de {@link EntityManagerFactory}.
     * Na primeira chamada, carrega {@code db.properties}, executa as
     * migrações Flyway e inicializa o Hibernate.
     *
     * @return fábrica de EntityManagers compartilhada pela aplicação
     */
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            Properties props = carregarProperties();
            executarMigracoes(props);
            emf = Persistence.createEntityManagerFactory(
                    props.getProperty("db.persistence_unit"),
                    buildJpaProperties(props)
            );
        }
        return emf;
    }

    /**
     * Cria e retorna um novo {@link EntityManager} a partir da fábrica.
     * <strong>O chamador é responsável por fechar o EntityManager</strong>
     * após o uso (idealmente em um bloco {@code finally}).
     *
     * @return novo EntityManager pronto para uso
     */
    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Encerra a {@link EntityManagerFactory} e libera todos os recursos
     * de conexão com o banco de dados.
     * Deve ser chamado uma única vez ao encerrar a aplicação.
     */
    public static synchronized void fechar() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    // -----------------------------------------------------------------------
    // Internos
    // -----------------------------------------------------------------------

    /**
     * Carrega {@code db.properties} do classpath.
     * Lança {@link IllegalStateException} se o arquivo não for encontrado,
     * orientando o desenvolvedor a criá-lo a partir do {@code db.properties.example}.
     */
    private static synchronized Properties carregarProperties() {
        if (dbProps != null) {
            return dbProps;
        }
        try (InputStream is = JpaUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Arquivo '" + CONFIG_FILE + "' não encontrado no classpath. " +
                        "Copie src/main/resources/db.properties.example para db.properties e ajuste as credenciais."
                );
            }
            dbProps = new Properties();
            dbProps.load(is);
            return dbProps;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler " + CONFIG_FILE, e);
        }
    }

    /**
     * Converte as propriedades de {@code db.properties} para o formato
     * esperado pelo JPA/Hibernate, sobrescrevendo qualquer valor fixo
     * que possa estar no {@code persistence.xml}.
     */
    private static Map<String, String> buildJpaProperties(Properties props) {
        Map<String, String> jpa = new HashMap<>();
        jpa.put("jakarta.persistence.jdbc.driver",   props.getProperty("db.driver"));
        jpa.put("jakarta.persistence.jdbc.url",      props.getProperty("db.url"));
        jpa.put("jakarta.persistence.jdbc.user",     props.getProperty("db.usuario"));
        jpa.put("jakarta.persistence.jdbc.password", props.getProperty("db.senha"));
        return jpa;
    }

    /**
     * Executa todas as migrations Flyway pendentes em {@code db/migration}.
     * O Hibernate está configurado com {@code hbm2ddl.auto=validate},
     * portanto o schema DEVE estar atualizado antes de o EMF ser criado.
     */
    private static void executarMigracoes(Properties props) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        props.getProperty("db.url"),
                        props.getProperty("db.usuario"),
                        props.getProperty("db.senha")
                )
                .locations("classpath:db/migration")
                .load();

        var resultado = flyway.migrate();
        System.out.printf(
                "[Flyway] Migrações executadas: %d | Schema atual: versão %s%n",
                resultado.migrationsExecuted,
                resultado.targetSchemaVersion
        );
    }
}
