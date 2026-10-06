package com.skillswap.skill;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A skill on a user's profile. For TEACH entries {@code level} is the user's proficiency; for LEARN entries it is
 * the level they want to reach.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_skills",
		uniqueConstraints = @UniqueConstraint(name = "uk_user_skill_type", columnNames = { "user_id", "skill_id", "type" }),
		indexes = @Index(name = "idx_user_skills_skill_type", columnList = "skill_id, type"))
public class UserSkill extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SkillType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProficiencyLevel level;

	/** Years of experience; only meaningful for TEACH entries. */
	@Column(name = "years_experience")
	private Double yearsExperience;

	@Column(length = 500)
	private String description;
}
