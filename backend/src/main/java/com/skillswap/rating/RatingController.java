package com.skillswap.rating;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.rating.dto.RatingDtos.CreateRatingRequest;
import com.skillswap.rating.dto.RatingDtos.RatingSummaryDto;
import com.skillswap.rating.dto.RatingDtos.ReviewDto;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

	private final RatingService ratingService;

	public RatingController(RatingService ratingService) {
		this.ratingService = ratingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ReviewDto> rate(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody CreateRatingRequest request) {
		return ApiResponse.created(ratingService.rate(me.getId(), request), "Thanks for rating your session!");
	}

	@GetMapping("/user/{id}")
	public ApiResponse<RatingSummaryDto> forUser(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(ratingService.forUser(id, page, size));
	}
}
