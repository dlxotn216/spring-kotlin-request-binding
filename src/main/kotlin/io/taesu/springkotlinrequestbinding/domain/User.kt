package io.taesu.springkotlinrequestbinding.domain

import org.hibernate.annotations.NaturalId
import javax.persistence.*

/**
 * Created by itaesu on 2021/06/03.
 *
 * @author Lee Tae Su
 * @version TBD
 * @since TBD
 */
@Entity
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_USER")
    @SequenceGenerator(name = "SEQ_USER", sequenceName = "SEQ_USER", allocationSize = 1)
    val key: Long,

    @NaturalId
    @Column(name = "USER_ID", unique = false, nullable = false, updatable = false, length = 512)
    val email: String,

    @Column(name = "NAME", nullable = false, length = 10)
    val name: String
)