package org.tron.core.services.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a concrete servlet under the registry-managed package as deliberately not exposed through
 * {@link HttpApiRegistry}, with the reason recorded on the class itself.
 *
 * <p>{@link HttpApiRegistry} requires every concrete servlet in that package to carry either
 * {@link HttpApi} or this annotation. Without that rule a servlet could be added and simply never
 * registered — the silent-omission failure this refactor exists to remove, only moved from the
 * registration lists to the annotations. Opting out is therefore an explicit, reviewable act.
 *
 * <p>Like {@link HttpApi} this is not {@code @Inherited} and is read only as a declared annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpApiExcluded {

  /** Why the servlet is not registered; must not be blank. */
  String value();
}
