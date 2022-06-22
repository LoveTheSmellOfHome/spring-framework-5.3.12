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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient base class for {@link ImportSelector} implementations that select imports
 * based on an {@link AdviceMode} value from an annotation (such as the {@code @Enable*}
 * annotations).
 *
 * @author Chris Beams
 * @since 3.1
 * @param <A> annotation containing {@linkplain #getAdviceModeAttributeName() AdviceMode attribute}
 */
// ImportSelector 实现的方便基类，它根据注释（例如 @Enable* 注释）中的 AdviceMode 值选择导入
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	/**
	 * The default advice mode attribute name.
	 */
	// 默认建议模式属性名称
	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";


	/**
	 * The name of the {@link AdviceMode} attribute for the annotation specified by the
	 * generic type {@code A}. The default is {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME},
	 * but subclasses may override in order to customize.
	 */
	// 泛型类型 A 指定的注解的 AdviceMode 属性的名称。默认值为 "mode" ，但子类可能会覆盖以进行自定义
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	/**
	 * This implementation resolves the type of annotation from generic metadata and
	 * validates that (a) the annotation is in fact present on the importing
	 * {@code @Configuration} class and (b) that the given annotation has an
	 * {@linkplain #getAdviceModeAttributeName() advice mode attribute} of type
	 * {@link AdviceMode}.
	 * <p>The {@link #selectImports(AdviceMode)} method is then invoked, allowing the
	 * concrete implementation to choose imports in a safe and convenient fashion.
	 * @throws IllegalArgumentException if expected annotation {@code A} is not present
	 * on the importing {@code @Configuration} class or if {@link #selectImports(AdviceMode)}
	 * returns {@code null}
	 */
	// 此实现从通用元数据中解析注释类型，并验证 (a) 注解实际上存在于导入的 @Configuration 类中，
	// 以及 (b) 给定注解具有 AdviceMode 类型的建议模式属性。
	//
	// 然后调用 selectImports(AdviceMode) 方法，允许具体实现以安全方便的方式选择导入。
	// 抛出：
	//			IllegalArgumentException – 如果预期的注解A不存在于导入的 @Configuration 类中，
	//			或者如果 selectImports(AdviceMode) 返回 null
	@Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
					"@%s is not present on importing class '%s' as expected",
					annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
		}
		return imports;
	}

	/**
	 * Determine which classes should be imported based on the given {@code AdviceMode}.
	 * <p>Returning {@code null} from this method indicates that the {@code AdviceMode}
	 * could not be handled or was unknown and that an {@code IllegalArgumentException}
	 * should be thrown.
	 * @param adviceMode the value of the {@linkplain #getAdviceModeAttributeName()
	 * advice mode attribute} for the annotation specified via generics.
	 * @return array containing classes to import (empty array if none;
	 * {@code null} if the given {@code AdviceMode} is unknown)
	 */
	// 根据给定的AdviceMode确定应导入哪些类。
	// 从该方法返回null表示AdviceMode无法处理或未知，并且应该抛出IllegalArgumentException 。
	// 参形：
	//			adviceMode——通过泛型指定的注释的建议模式属性的值。
	// 返回值：
	//			包含要导入的类的数组（如果没有，则为空数组；如果给定的AdviceMode未知，则为null ）
	@Nullable
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
