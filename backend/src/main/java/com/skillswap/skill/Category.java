package com.skillswap.skill;

import com.skillswap.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

	@Column(nullable = false, unique = true, length = 60)
	private String name;

	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	@Column(length = 300)
	private String description;

	/** Lucide icon name rendered by the frontend. */
	@Column(length = 40)
	private String icon;

	/** Hex accent colour used for chips and cards. */
	@Column(length = 9)
	private String color;
}
