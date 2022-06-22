/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.convert.support;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.lang.Nullable;

/**
 * A specialization of {@link GenericConversionService} configured by default
 * with converters appropriate for most environments.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad-hoc
 * use against any {@code ConverterRegistry} instance.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
// {@link GenericConversionService} 的特殊化默认配置了适合大多数环境的转换器。
//
// <p>专为直接实例化而设计，但也公开了静态 {@link addDefaultConverters(ConverterRegistry)}
// 实用程序方法，用于针对任何 {@code ConverterRegistry} 实例进行临时使用。
//
// Spring Framework 默认的类型转换服务
public class DefaultConversionService extends GenericConversionService {

	// 享元模式（Flyweight）实现：DefaultConversionService，基于 ClassLoader 级别的缓存，也就是在 JVM 内单例共享
	@Nullable
	private static volatile DefaultConversionService sharedInstance;


	/**
	 * Create a new {@code DefaultConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) default converters}.
	 */
	// 使用一组 {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) 默认转换器}
	// 创建一个新的 {@code DefaultConversionService}。
	public DefaultConversionService() {
		addDefaultConverters(this);
	}


	/**
	 * Return a shared default {@code ConversionService} instance,
	 * lazily building it once needed.
	 * <p><b>NOTE:</b> We highly recommend constructing individual
	 * {@code ConversionService} instances for customization purposes.
	 * This accessor is only meant as a fallback for code paths which
	 * need simple type coercion but cannot access a longer-lived
	 * {@code ConversionService} instance any other way.
	 * @return the shared {@code ConversionService} instance (never {@code null})
	 * @since 4.3.5
	 */
	// 返回一个共享的默认 {@code ConversionService} 实例，在需要时延迟构建它。
	// <p><b>注意：<b>我们强烈建议为自定义目的构建单独的 {@code ConversionService} 实例。
	// 此访问器仅用作需要简单类型强制但无法以任何其他方式访问寿命较长的 {@code ConversionService} 实例的代码路径的后备。
	// 懒加载的过程
	public static ConversionService getSharedInstance() {
		// 单例，一个 ClassLoader 只能有一个
		DefaultConversionService cs = sharedInstance;
		if (cs == null) {
			synchronized (DefaultConversionService.class) {
				cs = sharedInstance;
				if (cs == null) {
					cs = new DefaultConversionService();
					sharedInstance = cs;
				}
			}
		}
		return cs;
	}

	/**
	 * Add converters appropriate for most environments.
	 * @param converterRegistry the registry of converters to add to
	 * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
	 */
	// 添加适用于大多数环境的转换器。
	// @param converterRegistry 要添加到的转换器注册表（也必须可转换为 ConversionService，
	// 例如作为 {@link ConfigurableConversionService}）
	public static void addDefaultConverters(ConverterRegistry converterRegistry) {
		// 添加和精度相关的转换器
		addScalarConverters(converterRegistry);
		// 添加集合相关的转换器
		addCollectionConverters(converterRegistry);

		converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new StringToTimeZoneConverter());
		converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
		converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());

		converterRegistry.addConverter(new ObjectToObjectConverter());
		converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new FallbackObjectToStringConverter());
		converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
	}

	/**
	 * Add common collection converters.
	 * @param converterRegistry the registry of converters to add to
	 * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
	 * @since 4.2.3
	 */
	// 添加常用集合转换器。
	// @param converterRegistry 要添加到的转换器注册表（也必须可转换为 ConversionService，例如作为
	// {@link ConfigurableConversionService}）
	public static void addCollectionConverters(ConverterRegistry converterRegistry) {
		ConversionService conversionService = (ConversionService) converterRegistry;

		converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
		converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
		converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
		converterRegistry.addConverter(new MapToMapConverter(conversionService));

		converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));

		converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToCollectionConverter(conversionService));

		converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));

		converterRegistry.addConverter(new StreamConverter(conversionService));
	}

	// 添加和精度(刻度)相关的转换器
	private static void addScalarConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

		converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
		converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharacterConverter());
		converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new NumberToCharacterConverter());
		converterRegistry.addConverterFactory(new CharacterToNumberFactory());

		converterRegistry.addConverter(new StringToBooleanConverter());
		converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToStringConverter((ConversionService) converterRegistry));

		converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToIntegerConverter((ConversionService) converterRegistry));

		converterRegistry.addConverter(new StringToLocaleConverter());
		converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharsetConverter());
		converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCurrencyConverter());
		converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToPropertiesConverter());
		converterRegistry.addConverter(new PropertiesToStringConverter());

		converterRegistry.addConverter(new StringToUUIDConverter());
		converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
	}

}
