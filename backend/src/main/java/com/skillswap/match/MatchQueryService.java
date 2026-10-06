package com.skillswap.match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.match.MatchCalculator.MatchResult;
import com.skillswap.match.MatchService.ScoredUser;
import com.skillswap.match.dto.MatchDto;
import com.skillswap.user.User;
import com.skillswap.user.UserCardAssembler;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserCardDto;

/** Read-side composition of match results into student cards. */
@Service
public class MatchQueryService {

	private final MatchService matchService;
	private final UserCardAssembler cardAssembler;
	private final UserRepository userRepository;

	public MatchQueryService(MatchService matchService, UserCardAssembler cardAssembler,
			UserRepository userRepository) {
		this.matchService = matchService;
		this.cardAssembler = cardAssembler;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<MatchDto> list(Long meId, int minScore, boolean mutualOnly, int page, int size) {
		List<ScoredUser> all = matchService.findMatches(meId).stream()
				.filter(s -> s.result().score() >= minScore)
				.filter(s -> !mutualOnly || s.result().mutual())
				.toList();
		PageResponse<ScoredUser> slice = PageResponse.of(all, page, Math.min(50, size));
		return new PageResponse<>(toDtos(meId, slice.content()), slice.page(), slice.size(), slice.totalElements(),
				slice.totalPages(), slice.last());
	}

	@Transactional(readOnly = true)
	public List<MatchDto> recommended(Long meId, int limit) {
		List<ScoredUser> top = matchService.findMatches(meId).stream().limit(Math.min(10, Math.max(1, limit)))
				.toList();
		return toDtos(meId, top);
	}

	@Transactional(readOnly = true)
	public MatchDto with(Long meId, Long otherId) {
		User other = userRepository.findById(otherId).filter(User::isActive)
				.orElseThrow(() -> ResourceNotFoundException.of("User", otherId));
		MatchResult result = matchService.scoreAgainst(meId, List.of(other)).get(otherId);
		UserCardDto card = cardAssembler.build(meId, List.of(other), result == null ? Map.of() : Map.of(otherId, result))
				.get(0);
		return new MatchDto(card, result);
	}

	private List<MatchDto> toDtos(Long meId, List<ScoredUser> scored) {
		List<User> users = scored.stream().map(ScoredUser::user).toList();
		Map<Long, MatchResult> results = new HashMap<>();
		scored.forEach(s -> results.put(s.user().getId(), s.result()));
		List<UserCardDto> cards = cardAssembler.build(meId, users, results);
		return cards.stream().map(card -> new MatchDto(card, results.get(card.id()))).toList();
	}
}
