package com.skillswap.group;

import com.skillswap.common.BaseEntity;
import com.skillswap.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A message in a group's discussion board. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_posts", indexes = @Index(name = "idx_group_posts_group", columnList = "group_id, created_at"))
public class GroupPost extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private SkillGroup group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = 2000)
	private String content;
}
