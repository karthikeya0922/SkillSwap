package com.skillswap.group;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLJoinTableRestriction;

import com.skillswap.common.BaseEntity;
import com.skillswap.skill.Skill;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "skill_groups")
public class SkillGroup extends BaseEntity {

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false, length = 1000)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "skill_id", nullable = false)
	private Skill skill;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_id", nullable = false)
	private User creator;

	@Column(name = "max_members", nullable = false)
	private int maxMembers;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GroupPrivacy privacy = GroupPrivacy.PUBLIC;

	/** Inverse side of {@link GroupMember#getGroup()}: every membership row, including invitations. */
	@OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
	private List<GroupMember> memberships = new ArrayList<>();

	/**
	 * Students who are active members, read straight through the existing {@code group_members} join table. The
	 * mapping is read-only ({@link Immutable}) and declares no extra constraints, so it never changes the schema or
	 * bypasses {@code GroupService}, which owns joins, invites and removals through {@link GroupMember}.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "group_members",
			joinColumns = @JoinColumn(name = "group_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)),
			inverseJoinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)))
	@SQLJoinTableRestriction("status = 'ACTIVE'")
	@Immutable
	private List<User> activeMembers = new ArrayList<>();
}
