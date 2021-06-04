package io.taesu.springkotlinrequestbinding.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean


/**
 * Created by itaesu on 2021/06/03.
 *
 * @author Lee Tae Su
 * @version TBD
 * @since TBD
 */
@Configuration
class ApplicationConfig {
    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename("classpath:messages/message")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }

    @Bean
    fun localValidatorFactoryBean(messageSource: MessageSource): LocalValidatorFactoryBean {
        val bean = LocalValidatorFactoryBean()
        bean.setValidationMessageSource(messageSource)
        return bean
    }
}