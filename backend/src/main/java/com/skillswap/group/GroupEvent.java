package com.skillswap.group;

import java.time.LocalDateTime;

import com.skillswap.common.BaseEntity;
import com.skillswap.session.SessionMode;
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

/** A group study session or community event (hackathon, meetup) shared with all members. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_events", indexes = @Index(name = "idx_group_events_group", columnList = "group_id, start_time"))
public class GroupEvent extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private SkillGroup group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private User createdBy;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GroupEventType type;

	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalDateTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private SessionMode mode;

	@Column(length = 200)
	private String location;

	@Column(name = "meeting_link", length = 500)
	private String meetingLink;
}
