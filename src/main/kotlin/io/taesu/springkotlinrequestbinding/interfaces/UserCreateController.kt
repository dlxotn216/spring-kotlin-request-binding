package io.taesu.springkotlinrequestbinding.interfaces

import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.taesu.springkotlinrequestbinding.service.UserCreateService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

/**
 * Created by itaesu on 2021/06/03.
 *
 * @author Lee Tae Su
 * @version TBD
 * @since TBD
 */
@RestController
class UserCreateController(
    private val userCreateService: UserCreateService,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping("/api/v1/users")
    fun create(@Valid @RequestBody request: UserCreateRequest2): ResponseEntity<SuccessResponse<UserCreateResponse>> {
        return with(userCreateService.create(request)) {
            SuccessResponse(this).toCreated("/api/v1/users/${key}")
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidRequestException(e: HttpMessageNotReadableException): ResponseEntity<FailResponse> {
        with(e.cause) {
            if (this is MissingKotlinParameterException) {
                return handleMissingKotlinParameterException(this)
            } else if (this is ValueInstantiationException) {
                return handleValueInstantiationException(this)
            } else if (this is MethodArgumentNotValidException) {
                return handleMethodArgumentNotValidException(this)
            }
        }

        return with(e) {
            log.error(e.message, e)
            val errorMessage = "Http message is not readable"
            FailResponse(
                "INVALID_REQUEST",
                errorMessage = errorMessage
            ).toError()
        }
    }

    @ExceptionHandler(ValueInstantiationException::class)
    fun handleValueInstantiationException(e: ValueInstantiationException): ResponseEntity<FailResponse> {
        return with(e) {
            log.error(e.message, e)
            val errorMessage = "Http message is not readable"
            FailResponse(
                "INVALID_REQUEST",
                errorMessage = errorMessage
            ).toError()
        }
    }

    @ExceptionHandler(MissingKotlinParameterException::class)
    fun handleMissingKotlinParameterException(e: MissingKotlinParameterException): ResponseEntity<FailResponse> {
        return with(e) {
            log.error(e.message, e)
            val errorMessage = "${e.parameter.name} is missing"
            FailResponse(
                "INVALID_REQUEST",
                errorMessage = errorMessage
            ).toError()
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<FailResponse> {
        return with(e) {
            log.error(e.message, e)
            FailResponse(
                "INVALID_REQUEST",
                errorMessage = "Invalid request",
                fieldErrors = e.fieldErrors
                    .map {
                        FieldErrorResponse(
                            fieldName = it.field,
                            message = it.defaultMessage ?: ""
                        )
                    }
            ).toError()
        }
    }
}

/*
아래와 같은 요청을 보낼 경우
{
    "email": null,
    "name": "Lee Tae Su"
},
{
    "emwail": "taesu@crsube.co.kr",
    "name": "Lee Tae Su"
}
아래의 예외가 발생.
HttpMessageNotReadableException (cause:MissingKotlinParameterException)

따라서 validation 어노테이션이 동작하기 이전에 예외가 터짐.
 */
data class UserCreateRequest(
    @field:NotEmpty(message = "{UserCreateRequest.email.invalid}")
    @field:Email(message = "{UserCreateRequest.email.invalid}")
    val email: String,
    @field:NotEmpty(message = "{UserCreateRequest.name.invalid}")
    @field:Size(min = 1, max = 10, message = "{UserCreateRequest.name.invalid}")
    val name: String
)

/*
아래와 같은 요청을 보낼 경우
{
    "email": null,
    "name": "Lee Tae Su"
},
{
    "emwail": "taesu@crsube.co.kr",
    "name": "Lee Tae Su"
}
아래의 예외가 발생.
MethodArgumentNotValidException

따라서 validation 어노테이션이 동작 함.
단 data class는 Request Dto에 사용 불가.
 */
class UserCreateRequest2(
    email: String?,
    name: String?
) {
    @field:NotEmpty(message = "{UserCreateRequest.email.invalid}")
    @field:Email(message = "{UserCreateRequest.email.invalid}")
    val email: String = email ?: ""

    @field:NotEmpty(message = "{UserCreateRequest.name.invalid}")
    @field:Size(min = 1, max = 10, message = "{UserCreateRequest.name.invalid}")
    val name: String = name ?: ""
}

data class UserCreateResponse(
    val key: Long,
    val email: String,
    val name: String
)

data class SuccessResponse<T>(
    val result: T,
    val message: String = "Request was success."
) {
    val status = "success"
}

fun <T> SuccessResponse<T>.toCreated(uri: String): ResponseEntity<SuccessResponse<T>> =
    ResponseEntity.created(URI.create(uri)).body(this)

data class FailResponse(
    val errorCode: String,
    val errorMessage: String = "Request was fail.",
    val fieldErrors: List<FieldErrorResponse> = emptyList()
) {
    val status = "fail"
}

data class FieldErrorResponse(
    val fieldName: String,
    val message: String
)


fun FailResponse.toError(): ResponseEntity<FailResponse> =
    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(this)