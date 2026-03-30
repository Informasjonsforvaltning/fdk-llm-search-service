package no.digdir.fdk.search.llm.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.support.JdbcTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class TransactionManagerConfig(
    private val dataSource: DataSource,
) {
    @Bean
    fun transactionManager(): PlatformTransactionManager {
        val transactionManager = JdbcTransactionManager()
        transactionManager.dataSource = dataSource
        return transactionManager
    }
}
