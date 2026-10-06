package com.skillswap.reputation;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A badge awarded to a user; createdAt is the award time. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_badges",
		uniqueConstraints = @UniqueConstraint(name = "uk_user_badge", columnNames = { "user_id", "badge_id" }))
public class UserBadge extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "badge_id", nullable = false)
	private Badge badge;
}
