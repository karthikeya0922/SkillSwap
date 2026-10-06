package com.skillswap.request;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.common.api.PageRequests;
import com.skillswap.common.api.PageResponse;
import com.skillswap.common.exception.BadRequestException;
import com.skillswap.common.exception.ConflictException;
import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.common.exception.ResourceNotFoundException;
import com.skillswap.notification.NotificationService;
import com.skillswap.notification.NotificationType;
import com.skillswap.reputation.ReputationService;
import com.skillswap.request.dto.RequestDtos.MyOfferDto;
import com.skillswap.request.dto.RequestDtos.OfferDto;
import com.skillswap.request.dto.RequestDtos.RequestDetailDto;
import com.skillswap.request.dto.RequestDtos.RequestDto;
import com.skillswap.request.dto.RequestDtos.SkillRef;
import com.skillswap.request.dto.RequestDtos.UpsertRequest;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillService;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;

/**
 * Learning requests: OPEN (no offers) -> PENDING (teachers offered) -> ACCEPTED (learner picked a teacher) ->
 * COMPLETED (a linked session completed, or the learner closed it). CANCELLED is available until completion.
 */
@Service
public class SkillRequestService {

	private static final int MAX_ACTIVE_REQUESTS = 5;
	private static final Set<RequestStatus> ACTIVE = EnumSet.of(RequestStatus.OPEN, RequestStatus.PENDING,
			RequestStatus.ACCEPTED);

	private final SkillRequestRepository requestRepository;
	private final RequestOfferRepository offerRepository;
	private final UserSkillRepository userSkillRepository;
	private final UserRepository userRepository;
	private final SkillService skillService;
	private final NotificationService notificationService;
	private final ReputationService reputationService;

	public SkillRequestService(SkillRequestRepository requestRepository, RequestOfferRepository offerRepository,
			UserSkillRepository userSkillRepository, UserRepository userRepository, SkillService skillService,
			NotificationService notificationService, ReputationService reputationService) {
		this.requestRepository = requestRepository;
		this.offerRepository = offerRepository;
		this.userSkillRepository = userSkillRepository;
		this.userRepository = userRepository;
		this.skillService = skillService;
		this.notificationService = notificationService;
		this.reputationService = reputationService;
	}

	@Transactional
	public RequestDto create(Long meId, UpsertRequest body) {
		long active = requestRepository.findMine(meId, ACTIVE, PageRequest.of(0, 1)).getTotalElements();
		if (active >= MAX_ACTIVE_REQUESTS) {
			throw new BadRequestException("You can have up to " + MAX_ACTIVE_REQUESTS
					+ " active learning requests. Close or complete one first.");
		}
		SkillRequest request = new SkillRequest();
		request.setLearner(userRepository.getReferenceById(meId));
		request.setStatus(RequestStatus.OPEN);
		apply(request, body);
		requestRepository.save(request);
		return toDto(request, meId, 0, null, true);
	}

