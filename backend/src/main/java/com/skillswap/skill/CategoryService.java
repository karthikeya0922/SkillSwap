package com.skillswap.skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.skill.dto.SkillDtos.CategoryDto;
import com.skillswap.skill.dto.SkillDtos.CategoryUpsertRequest;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final SkillRepository skillRepository;

	public CategoryService(CategoryRepository categoryRepository, SkillRepository skillRepository) {
		this.categoryRepository = categoryRepository;
		this.skillRepository = skillRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryDto> list() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : categoryRepository.countActiveSkillsByCategory()) {
			counts.put((Long) row[0], (Long) row[1]);
		}
		return categoryRepository.findAllByOrderByNameAsc().stream()
				.map(c -> SkillMapper.toDto(c, counts.getOrDefault(c.getId(), 0L))).toList();
	}

	@Transactional
	public CategoryDto create(CategoryUpsertRequest request) {
		if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
			throw new ConflictException("A category with this name already exists.");
		}
		Category category = new Category();
		apply(category, request);
		return SkillMapper.toDto(categoryRepository.save(category), 0);
	}

	@Transactional
	public CategoryDto update(Long id, CategoryUpsertRequest request) {
		Category category = get(id);
		if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
			throw new ConflictException("A category with this name already exists.");
		}
		apply(category, request);
		return SkillMapper.toDto(category, skillRepository.countByCategoryId(id));
	}

	@Transactional
	public void delete(Long id) {
		Category category = get(id);
		if (skillRepository.countByCategoryId(id) > 0) {
			throw new ConflictException("Move or delete the skills in this category before deleting it.");
		}
		categoryRepository.delete(category);
	}

	Category get(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
	}

	private void apply(Category category, CategoryUpsertRequest request) {
		category.setName(request.name().trim());
		category.setSlug(SkillMapper.slugify(request.name()));
		category.setDescription(request.description());
		category.setIcon(request.icon() == null || request.icon().isBlank() ? "Sparkles" : request.icon());
		category.setColor(request.color() == null || request.color().isBlank() ? "#6366f1" : request.color());
	}
}
