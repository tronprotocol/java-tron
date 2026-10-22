package org.tron.core.services.admin.ipc.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.tron.common.math.StrictMathWrapper;

final class ActivePeerOutputFormatter {

  private static final int MAX_TABLE_CELL_LENGTH = 64;
  private static final String METHOD = "admin_listActivePeers";
  private static final String[] FIELD_MAPPINGS = {
      "Address = remoteAddress",
      "Conn = connectSeconds; Latency = averageLatencyMillis; Block = lastKnownBlockNum",
      "Sync P/U = needSyncFromPeer / needSyncFromUs (Y/N)",
      "Fetch Size/Head = syncToFetchSize / syncToFetchSizePeekNum",
      "Req/Remain = syncBlockRequestedSize / remainNum",
      "Chain = syncChainRequestedMillis; Idle = inactiveSeconds; Proc = syncBlockInProcessSize"
  };
  private static final String[] TABLE_HEADERS = {
      "Address", "Conn", "Latency", "Block", "Sync P/U", "Fetch Size/Head", "Req/Remain",
      "Chain", "Idle", "Proc"
  };

  ActivePeerOutputFormatter() {
  }

  boolean supports(String method) {
    return METHOD.equals(method);
  }

  String usage() {
    return METHOD + " [format:json|text]";
  }

  OutputFormat parseFormat(String value) {
    if ("json".equalsIgnoreCase(value)) {
      return OutputFormat.JSON;
    }
    if ("text".equalsIgnoreCase(value)) {
      return OutputFormat.TEXT;
    }
    throw new IllegalArgumentException("Invalid value for <format>; expected json or text");
  }

  String formatText(JsonNode result) {
    if (result == null || !result.isObject()) {
      return null;
    }
    JsonNode peers = result.get("peers");
    if (peers == null || !peers.isArray()) {
      return null;
    }

    List<List<String>> rows = new ArrayList<>();
    for (JsonNode peer : peers) {
      if (!peer.isObject()) {
        return null;
      }
      rows.add(createTableRow(peer));
    }

    int[] widths = new int[TABLE_HEADERS.length];
    for (int i = 0; i < TABLE_HEADERS.length; i++) {
      widths[i] = TABLE_HEADERS[i].length();
      for (List<String> row : rows) {
        widths[i] = StrictMathWrapper.max(widths[i], row.get(i).length());
      }
    }

    String lineSeparator = System.lineSeparator();
    StringBuilder table = new StringBuilder();
    table.append("Peers: all=").append(formatTableCell(result.get("allCount")))
        .append(", active=").append(formatTableCell(result.get("activeCount")))
        .append(", passive=").append(formatTableCell(result.get("passiveCount")))
        .append(", effective=").append(formatTableCell(result.get("effectiveCount")))
        .append(lineSeparator).append(lineSeparator);
    table.append("Fields:").append(lineSeparator);
    for (String mapping : FIELD_MAPPINGS) {
      table.append("  ").append(mapping).append(lineSeparator);
    }
    table.append("Units: Conn/Idle=s, Latency/Chain=ms")
        .append(lineSeparator).append(lineSeparator);
    appendTableRow(table, Arrays.asList(TABLE_HEADERS), widths);
    table.append(lineSeparator);
    appendTableSeparator(table, widths);
    for (List<String> row : rows) {
      table.append(lineSeparator);
      appendTableRow(table, row, widths);
    }
    return table.toString();
  }

  private List<String> createTableRow(JsonNode peer) {
    return Arrays.asList(
        formatTableCell(peer.get("remoteAddress")),
        formatTableCell(peer.get("connectSeconds")),
        formatTableCell(peer.get("averageLatencyMillis")),
        formatTableCell(peer.get("lastKnownBlockNum")),
        formatBooleanPair(peer.get("needSyncFromPeer"), peer.get("needSyncFromUs")),
        formatPair(peer.get("syncToFetchSize"), peer.get("syncToFetchSizePeekNum")),
        formatPair(peer.get("syncBlockRequestedSize"), peer.get("remainNum")),
        formatTableCell(peer.get("syncChainRequestedMillis")),
        formatTableCell(peer.get("inactiveSeconds")),
        formatTableCell(peer.get("syncBlockInProcessSize")));
  }

  private String formatBooleanPair(JsonNode fromPeer, JsonNode fromUs) {
    return formatBooleanCell(fromPeer) + "/" + formatBooleanCell(fromUs);
  }

  private String formatBooleanCell(JsonNode value) {
    if (value == null || !value.isBoolean()) {
      return "-";
    }
    return value.asBoolean() ? "Y" : "N";
  }

  private String formatPair(JsonNode first, JsonNode second) {
    return sanitizeTableCell(formatTableCell(first) + "/" + formatTableCell(second));
  }

  private String formatTableCell(JsonNode value) {
    if (value == null || value.isNull()) {
      return "-";
    }
    return sanitizeTableCell(value.asText());
  }

  private String sanitizeTableCell(String text) {
    StringBuilder sanitized = new StringBuilder(
        StrictMathWrapper.min(text.length(), MAX_TABLE_CELL_LENGTH));
    for (int i = 0; i < text.length() && sanitized.length() < MAX_TABLE_CELL_LENGTH; i++) {
      char character = text.charAt(i);
      // Limit cells to printable ASCII to prevent terminal control-sequence injection.
      sanitized.append(character >= 0x20 && character <= 0x7e && character != '|'
          ? character : '?');
    }
    if (text.length() > MAX_TABLE_CELL_LENGTH) {
      sanitized.replace(MAX_TABLE_CELL_LENGTH - 3, MAX_TABLE_CELL_LENGTH, "...");
    }
    return sanitized.toString();
  }

  private void appendTableRow(StringBuilder table, List<String> cells, int[] widths) {
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        table.append(" | ");
      }
      table.append(StringUtils.rightPad(cells.get(i), widths[i]));
    }
  }

  private void appendTableSeparator(StringBuilder table, int[] widths) {
    for (int i = 0; i < widths.length; i++) {
      if (i > 0) {
        table.append("-+-");
      }
      table.append(StringUtils.repeat('-', widths[i]));
    }
  }

  enum OutputFormat {
    JSON,
    TEXT
  }
}
