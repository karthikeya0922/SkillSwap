package com.skillswap.group;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.group.dto.GroupDtos.EventDto;
import com.skillswap.group.dto.GroupDtos.EventRequest;
import com.skillswap.group.dto.GroupDtos.GroupDetailDto;
import com.skillswap.group.dto.GroupDtos.GroupDto;
import com.skillswap.group.dto.GroupDtos.InvitationDto;
import com.skillswap.group.dto.GroupDtos.MemberDto;
import com.skillswap.group.dto.GroupDtos.PostDto;
import com.skillswap.group.dto.GroupDtos.UpsertGroupRequest;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.reputation.ReputationService;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.security.UserPrincipal;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillService;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

@Service
public class GroupService {

	private final SkillGroupRepository groupRepository;
	private final GroupMemberRepository memberRepository;
	private final GroupPostRepository postRepository;
	private final GroupEventRepository eventRepository;
	private final UserRepository userRepository;
	private final SkillService skillService;
	private final NotificationService notificationService;
	private final ReputationService reputationService;

	public GroupService(SkillGroupRepository groupRepository, GroupMemberRepository memberRepository,
			GroupPostRepository postRepository, GroupEventRepository eventRepository, UserRepository userRepository,
			SkillService skillService, NotificationService notificationService, ReputationService reputationService) {
		this.groupRepository = groupRepository;
		this.memberRepository = memberRepository;
		this.postRepository = postRepository;
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.skillService = skillService;
		this.notificationService = notificationService;
		this.reputationService = reputationService;
	}

	@Transactional(readOnly = true)
	public PageResponse<GroupDto> search(Long meId, String q, Long skillId, boolean mine, int page, int size) {
		Page<SkillGroup> groups = groupRepository.search(meId, q == null || q.isBlank() ? null : q.trim(), skillId,
				mine, PageRequests.of(page, size));
		List<Long> ids = groups.getContent().stream().map(SkillGroup::getId).toList();
		Map<Long, Long> counts = new HashMap<>();
		Map<Long, GroupMember> memberships = new HashMap<>();
		if (!ids.isEmpty()) {
			memberRepository.countActiveByGroups(ids).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
			memberRepository.findForUserInGroups(meId, ids).forEach(m -> memberships.put(m.getGroup().getId(), m));
		}
		return PageResponse.from(groups,
				g -> toDto(g, counts.getOrDefault(g.getId(), 0L), memberships.get(g.getId())));
	}

	@Transactional
	public GroupDto create(Long meId, UpsertGroupRequest request) {
		User me = userRepository.findById(meId).orElseThrow();
		SkillGroup group = new SkillGroup();
		group.setCreator(me);
		apply(group, request);
		groupRepository.save(group);
		GroupMember owner = new GroupMember();
		owner.setGroup(group);
		owner.setUser(me);
		owner.setRole(GroupRole.OWNER);
		owner.setStatus(MemberStatus.ACTIVE);
		memberRepository.save(owner);
		reputationService.addXp(meId, ReputationService.XP_CREATE_GROUP);
		reputationService.evaluateBadges(meId);
		return toDto(group, 1, owner);
	}

	@Transactional
	public GroupDto update(Long meId, Long id, UpsertGroupRequest request) {
		SkillGroup group = find(id);
		requireOwner(meId, group);
		long members = memberRepository.countByGroupIdAndStatus(id, MemberStatus.ACTIVE);
		if (request.maxMembers() < members) {
			throw new BadRequestException("The group already has " + members + " members.");
		}
		apply(group, request);
		return toDto(group, members, membership(id, meId));
	}

	@Transactional
	public void delete(UserPrincipal me, Long id) {
		SkillGroup group = find(id);
		if (!me.isAdmin()) {
			requireOwner(me.getId(), group);
		}
		deleteCascade(group);
	}

	/** Removes a group and everything that belongs to it (also used by moderation). */
	@Transactional
	public void deleteCascade(SkillGroup group) {
		postRepository.deleteByGroup(group.getId());
		eventRepository.deleteByGroup(group.getId());
		memberRepository.deleteByGroup(group.getId());
		groupRepository.delete(group);
	}

