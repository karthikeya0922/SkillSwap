package com.skillswap.skill;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.security.UserPrincipal;
import com.skillswap.skill.UserSkillService.UserSkillsDto;
import com.skillswap.skill.dto.SkillDtos.CategoryDto;
import com.skillswap.skill.dto.SkillDtos.CategoryUpsertRequest;
import com.skillswap.skill.dto.SkillDtos.SkillDto;
import com.skillswap.skill.dto.SkillDtos.SkillUpsertRequest;
import com.skillswap.skill.dto.SkillDtos.UserSkillDto;
import com.skillswap.skill.dto.SkillDtos.UserSkillRequest;
import com.skillswap.skill.dto.SkillDtos.UserSkillUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SkillController {

	private final SkillService skillService;
	private final CategoryService categoryService;
	private final UserSkillService userSkillService;

	public SkillController(SkillService skillService, CategoryService categoryService,
			UserSkillService userSkillService) {
		this.skillService = skillService;
		this.categoryService = categoryService;
		this.userSkillService = userSkillService;
	}

	// ---- Catalogue (public reads, admin writes) ----

	@GetMapping("/categories")
	public ApiResponse<List<CategoryDto>> categories() {
		return ApiResponse.ok(categoryService.list());
	}

	@PostMapping("/categories")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CategoryDto> createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
		return ApiResponse.created(categoryService.create(request), "Category created");
	}

	@PutMapping("/categories/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<CategoryDto> updateCategory(@PathVariable Long id,
			@Valid @RequestBody CategoryUpsertRequest request) {
		return ApiResponse.ok(categoryService.update(id, request), "Category updated");
	}

	@DeleteMapping("/categories/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
		categoryService.delete(id);
		return ApiResponse.message("Category deleted");
	}

	@GetMapping("/skills")
	public ApiResponse<List<SkillDto>> skills(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId) {
		return ApiResponse.ok(skillService.search(q, categoryId));
	}

	@GetMapping("/skills/{id}")
	public ApiResponse<SkillDto> skill(@PathVariable Long id) {
		return ApiResponse.ok(skillService.get(id));
	}

	@PostMapping("/skills")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SkillDto> createSkill(@Valid @RequestBody SkillUpsertRequest request) {
		return ApiResponse.created(skillService.create(request), "Skill created");
	}

	@PutMapping("/skills/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<SkillDto> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillUpsertRequest request) {
		return ApiResponse.ok(skillService.update(id, request), "Skill updated");
	}

	@DeleteMapping("/skills/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<Void> deleteSkill(@PathVariable Long id) {
		boolean deleted = skillService.deleteOrDeactivate(id);
		return ApiResponse.message(deleted ? "Skill deleted" : "Skill is in use, so it was deactivated instead");
	}

	// ---- The signed-in user's profile skills ----

	@GetMapping("/user-skills")
	public ApiResponse<UserSkillsDto> mySkills(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(userSkillService.listForUser(me.getId()));
	}

	@PostMapping("/user-skills")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<UserSkillDto> addSkill(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody UserSkillRequest request) {
		return ApiResponse.created(userSkillService.add(me.getId(), request), "Skill added to your profile");
	}

	@PutMapping("/user-skills/{id}")
	public ApiResponse<UserSkillDto> updateSkill(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody UserSkillUpdateRequest request) {
		return ApiResponse.ok(userSkillService.update(me.getId(), id, request), "Skill updated");
	}

	@DeleteMapping("/user-skills/{id}")
	public ApiResponse<Void> removeSkill(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		userSkillService.delete(me.getId(), id);
		return ApiResponse.message("Skill removed from your profile");
	}
}
