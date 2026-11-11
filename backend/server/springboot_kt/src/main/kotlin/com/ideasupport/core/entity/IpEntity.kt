package com.ideasupport.core.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import lombok.Data
import lombok.NoArgsConstructor

@Entity
@Data
@NoArgsConstructor
@Table(name = "ip")
open class IpEntity(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	open var id: Long? = null,
	@NotNull
	@Lob
	@Enumerated(EnumType.STRING)
	@Column(name = "ip_type", nullable = false)
	open var ipType: IpType? = null,
	@NotNull
	@Lob
	@Enumerated(EnumType.STRING)
	@Column(name = "ip_range", nullable = false)
	open var ipRange: IpRange? = null,
	@NotNull
	@Column(name = "ip", nullable = false)
	open var ip: String? = null,
)

enum class IpType {
	ALLOW,
	DENY,
}

enum class IpRange {
	ALL,
	USER,
	ADMIN,
}
