package errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

/**
 * Flags method references {@code String::toLowerCase} and {@code String::toUpperCase}
 * that resolve to the no-arg overload (which uses {@code Locale.getDefault()}).
 *
 * <p>The built-in ErrorProne {@code StringCaseLocaleUsage} checker only catches
 * direct method invocations ({@code s.toLowerCase()}), not method references
 * ({@code String::toLowerCase}). This checker closes that gap.
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "StringCaseLocaleUsageMethodRef",
    summary = "String::toLowerCase and String::toUpperCase method references use "
        + "Locale.getDefault(). Replace with a lambda that specifies Locale.ROOT, "
        + "e.g. s -> s.toLowerCase(Locale.ROOT).",
    severity = BugPattern.SeverityLevel.ERROR
)
public class StringCaseLocaleUsageMethodRef extends BugChecker
    implements BugChecker.MemberReferenceTreeMatcher {

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    String name = tree.getName().toString();
    if (!"toLowerCase".equals(name) && !"toUpperCase".equals(name)) {
      return Description.NO_MATCH;
    }
    // Verify the qualifier type is java.lang.String
    Type qualifierType = ASTHelpers.getType(tree.getQualifierExpression());
    if (qualifierType == null) {
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isSameType(qualifierType, state.getSymtab().stringType, state)) {
      return Description.NO_MATCH;
    }
    // Only flag the no-arg overload; the Locale-taking overload is safe
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym instanceof Symbol.MethodSymbol
        && ((Symbol.MethodSymbol) sym).getParameters().isEmpty()) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
