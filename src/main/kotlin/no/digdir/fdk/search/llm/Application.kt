package no.digdir.fdk.search.llm

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan


@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAutoConfiguration
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
