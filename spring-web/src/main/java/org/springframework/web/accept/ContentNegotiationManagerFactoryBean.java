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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * Factory to create a {@code ContentNegotiationManager} and configure it with
 * {@link ContentNegotiationStrategy} instances.
 *
 * <p>This factory offers properties that in turn result in configuring the
 * underlying strategies. The table below shows the property names, their
 * default settings, as well as the strategies that they help to configure:
 *
 * <table>
 * <tr>
 * <th>Property Setter</th>
 * <th>Default Value</th>
 * <th>Underlying Strategy</th>
 * <th>Enabled Or Not</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension favorPathExtension}</td>
 * <td>false (as of 5.3)</td>
 * <td>{@link PathExtensionContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>Enabled</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>Off</td>
 * </tr>
 * </table>
 *
 * <p>Alternatively you can avoid use of the above convenience builder
 * methods and set the exact strategies to use via
 * {@link #setStrategies(List)}.
 *
 * <p><strong>Deprecation Note:</strong> As of 5.2.4,
 * {@link #setFavorPathExtension(boolean) favorPathExtension} and
 * {@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}
 * are deprecated in order to discourage using path extensions for content
 * negotiation and for request mapping with similar deprecations on
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 * RequestMappingHandlerMapping}. For further context, please read issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>.
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	@Nullable
	private List<ContentNegotiationStrategy> strategies;


	private boolean favorParameter = false;

	private String parameterName = "format";

	private boolean favorPathExtension = false;

	private Map<String, MediaType> mediaTypes = new HashMap<>();

	private boolean ignoreUnknownPathExtensions = true;

	@Nullable
	private Boolean useRegisteredExtensionsOnly;

	private boolean ignoreAcceptHeader = false;

	@Nullable
	private ContentNegotiationStrategy defaultNegotiationStrategy;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private ServletContext servletContext;


	/**
	 * Set the exact list of strategies to use.
	 * <p><strong>Note:</strong> use of this method is mutually exclusive with
	 * use of all other setters in this class which customize a default, fixed
	 * set of strategies. See class level doc for more details.
	 * @param strategies the strategies to use
	 * @since 5.0
	 */
	// 设置要使用的确切策略列表。
	// 注意：此方法的使用与此类中自定义默认、固定策略集的所有其他设置器的使用是互斥的。
	// 有关更多详细信息，请参阅类级别文档。
	// 参形：
	//			策略——使用的策略
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * Whether a request parameter ("format" by default) should be used to
	 * determine the requested media type. For this option to work you must
	 * register {@link #setMediaTypes media type mappings}.
	 * <p>By default this is set to {@code false}.
	 * @see #setParameterName
	 */
	// 是否应使用请求参数（默认为“格式”）来确定请求的媒体类型。要使此选项起作用，您必须注册media type mappings 。
	// 默认情况下，这设置为false
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * Set the query parameter name to use when {@link #setFavorParameter} is on.
	 * <p>The default parameter name is {@code "format"}.
	 */
	// 设置打开setFavorParameter时要使用的查询参数名称。
	// 默认参数名称是"format
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * Whether the path extension in the URL path should be used to determine
	 * the requested media type.
	 * <p>By default this is set to {@code false} in which case path extensions
	 * have no impact on content negotiation.
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options. As there is no replacement for this method,
	 * in 5.2.x it is necessary to set it to {@code false}. In 5.3 the default
	 * changes to {@code false} and use of this property becomes unnecessary.
	 */
	// 是否应使用 URL 路径中的路径扩展来确定请求的媒体类型。
	// 默认情况下，这设置为false在这种情况下路径扩展对内容协商没有影响。
	// 已弃用
	// 从 5.2.4 开始。请参阅有关弃用路径扩展配置选项的类级别注释。由于此方法没有替代品，因此在 5.2.x 中
	// 必须将其设置为false 。在 5.3 中，默认更改为false并且不需要使用此属性。
	@Deprecated
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * Add a mapping from a key to a MediaType where the key are normalized to
	 * lowercase and may have been extracted from a path extension, a filename
	 * extension, or passed as a query parameter.
	 * <p>The {@link #setFavorParameter(boolean) parameter strategy} requires
	 * such mappings in order to work while the {@link #setFavorPathExtension(boolean)
	 * path extension strategy} can fall back on lookups via
	 * {@link ServletContext#getMimeType} and
	 * {@link org.springframework.http.MediaTypeFactory}.
	 * <p><strong>Note:</strong> Mappings registered here may be accessed via
	 * {@link ContentNegotiationManager#getMediaTypeMappings()} and may be used
	 * not only in the parameter and path extension strategies. For example,
	 * with the Spring MVC config, e.g. {@code @EnableWebMvc} or
	 * {@code <mvc:annotation-driven>}, the media type mappings are also plugged
	 * in to:
	 * <ul>
	 * <li>Determine the media type of static resources served with
	 * {@code ResourceHttpRequestHandler}.
	 * <li>Determine the media type of views rendered with
	 * {@code ContentNegotiatingViewResolver}.
	 * <li>List safe extensions for RFD attack detection (check the Spring
	 * Framework reference docs for details).
	 * </ul>
	 * @param mediaTypes media type mappings
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	// 添加从键到 MediaType 的映射，其中键被规范化为小写，并且可能已从路径扩展名、文件扩展名或作为查询参数传递。
	//
	// parameter strategy需要这样的映射才能工作，而path extension strategy 可以通过 ServletContext.getMimeType
	// 和 MediaTypeFactory 依靠查找。
	//
	// 注意：此处注册的映射可以通过 ContentNegotiationManager.getMediaTypeMappings() 访问，并且不仅可以用于参数和路径扩展策略。
	// 例如，使用 Spring MVC 配置，例如@EnableWebMvc或  ，媒体类型映射也插入到：
	// -确定使用ResourceHttpRequestHandler服务的静态资源的媒体类型。
	// -确定使用ContentNegotiatingViewResolver呈现的视图的媒体类型。
	// -列出 RFD 攻击检测的安全扩展（查看 Spring Framework
	public void setMediaTypes(Properties mediaTypes) {
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			mediaTypes.forEach((key, value) ->
					addMediaType((String) key, MediaType.valueOf((String) value)));
		}
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	// 用于编程注册的 setMediaTypes 的替代方法
	public void addMediaType(String key, MediaType mediaType) {
		this.mediaTypes.put(key.toLowerCase(Locale.ENGLISH), mediaType);
	}

	/**
	 * An alternative to {@link #setMediaTypes} for programmatic registrations.
	 */
	// 用于编程注册的 setMediaTypes 的替代方法
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			mediaTypes.forEach(this::addMediaType);
		}
	}

	/**
	 * Whether to ignore requests with path extension that cannot be resolved
	 * to any media type. Setting this to {@code false} will result in an
	 * {@code HttpMediaTypeNotAcceptableException} if there is no match.
	 * <p>By default this is set to {@code true}.
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options.
	 */
	// 是否忽略路径扩展无法解析为任何媒体类型的请求。如果不匹配，将此设置为 false 将导致 HttpMediaTypeNotAcceptableException 。
	// 默认情况下，这设置为 true 。
	// 已弃用
	//				从 5.2.4 开始。请参阅有关弃用路径扩展配置选项的类级别注释
	@Deprecated
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * Indicate whether to use the Java Activation Framework as a fallback option
	 * to map from file extensions to media types.
	 * @deprecated as of 5.0, in favor of {@link #setUseRegisteredExtensionsOnly(boolean)},
	 * which has reverse behavior.
	 */
	// 指示是否使用 Java 激活框架作为从文件扩展名映射到媒体类型的后备选项。
	//已弃用
	//从 5.0 开始，支持setUseRegisteredExtensionsOnly(boolean) ，它具有相反的行为。
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * When {@link #setFavorPathExtension favorPathExtension} or
	 * {@link #setFavorParameter(boolean)} is set, this property determines
	 * whether to use only registered {@code MediaType} mappings or to allow
	 * dynamic resolution, e.g. via {@link MediaTypeFactory}.
	 * <p>By default this is not set in which case dynamic resolution is on.
	 */
	// 当设置了喜爱路径扩展或favorPathExtension setFavorParameter(boolean)时，此属性确定是仅使用已注册的MediaType映射还是允许动态解析，例如通过MediaTypeFactory 。
	// 默认情况下，在启用动态分辨率的情况下未设置此项
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * Whether to disable checking the 'Accept' request header.
	 * <p>By default this value is set to {@code false}.
	 */
	// 是否禁用检查“接受”请求标头。
	// 默认情况下，此值设置为 false
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * Set the default content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @see #setDefaultContentTypeStrategy
	 */
	// 设置在没有请求内容类型时使用的默认内容类型。
	// 默认情况下未设置。
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * Set the default content types to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 5.0
	 * @see #setDefaultContentTypeStrategy
	 */
	// 设置在没有请求内容类型时使用的默认内容类型。
	// 默认情况下未设置
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * Set a custom {@link ContentNegotiationStrategy} to use to determine
	 * the content type to use when no content type is requested.
	 * <p>By default this is not set.
	 * @since 4.1.2
	 * @see #setDefaultContentType
	 */
	// 设置自定义 ContentNegotiationStrategy 用于确定在没有请求内容类型时使用的内容类型。
	// 默认情况下未设置
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * Invoked by Spring to inject the ServletContext.
	 */
	// 由 Spring 调用以注入 ServletContext
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * Create and initialize a {@link ContentNegotiationManager} instance.
	 * @since 5.0
	 */
	// 创建并初始化一个ContentNegotiationManager实例
	@SuppressWarnings("deprecation")
	public ContentNegotiationManager build() {
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		}
		else {
			if (this.favorPathExtension) {
				PathExtensionContentNegotiationStrategy strategy;
				if (this.servletContext != null && !useRegisteredExtensionsOnly()) {
					strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
				}
				else {
					strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
				}
				strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				strategies.add(strategy);
			}
			if (this.favorParameter) {
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				else {
					strategy.setUseRegisteredExtensionsOnly(true);  // backwards compatibility
				}
				strategies.add(strategy);
			}
			if (!this.ignoreAcceptHeader) {
				strategies.add(new HeaderContentNegotiationStrategy());
			}
			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		this.contentNegotiationManager = new ContentNegotiationManager(strategies);

		// Ensure media type mappings are available via ContentNegotiationManager#getMediaTypeMappings()
		// independent of path extension or parameter strategies.
		// 确保媒体类型映射可通过 ContentNegotiationManagergetMediaTypeMappings() 获得，而与路径扩展或参数策略无关。
		if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorPathExtension && !this.favorParameter) {
			this.contentNegotiationManager.addFileExtensionResolvers(
					new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
		}

		return this.contentNegotiationManager;
	}


	@Override
	@Nullable
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
