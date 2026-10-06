package com.skillswap.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.user.UserRepository;

/** Backs Spring Security with the users table (and stops Boot from creating a default in-memory user). */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return userRepository.findByEmailIgnoreCase(email).map(UserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
	}
}
