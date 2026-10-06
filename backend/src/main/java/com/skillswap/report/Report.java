package com.skillswap.report;

import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reports", indexes = @Index(name = "idx_reports_status", columnList = "status, created_at"))
public class Report extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	/** Owner of the reported content, resolved on the server (never taken from the client). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reported_user_id")
	private User reportedUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 20)
	private ReportTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	/** Short snapshot of the reported content so admins can review it even after removal. */
	@Column(name = "target_preview", length = 500)
	private String targetPreview;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReportReason reason;

	@Column(length = 1000)
	private String details;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReportStatus status = ReportStatus.OPEN;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_taken", nullable = false, length = 20)
	private ReportAction actionTaken = ReportAction.NONE;

	@Column(name = "admin_note", length = 1000)
	private String adminNote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by_id")
	private User resolvedBy;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;
}
