package com.ideasupport.core.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import lombok.Data
import lombok.NoArgsConstructor
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import com.ideasupport.core.dto.common.user.UserCommonDto

@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
data class UserEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var idx: Long? = null,
	@Column(name = "uid", nullable = false, unique = true)
	var uid: String? = null,
	@Column(name = "id", unique = true)
	var id: String? = null,
	@Column(name = "pw")
	var pw: String? = null,
	@Column(name = "email")
	var email: String? = null,
	@Column(name = "name")
	var name: String? = null,
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	var role: UserRole? = UserRole.USER,
	override var createdAt: Instant? = null,
	override var updatedAt: Instant? = null,
) : BasicEntity(),
	UserDetails {
	fun mappingUserCommonDto(): UserCommonDto =
		UserCommonDto(
			idx = idx,
			uid = uid,
			email = email,
			name = name,
			createdAt = createdAt,
			updatedAt = updatedAt,
		)

	override fun getAuthorities(): Collection<GrantedAuthority?> = listOf(SimpleGrantedAuthority(role?.name))

	override fun getPassword(): String? = null

	override fun getUsername(): String? = name

	override fun isAccountNonExpired(): Boolean {
		// TODO Auto-generated method stub
		return true
	}

	override fun isAccountNonLocked(): Boolean {
		// TODO Auto-generated method stub
		return true
	}

	override fun isCredentialsNonExpired(): Boolean {
		// TODO Auto-generated method stub
		return true
	}

	override fun isEnabled(): Boolean {
		// TODO Auto-generated method stub
		return true
	}
}

enum class UserRole : GrantedAuthority {
	ADMIN,
	USER,
	;

	override fun getAuthority() = name
}
