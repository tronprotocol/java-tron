package org.tron.core.services.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one http endpoint on the servlet that serves it: the path suffix it is mounted under,
 * whether it mutates state, and the surfaces (http services) it is exposed on.
 *
 * <p>This annotation is the single declaration of an endpoint's exposure. {@link HttpApiRegistry}
 * derives a read-only registry and the audit matrix from it, so no hand-maintained table repeats
 * the information and an endpoint cannot drift between its implementation and its registration.
 *
 * <p><b>Deliberately not {@code @Inherited}, and read only via
 * {@link Class#getDeclaredAnnotation}.</b> Servlets in this code base have historically been
 * subclassed to vary behaviour. If the annotation were inheritable, or were looked up with a
 * superclass-walking helper such as Spring's {@code AnnotatedElementUtils#findMergedAnnotation},
 * any future subclass would silently inherit its parent's suffix, access and surfaces: a
 * {@code WRITE} endpoint could reach a cursor surface without anyone declaring it.
 * {@link HttpApiRegistry} asserts both properties at startup so the guarantee cannot be removed
 * by accident.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpApi {

  /** Path suffix the endpoint is mounted under, with no leading slash and no {@code '/'}. */
  String value();

  /** Whether the endpoint mutates chain state. */
  Access access();

  /** Http services the endpoint is exposed on; must not be empty. */
  Surface[] surfaces();

  /**
   * Http services an endpoint can be exposed on. FULL / SOLIDITY / PBFT are the three
   * services run by the FullNode process (its HEAD, solidified and PBFT-finalized views);
   * SOLIDITY_NODE is run only when a node starts as a standalone SolidityNode process.
   */
  enum Surface {
    FULL, SOLIDITY, PBFT, SOLIDITY_NODE
  }

  /**
   * READ: no state mutation — queries, crypto derivation and constant VM calls.
   * BUILD: no state mutation — composes an unsigned transaction, or the parameters for one,
   *   for the client to sign and broadcast.
   * WRITE: pushes a transaction into the chain.
   */
  enum Access {
    READ, BUILD, WRITE
  }
}
