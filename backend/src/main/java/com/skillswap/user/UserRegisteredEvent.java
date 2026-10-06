package com.skillswap.user;

/** Published inside the registration transaction so other modules (wallet) can provision per-user state. */
public record UserRegisteredEvent(Long userId) {
}
