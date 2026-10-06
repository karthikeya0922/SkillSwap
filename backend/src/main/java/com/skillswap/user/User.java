package com.skillswap.user;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import com.skillswap.common.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_status", columnList = "status"),
		@Index(name = "idx_users_department", columnList = "department") })
public class User extends BaseEntity {

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 150)
	private String college;

	@Column(nullable = false, length = 100)
	private String department;

	@Column(name = "year_of_study", nullable = false)
	private Integer yearOfStudy;

	@Column(length = 1000)
	private String bio;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.STUDENT;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "suspension_reason", length = 500)
	private String suspensionReason;

	@Column(nullable = false)
	private int xp;

	/** Cached aggregate of ratings received; recalculated whenever a rating is added. */
	@Column(name = "rating_average", nullable = false)
	private double ratingAverage;

	@Column(name = "rating_count", nullable = false)
	private int ratingCount;

	@Column(name = "profile_completed", nullable = false)
	private boolean profileCompleted;

	@Column(name = "last_active_at")
	private LocalDateTime lastActiveAt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_availability", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "slot", length = 30)
	private Set<Availability> availability = EnumSet.noneOf(Availability.class);

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	public String getFirstName() {
		return fullName == null ? "" : fullName.trim().split("\\s+")[0];
	}
}
