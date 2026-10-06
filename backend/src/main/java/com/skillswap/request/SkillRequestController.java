package com.skillswap.request;

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

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.request.dto.RequestDtos.MyOfferDto;
import com.skillswap.request.dto.RequestDtos.OfferDto;
import com.skillswap.request.dto.RequestDtos.OfferRequest;
import com.skillswap.request.dto.RequestDtos.RequestDetailDto;
import com.skillswap.request.dto.RequestDtos.RequestDto;
import com.skillswap.request.dto.RequestDtos.UpsertRequest;
import com.skillswap.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class SkillRequestController {

	private final SkillRequestService service;

	public SkillRequestController(SkillRequestService service) {
		this.service = service;
	}

	/** scope=browse (others' requests, default) or scope=mine. */
	@GetMapping
	public ApiResponse<PageResponse<RequestDto>> list(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "browse") String scope, @RequestParam(required = false) RequestStatus status,
			@RequestParam(required = false) Long skillId, @RequestParam(defaultValue = "true") boolean teachableOnly,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		if ("mine".equalsIgnoreCase(scope)) {
			return ApiResponse.ok(service.mine(me.getId(), status, page, size));
		}
		return ApiResponse.ok(service.browse(me.getId(), skillId, teachableOnly, q, page, size));
	}

	@GetMapping("/offers/mine")
	public ApiResponse<PageResponse<MyOfferDto>> myOffers(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(service.myOffers(me.getId(), page, size));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RequestDto> create(@AuthenticationPrincipal UserPrincipal me,
			@Valid @RequestBody UpsertRequest body) {
		return ApiResponse.created(service.create(me.getId(), body), "Learning request posted");
	}

	@GetMapping("/{id}")
	public ApiResponse<RequestDetailDto> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.get(me.getId(), id));
	}

	@PutMapping("/{id}")
	public ApiResponse<RequestDto> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody UpsertRequest body) {
		return ApiResponse.ok(service.update(me.getId(), id, body), "Request updated");
	}

	@PostMapping("/{id}/cancel")
	public ApiResponse<RequestDto> cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.cancel(me.getId(), id), "Request cancelled");
	}

	@PostMapping("/{id}/complete")
	public ApiResponse<RequestDto> complete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		return ApiResponse.ok(service.complete(me.getId(), id), "Request marked as completed");
	}

	@PostMapping("/{id}/offers")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<OfferDto> offer(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@Valid @RequestBody OfferRequest body) {
		return ApiResponse.created(service.offer(me.getId(), id, body.message()), "Your offer was sent");
	}

	@DeleteMapping("/{id}/offers/mine")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
		service.withdrawOffer(me.getId(), id);
		return ApiResponse.message("Offer withdrawn");
	}

	@PostMapping("/{id}/offers/{offerId}/accept")
	public ApiResponse<RequestDetailDto> accept(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id,
			@PathVariable Long offerId) {
		return ApiResponse.ok(service.acceptOffer(me.getId(), id, offerId), "Offer accepted — now book a session");
	}
}
