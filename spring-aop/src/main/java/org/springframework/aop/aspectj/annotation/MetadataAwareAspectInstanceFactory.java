/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.lang.Nullable;

/**
 * Subinterface of {@link org.springframework.aop.aspectj.AspectInstanceFactory}
 * that returns {@link AspectMetadata} associated with AspectJ-annotated classes.
 *
 * <p>Ideally, AspectInstanceFactory would include this method itself, but because
 * AspectMetadata uses Java-5-only {@link org.aspectj.lang.reflect.AjType},
 * we need to split out this subinterface.
 *
 * @author Rod Johnson
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 */
// AspectInstanceFactory 的子接口，它返回与 AspectJ 注解类关联的 AspectMetadata 。
// 理想情况下，AspectInstanceFactory 将包含此方法本身，但因为 AspectMetadata 使用仅 Java-5
// 的 org.aspectj.lang.reflect.AjType ，我们需要拆分此子接口。
//
// 包含两部分：切面元数据和对象实例
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

	/**
	 * Return the AspectJ AspectMetadata for this factory's aspect.
	 * @return the aspect metadata
	 */
	// 返回此工厂切面的 AspectJ AspectMetadata。
	// 返回值：
	//			切面元数据
	AspectMetadata getAspectMetadata();

	/**
	 * Return the best possible creation mutex for this factory.
	 * @return the mutex object (may be {@code null} for no mutex to use)
	 * @since 4.3
	 */
	// 返回此工厂的最佳创建互斥锁。
	// 返回值：
	//			互斥对象（可能为null ，不使用互斥对象） 实例对象
	@Nullable
	Object getAspectCreationMutex();

}
