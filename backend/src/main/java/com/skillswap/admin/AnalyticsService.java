package com.skillswap.admin;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.admin.dto.AdminDtos.AnalyticsDto;
import com.skillswap.admin.dto.AdminDtos.NameValue;
import com.skillswap.admin.dto.AdminDtos.SessionMonthPoint;
import com.skillswap.admin.dto.AdminDtos.Totals;
import com.skillswap.admin.dto.AdminDtos.UserGrowthPoint;
import com.skillswap.connection.ConnectionRepository;
import com.skillswap.connection.ConnectionStatus;
import com.skillswap.rating.RatingRepository;
import com.skillswap.report.ReportRepository;
import com.skillswap.report.ReportStatus;
import com.skillswap.request.RequestStatus;
import com.skillswap.request.SkillRequestRepository;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionStatus;
import com.skillswap.skill.SkillRepository;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.Role;
import com.skillswap.user.UserRepository;
import com.skillswap.user.dto.UserSummaryDto;
import com.skillswap.wallet.CreditTransactionRepository;
import com.skillswap.wallet.TransactionType;

@Service
public class AnalyticsService {

	private static final int MONTHS = 6;
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

	private final UserRepository userRepository;
	private final SkillRepository skillRepository;
	private final UserSkillRepository userSkillRepository;
	private final LearningSessionRepository sessionRepository;
	private final SkillRequestRepository requestRepository;
	private final CreditTransactionRepository transactionRepository;
	private final RatingRepository ratingRepository;
	private final ReportRepository reportRepository;
	private final ConnectionRepository connectionRepository;

	public AnalyticsService(UserRepository userRepository, SkillRepository skillRepository,
			UserSkillRepository userSkillRepository, LearningSessionRepository sessionRepository,
			SkillRequestRepository requestRepository, CreditTransactionRepository transactionRepository,
			RatingRepository ratingRepository, ReportRepository reportRepository,
			ConnectionRepository connectionRepository) {
		this.userRepository = userRepository;
		this.skillRepository = skillRepository;
		this.userSkillRepository = userSkillRepository;
		this.sessionRepository = sessionRepository;
		this.requestRepository = requestRepository;
		this.transactionRepository = transactionRepository;
		this.ratingRepository = ratingRepository;
		this.reportRepository = reportRepository;
		this.connectionRepository = connectionRepository;
	}

	@Transactional(readOnly = true)
	public AnalyticsDto analytics() {
		LocalDateTime now = LocalDateTime.now();
		YearMonth current = YearMonth.from(now);
		YearMonth first = current.minusMonths(MONTHS - 1);
		LocalDateTime windowStart = first.atDay(1).atStartOfDay();

		long completed = sessionRepository.countByStatus(SessionStatus.COMPLETED);
		long cancelled = sessionRepository.countByStatus(SessionStatus.CANCELLED);
		long rejected = sessionRepository.countByStatus(SessionStatus.REJECTED);
		long upcoming = sessionRepository.countByStatus(SessionStatus.ACCEPTED)
				+ sessionRepository.countByStatus(SessionStatus.SCHEDULED)
				+ sessionRepository.countByStatus(SessionStatus.ONGOING);
		long requested = sessionRepository.countByStatus(SessionStatus.REQUESTED);
		Double avgRating = ratingRepository.averageStars();

		Totals totals = new Totals(userRepository.countByRole(Role.STUDENT),
				userRepository.countByRoleAndLastActiveAtAfter(Role.STUDENT, now.minusDays(30)),
				skillRepository.countByActiveTrue(), completed + cancelled + rejected + upcoming + requested, completed,
				requestRepository.countByStatusIn(EnumSet.of(RequestStatus.OPEN, RequestStatus.PENDING,
						RequestStatus.ACCEPTED)),
				transactionRepository.sumByType(TransactionType.TEACHING_EARNING),
				avgRating == null ? 0 : UserSummaryDto.round1(avgRating), reportRepository.countByStatus(ReportStatus.OPEN),
				connectionRepository.countByStatus(ConnectionStatus.ACCEPTED));

		// User growth: new students per month plus the running total.
		Map<YearMonth, Long> newUsers = new HashMap<>();
		for (Object[] row : userRepository.countNewStudentsByMonth(windowStart)) {
			newUsers.put(YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()), (Long) row[2]);
		}
		long running = userRepository.countByRoleAndCreatedAtBefore(Role.STUDENT, windowStart);
		List<UserGrowthPoint> growth = new ArrayList<>();
		for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1)) {
			long added = newUsers.getOrDefault(month, 0L);
			running += added;
			growth.add(new UserGrowthPoint(MONTH_LABEL.format(month), added, running));
		}

		// Sessions per month by outcome.
		Map<YearMonth, long[]> perMonth = new LinkedHashMap<>();
		for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1)) {
			perMonth.put(month, new long[3]);
		}
		for (Object[] row : sessionRepository.countByMonthAndStatus(windowStart)) {
			YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
			long[] bucket = perMonth.get(month);
			if (bucket == null) {
				continue;
			}
			SessionStatus status = (SessionStatus) row[2];
			long count = (Long) row[3];
			if (status == SessionStatus.COMPLETED) {
				bucket[0] += count;
			}
			else if (status == SessionStatus.CANCELLED || status == SessionStatus.REJECTED) {
				bucket[1] += count;
			}
			if (status != SessionStatus.REJECTED) {
				bucket[2] += count;
			}
		}
		List<SessionMonthPoint> sessions = perMonth.entrySet().stream()
				.map(e -> new SessionMonthPoint(MONTH_LABEL.format(e.getKey()), e.getValue()[0], e.getValue()[1],
						e.getValue()[2]))
				.toList();

		List<NameValue> popularSkills = userSkillRepository.findMostPopularSkills(PageRequest.of(0, 8)).stream()
				.map(row -> new NameValue((String) row[0], (Long) row[1])).toList();
		List<NameValue> categories = sessionRepository.countByCategory(PageRequest.of(0, 8)).stream()
				.map(row -> new NameValue((String) row[0], (Long) row[1])).toList();
		List<NameValue> outcomes = List.of(new NameValue("Completed", completed), new NameValue("Cancelled", cancelled),
				new NameValue("Declined", rejected), new NameValue("Upcoming", upcoming + requested));

		return new AnalyticsDto(totals, growth, sessions, popularSkills, categories, outcomes);
	}
}
