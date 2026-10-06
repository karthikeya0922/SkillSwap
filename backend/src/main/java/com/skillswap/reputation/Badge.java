package com.skillswap.reputation;

import com.skillswap.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "badges")
public class Badge extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 40)
	private BadgeCode code;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(nullable = false, length = 200)
	private String description;

	/** Lucide icon name. */
	@Column(nullable = false, length = 40)
	private String icon;

	@Column(nullable = false, length = 9)
	private String color;
}
