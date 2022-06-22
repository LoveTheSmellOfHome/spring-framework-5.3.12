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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Beans on which the current bean depends. Any beans specified are guaranteed to be
 * created by the container before this bean. Used infrequently in cases where a bean
 * does not explicitly depend on another through properties or constructor arguments,
 * but rather depends on the side effects of another bean's initialization.
 *
 * <p>A depends-on declaration can specify both an initialization-time dependency and,
 * in the case of singleton beans only, a corresponding destruction-time dependency.
 * Dependent beans that define a depends-on relationship with a given bean are destroyed
 * first, prior to the given bean itself being destroyed. Thus, a depends-on declaration
 * can also control shutdown order.
 *
 * <p>May be used on any class directly or indirectly annotated with
 * {@link org.springframework.stereotype.Component} or on methods annotated
 * with {@link Bean}.
 *
 * <p>Using {@link DependsOn} at the class level has no effect unless component-scanning
 * is being used. If a {@link DependsOn}-annotated class is declared via XML,
 * {@link DependsOn} annotation metadata is ignored, and
 * {@code <bean depends-on="..."/>} is respected instead.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
// 当前 bean 所依赖的 bean。任意指定的 bean 都保证由容器在此 bean 之前创建。
// 在 bean 不通过属性或构造函数参数显式依赖另一个 bean 而是依赖于另一个 bean 初始化的副作用的情况下很少使用。
//
// 依赖声明既可以指定初始化时间依赖项，也可以指定对应的销毁时间依赖项（仅在单例 bean 的情况下）。
// 与给定 bean 定义依赖关系的依赖 bean 首先被销毁，在给定 bean 本身被销毁之前。因此，依赖声明也可以控制关闭顺序。
//
// 可用于任何直接或间接使用 org.springframework.stereotype.@Component 注解的类或使用 @Bean 注解的方法。
//
// 除非正在使用组件扫描，否则在类级别使用 @DependsOn 无效。如果通过 XML 声明 DependsOn注释类，则忽略DependsOn注释元数据，并且  反而受到尊重。
//
// 1. 当前 bean 之前必须先创建 @DependsOn 指定的 bean,当前 bean 销毁前必须先销毁 @DependsOn 指定的 bean
// 2. 必须在注解环境 @ComponentScan 扫描作用的范围内使用 @DependsOn

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {

	String[] value() default {};

}
