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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated element represents a stereotype for the index.
 *
 * <p>The {@code CandidateComponentsIndex} is an alternative to classpath
 * scanning that uses a metadata file generated at compilation time. The
 * index allows retrieving the candidate components (i.e. fully qualified
 * name) based on a stereotype. This annotation instructs the generator to
 * index the element on which the annotated element is present or if it
 * implements or extends from the annotated element. The stereotype is the
 * fully qualified name of the annotated element.
 *
 * <p>Consider the default {@link Component} annotation that is meta-annotated
 * with this annotation. If a component is annotated with {@link Component},
 * an entry for that component will be added to the index using the
 * {@code org.springframework.stereotype.Component} stereotype.
 *
 * <p>This annotation is also honored on meta-annotations. Consider this
 * custom annotation:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Target(ElementType.TYPE)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Documented
 * &#064;Indexed
 * &#064;Service
 * public @interface PrivilegedService { ... }
 * </pre>
 *
 * If the above annotation is present on a type, it will be indexed with two
 * stereotypes: {@code org.springframework.stereotype.Component} and
 * {@code com.example.PrivilegedService}. While {@link Service} isn't directly
 * annotated with {@code Indexed}, it is meta-annotated with {@link Component}.
 *
 * <p>It is also possible to index all implementations of a certain interface or
 * all the subclasses of a given class by adding {@code @Indexed} on it.
 *
 * Consider this base interface:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Indexed
 * public interface AdminService { ... }
 * </pre>
 *
 * Now, consider an implementation of this {@code AdminService} somewhere:
 * <pre class="code">
 * package com.example.foo;
 *
 * import com.example.AdminService;
 *
 * public class ConfigurationAdminService implements AdminService { ... }
 * </pre>
 *
 * Because this class implements an interface that is indexed, it will be
 * automatically included with the {@code com.example.AdminService} stereotype.
 * If there are more {@code @Indexed} interfaces and/or superclasses in the
 * hierarchy, the class will map to all their stereotypes.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
// 指示带注释的元素表示索引的模型
//
// <p>{@code CandidateComponentsIndex} 是类路径扫描的替代方法，它使用在编译时生成的元数据文件。该索引允许基于范式
// 检索候选组件（即完全限定名称）。该注解指示生成器索引带有注解元素的元素，或者它是否实现或扩展自注解元素。范式是带注释元素的完全限定名称。
//
// <p>考虑使用此注解进行元注解的默认 {@link Component} 注解。如果一个组件用 {@link Component} 注解，该组件的条目将使用
// {@code org.springframework.stereotype.Component} 模式注解添加到索引中。
//
// 此注释也适用于元注释。 考虑这个自定义注释：
//   package com.example;
//
//   @Target(ElementType.TYPE)
//   @Retention(RetentionPolicy.RUNTIME)
//   @Documented
//   @Indexed
//   @Service
//   public @interface PrivilegedService { ... }
//
//如果上面的注解存在于一个类型上，它将被两个模式注解索引： org.springframework.stereotype.Component和
// com.example.PrivilegedService 。虽然 {@link @Service} 没有直接用 {@code @Indexed} 进行注解，但它是用 {@link @Component} 进行元注解的。
//
// 还可以通过在其上添加 {@code @Indexed}来索引某个接口的所有实现或给定类的所有子类。 考虑这个基本接口：
//   package com.example;
//
//   @Indexed
//   public interface AdminService { ... }
//
// 现在，考虑在某处实现这个AdminService ：
//   package com.example.foo;
//
//   import com.example.AdminService;
//
//   public class ConfigurationAdminService implements AdminService { ... }
//
// 因为这个类实现了一个被索引的接口，它会自动包含在 {@code com.example.AdminService} 模式注解中。
// 如果层次结构中有更多 {@code @Indexed} 接口和/或超类，则该类将映射到它们的所有模式注解。
//
// 通过 APT(Annotation Processor Tools)工具进行在编译时生成元信息，帮助我们减少性能损耗
// (比如类的扫描，ComponentScan以及无论是字节码也好，还是反射来进行读取元信息)，在编译时能确定的事情那么就会减少运行时的损耗。
// 对 Spring 的启动势必有帮助，所以在 Spring Boot 中启动会比 Sping 1 启动性能要好，因为在 Spring Boot 内部大量组件都被
// 进行静态化操作。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Indexed {
}
