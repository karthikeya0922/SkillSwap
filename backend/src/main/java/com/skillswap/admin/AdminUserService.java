package com.skillswap.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.admin.dto.AdminDtos.AdminUserDto;
import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.session.SessionService;
import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.UserStatus;

/** Admin user management. Suspension takes effect on the user's very next request. */
@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final SessionService sessionService;

	public AdminUserService(UserRepository userRepository, SessionService sessionService) {
		this.userRepository = userRepository;
		this.sessionService = sessionService;
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminUserDto> search(String q, UserStatus status, Role role, int page, int size) {
		String query = q == null || q.isBlank() ? null : q.trim();
		return PageResponse.from(userRepository.adminSearch(query, status, role,
				PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), AdminUserDto::from);
	}

	@Transactional
	public User suspend(Long adminId, Long userId, String reason) {
		if (adminId.equals(userId)) {
			throw new BadRequestException("You cannot suspend your own account.");
		}
		User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
		if (user.isAdmin()) {
			throw new BadRequestException("Administrators cannot be suspended.");
		}
		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new ConflictException("This user is already suspended.");
		}
		user.setStatus(UserStatus.SUSPENDED);
		user.setSuspensionReason(reason == null || reason.isBlank() ? null : reason.trim());
		sessionService.cancelFutureSessionsOf(userId, "Cancelled because the other participant's account was suspended.");
		return user;
	}

	@Transactional
	public User reactivate(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
		if (user.getStatus() == UserStatus.ACTIVE) {
			throw new ConflictException("This user is already active.");
		}
		user.setStatus(UserStatus.ACTIVE);
		user.setSuspensionReason(null);
		return user;
	}
}
