package com.skillswap.skill;

import com.skillswap.skill.dto.SkillDtos.CategoryDto;
import com.skillswap.skill.dto.SkillDtos.SkillDto;
import com.skillswap.skill.dto.SkillDtos.UserSkillDto;

public final class SkillMapper {

	private SkillMapper() {
	}

	public static CategoryDto toDto(Category category, long skillCount) {
		return new CategoryDto(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
				category.getIcon(), category.getColor(), skillCount);
	}

	public static SkillDto toDto(Skill skill, long teacherCount, long learnerCount) {
		Category category = skill.getCategory();
		return new SkillDto(skill.getId(), skill.getName(), skill.getSlug(), skill.getDescription(),
				category.getId(), category.getName(), category.getColor(), category.getIcon(), skill.isActive(),
				teacherCount, learnerCount);
	}

	public static UserSkillDto toDto(UserSkill userSkill) {
		Skill skill = userSkill.getSkill();
		Category category = skill.getCategory();
		return new UserSkillDto(userSkill.getId(), skill.getId(), skill.getName(), category.getId(), category.getName(),
				category.getColor(), userSkill.getType(), userSkill.getLevel(),
				userSkill.getType() == SkillType.TEACH ? userSkill.getYearsExperience() : null,
				userSkill.getDescription());
	}

	public static String slugify(String value) {
		String slug = value.trim().toLowerCase().replace("+", "plus").replace("#", "sharp")
				.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return slug.isEmpty() ? "skill" : slug;
	}
}
