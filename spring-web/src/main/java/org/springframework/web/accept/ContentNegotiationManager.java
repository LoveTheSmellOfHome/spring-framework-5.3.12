/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.*;
import java.util.function.Function;

/**
 * Central class to determine requested {@linkplain MediaType media types}
 * for a request. This is done by delegating to a list of configured
 * {@code ContentNegotiationStrategy} instances.
 *
 * <p>Also provides methods to look up file extensions for a media type.
 * This is done by delegating to the list of configured
 * {@code MediaTypeFileExtensionResolver} instances.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
// 确定请求的请求媒体类型的中心类。这是通过委托给已配置的ContentNegotiationStrategy实例列表来完成的。
//
// 还提供了查找媒体类型的文件扩展名的方法。这是通过委托给配置的MediaTypeFileExtensionResolver实例列表来完成的。
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

	private final List<ContentNegotiationStrategy> strategies = new ArrayList<>();

	private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();


	/**
	 * Create an instance with the given list of
	 * {@code ContentNegotiationStrategy} strategies each of which may also be
	 * an instance of {@code MediaTypeFileExtensionResolver}.
	 * @param strategies the strategies to use
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		this(Arrays.asList(strategies));
	}

	/**
	 * A collection-based alternative to
	 * {@link #ContentNegotiationManager(ContentNegotiationStrategy...)}.
	 * @param strategies the strategies to use
	 * @since 3.2.2
	 */
	// 使用给定的ContentNegotiationStrategy策略列表创建一个实例，每个策略也可能
	// 是 MediaTypeFileExtensionResolver 的一个实例。
	// 参形：
	//			策略——使用的策略
	public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		this.strategies.addAll(strategies);
		for (ContentNegotiationStrategy strategy : this.strategies) {
			if (strategy instanceof MediaTypeFileExtensionResolver) {
				this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
			}
		}
	}

	/**
	 * Create a default instance with a {@link HeaderContentNegotiationStrategy}.
	 */
	// 使用 HeaderContentNegotiationStrategy 创建一个默认实例
	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}


	/**
	 * Return the configured content negotiation strategies.
	 * @since 3.2.16
	 */
	// 返回配置的内容协商策略
	public List<ContentNegotiationStrategy> getStrategies() {
		return this.strategies;
	}

	/**
	 * Find a {@code ContentNegotiationStrategy} of the given type.
	 * @param strategyType the strategy type
	 * @return the first matching strategy, or {@code null} if none
	 * @since 4.3
	 */
	// 查找给定类型的ContentNegotiationStrategy 。
	// 参形：
	//			strategyType – 策略类型
	// 返回值：
	//			第一个匹配策略，如果没有，则返回null
	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
		for (ContentNegotiationStrategy strategy : getStrategies()) {
			if (strategyType.isInstance(strategy)) {
				return (T) strategy;
			}
		}
		return null;
	}

	/**
	 * Register more {@code MediaTypeFileExtensionResolver} instances in addition
	 * to those detected at construction.
	 * @param resolvers the resolvers to add
	 */
	// 除了在构造时检测到的实例之外，还注册更多的 MediaTypeFileExtensionResolver 实例。
	// 参形：
	//			resolvers – 要添加的解析器
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		Collections.addAll(this.resolvers, resolvers);
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
		for (ContentNegotiationStrategy strategy : this.strategies) {
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
			if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
				continue;
			}
			return mediaTypes;
		}
		return MEDIA_TYPE_ALL_LIST;
	}

	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		return doResolveExtensions(resolver -> resolver.resolveFileExtensions(mediaType));
	}

	/**
	 * {@inheritDoc}
	 * <p>At startup this method returns extensions explicitly registered with
	 * either {@link PathExtensionContentNegotiationStrategy} or
	 * {@link ParameterContentNegotiationStrategy}. At runtime if there is a
	 * "path extension" strategy and its
	 * {@link PathExtensionContentNegotiationStrategy#setUseRegisteredExtensionsOnly(boolean)
	 * useRegisteredExtensionsOnly} property is set to "false", the list of extensions may
	 * increase as file extensions are resolved via
	 * {@link org.springframework.http.MediaTypeFactory} and cached.
	 */
	// 获取所有注册的文件扩展名。
	//
	// 在启动时，此方法返回使用 PathExtensionContentNegotiationStrategy 或
	// ParameterContentNegotiationStrategy 显式注册的扩展。在运行时，如果存在“路径扩展”
	// 策略并且其 useRegisteredExtensionsOnly 属性设置为 “false”， 则扩展列表可能会随着文件扩展名
	// 通过 org.springframework.http.MediaTypeFactory 解析并缓存而增加。
	//
	// 指定的：
	//接口MediaTypeFileExtensionResolver中的getAllFileExtensions
	//返回值：
	//扩展列表或空列表（从不为null ）
	@Override
	public List<String> getAllFileExtensions() {
		return doResolveExtensions(MediaTypeFileExtensionResolver::getAllFileExtensions);
	}

	private List<String> doResolveExtensions(Function<MediaTypeFileExtensionResolver, List<String>> extractor) {
		List<String> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			List<String> extensions = extractor.apply(resolver);
			if (CollectionUtils.isEmpty(extensions)) {
				continue;
			}
			result = (result != null ? result : new ArrayList<>(4));
			for (String extension : extensions) {
				if (!result.contains(extension)) {
					result.add(extension);
				}
			}
		}
		return (result != null ? result : Collections.emptyList());
	}

	/**
	 * Return all registered lookup key to media type mappings by iterating
	 * {@link MediaTypeFileExtensionResolver}s.
	 * @since 5.2.4
	 */
	// 通过迭代 MediaTypeFileExtensionResolver 将所有已注册的查找键返回到媒体类型映射
	public Map<String, MediaType> getMediaTypeMappings() {
		Map<String, MediaType> result = null;
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			if (resolver instanceof MappingMediaTypeFileExtensionResolver) {
				Map<String, MediaType> map = ((MappingMediaTypeFileExtensionResolver) resolver).getMediaTypes();
				if (CollectionUtils.isEmpty(map)) {
					continue;
				}
				result = (result != null ? result : new HashMap<>(4));
				result.putAll(map);
			}
		}
		return (result != null ? result : Collections.emptyMap());
	}

}
