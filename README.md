## Kotlin 클래스로 Spring MVC Request Binding 받기

Kotlin에서 nullable type이 구분됨에 따라 Spring에서 요청을 바인딩 할 때 몇 가지 신경 써 주어야 하는 부분이 있는 것 같아 정리를 한다.  


### Data class
```kotlin
data class UserCreateRequest(
    @field:NotEmpty(message = "{UserCreateRequest.email.invalid}")
    @field:Email(message = "{UserCreateRequest.email.invalid}")
    val email: String,
    @field:NotEmpty(message = "{UserCreateRequest.name.invalid}")
    @field:Size(min = 1, max = 10, message = "{UserCreateRequest.name.invalid}")
    val name: String
)
```
위와 같이 Data class를 사용하려는 경우 @field:NotEmpty와 같이 필드에 @NotEmpty 어노테이션을 붙인 형태로 컴파일 되게 해야 한다.  
그렇지 않은 경우 Spring에서 생성자를 통해 바인딩을 하게 되고 이때 제대로 validation이 동작하지 않을 수 있다.  

예외에 대한 처리는 어떨까?  
email이나 name은 not empty가 걸려있다.  
따라서 아래와 같이 요청을 보낼 때   
```json
{
    "name": "Lee Tae Su"
},
{
    "email": null,
    "name": "Lee Tae Su"
},
```
기존의 자바라면 아래의 Exception handler로 validation 실패에 대한 처리가 가능 했을 것이다.  
```kotlin
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
``` 
그에 따른 응답은 아래와 같이 field 별 발생한 예외를 넘겨줄 수 있을 것이다.  
```json
{
  "errorCode": "INVALID_REQUEST",
  "errorMessage": "Invalid request",
  "fieldErrors": [
    {
      "fieldName": "name",
      "message": "Requested field 'name' should be between 1 and 10."
    }
  ],
  "status": "fail"
}
```

하지만 코틀린에선 HttpMessageNotReadableException (cause:MissingKotlinParameterException) 예외가 발생한다.  
> com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException: Instantiation of [simple type, class io.taesu.springkotlinrequestbinding.interfaces.UserCreateRequest] 
> value failed for JSON property email due to missing (therefore NULL) value for creator parameter email which is a non-nullable type   

따라서 아래와 같은 추가적인 Exception Handler를 구현 해 주어야 한다.  
```kotlin
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
```  

하지만 파라미터 이름만 알 수 있을 뿐 FieldError 객체 정보가 넘어오진 않기 때문에 localValidatorFactoryBean 설정에 따른 validation fail 메시지를 얻어올 순 없다.  
따라서 응답은 아래와 같을 것이다. 둘 다 똑같은 field 에러이지만 보내는 값에 따라 응답이 다르니 난감하다.  
```json
{
  "errorCode": "INVALID_REQUEST",
  "errorMessage": "email is missing",
  "fieldErrors": [],
  "status": "fail"
}
```

### 일반 클래스
두 번째 방법은 data 클래스를 사용하지 말고 생성자의 파라미터를 Nullable 하게 만들어주는 것이다.  
그리고 아래와 같이 프로퍼티에 대입 해준는 처리를 한다.
```kotlin
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
```

이 경우엔 아래와 같은 요청도 MethodArgumentNotValidException 예외에 대한 핸들러로 잘 전달이 된다.  
```json
{
    "wemail": "atetw",
    "name": "Lee Tae Su"
},
{
    "email": null,
    "name": "Lee Tae Su"
}
```

아래와 같이 empty json을 보낸 경우도
```json
{
  
}
```
잘 처리가 된다.
```json
{
  "errorCode": "INVALID_REQUEST",
  "errorMessage": "Invalid request",
  "fieldErrors": [
    {
      "fieldName": "email",
      "message": "Requested field 'email' invalid."
    },
    {
      "fieldName": "name",
      "message": "Requested field 'name' should be between 1 and 10."
    },
    {
      "fieldName": "name",
      "message": "Requested field 'name' should be between 1 and 10."
    }
  ],
  "status": "fail"
}
```
따라서 Request DTO 바인딩엔 data 클래스 보단 일반 클래스가 좀 더 적절 한 것 같다.  
Entity에도 Data 클래스를 사용하지 못하는데 언제 유용하게 쓸 수 있을 지... 도메인 영역의 VO는 immutable 해야 하니 사용이 가능 하려나 모르겠다.  


### 그 외
만약 아예 body를 비워서 요청을 보내면 HttpMessageNotReadableException 예외가 떨어진다.  
그래서 아래와 같은 추가적인 Exception handler를 넣어주는 것이 좋다.   

```kotlin
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
```
여기서 HttpMessageNotReadableException 예외가 나며 ValueInstantiationException이 cause가 되는 경우는 아래와 같이 생성자로 넘어온 Argument 값을    
프로퍼티에 할당 하는 시점에 validation을 하며 예외를 발생시킬 때 나는 예외이다.  

```kotlin
class InviteCreateRequest(
    from: InviteFrom?,
    target: InviteTarget?,
    requestedRecipients: RequestedRecipients?
) {
    val from: InviteFrom = from.required(REQUIRED_PARAMETER, "from")
    val target: InviteTarget = target.required(REQUIRED_PARAMETER, "from")
    val requestedRecipients: RequestedRecipients = requestedRecipients.required(REQUIRED_PARAMETER, "requestedRecipients")

```

required 함수는 아래와 같이 생겼다.  
```kotlin
fun <T : Any?> T?.required(requiredErrorCode: ErrorCode,
                           fieldName: String,
                           jsonPath: String = fieldName): T {
    if (this == null) {
        throwInvalidProperty(errorCode = requiredErrorCode,
                             debugMessage = "Required field $fieldName is null or empty",
                             fieldName = fieldName,
                             jsonPath = jsonPath)
    }

    return this
}

fun String?.requiredString(requiredErrorCode: ErrorCode,
                           limit: Int,
                           fieldName: String,
                           jsonPath: String = fieldName): String {
    if (this.isNullOrBlank()) {
        throwInvalidProperty(errorCode = requiredErrorCode,
                             debugMessage = "Required field $fieldName is null or empty",
                             fieldName = fieldName,
                             jsonPath = jsonPath)
    }
    this.throwWhenExceed(limit, fieldName)

    return this
}
```

이렇게 검증하는 것 보단 validator를 쓴느게 좋아 보이기도 한다.