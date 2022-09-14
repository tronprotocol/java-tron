package org.tron.program.generate;

import org.tron.protos.Protocol;

/**
 * @author liukai
 * @since 2022/9/9.
 */
public interface TransactionCreator {

  Protocol.Transaction create();

}
