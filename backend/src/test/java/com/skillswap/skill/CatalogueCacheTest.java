package com.skillswap.skill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import com.skillswap.auth.AuthService;
import com.skillswap.auth.dto.RegisterRequest;
import com.skillswap.config.CacheConfig;
import com.skillswap.skill.dto.SkillDtos.CategoryDto;
import com.skillswap.skill.dto.SkillDtos.CategoryUpsertRequest;
import com.skillswap.skill.dto.SkillDtos.SkillDto;
import com.skillswap.skill.dto.SkillDtos.SkillUpsertRequest;
import com.skillswap.skill.dto.SkillDtos.UserSkillRequest;

/**
 * Checks that the catalogue caches are filled by reads and emptied by every write that changes them. Not
 * {@code @Transactional}: evictions only happen once a transaction commits, which is exactly what is being tested.
 */
@SpringBootTest
class CatalogueCacheTest {

	@Autowired
	private CategoryService categoryService;
	@Autowired
	private SkillService skillService;
	@Autowired
	private UserSkillService userSkillService;
	@Autowired
	private AuthService authService;
	@Autowired
	private CacheManager cacheManager;

	private Cache categories;
	private Cache skills;

	@BeforeEach
	void setUp() {
		categories = cacheManager.getCache(CacheConfig.CATEGORIES);
		skills = cacheManager.getCache(CacheConfig.SKILLS);
		categories.clear();
		skills.clear();
	}

	@Test
	void categoryListIsCachedAndEvictedWhenACategoryChanges() {
		List<CategoryDto> first = categoryService.list();
		assertThat(categories.get(SimpleKey.EMPTY)).isNotNull();
		assertThat(categoryService.list()).isSameAs(first);

		CategoryDto created = categoryService.create(new CategoryUpsertRequest("Cache-" + System.nanoTime(), null,
				null, null));

		assertThat(categories.get(SimpleKey.EMPTY)).isNull();
		assertThat(categoryService.list()).extracting(CategoryDto::id).contains(created.id());
	}

	@Test
	void onlyTheUnfilteredSkillCatalogueIsCached() {
		skillService.search("java", null);
		skillService.search(null, 1L);
		assertThat(skills.get("all")).isNull();

		List<SkillDto> all = skillService.search(null, null);
		assertThat(skills.get("all")).isNotNull();
		assertThat(skillService.search("  ", null)).isSameAs(all);
	}

	@Test
	void skillWritesAndProfileSkillChangesEvictTheCatalogue() {
		CategoryDto category = categoryService.create(new CategoryUpsertRequest("Cat-" + System.nanoTime(), null,
				null, null));
		SkillDto skill = skillService.create(new SkillUpsertRequest("Skill-" + System.nanoTime(), null,
				category.id(), true));

		skillService.search(null, null);
		categoryService.list();
		skillService.update(skill.id(), new SkillUpsertRequest(skill.name(), "Updated", category.id(), true));
		assertThat(skills.get("all")).isNull();
		assertThat(categories.get(SimpleKey.EMPTY)).isNull();

		Long userId = authService.register(new RegisterRequest("Cache Tester", "cache" + System.nanoTime()
				+ "@test.dev", "secret123", "CBIT", "CSE", 2)).user().id();
		skillService.search(null, null);
		var added = userSkillService.add(userId, new UserSkillRequest(skill.id(), SkillType.TEACH,
				ProficiencyLevel.ADVANCED, 2.0, null));
		assertThat(skills.get("all")).isNull();
		assertThat(skillService.search(null, null)).filteredOn(s -> s.id().equals(skill.id()))
				.singleElement().extracting(SkillDto::teacherCount).isEqualTo(1L);

		userSkillService.delete(userId, added.id());
		assertThat(skills.get("all")).isNull();
	}
}
