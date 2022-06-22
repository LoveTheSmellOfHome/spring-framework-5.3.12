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

package org.springframework.core.convert;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Contextual descriptor about a type to convert from or to.
 * Capable of representing arrays and generic collection types.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 */
// 关于要转换的类型的上下文描述符。能够表示数组和泛型集合类型。类型转换中的泛型处理
//
// 类型描述：既可以表示 原生类型，又可以表示 自定义类型或者 Java 中常见类型，
// 它可以使用 ResolvableType 来处理泛型。它不限于某一种场景，比直接指定泛型 S 和 T 之间的转换更有弹性，
// 使二者之间的转换的判断更加前置（有了预判）
@SuppressWarnings("serial")
public class TypeDescriptor implements Serializable {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementAdapter annotatedElement;


	/**
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * constructor parameter, method parameter, or method return value.
	 * @param methodParameter the method parameter
	 */
	// 从 {@link MethodParameter} 创建一个新的类型描述符。
	// <p>当源或目标转换点是构造函数参数、方法参数或方法返回值时，使用此构造函数。
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Field}.
	 * <p>Use this constructor when a source or target conversion point is a field.
	 * @param field the field
	 */
	// 从 {@link Field} 创建一个新的类型描述符。
	// <p>当源或目标转换点是一个字段时使用此构造函数。
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link Property}.
	 * <p>Use this constructor when a source or target conversion point is a
	 * property on a Java class.
	 * @param property the property
	 */
	// 从 {@link Property} 创建一个新的类型描述符。
	// 当源或目标转换点是 Java 类上的属性时，使用此构造函数。
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
	}

	/**
	 * Create a new type descriptor from a {@link ResolvableType}.
	 * <p>This constructor is used internally and may also be used by subclasses
	 * that support non-Java languages with extended type systems. It is public
	 * as of 5.1.4 whereas it was protected before.
	 * @param resolvableType the resolvable type
	 * @param type the backing type (or {@code null} if it should get resolved)
	 * @param annotations the type annotations
	 * @since 4.0
	 */
	// 从 {@link ResolvableType} 创建一个新的类型描述符。
	// <p>此构造函数在内部使用，也可由支持具有扩展类型系统的非 Java 语言的子类使用。从 5.1.4 开始，它是公开的，而之前是受保护的。
	// @param resolvableType 可解析类型
	// @param 输入支持类型（或 {@code null} 如果它应该得到解决）
	// @param annotations 类型注解
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElement = new AnnotatedElementAdapter(annotations);
	}


	/**
	 * Variation of {@link #getType()} that accounts for a primitive type by
	 * returning its object wrapper type.
	 * <p>This is useful for conversion service implementations that wish to
	 * normalize to object-based types and not work with primitive types directly.
	 */
	// {@link getType()} 的变体，通过返回其对象包装器类型来解释原始类型。
	//
	// 这对于希望规范化为基于对象的类型而不直接使用原始类型的转换服务实现很有用。
	// 获取包装类型
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * The type of the backing class, method parameter, field, or property
	 * described by this TypeDescriptor.
	 * <p>Returns primitive types as-is. See {@link #getObjectType()} for a
	 * variation of this operation that resolves primitive types to their
	 * corresponding Object types if necessary.
	 * @see #getObjectType()
	 */
	// 此 TypeDescriptor 描述的支持类、方法参数、字段或属性的类型。
	//
	// 按原样返回原始类型。有关此操作的变体，请参阅 {@link getObjectType()}，必要时将原始类型解析为其相应的对象类型。
	// 返回原始类型
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return the underlying {@link ResolvableType}.
	 * @since 4.0
	 */
	// 返回底层 {@link ResolvableType}。
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * Return the underlying source of the descriptor. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link TypeDescriptor}
	 * was constructed. This method is primarily to provide access to additional
	 * type information or meta-data that alternative JVM languages may provide.
	 * @since 4.0
	 */
	// 返回描述符的底层源。将根据 {@link TypeDescriptor} 的构造方式返回
	// {@link Field}、{@link MethodParameter} 或 {@link Type}。
	// 此方法主要用于提供对其他 JVM 语言可能提供的附加类型信息或元数据的访问。
	public Object getSource() {
		return this.resolvableType.getSource();
	}

	/**
	 * Narrows this {@link TypeDescriptor} by setting its type to the class of the
	 * provided value.
	 * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
	 * is returned unchanged.
	 * <p>Designed to be called by binding frameworks when they read property, field,
	 * or method return values. Allows such frameworks to narrow a TypeDescriptor built
	 * from a declared property, field, or method return value type. For example, a field
	 * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
	 * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
	 * can then be used to convert the HashMap to some other type. Annotation and nested
	 * type context is preserved by the narrowed copy.
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
	 * class of the provided value)
	 */
	// 通过将其类型设置为所提供值的类来缩小此 {@link TypeDescriptor}。
	//
	// 如果值为 {@code null}，则不进行缩小，并且此 TypeDescriptor 原样返回。
	//
	// 设计为在绑定框架读取属性、字段或方法返回值时由它们调用。允许此类框架缩小从声明的属性、字段或方法返回值类型构建的 TypeDescriptor。
	// 例如，声明为 {@code java.lang.Object} 的字段如果设置为 {@code java.util.HashMap} 值，
	// 则会缩小为 {@code java.util.HashMap}。然后可以使用缩小的 TypeDescriptor 将 HashMap 转换为其他类型。
	// 注j解和嵌套类型上下文由缩小的副本保留。
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * Cast this {@link TypeDescriptor} to a superclass or implemented interface
	 * preserving annotations and nested type context.
	 * @param superType the super type to cast to (can be {@code null})
	 * @return a new TypeDescriptor for the up-cast type
	 * @throws IllegalArgumentException if this type is not assignable to the super-type
	 * @since 3.2
	 */
	// 将此 {@link TypeDescriptor} 转换为超类或实现的接口，保留注解和嵌套类型上下文
	// @param superType 要强制转换的超类型（可以是 {@code null}）
	// @return 向上转换类型的新 TypeDescriptor
	@Nullable
	public TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * Return the name of this type: the fully qualified class name.
	 */
	// 返回此类型的名称：完全限定的类名
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	// 这种类型是原始类型吗？
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * Return the annotations associated with this type descriptor, if any.
	 * @return the annotations, or an empty array if none
	 */
	// 返回与此类型描述符关联的注释（如果有）。 @return 注释，如果没有则返回一个空数组
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * Determine if this type descriptor has the specified annotation.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels
	 * of meta-annotations.
	 * @param annotationType the annotation type
	 * @return <tt>true</tt> if the annotation is present
	 */
	// 确定此类型描述符是否具有指定的注解。
	// <p>从 Spring Framework 4.2 开始，此方法支持任意级别的元注释。
	// @param annotationType 注释类型
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			// 捷径： AnnotatedElementUtils 必须期望 AnnotatedElement.getAnnotations() 返回数组的副本，
			// 而我们可以在这里更有效地做到这一点。
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels of meta-annotations.
	 * @param annotationType the annotation type
	 * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
	 */
	// 获取此类型描述符上指定的 {@code annotationType} 的注解。
	// <p>从 Spring Framework 4.2 开始，此方法支持任意级别的元注释。
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			// 捷径： AnnotatedElementUtils 必须期望 AnnotatedElement.getAnnotations() 返回数组的副本，
			// 而我们可以在这里更有效地做到这一点。
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
	}

	/**
	 * Returns true if an object of this type descriptor can be assigned to the location
	 * described by the given type descriptor.
	 * <p>For example, {@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}
	 * returns {@code true} because a String value can be assigned to a CharSequence variable.
	 * On the other hand, {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}
	 * returns {@code false} because, while all Integers are Numbers, not all Numbers are Integers.
	 * <p>For arrays, collections, and maps, element and key/value types are checked if declared.
	 * For example, a List&lt;String&gt; field value is assignable to a Collection&lt;CharSequence&gt;
	 * field, but List&lt;Number&gt; is not assignable to List&lt;Integer&gt;.
	 * @return {@code true} if this type is assignable to the type represented by the provided
	 * type descriptor
	 * @see #getObjectType()
	 */
	// 如果此类型描述符的对象可以分配给给定类型描述符所描述的位置，则返回 true。
	// 例如， valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))返回true因为可以将
	// String 值分配给 CharSequence 变量。 另一方面，
	// valueOf(Number.class).isAssignableTo(valueOf(Integer.class))返回false因为，虽然所有整数都是数字，
	// 但并非所有数字都是整数。
	// Spring 添加了 子类.isAssignableTo(父类);  JDK Class 中只有 父类.isAssignableFrom(子类)
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		}
		else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
				isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		}
		else {
			return true;
		}
	}

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
			@Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	// 这种类型是Collection类型吗？
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * Is this type an array type?
	 */
	// 这种类型是数组类型吗？
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * If this type is an array, returns the array's component type.
	 * If this type is a {@code Stream}, returns the stream's component type.
	 * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
	 * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
	 * @return the array component type or Collection element type, or {@code null} if this type is not
	 * an array type or a {@code java.util.Collection} or if its element type is not parameterized
	 * @see #elementTypeDescriptor(Object)
	 */
	// 如果此类型是数组，则返回数组的组件类型。如果此类型是流，则返回流的组件类型。如果此类型是 Collection 并且已参数化，
	// 则返回 Collection 的元素类型。如果 Collection 未参数化，则返回 null 指示未声明元素类型。
	@Nullable
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
	}

	/**
	 * If this type is a {@link Collection} or an array, creates a element TypeDescriptor
	 * from the provided collection or array element.
	 * <p>Narrows the {@link #getElementTypeDescriptor() elementType} property to the class
	 * of the provided collection or array element. For example, if this describes a
	 * {@code java.util.List&lt;java.lang.Number&lt;} and the element argument is an
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}.
	 * If this describes a {@code java.util.List&lt;?&gt;} and the element argument is an
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}
	 * as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param element the collection or array element
	 * @return a element type descriptor, narrowed to the type of the provided element
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	// 如果此类型是Collection或数组，则从提供的集合或数组元素创建元素 TypeDescriptor。
	// 将elementType属性elementType到提供的集合或数组元素的类。 例如，如果它描述的是java.util.
	// List<java.lang.Number<并且元素参数是java.lang.Integer ，则返回的 TypeDescriptor 将是java.
	// lang.Integer 。 如果它描述的是java.util.List<?>并且元素参数是java.lang.Integer ，则返回的
	// TypeDescriptor 也将是java.lang.Integer 。
	// 注解和嵌套类型上下文将保留在返回的缩小的 TypeDescriptor 中。
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * Is this type a {@link Map} type?
	 */
	// 这种类型是Map类型吗
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map} and its key type is parameterized,
	 * returns the map's key type. If the Map's key type is not parameterized,
	 * returns {@code null} indicating the key type is not declared.
	 * @return the Map key type, or {@code null} if this type is a Map
	 * but its key type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	// 如果此类型是Map并且其键类型已参数化，则返回映射的键类型。 如果 Map 的键类型未参数化，则返回null表示未声明键类型。
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor}
	 * from the provided map key.
	 * <p>Narrows the {@link #getMapKeyTypeDescriptor() mapKeyType} property
	 * to the class of the provided map key. For example, if this describes a
	 * {@code java.util.Map&lt;java.lang.Number, java.lang.String&lt;} and the key
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
	 * and the key argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	// 如果此类型是Map ，则从提供的地图键创建一个 mapKey TypeDescriptor 。
	// 将mapKeyType属性mapKeyType到提供的映射键的类。 例如，如果它描述了java.util.Map<java.lang.
	// Number, java.lang.String<并且关键参数是java.lang.Integer ，则返回的 TypeDescriptor 将是java.
	// lang.Integer 。 如果它描述了java.util.Map<?, ?>并且关键参数是java.lang.Integer ，则返回的
	// TypeDescriptor 也将是java.lang.Integer 。
	// 注解和嵌套类型上下文将保留在返回的缩小的 TypeDescriptor 中
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized,
	 * returns the map's value type.
	 * <p>If the Map's value type is not parameterized, returns {@code null}
	 * indicating the value type is not declared.
	 * @return the Map value type, or {@code null} if this type is a Map
	 * but its value type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	// 如果此类型是 Map 并且其值类型已参数化，则返回映射的值类型。如果 Map 的值类型未参数化，则返回 null 指示未声明值类型
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	/**
	 * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor}
	 * from the provided map value.
	 * <p>Narrows the {@link #getMapValueTypeDescriptor() mapValueType} property
	 * to the class of the provided map value. For example, if this describes a
	 * {@code java.util.Map&lt;java.lang.String, java.lang.Number&lt;} and the value
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
	 * and the value argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	// 如果此类型是地图，则从提供的地图值创建一个 mapValue TypeDescriptor。将 mapValueType 属性缩小到提供的地图值的类。
	// 例如，如果它描述了 java.util.Map<java.lang.String, java.lang.Number< 并且 value 参数是 java.lang.Integer，
	// 则返回的 TypeDescriptor 将是 java.lang.Integer。如果它描述的是 java.util.Map<?, ?> 并且 value 参数是
	// java.lang.Integer，则返回的 TypeDescriptor 也将是 java.lang.Integer。注解和嵌套类型上下文将保留在返回的
	// 缩小的 TypeDescriptor 中。
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	@Nullable
	private TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor otherDesc = (TypeDescriptor) other;
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
		}
		else if (isMap()) {
			return (ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor()) &&
					ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor()));
		}
		else {
			return true;
		}
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
		// Annotation.equals 是反射性的并且非常慢，所以让我们先检查身份和代理类型。
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(ann.annotationType().getName()).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}


	/**
	 * Create a new type descriptor for an object.
	 * <p>Use this factory method to introspect a source object before asking the
	 * conversion system to convert it to some another type.
	 * <p>If the provided object is {@code null}, returns {@code null}, else calls
	 * {@link #valueOf(Class)} to build a TypeDescriptor from the object's class.
	 * @param source the source object
	 * @return the type descriptor
	 */
	// 对象创建一个新的类型描述符。
	// 在要求转换系统将其转换为其他类型之前，使用此工厂方法自省源对象。如果提供的对象为 null，则返回 null，
	// 否则调用 valueOf(Class) 从对象的类构建 TypeDescriptor。
	@Nullable
	public static TypeDescriptor forObject(@Nullable Object source) {
		return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * Create a new type descriptor from the given type.
	 * <p>Use this to instruct the conversion system to convert an object to a
	 * specific target type, when no type location such as a method parameter or
	 * field is available to provide additional conversion context.
	 * <p>Generally prefer use of {@link #forObject(Object)} for constructing type
	 * descriptors from source objects, as it handles the {@code null} object case.
	 * @param type the class (may be {@code null} to indicate {@code Object.class})
	 * @return the corresponding type descriptor
	 */
	// 从给定的类型创建一个新的类型描述符。当没有类型位置（如方法参数或字段）可用于提供额外的转换上下文时，
	// 使用它来指示转换系统将对象转换为特定的目标类型。通常更喜欢使用 forObject(Object) 从源对象构造类型描述符，
	// 因为它处理空对象的情况。
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
		if (type == null) {
			type = Object.class;
		}
		TypeDescriptor desc = commonTypesCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * Create a new type descriptor from a {@link java.util.Collection} type.
	 * <p>Useful for converting to typed Collections.
	 * <p>For example, a {@code List<String>} could be converted to a
	 * {@code List<EmailAddress>} by converting to a targetType built with this method.
	 * The method call to construct such a {@code TypeDescriptor} would look something
	 * like: {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementTypeDescriptor a descriptor for the collection's element type,
	 * used to convert collection elements
	 * @return the collection type descriptor
	 */
	// 从 {@link java.util.Collection} 类型创建一个新的类型描述符。
	// <p>用于转换为类型化集合。 <p>例如，通过转换为使用此方法构建的 targetType，可以将 {@code List<String>} 转换为
	// {@code List<EmailAddress>}。构造这样一个 {@code TypeDescriptor} 的方法调用看起来像：
	// {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
		}
		ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * Create a new type descriptor from a {@link java.util.Map} type.
	 * <p>Useful for converting to typed Maps.
	 * <p>For example, a Map&lt;String, String&gt; could be converted to a Map&lt;Id, EmailAddress&gt;
	 * by converting to a targetType built with this method:
	 * The method call to construct such a TypeDescriptor would look something like:
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 * @param mapType the map type, which must implement {@link Map}
	 * @param keyTypeDescriptor a descriptor for the map's key type, used to convert map keys
	 * @param valueTypeDescriptor the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	// 从 {@link java.util.Map} 类型创建一个新的类型描述符。
	// <p>用于转换为类型化地图。
	// <p>例如，可以通过转换为使用此方法构建的 targetType 将 Map<String, String> 转换为 Map<Id, EmailAddress>：
	// 构造此类 TypeDescriptor 的方法调用将类似于：
	// <pre class ="code">
	// 	 map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	// </pre>
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
			@Nullable TypeDescriptor valueTypeDescriptor) {

		Assert.notNull(mapType, "Map type must not be null");
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("Map type must be a [java.util.Map]");
		}
		ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
		ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
		return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * Create a new type descriptor as an array of the specified type.
	 * <p>For example to create a {@code Map<String,String>[]} use:
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 * @param elementTypeDescriptor the {@link TypeDescriptor} of the array element or {@code null}
	 * @return an array {@link TypeDescriptor} or {@code null} if {@code elementTypeDescriptor} is {@code null}
	 * @since 3.2.1
	 */
	// 创建一个新的类型描述符作为指定类型的数组。
	// <p>例如创建一个 {@code Map<String,String>[]} 使用：
	// <pre class="code">
	//	 TypeDescriptor.array(TypeDescriptor.map
	//	 (Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	// </pre>
	@Nullable
	public static TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
		if (elementTypeDescriptor == null) {
			return null;
		}
		return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
				null, elementTypeDescriptor.getAnnotations());
	}

	/**
	 * Create a type descriptor for a nested type declared within the method parameter.
	 * <p>For example, if the methodParameter is a {@code List<String>} and the
	 * nesting level is 1, the nested type descriptor will be String.class.
	 * <p>If the methodParameter is a {@code List<List<String>>} and the nesting
	 * level is 2, the nested type descriptor will also be a String.class.
	 * <p>If the methodParameter is a {@code Map<Integer, String>} and the nesting
	 * level is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the methodParameter is a {@code List<Map<Integer, String>>} and the
	 * nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not declared.
	 * For example, if the method parameter is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the method parameter
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the nesting level of the input
	 * {@link MethodParameter} argument is not 1, or if the types up to the
	 * specified nesting level are not of collection, array, or map types
	 */
	// 为方法参数中声明的嵌套类型创建类型描述符。
	// <p>例如，如果methodParameter 是一个{@code List<String>} 并且嵌套级别为1，则嵌套的类型描述符将为String.class。
	// <p>如果methodParameter 是一个{@code List<List<String>>} 并且嵌套级别是2，那么嵌套的类型描述符也将是一个String.class。
	// <p>如果 methodParameter 是 {@code Map<Integer, String>} 并且嵌套级别为 1，则嵌套类型描述符将为 String，从映射值派生。
	// <p>如果 methodParameter 是 {@code List<Map<Integer, String>>} 并且嵌套级别为 2，则嵌套类型描述符将为 String，从映射值派生。
	// <p>如果嵌套类型由于未声明而无法获取，则返回 {@code null}。例如，如果方法参数是{@code List<?>}，
	// 则返回的嵌套类型描述符将为{@code null}。
	@Nullable
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		if (methodParameter.getNestingLevel() != 1) {
			throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
					"use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
		}
		return nested(new TypeDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the field.
	 * <p>For example, if the field is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the field is a {@code List<List<String>>} and the nesting level is
	 * 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the field is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the field is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the field is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the field
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	// 为方法参数中声明的嵌套类型创建类型描述符。
	// <p>例如，如果methodParameter 是一个{@code List<String>} 并且嵌套级别为1，则嵌套的类型描述符将为String.class。
	// <p>如果methodParameter 是一个{@code List<List<String>>} 并且嵌套级别是2，那么嵌套的类型描述符也将是一个String.class。
	// <p>如果 methodParameter 是 {@code Map<Integer, String>} 并且嵌套级别为 1，则嵌套类型描述符将为 String，从映射值派生。
	// <p>如果 methodParameter 是 {@code List<Map<Integer, String>>} 并且嵌套级别为 2，则嵌套类型描述符将为 String，从映射值派生。
	// <p>如果嵌套类型由于未声明而无法获取，则返回 {@code null}。例如，如果方法参数是{@code List<?>}，
	// 则返回的嵌套类型描述符将为{@code null}。
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the property.
	 * <p>For example, if the property is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the property is a {@code List<List<String>>} and the nesting level
	 * is 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the property is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the property is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the property is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or
	 * map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 * level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new TypeDescriptor(property), nestingLevel);
	}

	@Nullable
	private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
		ResolvableType nested = typeDescriptor.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
			}
			else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(typeDescriptor, nested);
	}

	@Nullable
	private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, source.getAnnotations());
	}


	/**
	 * Adapter class for exposing a {@code TypeDescriptor}'s annotations as an
	 * {@link AnnotatedElement}, in particular to {@link AnnotatedElementUtils}.
	 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
	 */
	// 用于将 {@code TypeDescriptor} 的注解公开为 {@link AnnotatedElement} 的适配器类，
	// 特别是 {@link AnnotatedElementUtils}。
	private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		@Nullable
		private final Annotation[] annotations;

		public AnnotatedElementAdapter(@Nullable Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return ObjectUtils.isEmpty(this.annotations);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter &&
					Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return TypeDescriptor.this.toString();
		}
	}

}
