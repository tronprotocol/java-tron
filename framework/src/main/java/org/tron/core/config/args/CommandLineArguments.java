package org.tron.core.config.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.tron.core.exception.TronError;

/**
 * Parses startup options without initializing node configuration or logging. Keep this class
 * independent of Args, CommonParameter, and logging so FullNode can dispatch the standalone client.
 */
@Getter
public final class CommandLineArguments {

  private final CLIParameter parameters = new CLIParameter();
  private final JCommander commander = JCommander.newBuilder().addObject(parameters).build();
  private final List<ParameterDescription> assignedParameters;

  public CommandLineArguments(String[] args) {
    commander.parse(args);
    assignedParameters = commander.getParameters().stream()
        .filter(ParameterDescription::isAssigned)
        .collect(Collectors.toList());
  }

  public boolean isAttachMode() {
    // Preserve the existing help/version precedence over attach validation.
    if (parameters.help || parameters.version) {
      return false;
    }
    if (!isAssigned("ipcSocketFile")) {
      if (isAssigned("ipcExecCommand")) {
        throwAttachParameterError("Error: --exec requires --attach <socket-path>");
      }
      return false;
    }
    if (isAssigned("shellConfFileName")) {
      throwAttachParameterError("Error: --attach cannot be combined with: --config");
    }
    if (StringUtils.isBlank(parameters.ipcSocketFile)) {
      throwAttachParameterError("Error: --attach requires a non-empty <socket-path>");
    }
    // Node-only CLI options are irrelevant to the standalone IPC client and are ignored.
    return true;
  }

  private boolean isAssigned(String fieldName) {
    return assignedParameters.stream()
        .anyMatch(pd -> fieldName.equals(pd.getParameterized().getName()));
  }

  private static void throwAttachParameterError(String message) {
    System.err.println(message);
    throw new TronError(message, TronError.ErrCode.PARAMETER_INIT);
  }
}
