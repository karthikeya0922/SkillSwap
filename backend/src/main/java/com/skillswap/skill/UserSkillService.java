package com.skillswap.skill;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.skill.dto.SkillDtos.UserSkillDto;
import com.skillswap.skill.dto.SkillDtos.UserSkillRequest;
import com.skillswap.skill.dto.SkillDtos.UserSkillUpdateRequest;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

@Service
public class UserSkillService {

	public static final int MAX_SKILLS_PER_TYPE = 15;

	public record UserSkillsDto(List<UserSkillDto> teach, List<UserSkillDto> learn) {
	}

	/** Published after a skill is added so the match module can tell interested students. */
	public record UserSkillAddedEvent(Long userId, Long skillId, SkillType type) {
	}

	private final UserSkillRepository userSkillRepository;
	private final UserRepository userRepository;
	private final SkillService skillService;
	private final ApplicationEventPublisher events;

	public UserSkillService(UserSkillRepository userSkillRepository, UserRepository userRepository,
			SkillService skillService, ApplicationEventPublisher events) {
		this.userSkillRepository = userSkillRepository;
		this.userRepository = userRepository;
		this.skillService = skillService;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public UserSkillsDto listForUser(Long userId) {
		List<UserSkill> all = userSkillRepository.findAllForUser(userId);
		return new UserSkillsDto(
				all.stream().filter(s -> s.getType() == SkillType.TEACH).map(SkillMapper::toDto).toList(),
				all.stream().filter(s -> s.getType() == SkillType.LEARN).map(SkillMapper::toDto).toList());
	}

	@Transactional
	public UserSkillDto add(Long userId, UserSkillRequest request) {
		Skill skill = skillService.requireActive(request.skillId());
		SkillType opposite = request.type() == SkillType.TEACH ? SkillType.LEARN : SkillType.TEACH;
		if (userSkillRepository.existsByUserIdAndSkillIdAndType(userId, skill.getId(), request.type())) {
			throw new ConflictException(skill.getName() + " is already on your profile.");
		}
		if (userSkillRepository.existsByUserIdAndSkillIdAndType(userId, skill.getId(), opposite)) {
			throw new ConflictException("You already listed " + skill.getName() + " as a skill you "
					+ (opposite == SkillType.TEACH ? "teach" : "want to learn") + ". Remove it there first.");
		}
		if (userSkillRepository.countByUserIdAndType(userId, request.type()) >= MAX_SKILLS_PER_TYPE) {
			throw new BadRequestException("You can list up to " + MAX_SKILLS_PER_TYPE + " skills in each section.");
		}
		User user = userRepository.getReferenceById(userId);
		UserSkill userSkill = new UserSkill();
		userSkill.setUser(user);
		userSkill.setSkill(skill);
		userSkill.setType(request.type());
		apply(userSkill, request.level(), request.yearsExperience(), request.description());
		userSkillRepository.save(userSkill);
		events.publishEvent(new UserSkillAddedEvent(userId, skill.getId(), request.type()));
		return SkillMapper.toDto(userSkill);
	}

	@Transactional
	public UserSkillDto update(Long userId, Long id, UserSkillUpdateRequest request) {
		UserSkill userSkill = owned(userId, id);
		apply(userSkill, request.level(), request.yearsExperience(), request.description());
		return SkillMapper.toDto(userSkill);
	}

	@Transactional
	public void delete(Long userId, Long id) {
		userSkillRepository.delete(owned(userId, id));
	}

	private UserSkill owned(Long userId, Long id) {
		UserSkill userSkill = userSkillRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.of("Profile skill", id));
		if (!userSkill.getUser().getId().equals(userId)) {
			throw new ForbiddenException("You can only change skills on your own profile.");
		}
		return userSkill;
	}

	private static void apply(UserSkill userSkill, ProficiencyLevel level, Double years, String description) {
		userSkill.setLevel(level);
		userSkill.setYearsExperience(userSkill.getType() == SkillType.TEACH ? years : null);
		userSkill.setDescription(description == null || description.isBlank() ? null : description.trim());
	}
}
