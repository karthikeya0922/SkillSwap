package com.skillswap.challenge;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Peer review of a challenge submission (one per reviewer per submission). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submission_reviews",
		uniqueConstraints = @UniqueConstraint(name = "uk_review_reviewer", columnNames = { "submission_id", "reviewer_id" }))
public class SubmissionReview extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "submission_id", nullable = false)
	private Submission submission;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reviewer_id", nullable = false)
	private User reviewer;

	@Column(nullable = false)
	private int rating;

	@Column(length = 1000)
	private String comment;
}
