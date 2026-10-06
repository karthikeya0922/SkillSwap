package com.skillswap.match;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillRepository;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkillService.UserSkillAddedEvent;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** When someone starts teaching a skill, tells the students who want to learn it. */
@Component
public class MatchNotifier {

	private static final int MAX_NOTIFIED = 25;

	private final UserSkillRepository userSkillRepository;
	private final UserRepository userRepository;
	private final SkillRepository skillRepository;
	private final NotificationService notificationService;

	public MatchNotifier(UserSkillRepository userSkillRepository, UserRepository userRepository,
			SkillRepository skillRepository, NotificationService notificationService) {
		this.userSkillRepository = userSkillRepository;
		this.userRepository = userRepository;
		this.skillRepository = skillRepository;
		this.notificationService = notificationService;
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onSkillAdded(UserSkillAddedEvent event) {
		if (event.type() != SkillType.TEACH) {
			return;
		}
		User teacher = userRepository.findById(event.userId()).orElse(null);
		Skill skill = skillRepository.findById(event.skillId()).orElse(null);
		if (teacher == null || skill == null) {
			return;
		}
		List<Long> learners = userSkillRepository.findUserIdsWithSkills(SkillType.LEARN, List.of(skill.getId()));
		learners.stream().filter(id -> !id.equals(teacher.getId())).limit(MAX_NOTIFIED)
				.forEach(learnerId -> notificationService.notify(learnerId, NotificationType.SKILL_MATCH,
						"New skill match: " + skill.getName(),
						teacher.getFullName() + " can now teach " + skill.getName() + ", a skill you want to learn.",
						"/users/" + teacher.getId(), teacher.getId()));
	}
}
