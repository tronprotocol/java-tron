package ethereum.ckzg4844;

public class CellsAndProofs {
  private final byte[] cells;
  private final byte[] proofs;

  public CellsAndProofs(final byte[] cells, final byte[] proofs) {
    this.cells = cells;
    this.proofs = proofs;
  }

  public byte[] getCells() {
    return cells;
  }

  public byte[] getProofs() {
    return proofs;
  }

  public static CellsAndProofs of(final byte[] cells, final byte[] proofs) {
    return new CellsAndProofs(cells, proofs);
  }
}
