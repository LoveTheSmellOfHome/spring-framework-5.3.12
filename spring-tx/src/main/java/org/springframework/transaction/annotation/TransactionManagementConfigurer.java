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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionManager;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableTransactionManagement} that wish to
 * (or need to) explicitly specify the default {@code PlatformTransactionManager} bean
 * (or {@code ReactiveTransactionManager} bean) to be used for annotation-driven
 * transaction management, as opposed to the default approach of a by-type lookup.
 * One reason this might be necessary is if there are two {@code PlatformTransactionManager}
 * beans (or two {@code ReactiveTransactionManager} beans) present in the container.
 *
 * <p>See @{@link EnableTransactionManagement} for general examples and context;
 * see {@link #annotationDrivenTransactionManager()} for detailed instructions.
 *
 * <p>Note that in by-type lookup disambiguation cases, an alternative approach to
 * implementing this interface is to simply mark one of the offending
 * {@code PlatformTransactionManager} {@code @Bean} methods (or
 * {@code ReactiveTransactionManager} {@code @Bean} methods) as
 * {@link org.springframework.context.annotation.Primary @Primary}.
 * This is even generally preferred since it doesn't lead to early initialization
 * of the {@code TransactionManager} bean.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see org.springframework.context.annotation.Primary
 * @see org.springframework.transaction.PlatformTransactionManager
 * @see org.springframework.transaction.ReactiveTransactionManager
 */
// 由带有 @Configuration 注解的 @Configuration 类实现的接口 EnableTransactionManagement 这些类
// 希望（或需要）显式指定用于注解驱动的事务管理的默认 PlatformTransactionManager bean
// （或 ReactiveTransactionManager bean），而不是通过类型查找。这可能是必要的一个原因是容器中是否存在
// 两个 PlatformTransactionManager bean（或两个ReactiveTransactionManager bean）。
//
// 一般示例和上下文见 EnableTransactionManagement ；有关详细说明，请参阅 annotationDrivenTransactionManager() 。
//
// 请注意，在按类型查找消歧的情况下，实现此接口的另一种方法是简单地将有问题的 PlatformTransactionManager @Bean方法
// （或 ReactiveTransactionManager @Bean方法）标记为 @Primary 。这甚至通常是首选，因为它不会
// 导致 TransactionManager bean 的早期初始化。
public interface TransactionManagementConfigurer {

	/**
	 * Return the default transaction manager bean to use for annotation-driven database
	 * transaction management, i.e. when processing {@code @Transactional} methods.
	 * <p>There are two basic approaches to implementing this method:
	 * <h3>1. Implement the method and annotate it with {@code @Bean}</h3>
	 * In this case, the implementing {@code @Configuration} class implements this method,
	 * marks it with {@code @Bean}, and configures and returns the transaction manager
	 * directly within the method body:
	 * <pre class="code">
	 * &#064;Bean
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }</pre>
	 * <h3>2. Implement the method without {@code @Bean} and delegate to another existing
	 * {@code @Bean} method</h3>
	 * <pre class="code">
	 * &#064;Bean
	 * public PlatformTransactionManager txManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }
	 *
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return txManager(); // reference the existing {@code @Bean} method above
	 * }</pre>
	 * If taking approach #2, be sure that <em>only one</em> of the methods is marked
	 * with {@code @Bean}!
	 * <p>In either scenario #1 or #2, it is important that the
	 * {@code PlatformTransactionManager} instance is managed as a Spring bean within the
	 * container since most {@code PlatformTransactionManager} implementations take advantage
	 * of Spring lifecycle callbacks such as {@code InitializingBean} and
	 * {@code BeanFactoryAware}. Note that the same guidelines apply to
	 * {@code ReactiveTransactionManager} beans.
	 * @return a {@link org.springframework.transaction.PlatformTransactionManager} or
	 * {@link org.springframework.transaction.ReactiveTransactionManager} implementation
	 */
	// 返回默认事务管理器 bean 以用于注解驱动的数据库事务管理，即在处理 @Transactional 方法时。
	// 实现此方法有两种基本方法：
	// 1.实现方法并用 @Bean 注解
	// 在这种情况下，实现类 @Configuration 实现了这个方法，用 @Bean 标记它，并直接在方法体中配置和返回事务管理器：
	//	   @Bean
	//	   @Override
	//	   public PlatformTransactionManager annotationDrivenTransactionManager() {
	//	       return new DataSourceTransactionManager(dataSource());
	//	   }
	// 2. 实现不带 @Bean 的方法，并委托给另一个现有的@Bean方法
	//	   @Bean
	//	   public PlatformTransactionManager txManager() {
	//	       return new DataSourceTransactionManager(dataSource());
	//	   }
	//	  
	//	   @Override
	//	   public PlatformTransactionManager annotationDrivenTransactionManager() {
	//	       // 引用上面已有的 @Bean 方法
	//	       return txManager(); // reference the existing @Bean method above
	//	   }
	// 如果采用方法#2，请确保只有一种方法标有 @Bean
	// 在场景 #1 或 #2 中，重要的是 PlatformTransactionManager 实例在容器中作为 Spring bean 进行管理，
	// 因为大多数 PlatformTransactionManager 实现都利用了 Spring 生命周期回调，例如 InitializingBean
	// 和 BeanFactoryAware 。请注意，相同的准则适用于 ReactiveTransactionManager bean。
	//
	// 返回值：
	//			org.springframework.transaction.PlatformTransactionManager 或
	//			org.springframework.transaction.ReactiveTransactionManager 实现
	TransactionManager annotationDrivenTransactionManager();

}