	@Transactional(readOnly = true)
	public GroupDetailDto detail(UserPrincipal me, Long id) {
		SkillGroup group = find(id);
		GroupMember membership = membership(id, me.getId());
		requireVisible(group, membership, me);
		boolean active = membership != null && membership.getStatus() == MemberStatus.ACTIVE;
		boolean owner = active && membership.getRole() == GroupRole.OWNER;
		List<MemberDto> members = memberRepository.findMembers(id, MemberStatus.ACTIVE).stream()
				.map(GroupService::toDto).toList();
		List<MemberDto> invited = owner || me.isAdmin()
				? memberRepository.findMembers(id, MemberStatus.INVITED).stream().map(GroupService::toDto).toList()
				: List.of();
		LocalDateTime now = LocalDateTime.now();
		List<EventDto> upcoming = eventRepository.findUpcoming(id, now).stream()
				.map(e -> toDto(e, me.getId(), owner || me.isAdmin())).toList();
		List<EventDto> past = eventRepository.findPast(id, now, PageRequest.of(0, 10)).stream()
				.map(e -> toDto(e, me.getId(), owner || me.isAdmin())).toList();
		return new GroupDetailDto(toDto(group, members.size(), membership), members, invited, upcoming, past,
				owner || me.isAdmin(), active);
	}

	@Transactional
	public GroupDto join(Long meId, Long id) {
		SkillGroup group = find(id);
		GroupMember membership = membership(id, meId);
		if (membership != null && membership.getStatus() == MemberStatus.ACTIVE) {
			throw new ConflictException("You are already a member of this group.");
		}
		if (group.getPrivacy() == GroupPrivacy.PRIVATE && membership == null) {
			throw new ForbiddenException("This is a private group. You need an invitation to join.");
		}
		long members = memberRepository.countByGroupIdAndStatus(id, MemberStatus.ACTIVE);
		if (members >= group.getMaxMembers()) {
			throw new ConflictException("This group is full.");
		}
		if (membership == null) {
			membership = new GroupMember();
			membership.setGroup(group);
			membership.setUser(userRepository.getReferenceById(meId));
			membership.setRole(GroupRole.MEMBER);
		}
		membership.setStatus(MemberStatus.ACTIVE);
		memberRepository.save(membership);
		return toDto(group, members + 1, membership);
	}

	@Transactional
	public void leave(Long meId, Long id) {
		GroupMember membership = membership(id, meId);
		if (membership == null) {
			throw new BadRequestException("You are not a member of this group.");
		}
		if (membership.getRole() == GroupRole.OWNER) {
			throw new BadRequestException("Owners cannot leave their group. Delete the group instead.");
		}
		memberRepository.delete(membership);
	}

	@Transactional
	public void invite(Long meId, Long id, Long inviteeId) {
		SkillGroup group = find(id);
		GroupMember mine = membership(id, meId);
		if (mine == null || mine.getStatus() != MemberStatus.ACTIVE) {
			throw new ForbiddenException("Only group members can invite others.");
		}
		if (group.getPrivacy() == GroupPrivacy.PRIVATE && mine.getRole() != GroupRole.OWNER) {
			throw new ForbiddenException("Only the owner can invite people to a private group.");
		}
		User invitee = userRepository.findById(inviteeId)
				.filter(u -> u.isActive() && !u.isAdmin())
				.orElseThrow(() -> ResourceNotFoundException.of("User", inviteeId));
		if (membership(id, inviteeId) != null) {
			throw new ConflictException(invitee.getFullName() + " is already a member or has been invited.");
		}
		GroupMember invitation = new GroupMember();
		invitation.setGroup(group);
		invitation.setUser(invitee);
		invitation.setRole(GroupRole.MEMBER);
		invitation.setStatus(MemberStatus.INVITED);
		invitation.setInvitedBy(mine.getUser());
		memberRepository.save(invitation);
		notificationService.notify(inviteeId, NotificationType.GROUP_INVITATION, "Group invitation",
				mine.getUser().getFullName() + " invited you to join \"" + group.getName() + "\".", "/groups/" + id,
				meId);
	}

