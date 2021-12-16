package org.tron.core.vm.program;

import java.util.Objects;
import org.tron.common.runtime.vm.DataWord;
import org.tron.core.vm.program.listener.ProgramListener;
import org.tron.core.vm.program.listener.ProgramListenerAware;

public class Stack extends java.util.Stack<DataWord> implements ProgramListenerAware {

  private static final long serialVersionUID = 1;

  private transient ProgramListener programListener;

  @Override
  public void setProgramListener(ProgramListener listener) {
    this.programListener = listener;
  }

  @Override
  public synchronized DataWord pop() {
    if (programListener != null) {
      programListener.onStackPop();
    }
    return super.pop();
  }

  @Override
  public DataWord push(DataWord item) {
    if (programListener != null) {
      programListener.onStackPush(item);
    }
    return super.push(item);
  }

  public void swap(int from, int to) {
    if (isAccessible(from) && isAccessible(to) && (from != to)) {
      if (programListener != null) {
        programListener.onStackSwap(from, to);
      }
      DataWord tmp = get(from);
      set(from, set(to, tmp));
    }
  }

  private boolean isAccessible(int from) {
    return from >= 0 && from < size();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (o.getClass() != this.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Stack dataWords = (Stack) o;
    return Objects.equals(programListener, dataWords.programListener);
  }


  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), programListener);
  }
}
