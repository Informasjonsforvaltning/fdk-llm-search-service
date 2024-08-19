package no.digdir.fdk.search.llm.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration

@Configuration()
open class SecurityConfig(
        @Value("\${application.cors.originPatterns}")
        val corsOriginPatterns: Array<String>
) {
    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { cors ->
                cors.configurationSource { _ ->
                    val config = CorsConfiguration()
                    config.allowCredentials = false
                    config.allowedHeaders = listOf("*")
                    config.maxAge = 3600L
                    config.allowedOriginPatterns = corsOriginPatterns.toList()
                    config.allowedMethods = listOf("GET", "POST", "OPTIONS")
                    config
                }
            }
            .csrf {
                it.disable()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
        return http.build()
    }
}