	@Transactional
	public void declineInvitation(Long meId, Long id) {
		GroupMember membership = membership(id, meId);
		if (membership == null || membership.getStatus() != MemberStatus.INVITED) {
			throw new BadRequestException("You have no pending invitation to this group.");
		}
		memberRepository.delete(membership);
	}

	@Transactional(readOnly = true)
	public List<InvitationDto> invitations(Long meId) {
		return memberRepository.findInvitations(meId).stream().map(m -> new InvitationDto(m.getGroup().getId(),
				m.getGroup().getName(), skillRef(m.getGroup().getSkill()), UserSummaryDto.from(m.getInvitedBy()),
				m.getCreatedAt())).toList();
	}

	@Transactional
	public void removeMember(Long meId, Long id, Long userId) {
		SkillGroup group = find(id);
		requireOwner(meId, group);
		if (meId.equals(userId)) {
			throw new BadRequestException("You cannot remove yourself from your own group.");
		}
		GroupMember membership = membership(id, userId);
		if (membership == null) {
			throw new ResourceNotFoundException("That user is not in this group.");
		}
		memberRepository.delete(membership);
	}

	@Transactional(readOnly = true)
	public PageResponse<PostDto> posts(UserPrincipal me, Long id, int page, int size) {
		SkillGroup group = find(id);
		GroupMember membership = membership(id, me.getId());
		requireVisible(group, membership, me);
		boolean owner = membership != null && membership.getRole() == GroupRole.OWNER;
		return PageResponse.from(postRepository.findForGroup(id, PageRequests.of(page, size)),
				p -> toDto(p, me.getId(), owner || me.isAdmin()));
	}

	@Transactional
	public PostDto addPost(Long meId, Long id, String content) {
		SkillGroup group = find(id);
		GroupMember membership = requireActiveMember(meId, id);
		GroupPost post = new GroupPost();
		post.setGroup(group);
		post.setAuthor(membership.getUser());
		post.setContent(content.trim());
		postRepository.save(post);
		return toDto(post, meId, membership.getRole() == GroupRole.OWNER);
	}

	@Transactional
	public void deletePost(UserPrincipal me, Long id, Long postId) {
		GroupPost post = postRepository.findById(postId).filter(p -> p.getGroup().getId().equals(id))
				.orElseThrow(() -> ResourceNotFoundException.of("Post", postId));
		GroupMember membership = membership(id, me.getId());
		boolean owner = membership != null && membership.getRole() == GroupRole.OWNER;
		if (!post.getAuthor().getId().equals(me.getId()) && !owner && !me.isAdmin()) {
			throw new ForbiddenException("You can only delete your own posts.");
		}
		postRepository.delete(post);
	}

