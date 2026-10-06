package com.skillswap.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skillswap.challenge.Challenge;
import com.skillswap.challenge.ChallengeRepository;
import com.skillswap.challenge.Difficulty;
import com.skillswap.challenge.Submission;
import com.skillswap.challenge.SubmissionRepository;
import com.skillswap.challenge.SubmissionReview;
import com.skillswap.challenge.SubmissionReviewRepository;
import com.skillswap.chat.Message;
import com.skillswap.chat.MessageRepository;
import com.skillswap.connection.Connection;
import com.skillswap.connection.ConnectionRepository;
import com.skillswap.connection.ConnectionStatus;
import com.skillswap.group.GroupEvent;
import com.skillswap.group.GroupEventRepository;
import com.skillswap.group.GroupEventType;
import com.skillswap.group.GroupMember;
import com.skillswap.group.GroupMemberRepository;
import com.skillswap.group.GroupPost;
import com.skillswap.group.GroupPostRepository;
import com.skillswap.group.GroupPrivacy;
import com.skillswap.group.GroupRole;
import com.skillswap.group.MemberStatus;
import com.skillswap.group.SkillGroup;
import com.skillswap.group.SkillGroupRepository;
import com.skillswap.notification.Notification;
import com.skillswap.notification.NotificationRepository;
import com.skillswap.notification.NotificationType;
import com.skillswap.rating.Rating;
import com.skillswap.rating.RatingRepository;
import com.skillswap.rating.RatingService;
import com.skillswap.report.Report;
import com.skillswap.report.ReportAction;
import com.skillswap.report.ReportReason;
import com.skillswap.report.ReportRepository;
import com.skillswap.report.ReportStatus;
import com.skillswap.report.ReportTargetType;
import com.skillswap.reputation.ReputationService;
import com.skillswap.request.OfferStatus;
import com.skillswap.request.RequestOffer;
import com.skillswap.request.RequestOfferRepository;
import com.skillswap.request.RequestStatus;
import com.skillswap.request.SkillRequest;
import com.skillswap.request.SkillRequestRepository;
import com.skillswap.session.LearningSession;
import com.skillswap.session.LearningSessionRepository;
import com.skillswap.session.SessionMode;
import com.skillswap.session.SessionStatus;
import com.skillswap.skill.Category;
import com.skillswap.skill.CategoryRepository;
import com.skillswap.skill.ProficiencyLevel;
import com.skillswap.skill.Skill;
import com.skillswap.skill.SkillMapper;
import com.skillswap.skill.SkillRepository;
import com.skillswap.skill.SkillType;
import com.skillswap.skill.UserSkill;
import com.skillswap.skill.UserSkillRepository;
import com.skillswap.user.Availability;
import com.skillswap.user.Role;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;
import com.skillswap.user.UserStatus;
import com.skillswap.wallet.TransactionType;
import com.skillswap.wallet.WalletService;

/**
 * Populates an empty database with a realistic campus so the app looks alive on first launch. Runs only when
 * {@code app.seed.enabled=true}, the users table is empty and a demo password is configured. Every credit movement
 * goes through {@link WalletService}, so seeded balances always match their ledgers.
 */