	@Transactional
	public RequestDto update(Long meId, Long id, UpsertRequest body) {
		SkillRequest request = owned(meId, id);
		if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.PENDING) {
			throw new ConflictException("Only open requests can be edited.");
		}
		if (!request.getSkill().getId().equals(body.skillId()) && request.getStatus() == RequestStatus.PENDING) {
			throw new ConflictException("Teachers have already offered help, so the skill can no longer change.");
		}
		apply(request, body);
		return toDto(request, meId, offerCount(id), null, true);
	}

	@Transactional
	public RequestDto cancel(Long meId, Long id) {
		SkillRequest request = owned(meId, id);
		if (!ACTIVE.contains(request.getStatus())) {
			throw new ConflictException("This request is already closed.");
		}
		request.setStatus(RequestStatus.CANCELLED);
		for (RequestOffer offer : offerRepository.findForRequest(id)) {
			if (offer.getStatus() == OfferStatus.PENDING) {
				offer.setStatus(OfferStatus.DECLINED);
			}
		}
		return toDto(request, meId, offerCount(id), null, true);
	}

	@Transactional
	public RequestDto complete(Long meId, Long id) {
		SkillRequest request = owned(meId, id);
		if (request.getStatus() != RequestStatus.ACCEPTED) {
			throw new ConflictException("Only accepted requests can be marked as completed.");
		}
		request.setStatus(RequestStatus.COMPLETED);
		return toDto(request, meId, offerCount(id), null, true);
	}

	/** Called when a session linked to this request completes. */
	@Transactional
	public void markCompletedBySession(SkillRequest request) {
		if (request != null && request.getStatus() == RequestStatus.ACCEPTED) {
			request.setStatus(RequestStatus.COMPLETED);
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<RequestDto> mine(Long meId, RequestStatus status, int page, int size) {
		Collection<RequestStatus> statuses = status == null ? EnumSet.allOf(RequestStatus.class) : List.of(status);
		Page<SkillRequest> result = requestRepository.findMine(meId, statuses, PageRequests.of(page, size));
		return toPage(result, meId);
	}

	@Transactional(readOnly = true)
	public List<RequestDto> activeForLearner(Long meId, int limit) {
		List<SkillRequest> requests = requestRepository.findActiveForLearner(meId, ACTIVE, PageRequest.of(0, limit));
		Map<Long, Long> counts = offerCounts(requests.stream().map(SkillRequest::getId).toList());
		return requests.stream().map(r -> toDto(r, meId, counts.getOrDefault(r.getId(), 0L), null, true)).toList();
	}

	/** Requests from other students; by default only for skills the viewer teaches. */
	@Transactional(readOnly = true)
	public PageResponse<RequestDto> browse(Long meId, Long skillId, boolean teachableOnly, String q, int page,
			int size) {
		Set<Long> teachable = teachableSkillIds(meId);
		Collection<Long> skillIds = teachable.isEmpty() ? List.of(-1L) : teachable;
		Page<SkillRequest> result = requestRepository.browse(meId, skillId, teachableOnly, skillIds,
				q == null || q.isBlank() ? null : q.trim(), PageRequests.of(page, size));
		List<Long> ids = result.getContent().stream().map(SkillRequest::getId).toList();
		Map<Long, Long> counts = offerCounts(ids);
		Map<Long, OfferStatus> myOffers = new HashMap<>();
		if (!ids.isEmpty()) {
			offerRepository.findByTeacherAndRequests(meId, ids)
					.forEach(o -> myOffers.put(o.getRequest().getId(), o.getStatus()));
		}
		return PageResponse.from(result, r -> toDto(r, meId, counts.getOrDefault(r.getId(), 0L),
				myOffers.get(r.getId()), teachable.contains(r.getSkill().getId())));
	}

	@Transactional(readOnly = true)
	public RequestDetailDto get(Long meId, Long id) {
		SkillRequest request = find(id);
		boolean mine = request.getLearner().getId().equals(meId);
		List<RequestOffer> offers = offerRepository.findForRequest(id);
		OfferDto myOffer = offers.stream().filter(o -> o.getTeacher().getId().equals(meId)).findFirst()
				.map(SkillRequestService::toDto).orElse(null);
		long visibleOffers = offers.stream().filter(o -> o.getStatus() != OfferStatus.WITHDRAWN).count();
		RequestDto dto = toDto(request, meId, visibleOffers, myOffer == null ? null : myOffer.status(),
				teachableSkillIds(meId).contains(request.getSkill().getId()));
		List<OfferDto> offerDtos = mine
				? offers.stream().filter(o -> o.getStatus() != OfferStatus.WITHDRAWN).map(SkillRequestService::toDto).toList()
				: List.of();
		return new RequestDetailDto(dto, offerDtos, myOffer);
	}

	@Transactional
	public OfferDto offer(Long meId, Long requestId, String message) {
		SkillRequest request = find(requestId);
		if (request.getLearner().getId().equals(meId)) {
			throw new BadRequestException("You cannot offer to teach your own request.");
		}
		if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.PENDING) {
			throw new ConflictException("This request is no longer accepting offers.");
		}
		if (!userSkillRepository.existsByUserIdAndSkillIdAndType(meId, request.getSkill().getId(), SkillType.TEACH)) {
			throw new BadRequestException("Add " + request.getSkill().getName()
					+ " to the skills you teach before offering to help.");
		}
		if (offerRepository.existsByRequestIdAndTeacherId(requestId, meId)) {
			throw new ConflictException("You have already responded to this request.");
		}
		User me = userRepository.getReferenceById(meId);
		RequestOffer offer = new RequestOffer();
		offer.setRequest(request);
		offer.setTeacher(me);
		offer.setMessage(message.trim());
		offer.setStatus(OfferStatus.PENDING);
		offerRepository.save(offer);
		request.setStatus(RequestStatus.PENDING);
		notificationService.notify(request.getLearner().getId(), NotificationType.REQUEST_RESPONSE,
				"Someone can help with " + request.getSkill().getName(),
				me.getFullName() + " offered to help with \"" + request.getTitle() + "\".", "/requests/" + requestId,
				meId);
		return toDto(offer);
	}

	@Transactional
	public void withdrawOffer(Long meId, Long requestId) {
		SkillRequest request = find(requestId);
		RequestOffer offer = offerRepository.findForRequest(requestId).stream()
				.filter(o -> o.getTeacher().getId().equals(meId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("You have not offered to help with this request."));
		if (offer.getStatus() != OfferStatus.PENDING) {
			throw new ConflictException("Only pending offers can be withdrawn.");
		}
		offer.setStatus(OfferStatus.WITHDRAWN);
		boolean anyPending = offerRepository.findForRequest(requestId).stream()
				.anyMatch(o -> o.getStatus() == OfferStatus.PENDING);
		if (!anyPending && request.getStatus() == RequestStatus.PENDING) {
			request.setStatus(RequestStatus.OPEN);
		}
	}

	@Transactional
	public RequestDetailDto acceptOffer(Long meId, Long requestId, Long offerId) {
		SkillRequest request = owned(meId, requestId);
		if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.OPEN) {
			throw new ConflictException("This request already has a teacher.");
		}
		List<RequestOffer> offers = offerRepository.findForRequest(requestId);
		RequestOffer chosen = offers.stream().filter(o -> o.getId().equals(offerId)).findFirst()
				.orElseThrow(() -> ResourceNotFoundException.of("Offer", offerId));
		if (chosen.getStatus() != OfferStatus.PENDING) {
			throw new ConflictException("This offer is no longer available.");
		}
		chosen.setStatus(OfferStatus.ACCEPTED);
		offers.stream().filter(o -> o != chosen && o.getStatus() == OfferStatus.PENDING)
				.forEach(o -> o.setStatus(OfferStatus.DECLINED));
		request.setStatus(RequestStatus.ACCEPTED);
		request.setAcceptedTeacher(chosen.getTeacher());
		Long teacherId = chosen.getTeacher().getId();
		reputationService.addXp(teacherId, ReputationService.XP_HELPING);
		reputationService.evaluateBadges(teacherId);
		notificationService.notify(teacherId, NotificationType.REQUEST_ACCEPTED, "Your offer was accepted",
				request.getLearner().getFullName() + " accepted your offer to teach " + request.getSkill().getName()
						+ ". They will book a session with you next.",
				"/requests/" + requestId, meId);
		return get(meId, requestId);
	}

	@Transactional(readOnly = true)
	public PageResponse<MyOfferDto> myOffers(Long meId, int page, int size) {
		Page<RequestOffer> result = offerRepository.findByTeacher(meId, PageRequests.of(page, size));
		Map<Long, Long> counts = offerCounts(result.getContent().stream().map(o -> o.getRequest().getId()).toList());
		return PageResponse.from(result, o -> new MyOfferDto(toDto(o), toDto(o.getRequest(), meId,
				counts.getOrDefault(o.getRequest().getId(), 0L), o.getStatus(), true)));
	}

	/** Used by session booking to validate a request link. */
	@Transactional(readOnly = true)
	public SkillRequest find(Long id) {
		return requestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Learning request", id));
	}

	private SkillRequest owned(Long meId, Long id) {
		SkillRequest request = find(id);
		if (!request.getLearner().getId().equals(meId)) {
			throw new ForbiddenException("Only the student who posted this request can change it.");
		}
		return request;
	}

	private void apply(SkillRequest request, UpsertRequest body) {
		Skill skill = skillService.requireActive(body.skillId());
		request.setSkill(skill);
		request.setTitle(body.title().trim());
		request.setDescription(body.description().trim());
		request.setDesiredLevel(body.desiredLevel());
		request.setPreferredSchedule(body.preferredSchedule() == null || body.preferredSchedule().isBlank() ? null
				: body.preferredSchedule().trim());
		request.setMode(body.mode());
		request.setDurationHours(body.durationHours());
	}

	private Set<Long> teachableSkillIds(Long meId) {
		return userSkillRepository.findAllForUser(meId).stream().filter(us -> us.getType() == SkillType.TEACH)
				.map(UserSkill::getSkill).map(Skill::getId).collect(Collectors.toSet());
	}

	private long offerCount(Long requestId) {
		return offerCounts(List.of(requestId)).getOrDefault(requestId, 0L);
	}

	private Map<Long, Long> offerCounts(Collection<Long> requestIds) {
		Map<Long, Long> counts = new HashMap<>();
		if (!requestIds.isEmpty()) {
			offerRepository.countByRequests(requestIds).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
		}
		return counts;
	}

	private PageResponse<RequestDto> toPage(Page<SkillRequest> page, Long meId) {
		Map<Long, Long> counts = offerCounts(page.getContent().stream().map(SkillRequest::getId).toList());
		return PageResponse.from(page, r -> toDto(r, meId, counts.getOrDefault(r.getId(), 0L), null, false));
	}

	static RequestDto toDto(SkillRequest r, Long meId, long offerCount, OfferStatus myOfferStatus, boolean canOffer) {
		Skill skill = r.getSkill();
		boolean mine = r.getLearner().getId().equals(meId);
		boolean open = r.getStatus() == RequestStatus.OPEN || r.getStatus() == RequestStatus.PENDING;
		return new RequestDto(r.getId(), r.getTitle(), r.getDescription(),
				new SkillRef(skill.getId(), skill.getName(), skill.getCategory().getName(), skill.getCategory().getColor()),
				UserSummaryDto.from(r.getLearner()), r.getDesiredLevel(), r.getPreferredSchedule(), r.getMode(),
				r.getDurationHours(), r.getStatus(), UserSummaryDto.from(r.getAcceptedTeacher()), offerCount,
				myOfferStatus, mine, !mine && open && canOffer && myOfferStatus == null, r.getCreatedAt());
	}

	static OfferDto toDto(RequestOffer o) {
		return new OfferDto(o.getId(), o.getRequest().getId(), UserSummaryDto.from(o.getTeacher()), o.getMessage(),
				o.getStatus(), o.getCreatedAt());
	}
}
