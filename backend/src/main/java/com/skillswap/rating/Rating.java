package com.skillswap.rating;

import com.skillswap.common.BaseEntity;
import com.skillswap.session.LearningSession;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A learner's review of the teacher for one completed session (at most one per session). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ratings", indexes = @Index(name = "idx_ratings_ratee", columnList = "ratee_id, created_at"))
public class Rating extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false, unique = true)
	private LearningSession session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rater_id", nullable = false)
	private User rater;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ratee_id", nullable = false)
	private User ratee;

	@Column(nullable = false)
	private int stars;

	@Column(name = "teaching_quality", nullable = false)
	private int teachingQuality;

	@Column(nullable = false)
	private int communication;

	@Column(nullable = false)
	private int knowledge;

	@Column(length = 1000)
	private String feedback;
}
