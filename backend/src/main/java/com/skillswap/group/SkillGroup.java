package com.skillswap.group;

import com.skillswap.common.BaseEntity;
import com.skillswap.skill.Skill;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
}
