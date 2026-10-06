package com.skillswap.skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.skill.dto.SkillDtos.SkillDto;
import com.skillswap.skill.dto.SkillDtos.SkillUpsertRequest;

@Service
public class SkillService {

	private final SkillRepository skillRepository;
	private final UserSkillRepository userSkillRepository;
	private final CategoryService categoryService;

	public SkillService(SkillRepository skillRepository, UserSkillRepository userSkillRepository,
			CategoryService categoryService) {
		this.skillRepository = skillRepository;
		this.userSkillRepository = userSkillRepository;
		this.categoryService = categoryService;
	}

	@Transactional(readOnly = true)
	public List<SkillDto> search(String q, Long categoryId) {
		Map<Long, long[]> counts = counts();
		return skillRepository.search(blankToNull(q), categoryId, false).stream().map(s -> toDto(s, counts)).toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<SkillDto> adminSearch(String q, Long categoryId, int page, int size) {
		Map<Long, long[]> counts = counts();
		Page<Skill> skills = skillRepository.adminSearch(blankToNull(q), categoryId,
				PageRequests.of(page, size, Sort.by("name")));
		return PageResponse.from(skills, s -> toDto(s, counts));
	}

	@Transactional(readOnly = true)
	public SkillDto get(Long id) {
		return toDto(getEntity(id), counts());
	}

	@Transactional
	public SkillDto create(SkillUpsertRequest request) {
		if (skillRepository.existsByNameIgnoreCase(request.name().trim())) {
			throw new ConflictException("A skill with this name already exists.");
		}
		Skill skill = new Skill();
		apply(skill, request);
		return SkillMapper.toDto(skillRepository.save(skill), 0, 0);
	}

	@Transactional
	public SkillDto update(Long id, SkillUpsertRequest request) {
		Skill skill = getEntity(id);
		if (skillRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
			throw new ConflictException("A skill with this name already exists.");
		}
		apply(skill, request);
		return toDto(skill, counts());
	}

	/**
	 * Hard-deletes unused skills. Skills referenced by profiles or history are deactivated instead so past sessions
	 * and ratings keep their meaning.
	 *
	 * @return true when the skill was deleted, false when it was deactivated
	 */
	@Transactional
	public boolean deleteOrDeactivate(Long id) {
		Skill skill = getEntity(id);
		if (skillRepository.countReferences(id) == 0) {
			skillRepository.delete(skill);
			return true;
		}
		skill.setActive(false);
		return false;
	}

	/** Loads a skill that users may still attach to profiles, sessions and requests. */
	@Transactional(readOnly = true)
	public Skill requireActive(Long id) {
		Skill skill = getEntity(id);
		if (!skill.isActive()) {
			throw new BadRequestException("The skill \"" + skill.getName() + "\" is no longer available.");
		}
		return skill;
	}

	public Skill getEntity(Long id) {
		return skillRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Skill", id));
	}

	private void apply(Skill skill, SkillUpsertRequest request) {
		skill.setName(request.name().trim());
		skill.setSlug(SkillMapper.slugify(request.name()));
		skill.setDescription(request.description());
		skill.setCategory(categoryService.get(request.categoryId()));
		if (request.active() != null) {
			skill.setActive(request.active());
		}
	}

	private Map<Long, long[]> counts() {
		Map<Long, long[]> counts = new HashMap<>();
		for (Object[] row : userSkillRepository.countBySkillAndType()) {
			long[] pair = counts.computeIfAbsent((Long) row[0], k -> new long[2]);
			pair[row[1] == SkillType.TEACH ? 0 : 1] = (Long) row[2];
		}
		return counts;
	}

	private static SkillDto toDto(Skill skill, Map<Long, long[]> counts) {
		long[] pair = counts.getOrDefault(skill.getId(), new long[2]);
		return SkillMapper.toDto(skill, pair[0], pair[1]);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
