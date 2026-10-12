package com.skillswap.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caches for the read-heavy skill catalogue. Only bounded, non-user-specific results are cached, and every
 * write that changes them evicts the whole cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	/** Category list with active-skill counts. */
	public static final String CATEGORIES = "categories";

	/** The full active skill catalogue with teacher and learner counts (unfiltered searches only). */
	public static final String SKILLS = "skills";

	/**
	 * Evictions and puts are deferred until the surrounding transaction commits, so a concurrent read can never
	 * re-cache data from before the change.
	 */
	@Bean
	public CacheManager cacheManager() {
		ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(CATEGORIES, SKILLS);
		manager.setAllowNullValues(false);
		return new TransactionAwareCacheManagerProxy(manager);
	}
}
