package com.skillswap.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserStatus;

/** Authenticated user as seen by Spring Security. Never carries the entity itself. */
public final class UserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String passwordHash;
	private final Role role;
	private final UserStatus status;

	private UserPrincipal(Long id, String email, String passwordHash, Role role, UserStatus status) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}

	public static UserPrincipal from(User user) {
		return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(),
				user.getStatus());
	}

	public Long getId() {
		return id;
	}

	public Role getRole() {
		return role;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	/** Used as the STOMP user-destination key, so it must be stable for the lifetime of the account. */
	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status == UserStatus.ACTIVE;
	}
}
