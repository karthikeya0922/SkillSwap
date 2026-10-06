package com.skillswap.user.dto;

import com.skillswap.user.User;

/** The signed-in user's own identity, returned on login and by /api/auth/me. */
public record AuthUserDto(Long id, String fullName, String email, String role, String avatarUrl, String college,
		String department, Integer yearOfStudy, boolean profileCompleted) {

	public static AuthUserDto from(User user) {
		return new AuthUserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
				user.getAvatarUrl(), user.getCollege(), user.getDepartment(), user.getYearOfStudy(),
				user.isProfileCompleted());
	}
}
