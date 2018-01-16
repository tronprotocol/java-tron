/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.protos.core.TronTXOutput;
import org.tron.protos.core.TronTXOutputs;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.protos.core.TronTXOutput;
import org.tron.protos.core.TronTXOutputs;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class UTXOSetTest {

    private LevelDbDataSourceImpl mockTransactionDb;
    private Blockchain mockBlockchain;
    private UTXOSet utxoSet;
    private Wallet testWallet;
    private TronTXOutput.TXOutput.Builder outputBuilder;
    private TronTXOutputs.TXOutputs.Builder outputsBuilder;

    @Before
    public void setup() {
        mockTransactionDb = Mockito.mock(LevelDbDataSourceImpl.class);
        mockBlockchain = Mockito.mock(Blockchain.class);
        utxoSet = new UTXOSet(mockTransactionDb, mockBlockchain);

        testWallet = new Wallet();
        outputBuilder = TronTXOutput.TXOutput.newBuilder();
        outputsBuilder = TronTXOutputs.TXOutputs.newBuilder();
    }

    @Test
    public void testReindex() {
        String key = "15f3988aa8d56eab3bfca45144bad77fc60acce50437a0a9d794a03a83c15c5e";
        TronTXOutputs.TXOutputs testOutputs = outputsBuilder
                .addOutputs(outputBuilder.build())
                .build();

        HashMap<String, TronTXOutputs.TXOutputs> testUTXO = new HashMap<>();
        testUTXO.put(key, outputsBuilder.build());

        when(mockBlockchain.findUTXO()).thenReturn(testUTXO);

        utxoSet.reindex();
        Mockito.verify(mockTransactionDb, times(1)).resetDB();
        Mockito.verify(mockTransactionDb, times(1))
                .putData(eq(ByteArray.fromHexString(key)), eq(testOutputs.toByteArray()));
    }

    @Test
    public void testNoSpendableOutputsWhenNoTransactions() {
        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), 10);
        assertEquals(0, result.getAmount());
        assertTrue(result.getUnspentOutputs().isEmpty());
    }

    @Test
    public void testMatchingSpendableOutputWhenOutputForFullAmountMatchesWalletPublicKey() {
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(testWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);

        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), 10);

        assertEquals(outputBuilder.build().getValue(), result.getAmount());
        assertEquals(1, result.getUnspentOutputs().size());
    }

    @Test
    public void testReturnAvailableSpendableOutputWhenInsufficientToCoverTheAmount() {
        long testAmount = 100;
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(testWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);

        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), testAmount);

        assertEquals(outputBuilder.build().getValue(), result.getAmount());
        assertEquals(1, result.getUnspentOutputs().size());
    }

    @Test
    public void testFindMultipleSpendableOutputsWhenRequiredToCoverTheAmount() {
        long testAmount = 20L;
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(testWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        byte[] key2 = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        keySet.add(key2);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);
        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key2)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), testAmount);

        assertEquals(testAmount, result.getAmount());
        assertEquals(2, result.getUnspentOutputs().size());
    }

    @Test
    public void testReturnFullAmountOfSpendableOutputsRequiredToCoverTheAmount() {
        long testAmount = 17L;
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(testWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        byte[] key2 = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        keySet.add(key2);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);
        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key2)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), testAmount);

        assertEquals(outputBuilder.build().getValue() * 2, result.getAmount());
        assertEquals(2, result.getUnspentOutputs().size());
    }

    @Test
    public void testOnlyReturnFullSpendableOutputsRequiredToCoverTheAmountWhenMoreSpendableOutputAvailable() {
        long testAmount = 17L;
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(testWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        byte[] key2 = RandomUtils.nextBytes(20);
        byte[] key3 = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        keySet.add(key2);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);
        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key2)).thenReturn(outputsBuilder.build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key3)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), testAmount);

        assertEquals(outputBuilder.build().getValue() * 2, result.getAmount());
        assertTrue(result.getUnspentOutputs().containsKey(ByteArray.toHexString(key)));
        assertTrue(result.getUnspentOutputs().containsKey(ByteArray.toHexString(key2)));
        assertFalse(result.getUnspentOutputs().containsKey(ByteArray.toHexString(key3)));
    }

    @Test
    public void testNoSpendableOutputWhenNoOutputMatchesWalletPublicKey(){
        Wallet aDifferentWallet = new Wallet();
        outputBuilder.setValue(10).setPubKeyHash(ByteString.copyFrom(aDifferentWallet.getAddress()));

        outputsBuilder.addOutputs(outputBuilder.build());

        byte[] key = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);

        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.build().toByteArray());

        SpendableOutputs result = utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), 10);

        assertEquals(0, result.getAmount());
        assertTrue(result.getUnspentOutputs().isEmpty());
    }

    @Test
    public void testInvalidProtocolBufferExceptionInFindSpendableOutput() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));

        byte[] key = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);

        Mockito.when(mockTransactionDb.getData(key)).thenReturn(new byte[20]);

        utxoSet.findSpendableOutputs(testWallet.getEcKey().getPubKey(), 10);

        assertTrue(outContent.toString().contains("com.google.protobuf.InvalidProtocolBufferException"));
    }

    @Test
    public void testFindUTXOFindsAllUTXOForTheSuppliedWallet() {
        Wallet aDifferentWallet = new Wallet();

        TronTXOutput.TXOutput output1 = outputBuilder.setPubKeyHash(ByteString.copyFrom(testWallet.getAddress())).build();
        TronTXOutput.TXOutput output2 = outputBuilder.setPubKeyHash(ByteString.copyFrom(testWallet.getAddress())).build();
        TronTXOutput.TXOutput output3 = outputBuilder.setPubKeyHash(ByteString.copyFrom(aDifferentWallet.getAddress())).build();

        byte[] key = RandomUtils.nextBytes(20);
        byte[] key2 = RandomUtils.nextBytes(20);
        byte[] key3 = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        keySet.add(key2);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);
        Mockito.when(mockTransactionDb.getData(key)).thenReturn(outputsBuilder.addOutputs(output1).build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key2)).thenReturn(outputsBuilder.addOutputs(output2).build().toByteArray());
        Mockito.when(mockTransactionDb.getData(key3)).thenReturn(outputsBuilder.addOutputs(output3).build().toByteArray());


        ArrayList<TronTXOutput.TXOutput> result = utxoSet.findUTXO(testWallet.getEcKey().getPubKey());

        assertTrue(result.contains(output1));
        assertTrue(result.contains(output2));
        assertFalse(result.contains(output3));
    }

    @Test
    public void testInvalidProtocolBufferExceptionInFindUTXO() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));

        byte[] key = RandomUtils.nextBytes(20);
        Set<byte[]> keySet = new HashSet<>();
        keySet.add(key);
        Mockito.when(mockTransactionDb.allKeys()).thenReturn(keySet);

        Mockito.when(mockTransactionDb.getData(key)).thenReturn(new byte[20]);

        utxoSet.findUTXO(testWallet.getEcKey().getPubKey());

        assertTrue(outContent.toString().contains("com.google.protobuf.InvalidProtocolBufferException"));
    }
}
