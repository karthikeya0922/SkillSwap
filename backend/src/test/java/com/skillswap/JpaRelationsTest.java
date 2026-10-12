package com.skillswap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.group.GroupMember;
import com.skillswap.group.GroupRole;
import com.skillswap.group.MemberStatus;
import com.skillswap.group.SkillGroup;
import com.skillswap.skill.Category;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.user.User;

import jakarta.persistence.EntityManager;

/** The inverse @OneToMany sides and the read-only @ManyToMany, loaded fresh from the database. */
@SpringBootTest
@Transactional
class JpaRelationsTest {

	@Autowired
	private EntityManager em;

	private User user(String name) {
		User user = new User();
		user.setFullName(name);
		user.setEmail(name.toLowerCase().replace(' ', '.') + System.nanoTime() + "@test.dev");
		user.setPasswordHash("not-a-real-hash");
		user.setCollege("CBIT");
		user.setDepartment("CSE");
		user.setYearOfStudy(2);
		em.persist(user);
		return user;
	}

	private Skill skill(String name) {
		Category category = new Category();
		category.setName("Rel-" + System.nanoTime());
		category.setSlug(category.getName().toLowerCase());
		em.persist(category);
		Skill skill = new Skill();
		skill.setName(name + "-" + System.nanoTime());
		skill.setSlug(skill.getName().toLowerCase());
		skill.setCategory(category);
		em.persist(skill);
		return skill;
	}

	private void member(SkillGroup group, User user, GroupRole role, MemberStatus status) {
		GroupMember member = new GroupMember();
		member.setGroup(group);
		member.setUser(user);
		member.setRole(role);
		member.setStatus(status);
		em.persist(member);
	}

	@Test
	void userSkillsAreReachableFromTheUser() {
		User asha = user("Asha Rao");
		for (SkillType type : SkillType.values()) {
			UserSkill userSkill = new UserSkill();
			userSkill.setUser(asha);
			userSkill.setSkill(skill(type.name()));
			userSkill.setType(type);
			userSkill.setLevel(ProficiencyLevel.INTERMEDIATE);
			em.persist(userSkill);
		}
		em.flush();
		em.clear();

		User reloaded = em.find(User.class, asha.getId());
		assertThat(reloaded.getSkills()).hasSize(2)
				.extracting(UserSkill::getType).containsExactlyInAnyOrder(SkillType.TEACH, SkillType.LEARN);
	}

	@Test
	void groupExposesAllMembershipsAndOnlyActiveMembers() {
		User owner = user("Owner One");
		User member = user("Member Two");
		User invited = user("Invited Three");
		SkillGroup group = new SkillGroup();
		group.setName("Java Circle");
		group.setDescription("Weekly Java practice");
		group.setSkill(skill("Java"));
		group.setCreator(owner);
		group.setMaxMembers(10);
		em.persist(group);
		member(group, owner, GroupRole.OWNER, MemberStatus.ACTIVE);
		member(group, member, GroupRole.MEMBER, MemberStatus.ACTIVE);
		member(group, invited, GroupRole.MEMBER, MemberStatus.INVITED);
		em.flush();
		em.clear();

		SkillGroup reloaded = em.find(SkillGroup.class, group.getId());
		assertThat(reloaded.getMemberships()).hasSize(3);
		assertThat(reloaded.getActiveMembers()).extracting(User::getId)
				.containsExactlyInAnyOrder(owner.getId(), member.getId());
	}
}
