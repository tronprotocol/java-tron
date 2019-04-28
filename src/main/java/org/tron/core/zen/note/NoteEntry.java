package org.tron.core.zen.note;

import lombok.AllArgsConstructor;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.note.BaseNote.Note;
import org.tron.core.zen.transaction.BaseOutPoint.OutPoint;

@AllArgsConstructor
public class NoteEntry {

  /**
   * Sapling note, its location in a transaction, and number of confirmations.
   */
  public OutPoint op;

  public PaymentAddress address;
  public Note note;
  public byte[] memo; // ZC_MEMO_SIZE
}
