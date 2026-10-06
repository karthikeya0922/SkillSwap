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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "challenge_submissions",
		uniqueConstraints = @UniqueConstraint(name = "uk_submission_user", columnNames = { "challenge_id", "user_id" }))
public class Submission extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "challenge_id", nullable = false)
	private Challenge challenge;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "project_title", nullable = false, length = 120)
	private String projectTitle;

	@Column(nullable = false, length = 1500)
	private String description;

	@Column(name = "github_url", length = 300)
	private String githubUrl;

	@Column(name = "demo_url", length = 300)
	private String demoUrl;

	@Column(name = "submission_text", length = 4000)
	private String submissionText;
}
