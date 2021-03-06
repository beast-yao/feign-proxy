package org.devil.proxy.annotation;

import org.devil.proxy.FeignClientsProxyRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author yaojun
 * 2020/5/8 14:28
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(FeignClientsProxyRegistrar.class)
public @interface EnableAutoProxyFeign {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @ComponentScan("org.my.pkg")} instead of
     * {@code @ComponentScan(basePackages="org.my.pkg")}.
     * @return the array of 'basePackages'.
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components.
     * <p>
     * {@link #value()} is an alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the array of 'basePackages'.
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated components. The package of each class specified will be scanned.
     * <p>
     * Consider creating a special no-op marker class or interface in each package that
     * serves no purpose other than being referenced by this attribute.
     *
     * @return the array of 'basePackageClasses'.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * List of classes annotated with @FeignClient. If not empty, disables classpath scanning.
     * @return
     */
    Class<?>[] clients() default {};

    /**
     * whether  to use feign proxy
     * @return
     */
    boolean enable() default true;

}
