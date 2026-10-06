package com.skillswap.user;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.security.UserPrincipal;
import com.skillswap.user.dto.ProfileDtos.DiscoverFilters;
import com.skillswap.user.dto.ProfileDtos.DiscoverMetaDto;
import com.skillswap.user.dto.ProfileDtos.ProfileDto;
import com.skillswap.user.dto.ProfileDtos.UpdateProfileRequest;
import com.skillswap.user.dto.UserCardDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public ApiResponse<ProfileDto> myProfile(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(userService.getProfile(me, me.getId()));
	}

	@PutMapping("/profile")
	public ApiResponse<ProfileDto> updateProfile(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ApiResponse.ok(userService.updateProfile(me, request), "Profile updated");
	}

	@PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Map<String, String>> uploadAvatar(@AuthenticationPrincipal UserPrincipal me,
			@RequestPart("file") MultipartFile file) {
		return ApiResponse.ok(Map.of("avatarUrl", userService.updateAvatar(me.getId(), file)), "Profile photo updated");
	}

	@DeleteMapping("/profile/avatar")
	public ApiResponse<Void> removeAvatar(@AuthenticationPrincipal UserPrincipal me) {
		userService.removeAvatar(me.getId());
		return ApiResponse.message("Profile photo removed");
	}

	@GetMapping("/discover")
	public ApiResponse<PageResponse<UserCardDto>> discover(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(required = false) String q, @RequestParam(required = false) Long skillId,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String level,
			@RequestParam(required = false) Double minRating, @RequestParam(required = false) String department,
			@RequestParam(required = false) Availability availability,
			@RequestParam(defaultValue = "match") String sort, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		DiscoverFilters filters = new DiscoverFilters(q, skillId, categoryId, level, minRating, department,
				availability, sort);
		return ApiResponse.ok(userService.discover(me.getId(), filters, page, size));
	}

	@GetMapping("/discover/meta")
	public ApiResponse<DiscoverMetaDto> discoverMeta() {
		return ApiResponse.ok(new DiscoverMetaDto(userService.departments()));
	}

	@GetMapping("/{id}")
	public ApiResponse<ProfileDto> profile(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(userService.getProfile(me, id));
	}
}
