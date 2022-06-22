/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LinkedHashMap} subclass representing annotation attribute
 * <em>key-value</em> pairs as read by {@link AnnotationUtils},
 * {@link AnnotatedElementUtils}, and Spring's reflection- and ASM-based
 * {@link org.springframework.core.type.AnnotationMetadata} implementations.
 *
 * <p>Provides 'pseudo-reification' to avoid noisy Map generics in the calling
 * code as well as convenience methods for looking up annotation attributes
 * in a type-safe fashion.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
// LinkedHashMap 子类表示由 AnnotationUtils、AnnotatedElementUtils 和 Spring 的基于反射和 ASM 的
// org.springframework.core.type.AnnotationMetadata 实现读取的注解属性键值对
//
// 提供“伪具体化”以避免调用代码中出现嘈杂的 Map 泛型，以及以类型安全的方式查找注解属性的便捷方法。
// AnnotationAttributes 将注解中的属性按照 Map<String,Object> 方式存储，
// key 是注解属性名称，value 是注解属性对象，需要预知对象类型。
//
// 底层实际上是个 Map,将 Annotation 里边的属性方法变成属性
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	@Nullable
	private final Class<? extends Annotation> annotationType; // 注解类型

	final String displayName; // 注解属性名称

	boolean validated = false;


	/**
	 * Create a new, empty {@link AnnotationAttributes} instance.
	 */
	// 创建一个新的空AnnotationAttributes实例。
	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance with the
	 * given initial capacity to optimize performance.
	 * @param initialCapacity initial size of the underlying map
	 */
	// 使用给定的初始容量创建一个新的空AnnotationAttributes实例以优化性能。
	// 形参：
	//			initialCapacity – 底层 Map 的初始大小
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new {@link AnnotationAttributes} instance, wrapping the provided
	 * map and all its <em>key-value</em> pairs.
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	// 创建一个新的AnnotationAttributes实例，包装提供的地图及其所有键值对。
	// 形参：
	// 			map – 注解属性键值对的原始来源
	// 请参阅：
	//			fromMap(Map)
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * Create a new {@link AnnotationAttributes} instance, wrapping the provided
	 * map and all its <em>key-value</em> pairs.
	 * @param other original source of annotation attribute <em>key-value</em> pairs
	 * @see #fromMap(Map)
	 */
	// 创建一个新的AnnotationAttributes实例，包装提供的地图及其所有键值对。
	// 形参：
	//			other - 注解属性键值对的原始来源
	// 请参阅：
	//			fromMap(Map)
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance for the
	 * specified {@code annotationType}.
	 * @param annotationType the type of annotation represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @since 4.2
	 */
	// 为指定的annotationType创建一个新的空 AnnotationAttributes 实例。
	// 形参：
	//			annotationType – 此 AnnotationAttributes 实例表示的注解类型； 从不为 null
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * Create a possibly already validated new, empty
	 * {@link AnnotationAttributes} instance for the specified
	 * {@code annotationType}.
	 * @param annotationType the type of annotation represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @param validated if the attributes are considered already validated
	 * @since 5.2
	 */
	// 为指定的 annotationType 创建一个可能已经过验证的新的空 AnnotationAttributes 实例。
	// 形参：
	//			annotationType – 此 AnnotationAttributes 实例表示的注解类型； 从不为 null
	//			validated - 如果属性被认为已经验证
	AnnotationAttributes(Class<? extends Annotation> annotationType, boolean validated) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
		this.validated = validated;
	}

	/**
	 * Create a new, empty {@link AnnotationAttributes} instance for the
	 * specified {@code annotationType}.
	 * @param annotationType the annotation type name represented by this
	 * {@code AnnotationAttributes} instance; never {@code null}
	 * @param classLoader the ClassLoader to try to load the annotation type on,
	 * or {@code null} to just store the annotation type name
	 * @since 4.3.2
	 */
	// 为指定的annotationType创建一个新的空AnnotationAttributes实例。
	// 形参：
	//			annotationType – 此 AnnotationAttributes 实例表示的注解类型名称； 从不为 null
	//			classLoader – 尝试加载注解类型的类加载器，或 null 仅存储注解类型名称
	public AnnotationAttributes(String annotationType, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> getAnnotationType(String annotationType, @Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// Annotation Class not resolvable
				// 注解类不可解析
			}
		}
		return null;
	}


	/**
	 * Get the type of annotation represented by this {@code AnnotationAttributes}.
	 * @return the annotation type, or {@code null} if unknown
	 * @since 4.2
	 */
	// 获取由此AnnotationAttributes表示的AnnotationAttributes 。
	// 返回值：
	//			注解类型，如果未知，则为null
	@Nullable
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a string.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定 attributeName 下的值作为字符串。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an
	 * array of strings.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * a string, it will be wrapped in a single-element array before
	 * returning it.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为字符串数组。
	// 如果存储在指定attributeName下的值是字符串，则在返回之前将其包装在单元素数组中。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a boolean.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为布尔值。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a number.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为数字。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an enum.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为枚举。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as a class.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为一个类。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * Get the value stored under the specified {@code attributeName} as an
	 * array of classes.
	 * <p>If the value stored under the specified {@code attributeName} is a class,
	 * it will be wrapped in a single-element array before returning it.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的值作为类数组。
	// 如果存储在指定attributeName下的值是一个类，它将在返回之前包装在一个单元素数组中。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * Get the {@link AnnotationAttributes} stored under the specified
	 * {@code attributeName}.
	 * <p>Note: if you expect an actual annotation, invoke
	 * {@link #getAnnotation(String, Class)} instead.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定attributeName下的AnnotationAttributes 。
	// 注意：如果您需要实际的注解，请getAnnotation(String, Class)调用getAnnotation(String, Class) 。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			AnnotationAttributes
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * Get the annotation of type {@code annotationType} stored under the
	 * specified {@code attributeName}.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	// 获取存储在指定attributeName下的 annotationType 类型的annotationType 。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	//			annotationType – 预期的注解类型； 从不为null
	// 返回值：
	//			the annotation
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * Get the array of {@link AnnotationAttributes} stored under the specified
	 * {@code attributeName}.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * an instance of {@code AnnotationAttributes}, it will be wrapped in
	 * a single-element array before returning it.
	 * <p>Note: if you expect an actual array of annotations, invoke
	 * {@link #getAnnotationArray(String, Class)} instead.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @return the array of {@code AnnotationAttributes}
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定 attributeName 下的 AnnotationAttributes 数组。
	// 如果存储在指定 attributeName 下的值是 AnnotationAttributes 一个实例，它将在返回之前包装在一个单元素数组中。
	// 注意：如果您需要实际的注解数组，请 getAnnotationArray(String, Class) 调用 getAnnotationArray(String, Class) 。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	// 返回值：
	//			AnnotationAttributes数组
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * Get the array of type {@code annotationType} stored under the specified
	 * {@code attributeName}.
	 * <p>If the value stored under the specified {@code attributeName} is
	 * an {@code Annotation}, it will be wrapped in a single-element array
	 * before returning it.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param annotationType the expected annotation type; never {@code null}
	 * @return the annotation array
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 * @since 4.2
	 */
	// 获取存储在指定 attributeName 下的类型为 annotationType 的数组。
	// 如果存储在指定 attributeName下的值是 Annotation ，则在返回之前将其包装在一个单元素数组中。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	//			annotationType – 预期的注解类型； 从不为null
	// 返回值：
	//			注解数组
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * Get the value stored under the specified {@code attributeName},
	 * ensuring that the value is of the {@code expectedType}.
	 * <p>If the {@code expectedType} is an array and the value stored
	 * under the specified {@code attributeName} is a single element of the
	 * component type of the expected array type, the single element will be
	 * wrapped in a single-element array of the appropriate type before
	 * returning it.
	 * @param attributeName the name of the attribute to get;
	 * never {@code null} or empty
	 * @param expectedType the expected type; never {@code null}
	 * @return the value
	 * @throws IllegalArgumentException if the attribute does not exist or
	 * if it is not of the expected type
	 */
	// 获取存储在指定 attributeName 下的值，确保该值是 expectedType 。
	// 如果 expectedType 是一个数组，并且存储在指定 attributeName 下的值是预期数组类型的组件类型的单个元素，
	// 则该单个元素将在返回之前包装在适当类型的单元素数组中。
	// 形参：
	//			attributeName - 要获取的属性的名称； 从不为null或为空
	//			expectedType - 预期类型; 从不为null
	// 返回值：
	// 			the value
	// IllegalArgumentException – 如果属性不存在或不是预期类型
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		Assert.notNull(attributeValue, () -> String.format(
				"Attribute '%s' not found in attributes for annotation [%s]",
				attributeName, this.displayName));
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Throwable) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Throwable) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			if (entries.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * Return an {@link AnnotationAttributes} instance based on the given map.
	 * <p>If the map is already an {@code AnnotationAttributes} instance, it
	 * will be cast and returned immediately without creating a new instance.
	 * Otherwise a new instance will be created by passing the supplied map
	 * to the {@link #AnnotationAttributes(Map)} constructor.
	 * @param map original source of annotation attribute <em>key-value</em> pairs
	 */
	// 根据给定的地图返回 AnnotationAttributes 实例。
	// 如果 Map 已经是 AnnotationAttributes 实例，它将被立即转换并返回，而无需创建新实例。 
	// 否则，将通过将提供的映射传递给 AnnotationAttributes(Map) 构造函数来创建一个新实例。
	// 形参：
	//			map – 注解属性键值对的原始来源
	@Nullable
	public static AnnotationAttributes fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}

}
