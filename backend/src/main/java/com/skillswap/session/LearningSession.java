package com.skillswap.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import com.skillswap.common.BaseEntity;
import com.skillswap.request.SkillRequest;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A one-to-one teaching session. Lifecycle:
 * REQUESTED -> ACCEPTED (credits held; logistics pending) -> SCHEDULED (link/location confirmed) -> ONGOING ->
 * COMPLETED, with REJECTED and CANCELLED as terminal exits.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sessions", indexes = { @Index(name = "idx_sessions_teacher_time", columnList = "teacher_id, start_time"),
		@Index(name = "idx_sessions_learner_time", columnList = "learner_id, start_time"),
		@Index(name = "idx_sessions_status_time", columnList = "status, start_time") })
public class LearningSession extends BaseEntity {

	/** Statuses that occupy a slot in both participants' calendars. */
	public static final Set<SessionStatus> BLOCKING = EnumSet.of(SessionStatus.ACCEPTED, SessionStatus.SCHEDULED,
			SessionStatus.ONGOING);

	/** Statuses in which the learner's credits are held in escrow. */
	public static final Set<SessionStatus> CREDITS_HELD = BLOCKING;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private User teacher;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "learner_id", nullable = false)
	private User learner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "skill_request_id")
	private SkillRequest skillRequest;

	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalDateTime endTime;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	/** Credits the learner pays and the teacher earns: one credit per hour. */
	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal credits;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SessionMode mode;

	@Column(length = 200)
	private String location;

	@Column(name = "meeting_link", length = 500)
	private String meetingLink;

	@Column(length = 1000)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status = SessionStatus.REQUESTED;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cancelled_by_id")
	private User cancelledBy;

	@Column(name = "cancel_reason", length = 300)
	private String cancelReason;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "reminder_sent", nullable = false)
	private boolean reminderSent;

	@Version
	private Long version;

	public boolean isParticipant(Long userId) {
		return teacher.getId().equals(userId) || learner.getId().equals(userId);
	}

	public boolean isTeacher(Long userId) {
		return teacher.getId().equals(userId);
	}

	public boolean isLearner(Long userId) {
		return learner.getId().equals(userId);
	}

	public boolean hasLogistics() {
		return mode == SessionMode.ONLINE ? notBlank(meetingLink) : notBlank(location);
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}
}
