package org.tron.core.services.admin.ipc.client;

import java.util.List;
import java.util.Locale;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class IpcCommandCompleter implements Completer {

  private final String[] commands;

  public IpcCommandCompleter(String... commands) {
    this.commands = commands;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String buffer = line.word().toLowerCase(Locale.ROOT);

    for (String cmd : commands) {
      if (cmd.toLowerCase(Locale.ROOT).startsWith(buffer)) {
        candidates.add(new Candidate(cmd, cmd, null, null, null, null, true));
      }
    }
  }
}
