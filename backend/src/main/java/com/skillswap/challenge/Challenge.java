package com.skillswap.challenge;

import java.time.LocalDateTime;

import com.skillswap.common.BaseEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "challenges", indexes = @Index(name = "idx_challenges_deadline", columnList = "deadline"))
public class Challenge extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 4000)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Difficulty difficulty;

	@Column(nullable = false)
	private LocalDateTime deadline;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_id", nullable = false)
	private User creator;

	@Column(name = "xp_reward", nullable = false)
	private int xpReward;

	public boolean isOpen() {
		return deadline.isAfter(LocalDateTime.now());
	}
}
