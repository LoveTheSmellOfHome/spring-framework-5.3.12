/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Selects which implementation of {@link AbstractCachingConfiguration} should
 * be used based on the value of {@link EnableCaching#mode} on the importing
 * {@code @Configuration} class.
 *
 * <p>Detects the presence of JSR-107 and enables JCache support accordingly.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 * @see ProxyCachingConfiguration
 */
// 根据导入 @Configuration 类的 EnableCaching.mode 值选择应该使用哪个 AbstractCachingConfiguration 实现。
// 检测 JSR-107 的存在并相应地启用 JCache 支持。
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

	private static final String PROXY_JCACHE_CONFIGURATION_CLASS =
			"org.springframework.cache.jcache.config.ProxyJCacheConfiguration";

	private static final String CACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJCachingConfiguration";

	private static final String JCACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJJCacheConfiguration";


	// JAVA JCashe 标准
	private static final boolean jsr107Present;

	private static final boolean jcacheImplPresent;

	static {
		ClassLoader classLoader = CachingConfigurationSelector.class.getClassLoader();
		// 确定 Cache 是否存在并且可以加载
		jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
		// 确定 ProxyJCacheConfiguration 是否存在并且可以加载
		jcacheImplPresent = ClassUtils.isPresent(PROXY_JCACHE_CONFIGURATION_CLASS, classLoader);
	}


	/**
	 * Returns {@link ProxyCachingConfiguration} or {@code AspectJCachingConfiguration}
	 * for {@code PROXY} and {@code ASPECTJ} values of {@link EnableCaching#mode()},
	 * respectively. Potentially includes corresponding JCache configuration as well.
	 */
	// 分别为 EnableCaching.mode() 的 PROXY 和 ASPECTJ 值返回 ProxyCachingConfiguration或AspectJCachingConfiguration 。
	// 可能还包括相应的 JCache 配置。
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return getProxyImports();
			case ASPECTJ:
				return getAspectJImports();
			default:
				return null;
		}
	}

	/**
	 * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#PROXY}.
	 * <p>Take care of adding the necessary JSR-107 import if it is available.
	 */
	// 如果 AdviceMode 设置为 AdviceMode.PROXY ，则返回要使用的导入。
	// 如果可用，请注意添加必要的 JSR-107 导入
	private String[] getProxyImports() {
		List<String> result = new ArrayList<>(3);
		// 添加自动代理创建者
		result.add(AutoProxyRegistrar.class.getName());
		// 添加配置类 ProxyCachingConfiguration
		result.add(ProxyCachingConfiguration.class.getName());
		if (jsr107Present && jcacheImplPresent) {
			result.add(PROXY_JCACHE_CONFIGURATION_CLASS);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#ASPECTJ}.
	 * <p>Take care of adding the necessary JSR-107 import if it is available.
	 */
	// 如果 AdviceMode 设置为 AdviceMode.ASPECTJ ，则返回要使用的导入。
	// 如果可用，请注意添加必要的 JSR-107 导入。
	private String[] getAspectJImports() {
		List<String> result = new ArrayList<>(2);
		result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		if (jsr107Present && jcacheImplPresent) {
			result.add(JCACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		}
		return StringUtils.toStringArray(result);
	}

}