	@Transactional
	public EventDto addEvent(Long meId, Long id, EventRequest request) {
		SkillGroup group = find(id);
		GroupMember membership = requireActiveMember(meId, id);
		LocalDateTime start = request.date().atTime(request.startTime());
		LocalDateTime end = request.date().atTime(request.endTime());
		if (!start.isAfter(LocalDateTime.now())) {
			throw new BadRequestException("Choose a start time in the future.");
		}
		if (!end.isAfter(start)) {
			throw new BadRequestException("The end time must be after the start time.");
		}
		GroupEvent event = new GroupEvent();
		event.setGroup(group);
		event.setCreatedBy(membership.getUser());
		event.setTitle(request.title().trim());
		event.setDescription(request.description() == null || request.description().isBlank() ? null
				: request.description().trim());
		event.setType(request.type());
		event.setStartTime(start);
		event.setEndTime(end);
		event.setMode(request.mode());
		event.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().trim());
		event.setMeetingLink(request.meetingLink() == null || request.meetingLink().isBlank() ? null
				: request.meetingLink().trim());
		eventRepository.save(event);
		String kind = request.type() == GroupEventType.SESSION ? "group session" : "event";
		for (Long memberId : memberRepository.findActiveMemberIds(id)) {
			if (!memberId.equals(meId)) {
				notificationService.notify(memberId, NotificationType.GROUP_EVENT, "New " + kind + " in " + group.getName(),
						event.getTitle() + " — " + start.toLocalDate() + " at " + start.toLocalTime(), "/groups/" + id,
						meId);
			}
		}
		return toDto(event, meId, membership.getRole() == GroupRole.OWNER);
	}

	@Transactional
	public void deleteEvent(UserPrincipal me, Long id, Long eventId) {
		GroupEvent event = eventRepository.findById(eventId).filter(e -> e.getGroup().getId().equals(id))
				.orElseThrow(() -> ResourceNotFoundException.of("Event", eventId));
		GroupMember membership = membership(id, me.getId());
		boolean owner = membership != null && membership.getRole() == GroupRole.OWNER;
		if (!event.getCreatedBy().getId().equals(me.getId()) && !owner && !me.isAdmin()) {
			throw new ForbiddenException("Only the organiser or group owner can delete this event.");
		}
		eventRepository.delete(event);
	}

	public SkillGroup find(Long id) {
		return groupRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Group", id));
	}

	private GroupMember membership(Long groupId, Long userId) {
		return memberRepository.findByGroupIdAndUserId(groupId, userId).orElse(null);
	}

	private GroupMember requireActiveMember(Long meId, Long groupId) {
		GroupMember membership = membership(groupId, meId);
		if (membership == null || membership.getStatus() != MemberStatus.ACTIVE) {
			throw new ForbiddenException("Join the group to take part.");
		}
		return membership;
	}

	private void requireOwner(Long meId, SkillGroup group) {
		GroupMember membership = membership(group.getId(), meId);
		if (membership == null || membership.getRole() != GroupRole.OWNER) {
			throw new ForbiddenException("Only the group owner can do this.");
		}
	}

	private static void requireVisible(SkillGroup group, GroupMember membership, UserPrincipal me) {
		if (group.getPrivacy() == GroupPrivacy.PRIVATE && membership == null && !me.isAdmin()) {
			throw new ForbiddenException("This is a private group.");
		}
	}

	private void apply(SkillGroup group, UpsertGroupRequest request) {
		Skill skill = skillService.requireActive(request.skillId());
		group.setName(request.name().trim());
		group.setDescription(request.description().trim());
		group.setSkill(skill);
		group.setMaxMembers(request.maxMembers());
		group.setPrivacy(request.privacy());
	}

	static SkillRef skillRef(Skill skill) {
		return new SkillRef(skill.getId(), skill.getName(), skill.getCategory().getName(),
				skill.getCategory().getColor());
	}

	static GroupDto toDto(SkillGroup g, long memberCount, GroupMember membership) {
		return new GroupDto(g.getId(), g.getName(), g.getDescription(), skillRef(g.getSkill()),
				UserSummaryDto.from(g.getCreator()), g.getMaxMembers(), memberCount, g.getPrivacy(),
				membership == null ? null : membership.getStatus(), membership == null ? null : membership.getRole(),
				g.getCreatedAt());
	}

	static MemberDto toDto(GroupMember m) {
		return new MemberDto(UserSummaryDto.from(m.getUser()), m.getRole(), m.getStatus(), m.getCreatedAt());
	}

	static EventDto toDto(GroupEvent e, Long meId, boolean canManage) {
		return new EventDto(e.getId(), e.getGroup().getId(), e.getTitle(), e.getDescription(), e.getType(),
				e.getStartTime(), e.getEndTime(), e.getMode(), e.getLocation(), e.getMeetingLink(),
				UserSummaryDto.from(e.getCreatedBy()), canManage || e.getCreatedBy().getId().equals(meId));
	}

	static PostDto toDto(GroupPost p, Long meId, boolean canManage) {
		return new PostDto(p.getId(), p.getGroup().getId(), UserSummaryDto.from(p.getAuthor()), p.getContent(),
				p.getCreatedAt(), canManage || p.getAuthor().getId().equals(meId));
	}
}
