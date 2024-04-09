package eu.gaiax.wizard.dao;

import eu.gaiax.wizard.api.utils.TenantContext;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public class TenantDAO {
    @Value("${wizard.database.postgres.name}")
    private String masterDbName;

    @Value("${wizard.database.postgres.username}")
    private String dbUsername;

    @Value("${wizard.database.postgres.password}")
    private String dbPassword;

    @Value("${wizard.database.postgres.host}")
    private String dbHost;

    @Value("${wizard.database.postgres.port}")
    private String dbPort;
    @Value("${wizard.tenant.liquibase}")
    private String tenantLiquibaseConfig;

    public void createDatabase(String dbName) {
        DataSource dataSource = getMasterDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            statement.execute("CREATE DATABASE " + dbName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String jdbcUrl(String dbName) {
        return "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    public DataSource getMasterDataSource() {
        return new DriverManagerDataSource(jdbcUrl(masterDbName), dbUsername, dbPassword);
    }

    public DataSource getTenantDataSource() {
        /*HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setInitializationFailTimeout(0);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setPoolName("pool@" + TenantContext.getCurrentTenant());
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setMaxLifetime(30000);
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/" + TenantContext.getCurrentTenant());
        hikariConfig.setUsername("root");
        hikariConfig.setPassword("root");
        return new HikariDataSource(hikariConfig);*/

        return new DriverManagerDataSource(jdbcUrl(TenantContext.getCurrentTenant()), dbUsername, dbPassword);
    }

    public DataSource getTenantDataSource(String dbName) {
        return new DriverManagerDataSource(jdbcUrl(dbName), dbUsername, dbPassword);
    }

    public void runLiquibase() {
        try (Connection connection = getTenantDataSource().getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            new CommandScope(UpdateCommandStep.COMMAND_NAME)
                    .addArgumentValue("changelogFile", tenantLiquibaseConfig)
                    .addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, database)
                    .execute();
        } catch (LiquibaseException | SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void runLiquibase(String tenantAlias) {
        try (Connection connection = getTenantDataSource(tenantAlias).getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            new CommandScope(UpdateCommandStep.COMMAND_NAME)
                    .addArgumentValue("changelogFile", tenantLiquibaseConfig)
                    //.addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, database)
                    .addArgumentValue(DbUrlConnectionCommandStep.URL_ARG, jdbcUrl(tenantAlias))
                    .addArgumentValue(DbUrlConnectionCommandStep.PASSWORD_ARG, dbPassword)
                    .addArgumentValue(DbUrlConnectionCommandStep.USERNAME_ARG, dbUsername)
                    .execute();
        } catch (LiquibaseException | SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
}
