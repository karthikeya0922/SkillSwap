package com.skillswap.group;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.skillswap.common.api.PageResponse;
import com.skillswap.group.dto.GroupDtos.EventDto;
import com.skillswap.group.dto.GroupDtos.EventRequest;
import com.skillswap.group.dto.GroupDtos.GroupDetailDto;
import com.skillswap.group.dto.GroupDtos.GroupDto;
import com.skillswap.group.dto.GroupDtos.InvitationDto;
import com.skillswap.group.dto.GroupDtos.InviteRequest;
import com.skillswap.group.dto.GroupDtos.PostDto;
import com.skillswap.group.dto.GroupDtos.PostRequest;
import com.skillswap.group.dto.GroupDtos.UpsertGroupRequest;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

	private final GroupService groupService;

	public GroupController(GroupService groupService) {
		this.groupService = groupService;
	}

	@GetMapping
	public ApiResponse<PageResponse<GroupDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(required = false) String q, @RequestParam(required = false) Long skillId,
			@RequestParam(defaultValue = "false") boolean mine, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(groupService.search(me.getId(), q, skillId, mine, page, size));
	}

	@GetMapping("/invitations")
	public ApiResponse<List<InvitationDto>> invitations(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(groupService.invitations(me.getId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<GroupDto> create(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody UpsertGroupRequest request) {
		return ApiResponse.created(groupService.create(me.getId(), request), "Group created");
	}

	@GetMapping("/{id}")
	public ApiResponse<GroupDetailDto> detail(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(groupService.detail(me, id));
	}

	@PutMapping("/{id}")
	public ApiResponse<GroupDto> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody UpsertGroupRequest request) {
		return ApiResponse.ok(groupService.update(me.getId(), id, request), "Group updated");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		groupService.delete(me, id);
		return ApiResponse.message("Group deleted");
	}

	@PostMapping("/{id}/join")
	public ApiResponse<GroupDto> join(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(groupService.join(me.getId(), id), "Welcome to the group!");
	}

	@PostMapping("/{id}/leave")
	public ApiResponse<Void> leave(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		groupService.leave(me.getId(), id);
		return ApiResponse.message("You left the group");
	}

	@PostMapping("/{id}/invitations")
	public ApiResponse<Void> invite(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody InviteRequest request) {
		groupService.invite(me.getId(), id, request.userId());
		return ApiResponse.message("Invitation sent");
	}

	@DeleteMapping("/{id}/invitations/mine")
	public ApiResponse<Void> decline(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		groupService.declineInvitation(me.getId(), id);
		return ApiResponse.message("Invitation declined");
	}

	@DeleteMapping("/{id}/members/{userId}")
	public ApiResponse<Void> removeMember(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@PathVariable Long userId) {
		groupService.removeMember(me.getId(), id, userId);
		return ApiResponse.message("Member removed");
	}

	@GetMapping("/{id}/posts")
	public ApiResponse<PageResponse<PostDto>> posts(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(groupService.posts(me, id, page, size));
	}

	@PostMapping("/{id}/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PostDto> addPost(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody PostRequest request) {
		return ApiResponse.created(groupService.addPost(me.getId(), id, request.content()), "Posted");
	}

	@DeleteMapping("/{id}/posts/{postId}")
	public ApiResponse<Void> deletePost(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@PathVariable Long postId) {
		groupService.deletePost(me, id, postId);
		return ApiResponse.message("Post deleted");
	}

	@PostMapping("/{id}/events")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<EventDto> addEvent(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody EventRequest request) {
		return ApiResponse.created(groupService.addEvent(me.getId(), id, request), "Event scheduled");
	}

	@DeleteMapping("/{id}/events/{eventId}")
	public ApiResponse<Void> deleteEvent(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@PathVariable Long eventId) {
		groupService.deleteEvent(me, id, eventId);
		return ApiResponse.message("Event deleted");
	}
}
