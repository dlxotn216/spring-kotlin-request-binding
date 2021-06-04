package io.taesu.springkotlinrequestbinding.interfaces

import io.taesu.springkotlinrequestbinding.config.ApplicationConfig
import io.taesu.springkotlinrequestbinding.service.UserCreateService
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * Created by itaesu on 2021/06/03.
 *
 * @author Lee Tae Su
 * @version TBD
 * @since TBD
 */
@ActiveProfiles("local")
@ExtendWith(SpringExtension::class)
@WebMvcTest(UserCreateController::class)
@Import(ApplicationConfig::class)
internal class UserCreateControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userCreateService: UserCreateService

    companion object {
        @JvmStatic
        fun requiredParameterNotExists() = arrayListOf(
            // empty body -> HttpMessageNotReadableException
            """
            """.trimIndent(),

            // empty json -> HttpMessageNotReadableException
            """{
            
            }
            """.trimIndent(),

            // empty email
            """{
                |"email": "",
                |"name": "Lee Tae Su"
            }""".trimMargin(),


            // null email -> MissingKotlinParameterException
            """{
                |"email": null,
                |"name": "Lee Tae Su"
            }""".trimMargin(),

            // empty email property -> MissingKotlinParameterException
            """{
                |"wemail": "atetw",
                |"name": "Lee Tae Su"
            }""".trimMargin(),

              // not email format-> MethodArgumentNotValidException
            """{
                |"email": "taesu",
                |"name": "Lee Tae Su"
            }""".trimMargin(),

            // name length exceed-> MethodArgumentNotValidException
            """{
                |"email": "taesu@aefwf.com",
                |"name": "aweawfafawfwafawfawf"
            }""".trimMargin(),
        )
    }

    @ParameterizedTest
    @MethodSource("requiredParameterNotExists")
    fun `Should fail to create invites with required parameter not exists`(body: String) {
        // when
        val post = MockMvcRequestBuilders.post("/api/v1/users")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

        // then
        val perform = this.mockMvc.perform(post)

        // then
        perform.andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("fail"))
            .andDo(MockMvcResultHandlers.print())
    }

}