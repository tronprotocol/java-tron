package org.tron.core.consensus;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.consensus.base.BlockHandle;
import org.tron.consensus.base.State;
import org.tron.consensus.dpos.DposService;
import org.tron.consensus.dpos.DposSlot;
import org.tron.consensus.dpos.DposTask;
import org.tron.consensus.dpos.StateManager;

public class DposTaskTest {
  private DposTask dposTask = new DposTask();

  @Test
  public void tet() throws Exception {
    StateManager stateManager = mock(StateManager.class);
    Mockito.when(stateManager.getState()).thenReturn(State.BACKUP_IS_NOT_MASTER);

    Field field = dposTask.getClass().getDeclaredField("stateManager");
    field.setAccessible(true);
    field.set(dposTask, stateManager);

    Method method = dposTask.getClass().getDeclaredMethod("produceBlock");
    method.setAccessible(true);
    State state = (State) method.invoke(dposTask);

    Assert.assertEquals(State.BACKUP_IS_NOT_MASTER, state);


    Mockito.when(stateManager.getState()).thenReturn(State.OK);

    DposSlot dposSlot = mock(DposSlot.class);
    Mockito.when(dposSlot.getTime(1)).thenReturn(Long.MAX_VALUE);

    field = dposTask.getClass().getDeclaredField("dposSlot");
    field.setAccessible(true);
    field.set(dposTask, dposSlot);


    Mockito.when(stateManager.getState()).thenReturn(State.OK);

    BlockHandle blockHandle = mock(BlockHandle.class);
    Mockito.when(blockHandle.getLock()).thenReturn(new Object());


    DposService dposService = mock(DposService.class);
    Mockito.when(dposService.getBlockHandle()).thenReturn(blockHandle);

    field = dposTask.getClass().getDeclaredField("dposService");
    field.setAccessible(true);
    field.set(dposTask, dposService);

    state = (State) method.invoke(dposTask);

    Assert.assertEquals(State.NOT_TIME_YET, state);
  }

}
