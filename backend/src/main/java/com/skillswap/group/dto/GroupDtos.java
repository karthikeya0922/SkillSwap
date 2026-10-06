package com.skillswap.group.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.skillswap.group.GroupEventType;
import com.skillswap.group.GroupPrivacy;
import com.skillswap.group.GroupRole;
import com.skillswap.group.MemberStatus;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.session.SessionMode;
import com.skillswap.user.dto.UserSummaryDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class GroupDtos {

	private GroupDtos() {
	}

	public record GroupDto(Long id, String name, String description, SkillRef skill, UserSummaryDto creator,
			int maxMembers, long memberCount, GroupPrivacy privacy, MemberStatus myStatus, GroupRole myRole,
			LocalDateTime createdAt) {
	}

	public record MemberDto(UserSummaryDto user, GroupRole role, MemberStatus status, LocalDateTime joinedAt) {
	}

	public record EventDto(Long id, Long groupId, String title, String description, GroupEventType type,
			LocalDateTime startTime, LocalDateTime endTime, SessionMode mode, String location, String meetingLink,
			UserSummaryDto createdBy, boolean canDelete) {
	}

	public record PostDto(Long id, Long groupId, UserSummaryDto author, String content, LocalDateTime createdAt,
			boolean canDelete) {
	}

	public record GroupDetailDto(GroupDto group, List<MemberDto> members, List<MemberDto> invited,
			List<EventDto> upcomingEvents, List<EventDto> pastEvents, boolean canManage, boolean canPost) {
	}

	public record InvitationDto(Long groupId, String groupName, SkillRef skill, UserSummaryDto invitedBy,
			LocalDateTime invitedAt) {
	}

	public record UpsertGroupRequest(
			@NotBlank(message = "Group name is required") @Size(min = 3, max = 80, message = "Name must be 3-80 characters") String name,
			@NotBlank(message = "Describe what the group is about") @Size(max = 1000, message = "Description can be at most 1000 characters") String description,
			@NotNull(message = "Choose the skill this group focuses on") Long skillId,
			@NotNull(message = "Set a member limit") @Min(value = 2, message = "Groups need room for at least 2 members") @Max(value = 200, message = "Groups can have at most 200 members") Integer maxMembers,
			@NotNull(message = "Choose public or private") GroupPrivacy privacy) {
	}

	public record InviteRequest(@NotNull(message = "Choose who to invite") Long userId) {
	}

	public record PostRequest(
			@NotBlank(message = "Write something first") @Size(max = 2000, message = "Posts can be at most 2000 characters") String content) {
	}

	public record EventRequest(
			@NotBlank(message = "Event title is required") @Size(max = 120) String title,
			@Size(max = 1000) String description,
			@NotNull(message = "Choose session or event") GroupEventType type,
			@NotNull(message = "Choose a date") LocalDate date,
			@NotNull(message = "Choose a start time") LocalTime startTime,
			@NotNull(message = "Choose an end time") LocalTime endTime,
			@NotNull(message = "Choose online or offline") SessionMode mode,
			@Size(max = 200) String location,
			@Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Meeting link must start with http:// or https://") String meetingLink) {
	}
}
