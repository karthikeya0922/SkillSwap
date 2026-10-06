package com.skillswap.skill.dto;

import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.SkillType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response payloads for categories, skills and profile skills. */
public final class SkillDtos {

	private SkillDtos() {
	}

	public record CategoryDto(Long id, String name, String slug, String description, String icon, String color,
			long skillCount) {
	}

	public record SkillDto(Long id, String name, String slug, String description, Long categoryId,
			String categoryName, String categoryColor, String categoryIcon, boolean active, long teacherCount,
			long learnerCount) {
	}

	public record UserSkillDto(Long id, Long skillId, String skillName, Long categoryId, String categoryName,
			String categoryColor, SkillType type, ProficiencyLevel level, Double yearsExperience,
			String description) {
	}

	public record CategoryUpsertRequest(
			@NotBlank(message = "Category name is required") @Size(max = 60) String name,
			@Size(max = 300) String description,
			@Size(max = 40) String icon,
			@Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Colour must be a hex value like #6366f1") String color) {
	}

	public record SkillUpsertRequest(
			@NotBlank(message = "Skill name is required") @Size(max = 80) String name,
			@Size(max = 500) String description,
			@NotNull(message = "Category is required") Long categoryId,
			Boolean active) {
	}

	public record UserSkillRequest(
			@NotNull(message = "Choose a skill") Long skillId,
			@NotNull(message = "Choose whether you teach or want to learn this skill") SkillType type,
			@NotNull(message = "Choose a level") ProficiencyLevel level,
			@DecimalMin(value = "0", message = "Experience cannot be negative") @DecimalMax(value = "50", message = "Experience looks too high") Double yearsExperience,
			@Size(max = 500, message = "Description can be at most 500 characters") String description) {
	}

	public record UserSkillUpdateRequest(
			@NotNull(message = "Choose a level") ProficiencyLevel level,
			@DecimalMin(value = "0", message = "Experience cannot be negative") @DecimalMax(value = "50", message = "Experience looks too high") Double yearsExperience,
			@Size(max = 500, message = "Description can be at most 500 characters") String description) {
	}
}
