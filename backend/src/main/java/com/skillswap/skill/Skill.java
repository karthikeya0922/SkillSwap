package com.skillswap.skill;

import com.skillswap.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "skills", indexes = @Index(name = "idx_skills_category", columnList = "category_id"))
public class Skill extends BaseEntity {

	@Column(nullable = false, unique = true, length = 80)
	private String name;

	@Column(nullable = false, unique = true, length = 80)
	private String slug;

	@Column(length = 500)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	/** Deactivated skills stay attached to history but can no longer be added or booked. */
	@Column(nullable = false)
	private boolean active = true;
}
