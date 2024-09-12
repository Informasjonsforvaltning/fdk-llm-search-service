package no.digdir.fdk.search.llm.configuration

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest


@Component
class CustomBadRequestAttributes : DefaultErrorAttributes() {

    override fun getErrorAttributes(webRequest: WebRequest, options: ErrorAttributeOptions): Map<String, Any> {
        val defaultAttributes: MutableMap<String, Any> = super.getErrorAttributes(webRequest, options)

        return if (defaultAttributes["status"] == HttpStatus.BAD_REQUEST.value()) {
            super.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, ErrorAttributeOptions.Include.EXCEPTION))
        } else defaultAttributes

    }

}