@Component
@Order(2)
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DOMAIN = "@skillswap.dev";

	private final boolean enabled;
	private final String demoPassword;
	private final String adminEmail;
	private final String adminPassword;
	private final PasswordEncoder passwordEncoder;
	private final UserRepository users;
	private final CategoryRepository categories;
	private final SkillRepository skills;
	private final UserSkillRepository userSkills;
	private final ConnectionRepository connections;
	private final LearningSessionRepository sessions;
	private final RatingRepository ratings;
	private final RatingService ratingService;
	private final WalletService wallet;
	private final ReputationService reputation;
	private final SkillRequestRepository requests;
	private final RequestOfferRepository offers;
	private final MessageRepository messages;
	private final NotificationRepository notifications;
	private final SkillGroupRepository groups;
	private final GroupMemberRepository members;
	private final GroupPostRepository posts;
	private final GroupEventRepository events;
	private final ChallengeRepository challenges;
	private final SubmissionRepository submissions;
	private final SubmissionReviewRepository reviews;
	private final ReportRepository reports;

	private final Map<String, Skill> skillByName = new HashMap<>();
	private final Map<String, User> userByKey = new LinkedHashMap<>();
	private final LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

	public DataSeeder(@Value("${app.seed.enabled:false}") boolean enabled,
			@Value("${app.seed.demo-password:}") String demoPassword, @Value("${app.seed.admin-email}") String adminEmail,
			@Value("${app.seed.admin-password:}") String adminPassword, PasswordEncoder passwordEncoder,
			UserRepository users, CategoryRepository categories, SkillRepository skills, UserSkillRepository userSkills,
			ConnectionRepository connections, LearningSessionRepository sessions, RatingRepository ratings,
			RatingService ratingService, WalletService wallet, ReputationService reputation,
			SkillRequestRepository requests, RequestOfferRepository offers, MessageRepository messages,
			NotificationRepository notifications, SkillGroupRepository groups, GroupMemberRepository members,
			GroupPostRepository posts, GroupEventRepository events, ChallengeRepository challenges,
			SubmissionRepository submissions, SubmissionReviewRepository reviews, ReportRepository reports) {
		this.enabled = enabled;
		this.demoPassword = demoPassword;
		this.adminEmail = adminEmail;
		this.adminPassword = adminPassword;
		this.passwordEncoder = passwordEncoder;
		this.users = users;
		this.categories = categories;
		this.skills = skills;
		this.userSkills = userSkills;
		this.connections = connections;
		this.sessions = sessions;
		this.ratings = ratings;
		this.ratingService = ratingService;
		this.wallet = wallet;
		this.reputation = reputation;
		this.requests = requests;
		this.offers = offers;
		this.messages = messages;
		this.notifications = notifications;
		this.groups = groups;
		this.members = members;
		this.posts = posts;
		this.events = events;
		this.challenges = challenges;
		this.submissions = submissions;
		this.reviews = reviews;
		this.reports = reports;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!enabled) {
			return;
		}
		if (categories.count() == 0) {
			seedCatalogue();
			log.info("Seeded {} categories and {} skills", categories.count(), skills.count());
		}
		if (adminPassword != null && !adminPassword.isBlank() && users.findByEmailIgnoreCase(adminEmail).isEmpty()) {
			seedAdmin();
		}
		if (users.countByRole(Role.STUDENT) > 0) {
			return;
		}
		if (demoPassword == null || demoPassword.isBlank()) {
			log.warn("Skipping demo students: set SEED_DEMO_PASSWORD to seed demo accounts.");
			return;
		}
		loadSkills();
		seedStudents();
		seedSessionsAndRatings();
		seedConnections();
		seedRequests();
		seedMessages();
		seedGroups();
		seedChallenges();
		seedReports();
		seedNotifications();
		userByKey.values().forEach(u -> reputation.evaluateBadges(u.getId(), false));
		log.info("Seeded {} demo students (sign in with any <firstname>{} and the SEED_DEMO_PASSWORD)", userByKey.size(),
				DOMAIN);
	}

	// ------------------------------------------------------------------ catalogue

	private void seedCatalogue() {
		Object[][] data = {
				{ "Programming", "Code2", "#6366f1", "Core programming languages, algorithms and computer science.",
						new String[] { "Java", "Python", "C++", "Data Structures & Algorithms", "Machine Learning", "SQL" } },
				{ "Web Development", "Globe", "#0ea5e9", "Frontend, backend and full-stack web technologies.",
						new String[] { "React", "Spring Boot", "Node.js", "HTML & CSS", "Django", "TypeScript" } },
				{ "Mobile Development", "Smartphone", "#14b8a6", "Native and cross-platform app development.",
						new String[] { "Flutter", "Android (Kotlin)", "React Native", "Swift" } },
				{ "UI/UX", "PenTool", "#ec4899", "Designing usable, delightful product experiences.",
						new String[] { "UI/UX Design", "Figma", "User Research" } },
				{ "Design", "Palette", "#f97316", "Visual design, illustration and branding.",
						new String[] { "Graphic Design", "Adobe Photoshop", "Adobe Illustrator", "Canva" } },
				{ "Marketing", "Megaphone", "#eab308", "Growing audiences and telling stories that sell.",
						new String[] { "Digital Marketing", "SEO", "Content Writing", "Social Media Marketing" } },
				{ "Communication", "MessageCircle", "#22c55e", "Speaking, writing and presenting with confidence.",
						new String[] { "Public Speaking", "English Communication", "Interview Preparation" } },
				{ "Academics", "GraduationCap", "#8b5cf6", "Coursework, exam preparation and core subjects.",
						new String[] { "Calculus", "Physics", "Engineering Mathematics", "GATE Preparation" } },
				{ "Music", "Music", "#f43f5e", "Instruments, vocals and music production.",
						new String[] { "Guitar", "Piano", "Singing" } },
				{ "Video Editing", "Clapperboard", "#a855f7", "Cutting, grading and motion graphics.",
						new String[] { "Video Editing", "Adobe Premiere Pro", "After Effects", "DaVinci Resolve" } },
				{ "Photography", "Camera", "#64748b", "Composition, lighting and photo editing.",
						new String[] { "Photography", "Lightroom" } },
				{ "Sports", "Trophy", "#10b981", "Games, fitness and athletic coaching.",
						new String[] { "Chess", "Badminton", "Fitness Training" } },
				{ "Business", "Briefcase", "#0f766e", "Startups, finance and product thinking.",
						new String[] { "Entrepreneurship", "Excel & Financial Modelling", "Product Management" } },
				{ "Other", "Sparkles", "#94a3b8", "Everything else worth sharing.",
						new String[] { "Cooking", "Japanese Language" } } };
		for (Object[] row : data) {
			Category category = new Category();
			category.setName((String) row[0]);
			category.setSlug(SkillMapper.slugify((String) row[0]));
			category.setIcon((String) row[1]);
			category.setColor((String) row[2]);
			category.setDescription((String) row[3]);
			categories.save(category);
			for (String name : (String[]) row[4]) {
				Skill skill = new Skill();
				skill.setName(name);
				skill.setSlug(SkillMapper.slugify(name));
				skill.setCategory(category);
				skill.setDescription(name + " — learn it from fellow students who use it every day.");
				skills.save(skill);
			}
		}
	}

	private void loadSkills() {
		skills.findAll().forEach(s -> skillByName.put(s.getName(), s));
	}

	private void seedAdmin() {
		User admin = new User();
		admin.setFullName("SkillSwap Admin");
		admin.setEmail(adminEmail.toLowerCase());
		admin.setPasswordHash(passwordEncoder.encode(adminPassword));
		admin.setCollege("SkillSwap HQ");
		admin.setDepartment("Platform Operations");
		admin.setYearOfStudy(1);
		admin.setRole(Role.ADMIN);
		admin.setProfileCompleted(true);
		admin.setCreatedAt(now.minusDays(200));
		admin.setLastActiveAt(now);
		users.save(admin);
		log.info("Seeded admin account {}", adminEmail);
	}

	// ------------------------------------------------------------------ students

	private record StudentSpec(String key, String name, String college, String department, int year, String bio,
			String availability, String teach, String learn, int joinedDaysAgo, int lastActiveDaysAgo) {
	}

	private void seedStudents() {
		String cbit = "CBIT Hyderabad";
		String iiit = "IIIT Hyderabad";
		String bits = "BITS Pilani, Hyderabad Campus";
		String ou = "Osmania University";
		String vnr = "VNR VJIET";
		List<StudentSpec> specs = List.of(
				new StudentSpec("karthikeya", "Karthikeya Gupta", cbit, "Computer Science", 3,
						"Backend-focused developer who loves building APIs with Java and Spring Boot. Happy to pair on projects, and currently trying to get better at design and video storytelling.",
						"WEEKDAY_EVENING,WEEKEND_MORNING,WEEKEND_AFTERNOON",
						"Java:EXPERT:3|Spring Boot:ADVANCED:2|Python:ADVANCED:3|React:ADVANCED:2",
						"UI/UX Design:INTERMEDIATE|Video Editing:BEGINNER", 182, 0),
				new StudentSpec("rahul", "Rahul Verma", iiit, "Information Technology", 3,
						"Product designer at heart. I run the campus design club and love turning messy ideas into clean interfaces.",
						"WEEKDAY_EVENING,WEEKEND_AFTERNOON", "UI/UX Design:EXPERT:3|Figma:ADVANCED:3",
						"Java:INTERMEDIATE|Spring Boot:BEGINNER", 178, 0),
				new StudentSpec("ananya", "Ananya Sharma", bits, "Electronics & Communication", 2,
						"Freelance video editor for college fests and YouTubers. Learning Python to automate my editing workflow.",
						"WEEKDAY_AFTERNOON,WEEKDAY_EVENING,WEEKEND_EVENING",
						"Video Editing:ADVANCED:3|Adobe Premiere Pro:ADVANCED:3|After Effects:INTERMEDIATE:1",
						"Python:INTERMEDIATE|Machine Learning:BEGINNER", 176, 1),
				new StudentSpec("priya", "Priya Nair", iiit, "Computer Science", 4,
						"ML research intern. I enjoy explaining the maths behind models in plain language.",
						"WEEKDAY_MORNING,WEEKEND_MORNING", "Machine Learning:ADVANCED:2|Python:EXPERT:4|SQL:ADVANCED:3",
						"Public Speaking:INTERMEDIATE|Guitar:BEGINNER", 175, 2),
				new StudentSpec("arjun", "Arjun Reddy", vnr, "Mechanical Engineering", 3,
						"Guitarist in two campus bands and a gym regular. Want to pick up coding for robotics.",
						"WEEKDAY_EVENING,WEEKEND_EVENING", "Guitar:EXPERT:6|Fitness Training:ADVANCED:4",
						"Python:BEGINNER|Video Editing:INTERMEDIATE", 172, 3),
				new StudentSpec("sneha", "Sneha Iyer", ou, "Management Studies", 2,
						"Growth marketer who has run campaigns for three student startups. Writing is my superpower.",
						"WEEKDAY_MORNING,WEEKDAY_AFTERNOON", "Digital Marketing:ADVANCED:2|Content Writing:EXPERT:4|SEO:ADVANCED:2",
						"Excel & Financial Modelling:INTERMEDIATE|React:BEGINNER", 168, 1),
				new StudentSpec("vikram", "Vikram Singh", bits, "Computer Science", 4,
						"Competitive programmer (Codeforces Expert). I help juniors prepare for placements.",
						"WEEKDAY_EVENING,WEEKEND_MORNING,WEEKEND_AFTERNOON",
						"Data Structures & Algorithms:EXPERT:4|C++:EXPERT:4|Interview Preparation:ADVANCED:2",
						"Flutter:INTERMEDIATE|Public Speaking:INTERMEDIATE", 170, 0),
				new StudentSpec("meera", "Meera Kulkarni", ou, "Architecture", 3,
						"Visual designer and illustrator. Posters, branding, zines — if it is print, I am in.",
						"WEEKDAY_AFTERNOON,WEEKEND_AFTERNOON",
						"Graphic Design:ADVANCED:3|Adobe Photoshop:EXPERT:4|Adobe Illustrator:ADVANCED:3",
						"UI/UX Design:ADVANCED|Photography:INTERMEDIATE", 165, 4),
				new StudentSpec("aditya", "Aditya Rao", vnr, "Electronics & Communication", 3,
						"Street and event photographer. I lead photo walks around the city every month.",
						"WEEKEND_MORNING,WEEKEND_AFTERNOON,WEEKDAY_EVENING", "Photography:EXPERT:5|Lightroom:ADVANCED:3",
						"Graphic Design:INTERMEDIATE|Video Editing:INTERMEDIATE", 166, 1),
				new StudentSpec("kavya", "Kavya Menon", ou, "Mass Communication", 2,
						"Debater and MUN chair. I coach first-years on public speaking and interviews.",
						"WEEKDAY_EVENING,WEEKEND_MORNING", "Public Speaking:EXPERT:5|English Communication:EXPERT:5",
						"Photography:BEGINNER|Digital Marketing:INTERMEDIATE", 160, 2),
				new StudentSpec("rohan", "Rohan Das", cbit, "Computer Science", 3,
						"Android and Flutter developer with two apps on the Play Store.",
						"WEEKDAY_EVENING,WEEKEND_EVENING", "Flutter:ADVANCED:2|Android (Kotlin):ADVANCED:2",
						"Machine Learning:INTERMEDIATE|Data Structures & Algorithms:ADVANCED", 158, 0),
				new StudentSpec("ishita", "Ishita Patel", iiit, "Mathematics", 2,
						"Maths olympiad alumna. I make calculus feel less scary, promise.",
						"WEEKDAY_MORNING,WEEKDAY_AFTERNOON", "Calculus:EXPERT:4|Engineering Mathematics:EXPERT:3|Physics:ADVANCED:3",
						"Python:INTERMEDIATE|Piano:BEGINNER", 155, 5),
				new StudentSpec("siddharth", "Siddharth Joshi", cbit, "Information Technology", 4,
						"Full-stack JavaScript developer. Interning at a fintech startup this summer.",
						"WEEKDAY_EVENING,WEEKEND_AFTERNOON", "Node.js:ADVANCED:3|TypeScript:ADVANCED:2|HTML & CSS:EXPERT:4",
						"Product Management:INTERMEDIATE|Figma:INTERMEDIATE", 150, 1),
				new StudentSpec("neha", "Neha Gupta", vnr, "Electrical Engineering", 2,
						"Trained in Hindustani classical and Western piano. Learning to code so I can build music apps.",
						"WEEKEND_MORNING,WEEKEND_EVENING", "Piano:EXPERT:8|Singing:ADVANCED:6",
						"React:INTERMEDIATE|Java:BEGINNER", 152, 3),
				new StudentSpec("aman", "Aman Khan", ou, "Civil Engineering", 1,
						"State-level chess player and badminton enthusiast. New to coding and excited to learn.",
						"WEEKDAY_EVENING,WEEKEND_MORNING", "Chess:EXPERT:7|Badminton:ADVANCED:4",
						"Python:BEGINNER|Public Speaking:BEGINNER", 163, 0),
				new StudentSpec("divya", "Divya Reddy", bits, "Computer Science", 3,
						"Frontend engineer obsessed with React performance and accessibility.",
						"WEEKDAY_EVENING,WEEKEND_AFTERNOON", "React:EXPERT:3|TypeScript:ADVANCED:2|HTML & CSS:ADVANCED:3",
						"Machine Learning:INTERMEDIATE|Swift:BEGINNER", 171, 0),
				new StudentSpec("harsh", "Harsh Vardhan", ou, "Business Administration", 3,
						"Co-founder of a campus food-delivery startup. Spreadsheets are my love language.",
						"WEEKDAY_MORNING,WEEKDAY_EVENING",
						"Entrepreneurship:ADVANCED:2|Excel & Financial Modelling:EXPERT:3|Product Management:INTERMEDIATE:1",
						"Digital Marketing:INTERMEDIATE|Video Editing:BEGINNER", 148, 6),
				new StudentSpec("pooja", "Pooja Shetty", vnr, "Biotechnology", 2,
						"Home chef and anime fan learning Japanese. Looking to get into design.",
						"WEEKEND_MORNING,WEEKEND_AFTERNOON", "Cooking:EXPERT:6|Japanese Language:INTERMEDIATE:2",
						"Photography:BEGINNER|Graphic Design:BEGINNER", 140, 2),
				new StudentSpec("nikhil", "Nikhil Kumar", cbit, "Computer Science", 4,
						"Backend developer working with Django and PostgreSQL. Database nerd.",
						"WEEKDAY_EVENING,WEEKEND_EVENING", "Django:ADVANCED:2|SQL:EXPERT:3|Python:ADVANCED:3",
						"React:INTERMEDIATE|UI/UX Design:BEGINNER", 145, 1),
				new StudentSpec("tanvi", "Tanvi Desai", iiit, "Design", 3,
						"UX researcher and Figma power user. I mentor the design track at our hackathons.",
						"WEEKDAY_EVENING,WEEKEND_MORNING,WEEKEND_AFTERNOON",
						"Figma:EXPERT:3|User Research:ADVANCED:2|UI/UX Design:ADVANCED:3",
						"React:INTERMEDIATE|Java:BEGINNER", 167, 0),
				new StudentSpec("karan", "Karan Malhotra", bits, "Electronics & Communication", 4,
						"Colourist and editor for short films. DaVinci Resolve certified.",
						"WEEKDAY_EVENING,WEEKEND_EVENING", "DaVinci Resolve:ADVANCED:3|Video Editing:EXPERT:5",
						"Java:INTERMEDIATE|Guitar:INTERMEDIATE", 174, 1),
				new StudentSpec("riya", "Riya Saxena", cbit, "Computer Science", 4,
						"GATE AIR 412. Now preparing for placements and building my final-year project in Spring Boot.",
						"WEEKDAY_MORNING,WEEKDAY_EVENING", "GATE Preparation:EXPERT:2|Engineering Mathematics:ADVANCED:3",
						"Spring Boot:INTERMEDIATE|Interview Preparation:ADVANCED", 169, 0),
				new StudentSpec("yash", "Yash Agarwal", vnr, "Information Technology", 2,
						"Runs Instagram pages with 50k+ followers. Canva templates for days.",
						"WEEKDAY_AFTERNOON,WEEKEND_EVENING", "Social Media Marketing:ADVANCED:3|Canva:EXPERT:4",
						"Node.js:INTERMEDIATE|Photography:INTERMEDIATE", 120, 3),
				new StudentSpec("lakshmi", "Lakshmi Prasad", iiit, "Computer Science", 2,
						"iOS and React Native developer. I sing in the college choir too.",
						"WEEKDAY_EVENING,WEEKEND_AFTERNOON", "React Native:ADVANCED:2|Swift:INTERMEDIATE:1",
						"Graphic Design:INTERMEDIATE|Singing:BEGINNER", 130, 2),
				new StudentSpec("deals", "Deals4U Official", ou, "Commerce", 1,
						"BUY INSTAGRAM FOLLOWERS CHEAP!!! DM for 1000 followers @ Rs 99. Guaranteed results.",
						"", "Social Media Marketing:EXPERT:1", "", 12, 1));

		String hash = passwordEncoder.encode(demoPassword);
		for (StudentSpec spec : specs) {
			User user = new User();
			user.setFullName(spec.name());
			user.setEmail(spec.key() + DOMAIN);
			user.setPasswordHash(hash);
			user.setCollege(spec.college());
			user.setDepartment(spec.department());
			user.setYearOfStudy(spec.year());
			user.setBio(spec.bio());
			user.setRole(Role.STUDENT);
			user.setStatus(UserStatus.ACTIVE);
			user.setProfileCompleted(true);
			user.setCreatedAt(now.minusDays(spec.joinedDaysAgo()));
			user.setLastActiveAt(now.minusDays(spec.lastActiveDaysAgo()).minusHours(spec.lastActiveDaysAgo() * 3L));
			if (!spec.availability().isBlank()) {
				for (String slot : spec.availability().split(",")) {
					user.getAvailability().add(Availability.valueOf(slot));
				}
			}
			users.save(user);
			userByKey.put(spec.key(), user);
			wallet.openWallet(user.getId(), user.getCreatedAt());
			addSkills(user, spec.teach(), SkillType.TEACH);
			addSkills(user, spec.learn(), SkillType.LEARN);
		}
	}

	private void addSkills(User user, String spec, SkillType type) {
		if (spec == null || spec.isBlank()) {
			return;
		}
		for (String entry : spec.split("\\|")) {
			String[] parts = entry.split(":");
			UserSkill userSkill = new UserSkill();
			userSkill.setUser(user);
			userSkill.setSkill(skill(parts[0]));
			userSkill.setType(type);
			userSkill.setLevel(ProficiencyLevel.valueOf(parts[1]));
			if (type == SkillType.TEACH && parts.length > 2) {
				userSkill.setYearsExperience(Double.valueOf(parts[2]));
				userSkill.setDescription("I have used " + parts[0] + " for " + parts[2]
						+ (parts[2].equals("1") ? " year" : " years") + " in coursework and personal projects.");
			}
			userSkill.setCreatedAt(user.getCreatedAt().plusHours(1));
			userSkills.save(userSkill);
		}
	}

	// ------------------------------------------------------------------ sessions, credits, ratings

	private record SessionSpec(String teacher, String learner, String skill, int daysAgo, int hour, int minutes,
			int stars, String feedback) {
	}

	private final Map<String, LearningSession> namedSessions = new HashMap<>();

	private void seedSessionsAndRatings() {
		List<SessionSpec> completed = new ArrayList<>(List.of(
				new SessionSpec("arjun", "aman", "Fitness Training", 160, 7, 60, 5, "Super practical plan, I already feel stronger."),
				new SessionSpec("priya", "ishita", "Python", 150, 10, 60, 5, "Priya made NumPy click for me. Brilliant teacher!"),
				new SessionSpec("divya", "neha", "React", 140, 18, 60, 5, "Clear explanations of components and props."),
				new SessionSpec("meera", "pooja", "Adobe Photoshop", 130, 11, 60, 5, "Learnt layers, masks and so many shortcuts."),
				new SessionSpec("vikram", "rohan", "Data Structures & Algorithms", 120, 19, 60, 5, "Graph problems finally make sense."),
				new SessionSpec("kavya", "aman", "English Communication", 110, 18, 60, 4, "Helpful tips for group discussions."),
				new SessionSpec("sneha", "kavya", "Content Writing", 100, 15, 60, 4, "Great frameworks for writing hooks."),
				new SessionSpec("rahul", "meera", "Figma", 95, 17, 90, 5, "Auto-layout and components explained perfectly."),
				new SessionSpec("aditya", "meera", "Photography", 80, 8, 120, 5, "Golden hour walk was magical. Learnt so much about light."),
				new SessionSpec("meera", "aditya", "Graphic Design", 75, 15, 120, 4, "Good intro to typography and grids."),
				new SessionSpec("priya", "ananya", "Machine Learning", 70, 11, 120, 5, "Patient, structured and full of intuition."),
				new SessionSpec("priya", "arjun", "Python", 65, 18, 60, 4, "Nice beginner-friendly pace."),
				new SessionSpec("karthikeya", "karan", "Java", 60, 19, 60, 4, "Good walkthrough of OOP basics with examples."),
				new SessionSpec("arjun", "priya", "Guitar", 60, 20, 60, 5, "Learnt my first three chords and a strumming pattern!"),
				new SessionSpec("vikram", "rohan", "Data Structures & Algorithms", 55, 19, 120, 5, "DP patterns explained like a story."),
				new SessionSpec("rohan", "vikram", "Flutter", 50, 20, 90, 4, "Built a working app in one session."),
				new SessionSpec("kavya", "vikram", "Public Speaking", 45, 18, 60, 5, "Kavya's feedback on my mock talk was gold."),
				new SessionSpec("kavya", "priya", "Public Speaking", 42, 9, 60, 5, "So much more confident for my conference talk."),
				new SessionSpec("karthikeya", "rahul", "Java", 40, 19, 120, 5, "Karthikeya explained OOP and collections brilliantly with real project examples."),
				new SessionSpec("sneha", "harsh", "Digital Marketing", 38, 10, 90, 5, "Actionable funnel ideas for our startup."),
				new SessionSpec("harsh", "sneha", "Excel & Financial Modelling", 36, 11, 60, 4, "Pivot tables are now my friends."),
				new SessionSpec("rahul", "karthikeya", "UI/UX Design", 35, 18, 90, 5, "Rahul showed me how to think in user flows. Eye-opening."),
				new SessionSpec("harsh", "siddharth", "Product Management", 33, 17, 60, 4, "Good frameworks for prioritisation."),
				new SessionSpec("neha", "ishita", "Piano", 30, 10, 60, 5, "Lovely, patient teacher."),
				new SessionSpec("divya", "nikhil", "React", 28, 19, 120, 5, "Hooks and state management explained really well."),
				new SessionSpec("karan", "arjun", "Video Editing", 26, 20, 120, 4, "Learnt J-cuts and colour basics."),
				new SessionSpec("karthikeya", "riya", "Spring Boot", 25, 18, 120, 5, "Built a complete REST API with validation and JPA. Exactly what my project needed."),
				new SessionSpec("ananya", "aditya", "Video Editing", 24, 17, 60, 5, "Ananya's editing workflow tips saved me hours."),
				new SessionSpec("sneha", "kavya", "Digital Marketing", 22, 11, 60, 5, "Loved the campaign teardown exercise."),
				new SessionSpec("vikram", "riya", "Interview Preparation", 21, 19, 60, 5, "Mock interview felt like the real thing."),
				new SessionSpec("karthikeya", "ananya", "Python", 20, 17, 60, 4, "Wrote my first automation script for renaming clips!"),
				new SessionSpec("meera", "lakshmi", "Graphic Design", 19, 16, 60, 4, "Learnt colour theory basics."),
				new SessionSpec("ananya", "karthikeya", "Video Editing", 18, 18, 60, 5, "Ananya is an amazing editor and teacher. Learnt cuts, pacing and audio sync."),
				new SessionSpec("tanvi", "siddharth", "Figma", 16, 19, 60, 5, "Components and variants — finally!"),
				new SessionSpec("aditya", "yash", "Photography", 15, 8, 60, 4, "Great composition exercises."),
				new SessionSpec("siddharth", "yash", "Node.js", 14, 18, 90, 5, "Express + MongoDB from scratch, very clear."),
				new SessionSpec("karthikeya", "tanvi", "React", 12, 18, 90, 5, "Karthikeya is so patient. Understood useEffect properly for the first time."),
				new SessionSpec("priya", "rohan", "Machine Learning", 11, 10, 60, 5, "Great intuition for gradient descent."),
				new SessionSpec("aditya", "pooja", "Photography", 10, 9, 60, 5, "Phone photography tips that actually work."),
				new SessionSpec("arjun", "karan", "Guitar", 9, 20, 60, 5, "Fun and energetic session."),
				new SessionSpec("karthikeya", "neha", "Java", 8, 17, 60, 5, "Loved the hands-on exercises with loops and classes."),
				new SessionSpec("neha", "lakshmi", "Singing", 6, 10, 60, 5, "Breathing exercises were super helpful."),
				new SessionSpec("tanvi", "karthikeya", "Figma", 5, 11, 60, 5, "Tanvi's Figma tricks are unreal. Prototyping is so much faster now."),
				new SessionSpec("aditya", "kavya", "Photography", 4, 8, 60, 0, null)));
		completed.sort(Comparator.comparingInt(SessionSpec::daysAgo).reversed());

		for (SessionSpec spec : completed) {
			User teacher = user(spec.teacher());
			User learner = user(spec.learner());
			LocalDateTime start = LocalDate.now().minusDays(spec.daysAgo()).atTime(spec.hour(), 0);
			LearningSession session = newSession(teacher, learner, spec.skill(), start, spec.minutes(),
					spec.daysAgo() % 3 == 0 ? SessionMode.OFFLINE : SessionMode.ONLINE);
			session.setCreatedAt(start.minusDays(3));
			if (wallet.balance(learner.getId()).compareTo(session.getCredits()) < 0) {
				continue;
			}
			session.setStatus(SessionStatus.COMPLETED);
			session.setCompletedAt(session.getEndTime().plusMinutes(10));
			sessions.save(session);
			wallet.debit(learner.getId(), session.getCredits(), TransactionType.SESSION_PAYMENT,
					spec.skill() + " Learning Session with " + teacher.getFullName(), session, start.minusDays(2));
			wallet.credit(teacher.getId(), session.getCredits(), TransactionType.TEACHING_EARNING,
					spec.skill() + " Teaching Session with " + learner.getFullName(), session,
					session.getCompletedAt());
			reputation.addXp(teacher.getId(), ReputationService.XP_TEACH_SESSION);
			reputation.addXp(learner.getId(), ReputationService.XP_LEARN_SESSION);
			namedSessions.put(spec.teacher() + ">" + spec.learner() + ":" + spec.skill(), session);
			if (spec.stars() > 0) {
				Rating rating = new Rating();
				rating.setSession(session);
				rating.setRater(learner);
				rating.setRatee(teacher);
				rating.setStars(spec.stars());
				rating.setTeachingQuality(Math.min(5, spec.stars() + (spec.daysAgo() % 2 == 0 ? 0 : -1) + 1));
				rating.setCommunication(spec.stars());
				rating.setKnowledge(Math.min(5, spec.stars() + 1));
				rating.setFeedback(spec.feedback());
				rating.setCreatedAt(session.getCompletedAt().plusHours(2));
				ratings.save(rating);
				reputation.addXp(teacher.getId(),
						spec.stars() == 5 ? ReputationService.XP_FIVE_STAR : spec.stars() == 4 ? ReputationService.XP_GOOD_RATING : 0);
			}
		}
		ratings.flush();
		userByKey.values().forEach(ratingService::refreshAggregate);

		// A cancelled and a declined session for realistic history.
		LearningSession cancelled = newSession(user("divya"), user("neha"), "React",
				LocalDate.now().minusDays(14).atTime(18, 0), 60, SessionMode.ONLINE);
		cancelled.setStatus(SessionStatus.CANCELLED);
		cancelled.setCancelledBy(user("neha"));
		cancelled.setCancelReason("Clashes with my mid-semester exam, sorry!");
		cancelled.setCreatedAt(now.minusDays(17));
		sessions.save(cancelled);
		LearningSession declined = newSession(user("lakshmi"), user("divya"), "Swift",
				LocalDate.now().minusDays(20).atTime(16, 0), 60, SessionMode.ONLINE);
		declined.setStatus(SessionStatus.REJECTED);
		declined.setCancelledBy(user("lakshmi"));
		declined.setCancelReason("I'm travelling that week — could we try next month?");
		declined.setCreatedAt(now.minusDays(23));
		sessions.save(declined);

		// Upcoming sessions for the main demo account.
		LearningSession scheduled = upcoming("karthikeya", "rahul", "Spring Boot", 1, 18, 90, SessionStatus.SCHEDULED);
		scheduled.setMeetingLink("https://meet.google.com/skl-swap-demo");
		scheduled.setNotes("Let's build the user registration API for your portfolio project.");
		namedSessions.put("upcoming:rahul-spring", scheduled);
		LearningSession accepted = upcoming("tanvi", "karthikeya", "UI/UX Design", 3, 11, 60, SessionStatus.ACCEPTED);
		accepted.setMode(SessionMode.OFFLINE);
		accepted.setNotes("Bring your project wireframes so we can critique them together.");
		upcoming("karthikeya", "aman", "Python", 2, 17, 60, SessionStatus.REQUESTED)
				.setNotes("Hi! I'd love to learn Python basics for data analysis.");
		upcoming("karan", "karthikeya", "Video Editing", 4, 19, 60, SessionStatus.REQUESTED)
				.setNotes("Want to learn colour grading for my project reel.");
		upcoming("vikram", "riya", "Data Structures & Algorithms", 2, 19, 60, SessionStatus.SCHEDULED)
				.setMeetingLink("https://meet.google.com/dsa-prep-demo");
		upcoming("priya", "rohan", "Python", 5, 10, 60, SessionStatus.ACCEPTED);
		upcoming("divya", "nikhil", "React", 6, 18, 120, SessionStatus.REQUESTED);
	}

	private LearningSession upcoming(String teacherKey, String learnerKey, String skill, int inDays, int hour,
			int minutes, SessionStatus status) {
		User teacher = user(teacherKey);
		User learner = user(learnerKey);
		LearningSession session = newSession(teacher, learner, skill, LocalDate.now().plusDays(inDays).atTime(hour, 0),
				minutes, SessionMode.ONLINE);
		session.setStatus(status);
		session.setCreatedAt(now.minusDays(1));
		sessions.save(session);
		if (LearningSession.CREDITS_HELD.contains(status)) {
			wallet.debit(learner.getId(), session.getCredits(), TransactionType.SESSION_PAYMENT,
					skill + " Learning Session with " + teacher.getFullName(), session, now.minusHours(20));
		}
		return session;
	}

	private LearningSession newSession(User teacher, User learner, String skill, LocalDateTime start, int minutes,
			SessionMode mode) {
		LearningSession session = new LearningSession();
		session.setTeacher(teacher);
		session.setLearner(learner);
		session.setSkill(skill(skill));
		session.setStartTime(start);
		session.setEndTime(start.plusMinutes(minutes));
		session.setDurationMinutes(minutes);
		session.setCredits(WalletService.creditsForMinutes(minutes));
		session.setMode(mode);
		if (mode == SessionMode.ONLINE) {
			session.setMeetingLink("https://meet.google.com/" + teacher.getFirstName().toLowerCase() + "-session");
		}
		else {
			session.setLocation("Central Library, Discussion Room 2");
		}
		session.setReminderSent(start.isBefore(now));
		return session;
	}

	// ------------------------------------------------------------------ connections

	private void seedConnections() {
		String[][] accepted = { { "karthikeya", "rahul" }, { "ananya", "karthikeya" }, { "karthikeya", "priya" },
				{ "tanvi", "karthikeya" }, { "riya", "karthikeya" }, { "karthikeya", "neha" }, { "rahul", "meera" },
				{ "aditya", "meera" }, { "priya", "ananya" }, { "arjun", "priya" }, { "vikram", "rohan" },
				{ "kavya", "vikram" }, { "sneha", "harsh" }, { "divya", "nikhil" }, { "tanvi", "siddharth" },
				{ "siddharth", "yash" }, { "aditya", "pooja" }, { "neha", "lakshmi" }, { "vikram", "riya" },
				{ "karan", "arjun" }, { "kavya", "sneha" }, { "divya", "neha" }, { "priya", "rohan" },
				{ "ananya", "aditya" }, { "harsh", "siddharth" } };
		int i = 0;
		for (String[] pair : accepted) {
			connect(pair[0], pair[1], ConnectionStatus.ACCEPTED, 150 - i * 5);
			i++;
		}
		connect("karan", "karthikeya", ConnectionStatus.PENDING, 1).setMessage(
				"Hey! Saw you want to learn video editing — happy to help, and I'd love Java tips in return.");
		connect("karthikeya", "meera", ConnectionStatus.PENDING, 2)
				.setMessage("Hi Meera, would love to learn some graphic design basics from you!");
		connect("aman", "karthikeya", ConnectionStatus.PENDING, 0)
				.setMessage("Hi! I'm new to coding and saw you teach Python. Can we connect?");
	}

	private Connection connect(String from, String to, ConnectionStatus status, int daysAgo) {
		Connection connection = new Connection();
		connection.setRequester(user(from));
		connection.setAddressee(user(to));
		connection.setStatus(status);
		connection.setCreatedAt(now.minusDays(Math.max(0, daysAgo)).minusHours(3));
		if (status == ConnectionStatus.ACCEPTED) {
			connection.setRespondedAt(connection.getCreatedAt().plusHours(5));
		}
		return connections.save(connection);
	}

	// ------------------------------------------------------------------ learning requests

	private void seedRequests() {
		SkillRequest videoReel = request("karthikeya", "Video Editing", "Edit a 60-second project demo reel",
				"I'm building a launch video for my final-year project and want to learn pacing, cuts, music sync and simple text animations.",
				ProficiencyLevel.BEGINNER, "Weekday evenings after 6 pm", SessionMode.ONLINE, "2", RequestStatus.PENDING, 3);
		offer(videoReel, "karan", "Happy to help! I edit short films — we can cut your reel together in Resolve.", OfferStatus.PENDING, 2);
		offer(videoReel, "ananya", "I can show you a Premiere Pro workflow for punchy product reels.", OfferStatus.PENDING, 1);

		request("aman", "Python", "Python basics for data analysis",
				"Complete beginner. I want to learn variables, loops, lists and a little pandas so I can analyse chess game data.",
				ProficiencyLevel.BEGINNER, "Weekend mornings", SessionMode.ONLINE, "1.5", RequestStatus.OPEN, 2);
		request("neha", "React", "React hooks and state management",
				"I know basic components. I'd like to understand useState, useEffect and how to structure a small app with context.",
				ProficiencyLevel.INTERMEDIATE, "Saturday or Sunday", SessionMode.ONLINE, "2", RequestStatus.OPEN, 1);
		request("ishita", "Python", "Python for numerical methods",
				"Want to implement Newton-Raphson and Simpson's rule in Python for my numerical analysis course.",
				ProficiencyLevel.INTERMEDIATE, "Weekday mornings", SessionMode.OFFLINE, "1", RequestStatus.OPEN, 4);
		request("lakshmi", "Singing", "Breathing and pitch for choir",
				"I sing in the choir but struggle with breath control on long phrases. Looking for practical exercises.",
				ProficiencyLevel.BEGINNER, "Weekend afternoons", SessionMode.OFFLINE, "1", RequestStatus.OPEN, 5);

		SkillRequest posters = request("pooja", "Graphic Design", "Design posters for our cultural fest",
				"Need to learn layout, typography and colour basics so I can design posters for the fest committee.",
				ProficiencyLevel.BEGINNER, "Any weekend", SessionMode.OFFLINE, "2", RequestStatus.PENDING, 3);
		offer(posters, "meera", "Posters are my favourite thing to design! Let's do a hands-on session.", OfferStatus.PENDING, 2);

		SkillRequest springRahul = request("rahul", "Spring Boot", "Spring Boot from scratch",
				"I know Java basics now (thanks Karthikeya!). Want to build a REST API with Spring Boot for my portfolio.",
				ProficiencyLevel.BEGINNER, "Weekday evenings", SessionMode.ONLINE, "1.5", RequestStatus.ACCEPTED, 4);
		offer(springRahul, "karthikeya", "Let's build a real API together — registration, validation, JPA.", OfferStatus.ACCEPTED, 3);
		springRahul.setAcceptedTeacher(user("karthikeya"));
		LearningSession scheduled = namedSessions.get("upcoming:rahul-spring");
		if (scheduled != null) {
			scheduled.setSkillRequest(springRahul);
		}

		SkillRequest springRiya = request("riya", "Spring Boot", "Spring Boot REST APIs for my final-year project",
				"Building a hospital appointment system. Need help with REST controllers, JPA relationships and validation.",
				ProficiencyLevel.INTERMEDIATE, "Weekday evenings", SessionMode.ONLINE, "2", RequestStatus.COMPLETED, 30);
		offer(springRiya, "karthikeya", "I've built similar systems — happy to help you structure it properly.", OfferStatus.ACCEPTED, 29);
		springRiya.setAcceptedTeacher(user("karthikeya"));
		LearningSession riyaSession = namedSessions.get("karthikeya>riya:Spring Boot");
		if (riyaSession != null) {
			riyaSession.setSkillRequest(springRiya);
		}

		SkillRequest pm = request("siddharth", "Product Management", "Product management fundamentals",
				"How do PMs prioritise features and write PRDs? Preparing for APM interviews.",
				ProficiencyLevel.INTERMEDIATE, "Evenings", SessionMode.ONLINE, "1", RequestStatus.COMPLETED, 40);
		offer(pm, "harsh", "I run product at our startup — let's go through real examples.", OfferStatus.ACCEPTED, 39);
		pm.setAcceptedTeacher(user("harsh"));

		request("yash", "Photography", "Composition tips for product shots",
				"Want better product photos for the Instagram pages I manage.", ProficiencyLevel.INTERMEDIATE,
				"Weekend mornings", SessionMode.OFFLINE, "1", RequestStatus.CANCELLED, 25);
	}

	private SkillRequest request(String learner, String skill, String title, String description,
			ProficiencyLevel level, String schedule, SessionMode mode, String hours, RequestStatus status, int daysAgo) {
		SkillRequest request = new SkillRequest();
		request.setLearner(user(learner));
		request.setSkill(skill(skill));
		request.setTitle(title);
		request.setDescription(description);
		request.setDesiredLevel(level);
		request.setPreferredSchedule(schedule);
		request.setMode(mode);
		request.setDurationHours(new BigDecimal(hours));
		request.setStatus(status);
		request.setCreatedAt(now.minusDays(daysAgo).minusHours(4));
		return requests.save(request);
	}

	private void offer(SkillRequest request, String teacher, String message, OfferStatus status, int daysAgo) {
		RequestOffer offer = new RequestOffer();
		offer.setRequest(request);
		offer.setTeacher(user(teacher));
		offer.setMessage(message);
		offer.setStatus(status);
		offer.setCreatedAt(now.minusDays(daysAgo));
		offers.save(offer);
	}

	// ------------------------------------------------------------------ chat

	private void seedMessages() {
		conversation("karthikeya", "rahul", 3, true,
				"rahul:Hey Karthikeya! Thanks again for the Java session, collections finally make sense.",
				"karthikeya:Glad it helped! Your UI/UX session was amazing too — I redesigned my project's dashboard.",
				"rahul:Ha, show me sometime! Are we still on for Spring Boot tomorrow at 6?",
				"karthikeya:Yes! I've added a Google Meet link to the session.",
				"karthikeya:We'll start with a simple user registration API and add validation.",
				"rahul:Perfect, I'll set up the project with Spring Initializr before we start 🙌");
		conversation("ananya", "karthikeya", 6, true,
				"ananya:Your Python script for renaming clips works perfectly btw!",
				"karthikeya:Awesome! Next we can try auto-generating subtitles files.",
				"ananya:Yes please. Also saw your request for the demo reel — I sent an offer 😄");
		conversation("tanvi", "karthikeya", 1, false,
				"tanvi:Hey! For our UI/UX session, could you bring your current wireframes?",
				"karthikeya:Sure, I'll bring the dashboard and onboarding screens.",
				"tanvi:Great. We'll do a quick heuristic review and then improve the flows.",
				"tanvi:Also, the React study group has a hooks deep-dive this week if you're free!");
		conversation("priya", "karthikeya", 9, true,
				"priya:Hi! Do you know any good resources for Spring Security + JWT?",
				"karthikeya:The official reference docs are great. I can also walk you through my project's setup.");
		conversation("vikram", "rohan", 2, true,
				"vikram:Solve the 3 DP problems I shared before our next session.",
				"rohan:On it! Stuck on the knapsack variation though.");
	}

	private void conversation(String a, String b, int daysAgo, boolean allRead, String... lines) {
		LocalDateTime at = now.minusDays(daysAgo).withHour(17).withMinute(5);
		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(":", 2);
			User sender = user(parts[0]);
			User recipient = parts[0].equals(a) ? user(b) : user(a);
			Message message = new Message();
			message.setSender(sender);
			message.setRecipient(recipient);
			message.setContent(parts[1]);
			message.setCreatedAt(at.plusMinutes(i * 7L));
			boolean read = allRead || i < lines.length - 2;
			message.setReadAt(read ? at.plusMinutes(i * 7L + 2) : null);
			messages.save(message);
		}
	}

	// ------------------------------------------------------------------ groups

	private void seedGroups() {
		SkillGroup react = group("divya", "React Study Group", "React",
				"Weekly study group for React learners of all levels. We build small projects, review each other's code and share resources.",
				30, GroupPrivacy.PUBLIC, 90, "karthikeya", "nikhil", "neha", "siddharth", "tanvi", "rahul");
		post(react, "divya", "Welcome everyone! 👋 This week's theme: custom hooks. Share one hook you wrote recently.", 6);
		post(react, "tanvi", "Here's my useDebounce hook for search inputs — feedback welcome!", 5);
		post(react, "karthikeya", "Great idea. I'll share a useFetch hook with abort controller support.", 4);
		post(react, "neha", "First time here, excited to learn! Is there a recommended starter project?", 2);
		event(react, "divya", "Hooks deep-dive", "We'll build a todo app using useReducer and context.", GroupEventType.SESSION, 5, 18, 90, SessionMode.ONLINE);
		event(react, "divya", "Kickoff meetup", "Introductions and planning the semester.", GroupEventType.EVENT, -30, 18, 60, SessionMode.OFFLINE);

		SkillGroup spring = group("karthikeya", "Spring Boot Builders", "Spring Boot",
				"For students building backends with Spring Boot. Bring your project questions about REST APIs, JPA, security and testing.",
				25, GroupPrivacy.PUBLIC, 60, "riya", "rahul", "nikhil", "aman");
		post(spring, "karthikeya", "Kicking things off: what are you building this semester? Drop your project idea below.", 10);
		post(spring, "riya", "Hospital appointment system with JWT auth. Struggling with JPA many-to-many right now.", 9);
		post(spring, "nikhil", "Coming from Django — Spring Data repositories feel like magic.", 3);
		event(spring, "karthikeya", "Build a REST API live", "Live-coding a CRUD API with validation and exception handling.", GroupEventType.SESSION, 6, 19, 90, SessionMode.ONLINE);

		SkillGroup photo = group("aditya", "Campus Photography Walks", "Photography",
				"Monthly photo walks around the city plus online editing critiques. Phones welcome!",
				20, GroupPrivacy.PUBLIC, 120, "meera", "kavya", "pooja", "yash");
		post(photo, "aditya", "Next walk: Golden hour at Hussain Sagar. Meet at the Lumbini Park gate.", 4);
		post(photo, "meera", "Sharing my edits from last month's walk — loved the reflections!", 20);
		event(photo, "aditya", "Golden hour photo walk", "Bring any camera or phone. We'll practise composition with leading lines.", GroupEventType.EVENT, 3, 17, 120, SessionMode.OFFLINE);

		SkillGroup dsa = group("vikram", "DSA Grind — Placement Prep", "Data Structures & Algorithms",
				"Invite-only group for focused placement preparation. Daily problems and weekly mock interviews.",
				15, GroupPrivacy.PRIVATE, 100, "rohan", "riya", "priya");
		post(dsa, "vikram", "Today's problem: longest increasing subsequence in O(n log n). Post your approach!", 1);
		GroupMember invite = new GroupMember();
		invite.setGroup(dsa);
		invite.setUser(user("karthikeya"));
		invite.setRole(GroupRole.MEMBER);
		invite.setStatus(MemberStatus.INVITED);
		invite.setInvitedBy(user("vikram"));
		invite.setCreatedAt(now.minusHours(10));
		members.save(invite);

		SkillGroup music = group("arjun", "Indie Music Jam", "Guitar",
				"Jam sessions for guitarists, singers and pianists. All genres, all levels.", 12, GroupPrivacy.PUBLIC, 80,
				"neha", "karan", "priya");
		post(music, "arjun", "Jam this Friday in the music room — bring your instruments!", 7);
	}

	private SkillGroup group(String creator, String name, String skill, String description, int max,
			GroupPrivacy privacy, int daysAgo, String... memberKeys) {
		SkillGroup group = new SkillGroup();
		group.setName(name);
		group.setDescription(description);
		group.setSkill(skill(skill));
		group.setCreator(user(creator));
		group.setMaxMembers(max);
		group.setPrivacy(privacy);
		group.setCreatedAt(now.minusDays(daysAgo));
		groups.save(group);
		member(group, creator, GroupRole.OWNER, daysAgo);
		reputation.addXp(user(creator).getId(), ReputationService.XP_CREATE_GROUP);
		int i = 0;
		for (String key : memberKeys) {
			member(group, key, GroupRole.MEMBER, Math.max(1, daysAgo - 5 - i * 3));
			i++;
		}
		return group;
	}

	private void member(SkillGroup group, String key, GroupRole role, int daysAgo) {
		GroupMember member = new GroupMember();
		member.setGroup(group);
		member.setUser(user(key));
		member.setRole(role);
		member.setStatus(MemberStatus.ACTIVE);
		member.setCreatedAt(now.minusDays(daysAgo));
		members.save(member);
	}

	private void post(SkillGroup group, String author, String content, int daysAgo) {
		GroupPost post = new GroupPost();
		post.setGroup(group);
		post.setAuthor(user(author));
		post.setContent(content);
		post.setCreatedAt(now.minusDays(daysAgo).minusHours(daysAgo));
		posts.save(post);
	}

	private void event(SkillGroup group, String creator, String title, String description, GroupEventType type,
			int inDays, int hour, int minutes, SessionMode mode) {
		GroupEvent event = new GroupEvent();
		event.setGroup(group);
		event.setCreatedBy(user(creator));
		event.setTitle(title);
		event.setDescription(description);
		event.setType(type);
		LocalDateTime start = LocalDate.now().plusDays(inDays).atTime(hour, 0);
		event.setStartTime(start);
		event.setEndTime(start.plusMinutes(minutes));
		event.setMode(mode);
		if (mode == SessionMode.ONLINE) {
			event.setMeetingLink("https://meet.google.com/group-" + group.getId());
		}
		else {
			event.setLocation(group.getSkill().getName().equals("Photography") ? "Lumbini Park main gate"
					: "Student Activity Centre, Room 104");
		}
		event.setCreatedAt(now.minusDays(Math.max(1, Math.abs(inDays) + 2)));
		events.save(event);
	}

	// ------------------------------------------------------------------ challenges

	private void seedChallenges() {
		Challenge api = challenge("karthikeya", "Build a REST API using Spring Boot",
				"Build a small REST API for a library system with Spring Boot.\n\nRequirements:\n• CRUD endpoints for books and members\n• Validation with meaningful error messages\n• A global exception handler\n• At least three unit or integration tests\n\nBonus: add JWT authentication.",
				"Spring Boot", Difficulty.MEDIUM, 10, 12);
		Submission riyaApi = submission(api, "riya", "LibraryHub API",
				"Spring Boot 3 API with books, members and loans. Includes validation, exception handling and 12 tests.",
				"https://github.com/riya-saxena/libraryhub-api", null, null, 5);
		Submission nikhilApi = submission(api, "nikhil", "Shelfie",
				"Library API with pagination, sorting and Swagger docs.", "https://github.com/nikhil-k/shelfie", null,
				null, 3);
		review(riyaApi, "karthikeya", 5, "Clean layering and excellent test coverage. Great job!");
		review(riyaApi, "rahul", 4, "Easy to read code. Would love to see JWT next.");
		review(nikhilApi, "riya", 4, "Swagger docs are a nice touch.");

		Challenge onboarding = challenge("tanvi", "Design a mobile onboarding flow in Figma",
				"Design a 4-screen onboarding flow for a student budgeting app. Share a Figma prototype link and explain your design decisions.",
				"Figma", Difficulty.MEDIUM, 7, 9);
		Submission rahulFigma = submission(onboarding, "rahul", "PennyWise onboarding",
				"Playful illustrations with progressive disclosure of permissions.", null,
				"https://www.figma.com/proto/demo-pennywise", "Focused on reducing drop-off in the permissions step.",
				4);
		submission(onboarding, "meera", "Budget Buddy",
				"Illustrated onboarding with a mascot and bold typography.", null,
				"https://www.figma.com/proto/demo-budgetbuddy", null, 2);
		review(rahulFigma, "tanvi", 5, "Thoughtful flow and great micro-copy.");

		Challenge reel = challenge("karan", "30-second cinematic reel",
				"Shoot and edit a 30-second cinematic reel of your campus. Focus on pacing, transitions and colour.",
				"Video Editing", Difficulty.EASY, 14, 6);
		submission(reel, "ananya", "Campus at Dawn", "Shot on a phone, graded in Premiere Pro.", null,
				"https://youtu.be/demo-campus-dawn", null, 2);

		Challenge graphs = challenge("vikram", "Solve 5 classic graph problems",
				"Solve: (1) number of islands, (2) course schedule, (3) Dijkstra's shortest path, (4) bipartite check, (5) minimum spanning tree. Share your code with time complexity analysis.",
				"Data Structures & Algorithms", Difficulty.HARD, -5, 25);
		submission(graphs, "rohan", "Graph-5 in C++", "All five with complexity notes.",
				"https://github.com/rohan-das/graph-5", null, null, 10);
		submission(graphs, "riya", "Graphs in Java", "Solutions with BFS/DFS templates and notes.",
				"https://github.com/riya-saxena/graphs", null, null, 8);
		submission(graphs, "karthikeya", "Graph problems — Java",
				"Clean Java solutions with unit tests for each problem.",
				"https://github.com/karthikeya-gupta/graph-problems", null, null, 7);

		challenge("divya", "Landing page with React + Tailwind",
				"Build a responsive landing page for a fictional startup using React and Tailwind CSS. Lighthouse score above 90 is a bonus.",
				"React", Difficulty.EASY, 20, 3);
	}

	private Challenge challenge(String creator, String title, String description, String skill, Difficulty difficulty,
			int deadlineInDays, int createdDaysAgo) {
		Challenge challenge = new Challenge();
		challenge.setTitle(title);
		challenge.setDescription(description);
		challenge.setSkill(skill(skill));
		challenge.setDifficulty(difficulty);
		challenge.setDeadline(LocalDate.now().plusDays(deadlineInDays).atTime(23, 59));
		challenge.setCreator(user(creator));
		challenge.setXpReward(switch (difficulty) {
			case EASY -> 50;
			case MEDIUM -> 100;
			case HARD -> 150;
		});
		challenge.setCreatedAt(now.minusDays(createdDaysAgo));
		return challenges.save(challenge);
	}

	private Submission submission(Challenge challenge, String key, String title, String description, String github,
			String demo, String text, int daysAgo) {
		Submission submission = new Submission();
		submission.setChallenge(challenge);
		submission.setUser(user(key));
		submission.setProjectTitle(title);
		submission.setDescription(description);
		submission.setGithubUrl(github);
		submission.setDemoUrl(demo);
		submission.setSubmissionText(text);
		submission.setCreatedAt(now.minusDays(daysAgo));
		submissions.save(submission);
		reputation.addXp(user(key).getId(), challenge.getXpReward());
		return submission;
	}

	private void review(Submission submission, String reviewer, int rating, String comment) {
		SubmissionReview review = new SubmissionReview();
		review.setSubmission(submission);
		review.setReviewer(user(reviewer));
		review.setRating(rating);
		review.setComment(comment);
		review.setCreatedAt(submission.getCreatedAt().plusDays(1));
		reviews.save(review);
		reputation.addXp(user(reviewer).getId(), ReputationService.XP_REVIEW_SUBMISSION);
	}

	// ------------------------------------------------------------------ reports

	private void seedReports() {
		User spammer = user("deals");
		report("kavya", spammer, ReportTargetType.USER, spammer.getId(), spammer.getFullName(),
				ReportReason.SPAM, "This account keeps advertising paid followers in groups.", ReportStatus.OPEN, 1);
		report("yash", spammer, ReportTargetType.USER, spammer.getId(), spammer.getFullName(),
				ReportReason.FAKE_PROFILE, "Not a real student, just a promo page.", ReportStatus.OPEN, 0);
		Report resolved = report("pooja", user("aman"), ReportTargetType.USER, user("aman").getId(),
				user("aman").getFullName(), ReportReason.OTHER, "Accidentally double-booked me — probably a misunderstanding.",
				ReportStatus.DISMISSED, 12);
		resolved.setAdminNote("Scheduling mix-up, no violation.");
		resolved.setResolvedAt(now.minusDays(11));
		users.findByEmailIgnoreCase(adminEmail).ifPresent(resolved::setResolvedBy);
	}

	private Report report(String reporter, User reported, ReportTargetType type, Long targetId, String preview,
			ReportReason reason, String details, ReportStatus status, int daysAgo) {
		Report report = new Report();
		report.setReporter(user(reporter));
		report.setReportedUser(reported);
		report.setTargetType(type);
		report.setTargetId(targetId);
		report.setTargetPreview(preview);
		report.setReason(reason);
		report.setDetails(details);
		report.setStatus(status);
		report.setActionTaken(ReportAction.NONE);
		report.setCreatedAt(now.minusDays(daysAgo).minusHours(2));
		return reports.save(report);
	}

	// ------------------------------------------------------------------ notifications

	private void seedNotifications() {
		User me = user("karthikeya");
		notify(me, user("aman"), NotificationType.CONNECTION_REQUEST, "New connection request",
				"Aman Khan wants to connect with you.", "/connections", 0, false);
		notify(me, user("aman"), NotificationType.SESSION_REQUEST, "New session request",
				"Aman Khan wants to learn Python with you in 2 days.", "/sessions", 0, false);
		notify(me, user("vikram"), NotificationType.GROUP_INVITATION, "Group invitation",
				"Vikram Singh invited you to join \"DSA Grind — Placement Prep\".", "/groups", 0, false);
		notify(me, user("karan"), NotificationType.REQUEST_RESPONSE, "Someone can help with Video Editing",
				"Karan Malhotra offered to help with \"Edit a 60-second project demo reel\".", "/requests", 2, false);
		notify(me, user("tanvi"), NotificationType.SESSION_ACCEPTED, "Session accepted",
				"Tanvi Desai accepted your UI/UX Design session.", "/sessions", 1, true);
		notify(me, user("neha"), NotificationType.RATING_RECEIVED, "New 5★ rating",
				"Neha Gupta rated your Java session 5/5.", "/profile", 8, true);
		notify(me, user("divya"), NotificationType.SKILL_MATCH, "New skill match: UI/UX Design",
				"Tanvi Desai can teach UI/UX Design, a skill you want to learn.", "/matches", 10, true);
		notify(me, null, NotificationType.BADGE_EARNED, "New badge: Dedicated Mentor",
				"You earned the Dedicated Mentor badge. Taught 5 completed sessions.", "/profile", 8, true);
	}

	private void notify(User recipient, User actor, NotificationType type, String title, String message, String link,
			int daysAgo, boolean read) {
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		notification.setActor(actor);
		notification.setType(type);
		notification.setTitle(title);
		notification.setMessage(message);
		notification.setLink(link);
		notification.setRead(read);
		notification.setCreatedAt(now.minusDays(daysAgo).minusMinutes(30L * (daysAgo + 1)));
		notifications.save(notification);
	}

	// ------------------------------------------------------------------ helpers

	private User user(String key) {
		User user = userByKey.get(key);
		if (user == null) {
			throw new IllegalStateException("Unknown seed user " + key);
		}
		return user;
	}

	private Skill skill(String name) {
		Skill skill = skillByName.get(name);
		if (skill == null) {
			throw new IllegalStateException("Unknown seed skill " + name);
		}
		return skill;
	}
}
