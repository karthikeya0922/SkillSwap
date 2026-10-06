package com.skillswap.report;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.challenge.Challenge;
import com.skillswap.challenge.ChallengeService;
import com.skillswap.challenge.Submission;
import com.skillswap.chat.Message;
import com.skillswap.chat.MessageRepository;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.group.GroupPost;
import com.skillswap.group.GroupPostRepository;
import com.skillswap.group.GroupService;
import com.skillswap.group.SkillGroup;
import com.skillswap.request.RequestStatus;
import com.skillswap.request.SkillRequest;
import com.skillswap.request.SkillRequestRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** Resolves reportable content to its owner and removes it on an admin's decision. */
@Service
public class ModerationService {

	public record TargetInfo(User owner, String preview) {
	}

	private static final String REMOVED = "[Removed by a moderator]";

	private final UserRepository userRepository;
	private final MessageRepository messageRepository;
	private final GroupPostRepository postRepository;
	private final SkillRequestRepository requestRepository;
	private final GroupService groupService;
	private final ChallengeService challengeService;

	public ModerationService(UserRepository userRepository, MessageRepository messageRepository,
			GroupPostRepository postRepository, SkillRequestRepository requestRepository, GroupService groupService,
			ChallengeService challengeService) {
		this.userRepository = userRepository;
		this.messageRepository = messageRepository;
		this.postRepository = postRepository;
		this.requestRepository = requestRepository;
		this.groupService = groupService;
		this.challengeService = challengeService;
	}

	/**
	 * Looks up a report target. {@code reporterId} is used to stop users reporting private messages they were not
	 * part of.
	 */
	@Transactional(readOnly = true)
	public TargetInfo resolve(ReportTargetType type, Long id, Long reporterId) {
		return switch (type) {
			case USER -> {
				User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
				yield new TargetInfo(user, user.getFullName() + " (" + user.getDepartment() + ")");
			}
			case MESSAGE -> {
				Message message = messageRepository.findById(id)
						.orElseThrow(() -> ResourceNotFoundException.of("Message", id));
				if (reporterId != null && !message.getRecipient().getId().equals(reporterId)
						&& !message.getSender().getId().equals(reporterId)) {
					throw new ForbiddenException("You can only report messages from your own conversations.");
				}
				yield new TargetInfo(message.getSender(), message.getContent());
			}
			case GROUP_POST -> {
				GroupPost post = postRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Post", id));
				yield new TargetInfo(post.getAuthor(), post.getContent());
			}
			case SKILL_REQUEST -> {
				SkillRequest request = requestRepository.findById(id)
						.orElseThrow(() -> ResourceNotFoundException.of("Learning request", id));
				yield new TargetInfo(request.getLearner(), request.getTitle() + " — " + request.getDescription());
			}
			case SUBMISSION -> {
				Submission submission = challengeService.findSubmission(id);
				yield new TargetInfo(submission.getUser(),
						submission.getProjectTitle() + " — " + submission.getDescription());
			}
			case GROUP -> {
				SkillGroup group = groupService.find(id);
				yield new TargetInfo(group.getCreator(), group.getName() + " — " + group.getDescription());
			}
			case CHALLENGE -> {
				Challenge challenge = challengeService.find(id);
				yield new TargetInfo(challenge.getCreator(), challenge.getTitle());
			}
		};
	}

	@Transactional
	public void remove(ReportTargetType type, Long id) {
		switch (type) {
			case USER -> throw new BadRequestException("Users are suspended, not removed. Choose 'Suspend user'.");
			case MESSAGE -> messageRepository.findById(id).ifPresent(m -> {
				m.setRemoved(true);
				m.setContent(REMOVED);
			});
			case GROUP_POST -> postRepository.findById(id).ifPresent(postRepository::delete);
			case SKILL_REQUEST -> requestRepository.findById(id).ifPresent(r -> {
				r.setStatus(RequestStatus.CANCELLED);
				r.setTitle(REMOVED);
				r.setDescription("This request was removed for breaking the community guidelines.");
			});
			case SUBMISSION -> challengeService.deleteSubmission(challengeService.findSubmission(id));
			case GROUP -> groupService.deleteCascade(groupService.find(id));
			case CHALLENGE -> challengeService.deleteCascade(challengeService.find(id));
		}
	}
}
