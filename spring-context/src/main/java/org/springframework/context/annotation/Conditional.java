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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a component is only eligible for registration when all
 * {@linkplain #value specified conditions} match.
 *
 * <p>A <em>condition</em> is any state that can be determined programmatically
 * before the bean definition is due to be registered (see {@link Condition} for details).
 *
 * <p>The {@code @Conditional} annotation may be used in any of the following ways:
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype
 * annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Conditional},
 * all of the {@code @Bean} methods, {@link Import @Import} annotations, and
 * {@link ComponentScan @ComponentScan} annotations associated with that
 * class will be subject to the conditions.
 *
 * <p><strong>NOTE</strong>: Inheritance of {@code @Conditional} annotations
 * is not supported; any conditions from superclasses or from overridden
 * methods will not be considered. In order to enforce these semantics,
 * {@code @Conditional} itself is not declared as
 * {@link java.lang.annotation.Inherited @Inherited}; furthermore, any
 * custom <em>composed annotation</em> that is meta-annotated with
 * {@code @Conditional} must not be declared as {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see Condition
 */
// 表示只有在所有指定条件都匹配时，组件才有资格注册
//
// 条件是可以在注册 bean 定义之前以编程方式确定的任何状态（有关详细信息，请参阅{@link Condition}）
//
// @Conditional 注释可以通过以下任何一种方式使用：
// 作为任何直接或间接用@Component 注释的类的类型级注释，包括@Configuration 类
// 作为元注释，用于编写自定义构造型注释
// 作为任何@Bean 方法的方法级注解
//
// 如果@Configuration 类用@Conditional 标记，则与该类关联的所有@Bean 方法、@Import 注释和@ComponentScan 注释都将受条件约束。
//
// 注意：不支持@Conditional 注解的继承；不会考虑来自超类或覆盖方法的任何条件。为了强制执行这些语义，
// @Conditional 本身没有声明为@Inherited；此外，任何使用@Conditional 元注释的自定义组合注释不得声明为@Inherited。
//
// Bean 有条件的进行加载
//
// 基于编程条件注解-@Conditional
//		关联对象：- {@link org.springframework.context.annotation.Condition}
//	@Conditional 实现原理
//
//+ 上下文对象 - org.springframework.context.annotation.ConditionContext
//+ 条件判断 - org.springframework.context.annotation.ConditionEvaluator
//+ 配置阶段 org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
//+ 判断入口 - org.springframework.context.annotation.ConfigurationClassPostProcessor
//  + org.springframework.context.annotation.ConfigurationClassParser
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * All {@link Condition} classes that must {@linkplain Condition#matches match}
	 * in order for the component to be registered.
	 */
	// 必须匹配才能注册组件的所有条件类
	Class<? extends Condition>[] value();

}
