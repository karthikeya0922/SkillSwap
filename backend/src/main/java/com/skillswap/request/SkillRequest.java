package com.skillswap.request;

import java.math.BigDecimal;

import com.skillswap.common.BaseEntity;
import com.skillswap.session.SessionMode;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.Skill;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A public "I want to learn X" post that teachers can offer to help with. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "skill_requests", indexes = { @Index(name = "idx_requests_status_skill", columnList = "status, skill_id"),
		@Index(name = "idx_requests_learner", columnList = "learner_id") })
public class SkillRequest extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "learner_id", nullable = false)
	private User learner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 1500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "desired_level", nullable = false, length = 20)
	private ProficiencyLevel desiredLevel;

	@Column(name = "preferred_schedule", length = 200)
	private String preferredSchedule;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SessionMode mode;

	@Column(name = "duration_hours", nullable = false, precision = 4, scale = 2)
	private BigDecimal durationHours;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestStatus status = RequestStatus.OPEN;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accepted_teacher_id")
	private User acceptedTeacher;
}
