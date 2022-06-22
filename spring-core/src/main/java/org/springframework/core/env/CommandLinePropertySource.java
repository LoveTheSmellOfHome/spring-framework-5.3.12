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

package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Abstract base class for {@link PropertySource} implementations backed by command line
 * arguments. The parameterized type {@code T} represents the underlying source of command
 * line options. This may be as simple as a String array in the case of
 * {@link SimpleCommandLinePropertySource}, or specific to a particular API such as JOpt's
 * {@code OptionSet} in the case of {@link JOptCommandLinePropertySource}.
 *
 * <h3>Purpose and General Usage</h3>
 *
 * For use in standalone Spring-based applications, i.e. those that are bootstrapped via
 * a traditional {@code main} method accepting a {@code String[]} of arguments from the
 * command line. In many cases, processing command-line arguments directly within the
 * {@code main} method may be sufficient, but in other cases, it may be desirable to
 * inject arguments as values into Spring beans. It is this latter set of cases in which
 * a {@code CommandLinePropertySource} becomes useful. A {@code CommandLinePropertySource}
 * will typically be added to the {@link Environment} of the Spring
 * {@code ApplicationContext}, at which point all command line arguments become available
 * through the {@link Environment#getProperty(String)} family of methods. For example:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * With the bootstrap logic above, the {@code AppConfig} class may {@code @Inject} the
 * Spring {@code Environment} and query it directly for properties:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public void DataSource dataSource() {
 *         MyVendorDataSource dataSource = new MyVendorDataSource();
 *         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
 *         dataSource.setUsername(env.getRequiredProperty("db.username"));
 *         dataSource.setPassword(env.getRequiredProperty("db.password"));
 *         // ...
 *         return dataSource;
 *     }
 * }</pre>
 *
 * Because the {@code CommandLinePropertySource} was added to the {@code Environment}'s
 * set of {@link MutablePropertySources} using the {@code #addFirst} method, it has
 * highest search precedence, meaning that while "db.hostname" and other properties may
 * exist in other property sources such as the system environment variables, it will be
 * chosen from the command line property source first. This is a reasonable approach
 * given that arguments specified on the command line are naturally more specific than
 * those specified as environment variables.
 *
 * <p>As an alternative to injecting the {@code Environment}, Spring's {@code @Value}
 * annotation may be used to inject these properties, given that a {@link
 * PropertySourcesPropertyResolver} bean has been registered, either directly or through
 * using the {@code <context:property-placeholder>} element. For example:
 *
 * <pre class="code">
 * &#064;Component
 * public class MyComponent {
 *
 *     &#064;Value("my.property:defaultVal")
 *     private String myProperty;
 *
 *     public void getMyProperty() {
 *         return this.myProperty;
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>Working with option arguments</h3>
 *
 * <p>Individual command line arguments are represented as properties through the usual
 * {@link PropertySource#getProperty(String)} and
 * {@link PropertySource#containsProperty(String)} methods. For example, given the
 * following command line:
 *
 * <pre class="code">--o1=v1 --o2</pre>
 *
 * 'o1' and 'o2' are treated as "option arguments", and the following assertions would
 * evaluate true:
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("o3") == false;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("");
 * assert ps.getProperty("o3") == null;
 * </pre>
 *
 * Note that the 'o2' option has no argument, but {@code getProperty("o2")} resolves to
 * empty string ({@code ""}) as opposed to {@code null}, while {@code getProperty("o3")}
 * resolves to {@code null} because it was not specified. This behavior is consistent with
 * the general contract to be followed by all {@code PropertySource} implementations.
 *
 * <p>Note also that while "--" was used in the examples above to denote an option
 * argument, this syntax may vary across individual command line argument libraries. For
 * example, a JOpt- or Commons CLI-based implementation may allow for single dash ("-")
 * "short" option arguments, etc.
 *
 * <h3>Working with non-option arguments</h3>
 *
 * <p>Non-option arguments are also supported through this abstraction. Any arguments
 * supplied without an option-style prefix such as "-" or "--" are considered "non-option
 * arguments" and available through the special {@linkplain
 * #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"} property.  If multiple
 * non-option arguments are specified, the value of this property will be a
 * comma-delimited string containing all of the arguments. This approach ensures a simple
 * and consistent return type (String) for all properties from a {@code
 * CommandLinePropertySource} and at the same time lends itself to conversion when used
 * in conjunction with the Spring {@link Environment} and its built-in {@code
 * ConversionService}. Consider the following example:
 *
 * <pre class="code">--o1=v1 --o2=v2 /path/to/file1 /path/to/file2</pre>
 *
 * In this example, "o1" and "o2" would be considered "option arguments", while the two
 * filesystem paths qualify as "non-option arguments".  As such, the following assertions
 * will evaluate true:
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("nonOptionArgs") == true;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("v2");
 * assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 * </pre>
 *
 * <p>As mentioned above, when used in conjunction with the Spring {@code Environment}
 * abstraction, this comma-delimited string may easily be converted to a String array or
 * list:
 *
 * <pre class="code">
 * Environment env = applicationContext.getEnvironment();
 * String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 * assert nonOptionArgs[0].equals("/path/to/file1");
 * assert nonOptionArgs[1].equals("/path/to/file2");
 * </pre>
 *
 * <p>The name of the special "non-option arguments" property may be customized through
 * the {@link #setNonOptionArgsPropertyName(String)} method. Doing so is recommended as
 * it gives proper semantic value to non-option arguments. For example, if filesystem
 * paths are being specified as non-option arguments, it is likely preferable to refer to
 * these as something like "file.locations" than the default of "nonOptionArgs":
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     clps.setNonOptionArgsPropertyName("file.locations");
 *
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * <h3>Limitations</h3>
 *
 * This abstraction is not intended to expose the full power of underlying command line
 * parsing APIs such as JOpt or Commons CLI. It's intent is rather just the opposite: to
 * provide the simplest possible abstraction for accessing command line arguments
 * <em>after</em> they have been parsed. So the typical case will involve fully configuring
 * the underlying command line parsing API, parsing the {@code String[]} of arguments
 * coming into the main method, and then simply providing the parsing results to an
 * implementation of {@code CommandLinePropertySource}. At that point, all arguments can
 * be considered either 'option' or 'non-option' arguments and as described above can be
 * accessed through the normal {@code PropertySource} and {@code Environment} APIs.
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> the source type
 * @see PropertySource
 * @see SimpleCommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
// 由命令行参数支持的PropertySource实现的抽象基类。 参数化类型T表示命令行选项的底层来源。 这可能是一样简单在的情况下，
// 字符串数组SimpleCommandLinePropertySource ，或专用于特定API诸如JOPT的OptionSet中的情况下
// JOptCommandLinePropertySource 。
//用途和一般用途
//用于独立的基于 Spring 的应用程序，即那些通过从命令行接受String[]参数的传统main方法引导的应用程序。 在许多情况下，
// 直接在main方法中处理命令行参数可能就足够了，但在其他情况下，可能需要将参数作为值注入 Spring bean。
// 在后一组情况下， CommandLinePropertySource变得有用。 CommandLinePropertySource通常会添加到 Spring
// ApplicationContext的Environment中，此时所有命令行参数都可以通过Environment.getProperty(String)系列方法获得。 例如：
//   public static void main(String[] args) {
//       CommandLinePropertySource clps = ...;
//       AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
//       ctx.getEnvironment().getPropertySources().addFirst(clps);
//       ctx.register(AppConfig.class);
//       ctx.refresh();
//   }
//使用上面的引导逻辑， AppConfig类可以@Inject Spring Environment并直接查询它的属性：
//   @Configuration
//   public class AppConfig {
//
//       @Inject
//       Environment env;
//
//       @Bean
//       public void DataSource dataSource() {
//           MyVendorDataSource dataSource = new MyVendorDataSource();
//           dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
//           dataSource.setUsername(env.getRequiredProperty("db.username"));
//           dataSource.setPassword(env.getRequiredProperty("db.password"));
//           // ...
//           return dataSource;
//       }
//   }
//因为CommandLinePropertySource是使用#addFirst方法添加到Environment的MutablePropertySources #addFirst ，
// 所以它具有最高的搜索优先级，这意味着虽然“db.hostname”和其他属性可能存在于其他属性源（例如系统环境变量）中，
// 但它将首先从命令行属性源中选择。 这是一种合理的方法，因为在命令行上指定的参数自然比指定为环境变量的参数更具体。
//作为注入Environment的替代方法，Spring 的@Value注释可用于注入这些属性， @Value是已经注册了
// PropertySourcesPropertyResolver bean，直接或通过使用  元素。 例如：
//   @Component
//   public class MyComponent {
//
//       @Value("my.property:defaultVal")
//       private String myProperty;
//
//       public void getMyProperty() {
//           return this.myProperty;
//       }
//
//       // ...
//   }
//使用选项参数
//单个命令行参数通过通常的PropertySource.getProperty(String)和PropertySource.containsProperty(String)
// 方法表示为属性。 例如，给定以下命令行：
//--o1=v1 --o2
//'o1' 和 'o2' 被视为“选项参数”，以下断言将评估为真：
//   CommandLinePropertySource<?> ps = ...
//   assert ps.containsProperty("o1") == true;
//   assert ps.containsProperty("o2") == true;
//   assert ps.containsProperty("o3") == false;
//   assert ps.getProperty("o1").equals("v1");
//   assert ps.getProperty("o2").equals("");
//   assert ps.getProperty("o3") == null;
//
//请注意， 'o2' 选项没有参数，但getProperty("o2")解析为空字符串 ( "" ) 而不是null ，而getProperty("o3")
// 解析为null因为它没有被指定。 此行为与所有PropertySource实现要遵循的一般合同一致。
//另请注意，虽然在上面的示例中使用“--”来表示选项参数，但此语法可能因各个命令行参数库而异。 例如，基于 JOpt 或 Commons CLI
// 的实现可能允许单破折号（“-”）“短”选项参数等。
//使用非选项参数
//通过此抽象也支持非选项参数。 任何不带选项样式前缀（例如“-”或“--”）的参数都被视为“非选项参数”，可通过特殊的“nonOptionArgs”
// 属性获得。 如果指定了多个非选项参数，则此属性的值将是包含所有参数的逗号分隔字符串。 这种方法确保了来自
// CommandLinePropertySource所有属性的简单且一致的返回类型（String），同时在与 Spring Environment及其内置
// ConversionService结合使用时有助于ConversionService 。 考虑以下示例：
//--o1=v1 --o2=v2 /path/to/file1 /path/to/file2
//在此示例中，“o1”和“o2”将被视为“选项参数”，而两个文件系统路径则被视为“非选项参数”。 因此，以下断言将评估为真：
//   CommandLinePropertySource<?> ps = ...
//   assert ps.containsProperty("o1") == true;
//   assert ps.containsProperty("o2") == true;
//   assert ps.containsProperty("nonOptionArgs") == true;
//   assert ps.getProperty("o1").equals("v1");
//   assert ps.getProperty("o2").equals("v2");
//   assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
//
//如上所述，当与 Spring Environment抽象结合使用时，这个逗号分隔的字符串可以很容易地转换为 String 数组或列表：
//   Environment env = applicationContext.getEnvironment();
//   String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
//   assert nonOptionArgs[0].equals("/path/to/file1");
//   assert nonOptionArgs[1].equals("/path/to/file2");
//
//可以通过setNonOptionArgsPropertyName(String)方法自定义特殊“非选项参数”属性的setNonOptionArgsPropertyName(String) 。
// 建议这样做，因为它为非选项参数提供了适当的语义值。 例如，如果文件系统路径被指定为非选项参数，那么将它们称为“file.locations”
// 之类的东西可能比“nonOptionArgs”的默认值更可取：
//   public static void main(String[] args) {
//       CommandLinePropertySource clps = ...;
//       clps.setNonOptionArgsPropertyName("file.locations");
//
//       AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
//       ctx.getEnvironment().getPropertySources().addFirst(clps);
//       ctx.register(AppConfig.class);
//       ctx.refresh();
//   }
//限制
//此抽象并非旨在展示底层命令行解析 API（例如 JOpt 或 Commons CLI）的全部功能。 它的意图恰恰相反：提供最简单的抽象，
// 以便在解析后访问命令行参数。 因此，典型情况将涉及完全配置底层命令行解析 API，解析传入 main 方法的参数的String[] ，
// 然后简单地将解析结果提供给CommandLinePropertySource的实现。 此时，所有参数都可以被视为“选项”或“非选项”参数，
// 并且如上所述可以通过普通的PropertySource和Environment API 访问
//
// Spring 內建的配置属性源 - 命令行配置属性源
public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

	/** The default name given to {@link CommandLinePropertySource} instances: {@value}. */
	// 赋予 {@link CommandLinePropertySource} 实例的默认名称：{@value}。
	public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

	/** The default name of the property representing non-option arguments: {@value}. */
	// 表示非选项参数的属性的默认名称：{@value}。
	public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";


	private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;


	/**
	 * Create a new {@code CommandLinePropertySource} having the default name
	 * {@value #COMMAND_LINE_PROPERTY_SOURCE_NAME} and backed by the given source object.
	 */
	// 创建一个具有默认名称 {@value #COMMAND_LINE_PROPERTY_SOURCE_NAME} 并由给定源对象支持的新
	// {@code CommandLinePropertySource}。
	public CommandLinePropertySource(T source) {
		super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
	}

	/**
	 * Create a new {@link CommandLinePropertySource} having the given name
	 * and backed by the given source object.
	 */
	// 创建一个具有给定名称并由给定源对象支持的新 {@link CommandLinePropertySource}。
	public CommandLinePropertySource(String name, T source) {
		super(name, source);
	}


	/**
	 * Specify the name of the special "non-option arguments" property.
	 * The default is {@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}.
	 */
	// 指定特殊“非选项参数”属性的名称。默认值为 {@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}
	public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
		this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
	}

	/**
	 * This implementation first checks to see if the name specified is the special
	 * {@linkplain #setNonOptionArgsPropertyName(String) "non-option arguments" property},
	 * and if so delegates to the abstract {@link #getNonOptionArgs()} method
	 * checking to see whether it returns an empty collection. Otherwise delegates to and
	 * returns the value of the abstract {@link #containsOption(String)} method.
	 */
	// 此实现首先检查指定的名称是否是特殊的 {@linkplain #setNonOptionArgsPropertyName(String)
	// "non-option arguments" property}，如果是，则委托抽象的 {@link #getNonOptionArgs()} 方法
	// 检查它是否返回一个空的集合。否则委托并返回抽象 {@link #containsOption(String)} 方法的值。
	@Override
	public final boolean containsProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			return !this.getNonOptionArgs().isEmpty();
		}
		return this.containsOption(name);
	}

	/**
	 * This implementation first checks to see if the name specified is the special
	 * {@linkplain #setNonOptionArgsPropertyName(String) "non-option arguments" property},
	 * and if so delegates to the abstract {@link #getNonOptionArgs()} method. If so
	 * and the collection of non-option arguments is empty, this method returns {@code
	 * null}. If not empty, it returns a comma-separated String of all non-option
	 * arguments. Otherwise delegates to and returns the result of the abstract {@link
	 * #getOptionValues(String)} method.
	 */
	// 此实现首先检查指定的名称是否是特殊的 {@linkplain #setNonOptionArgsPropertyName(String) “非选项参数”属性}，
	// 如果是，则委托给抽象的 {@link #getNonOptionArgs()} 方法。如果是这样并且非选项参数的集合为空，则此方法返回 {@code null}。
	// 如果不为空，则返回所有非选项参数的逗号分隔字符串。否则委托并返回抽象 {@link #getOptionValues(String)} 方法的结果。
	@Override
	@Nullable
	public final String getProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			Collection<String> nonOptionArguments = this.getNonOptionArgs();
			if (nonOptionArguments.isEmpty()) {
				return null;
			}
			else {
				return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
			}
		}
		Collection<String> optionValues = this.getOptionValues(name);
		if (optionValues == null) {
			return null;
		}
		else {
			return StringUtils.collectionToCommaDelimitedString(optionValues);
		}
	}


	/**
	 * Return whether the set of option arguments parsed from the command line contains
	 * an option with the given name.
	 */
	// 返回从命令行解析的选项参数集是否包含具有给定名称的选项。
	protected abstract boolean containsOption(String name);

	/**
	 * Return the collection of values associated with the command line option having the
	 * given name.
	 * <ul>
	 * <li>if the option is present and has no argument (e.g.: "--foo"), return an empty
	 * collection ({@code []})</li>
	 * <li>if the option is present and has a single value (e.g. "--foo=bar"), return a
	 * collection having one element ({@code ["bar"]})</li>
	 * <li>if the option is present and the underlying command line parsing library
	 * supports multiple arguments (e.g. "--foo=bar --foo=baz"), return a collection
	 * having elements for each value ({@code ["bar", "baz"]})</li>
	 * <li>if the option is not present, return {@code null}</li>
	 * </ul>
	 */
	// 返回与具有给定名称的命令行选项关联的值的集合。
	//如果该选项存在且没有参数（例如：“--foo”），则返回一个空集合（ [] ）
	//如果该选项存在并且只有一个值（例如“--foo=bar”），则返回一个包含一个元素的集合（ ["bar"] ）
	//如果该选项存在并且底层命令行解析库支持多个参数（例如“--foo=bar --foo=baz”），则返回一个包含每个值元素的集合（ ["bar", "baz"] ）
	//如果该选项不存在，则返回null
	@Nullable
	protected abstract List<String> getOptionValues(String name);

	/**
	 * Return the collection of non-option arguments parsed from the command line.
	 * Never {@code null}.
	 */
	// 返回从命令行解析的非选项参数的集合。从不为空
	protected abstract List<String> getNonOptionArgs();

}
