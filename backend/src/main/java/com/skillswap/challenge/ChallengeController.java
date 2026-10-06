package com.skillswap.challenge;

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

import com.skillswap.challenge.dto.ChallengeDtos.ChallengeDto;
import com.skillswap.challenge.dto.ChallengeDtos.ReviewDto;
import com.skillswap.challenge.dto.ChallengeDtos.ReviewRequest;
import com.skillswap.challenge.dto.ChallengeDtos.SubmissionDto;
import com.skillswap.challenge.dto.ChallengeDtos.SubmissionRequest;
import com.skillswap.challenge.dto.ChallengeDtos.UpsertChallengeRequest;
import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

	private final ChallengeService challengeService;

	public ChallengeController(ChallengeService challengeService) {
		this.challengeService = challengeService;
	}

	@GetMapping
	public ApiResponse<PageResponse<ChallengeDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "active") String status, @RequestParam(required = false) Long skillId,
			@RequestParam(required = false) Difficulty difficulty, @RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(challengeService.search(me.getId(), !"past".equalsIgnoreCase(status), skillId,
				difficulty, q, page, size));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ChallengeDto> create(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody UpsertChallengeRequest request) {
		return ApiResponse.created(challengeService.create(me.getId(), request), "Challenge published");
	}

	@GetMapping("/{id}")
	public ApiResponse<ChallengeDto> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(challengeService.get(me.getId(), id));
	}

	@PutMapping("/{id}")
	public ApiResponse<ChallengeDto> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody UpsertChallengeRequest request) {
		return ApiResponse.ok(challengeService.update(me, id, request), "Challenge updated");
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		challengeService.delete(me, id);
		return ApiResponse.message("Challenge deleted");
	}

	@GetMapping("/{id}/submissions")
	public ApiResponse<List<SubmissionDto>> submissions(@AuthenticationPrincipal UserPrincipal me,
			@PathVariable Long id) {
		return ApiResponse.ok(challengeService.submissions(me.getId(), id));
	}

	@PostMapping("/{id}/submissions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SubmissionDto> submit(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody SubmissionRequest request) {
		return ApiResponse.created(challengeService.submit(me.getId(), id, request), "Submission received — nice work!");
	}

	@PutMapping("/{id}/submissions/mine")
	public ApiResponse<SubmissionDto> updateSubmission(@AuthenticationPrincipal UserPrincipal me,
			@PathVariable Long id, @Valid @RequestBody SubmissionRequest request) {
		return ApiResponse.ok(challengeService.updateSubmission(me.getId(), id, request), "Submission updated");
	}

	@GetMapping("/submissions/{submissionId}/reviews")
	public ApiResponse<List<ReviewDto>> reviews(@PathVariable Long submissionId) {
		return ApiResponse.ok(challengeService.reviews(submissionId));
	}

	@PostMapping("/submissions/{submissionId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ReviewDto> review(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long submissionId,
			@Valid @RequestBody ReviewRequest request) {
		return ApiResponse.created(challengeService.review(me.getId(), submissionId, request), "Review posted");
	}
}
