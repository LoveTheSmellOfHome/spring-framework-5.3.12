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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Annotation;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * A convenient {@link BeanPostProcessor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation), e.g.:
 *
 * <pre class="code">
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
 * </pre>
 *
 * <p>Target classes with such annotated methods need to be annotated with Spring's
 * {@link Validated} annotation at the type level, for their methods to be searched for
 * inline constraint annotations. Validation groups can be specified through {@code @Validated}
 * as well. By default, JSR-303 will validate against its default group only.
 *
 * <p>As of Spring 5.0, this functionality requires a Bean Validation 1.1+ provider.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationInterceptor
 * @see javax.validation.executable.ExecutableValidator
 */
// 一个方便的 {@link BeanPostProcessor} 实现，它委托给 JSR-303 提供者对带注解的方法执行方法级验证
//
// <p>适用的方法在其参数和/或返回值上具有 JSR-303 约束注解（在后一种情况下在方法级别指定，通常作为内联注释），例如：
//	<pre class="code">
//   public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
//  </pre>
//
// <p>具有此类注释方法的目标类需要在类型级别使用 Spring 的 {@link Validated} 注释进行注释，以便在它们的方法中搜索内联约束注释。
// 验证组也可以通过 {@code @Validated} 指定。默认情况下，JSR-303 将仅针对其默认组进行验证
//
// <p>从 Spring 5.0 开始，此功能需要 Bean Validation 1.1+ 提供程序
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements InitializingBean {

	// 根据我们在类上标注的 @Validated 注解，把一些待校验的对象放在方法参数里边
	private Class<? extends Annotation> validatedAnnotationType = Validated.class;

	@Nullable
	private Validator validator;


	/**
	 * Set the 'validated' annotation type.
	 * The default validated annotation type is the {@link Validated} annotation.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a class is supposed
	 * to be validated in the sense of applying method validation.
	 * @param validatedAnnotationType the desired annotation type
	 */
	// 设置“validated”注解类型。默认的经过验证的注解类型是 {@link Validated} 注释。
	// <p>存在此 setter 属性，以便开发人员可以提供他们自己的（非 Spring 特定的）注释类型，以指示在应用方法验证的意义上应该验证一个类。
	public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
		Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
		this.validatedAnnotationType = validatedAnnotationType;
	}

	/**
	 * Set the JSR-303 Validator to delegate to for validating methods.
	 * <p>Default is the default ValidatorFactory's default Validator.
	 */
	// 将 JSR-303 Validator 设置为委托给验证方法。
	// <p>Default 是默认的 ValidatorFactory 的默认验证器
	public void setValidator(Validator validator) {
		// Unwrap to the native Validator with forExecutables support
		// 使用 forExecutables 支持解包到本机验证器
		if (validator instanceof LocalValidatorFactoryBean) {
			this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
		}
		else if (validator instanceof SpringValidatorAdapter) {
			this.validator = validator.unwrap(Validator.class);
		}
		else {
			this.validator = validator;
		}
	}

	/**
	 * Set the JSR-303 ValidatorFactory to delegate to for validating methods,
	 * using its default Validator.
	 * <p>Default is the default ValidatorFactory's default Validator.
	 * @see javax.validation.ValidatorFactory#getValidator()
	 */
	// 使用其默认验证器将 JSR-303 ValidatorFactory 设置为委托以验证方法。
	// <p>Default 是默认的 ValidatorFactory 的默认验证器。
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}


	@Override
	public void afterPropertiesSet() {
		Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
		this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
	}

	/**
	 * Create AOP advice for method validation purposes, to be applied
	 * with a pointcut for the specified 'validated' annotation.
	 * @param validator the JSR-303 Validator to delegate to
	 * @return the interceptor to use (typically, but not necessarily,
	 * a {@link MethodValidationInterceptor} or subclass thereof)
	 * @since 4.2
	 */
	// 为方法验证目的创建 AOP 建议，与指定的“已验证”注释的切入点一起应用
	// @param validator 委托给的 JSR-303 验证器
	// @return 要使用的拦截器（通常但不一定是 {@link MethodValidationInterceptor} 或其子类）
	protected Advice createMethodValidationAdvice(@Nullable Validator validator) {
		return (validator != null ? new MethodValidationInterceptor(validator) : new MethodValidationInterceptor());
	}

}
