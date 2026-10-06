package com.skillswap.match.dto;

import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.user.dto.UserCardDto;

/** A student card plus the full explanation of why they match. */
public record MatchDto(UserCardDto user, MatchResult match) {
}
