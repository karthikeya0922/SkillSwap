package com.skillswap.group;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Membership row; INVITED rows represent pending invitations to (usually private) groups. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_members",
		uniqueConstraints = @UniqueConstraint(name = "uk_group_member", columnNames = { "group_id", "user_id" }),
		indexes = @Index(name = "idx_group_members_user", columnList = "user_id, status"))
public class GroupMember extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private SkillGroup group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GroupRole role = GroupRole.MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MemberStatus status = MemberStatus.ACTIVE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_by_id")
	private User invitedBy;
}
