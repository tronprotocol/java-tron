package org.tron.core.vm.repository;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.Strings;
import org.tron.common.utils.*;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.args.Parameter;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.TypeMismatchNamingException;
import org.tron.core.store.*;
import org.tron.core.vm.DataWord;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Storage;
import org.tron.protos.Protocol;

import java.util.HashMap;

import static java.lang.Long.max;
import static org.tron.core.config.args.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "Repository")
public class RepositoryImpl implements Repository {
    //for energycal
    private long precision = Parameter.ChainConstant.PRECISION;;
    private long windowSize =  Parameter.ChainConstant.WINDOW_SIZE_MS /
            Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

    private StoreFactory storeFactory;
    @Getter
    private DynamicPropertiesStore dynamicPropertiesStore;
    @Getter
    private AccountStore accountStore;
    @Getter
    private AssetIssueStore assetIssueStore;
    @Getter
    private AssetIssueV2Store assetIssueV2Store;

    //TODO：need codestore
    //@Getter
    //private CodeStore codeStore;

    @Getter
    private ContractStore contractStore;

    private Repository parent = null;

    private HashMap<Key, Value> accountCache = new HashMap<>();
    private HashMap<Key, Value> codeCache = new HashMap<>();
    private HashMap<Key, Value> contractCache = new HashMap<>();
    private HashMap<Key, Value> dynamicPropertiesCache = new HashMap<>();
    private HashMap<Key, Storage> storageCache = new HashMap<>();

    private HashMap<Key, Value> assetIssueCache = new HashMap<>();


    public RepositoryImpl(StoreFactory storeFactory, RepositoryImpl repository) {
        init(storeFactory,repository);
    }

    protected void init(StoreFactory storeFactory, RepositoryImpl parent) {
        try {
            dynamicPropertiesStore = storeFactory.getStore(DynamicPropertiesStore.class);
            accountStore = storeFactory.getStore(AccountStore.class);
            //TODO：need codestore
            //codeStore = storeFactory.getStore(CodeStore.class);
            contractStore = storeFactory.getStore(ContractStore.class);
            assetIssueStore = storeFactory.getStore(AssetIssueStore.class);
            assetIssueV2Store = storeFactory.getStore(AssetIssueV2Store.class);

        } catch (TypeMismatchNamingException e) {
            logger.error("Repository init error",e);
        }
        this.parent = parent;
    }

    @Override
    public Repository newRepositoryChild() {
        return new RepositoryImpl(storeFactory,this);
    }



    @Override
    public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {
        long now = getHeadSlot();

        long energyUsage = accountCapsule.getEnergyUsage();
        long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
        long energyLimit = calculateGlobalEnergyLimit(accountCapsule);

        long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

        return max(energyLimit - newEnergyUsage, 0); // us
    }


    @Override
    public AssetIssueCapsule getAssetIssue(byte[] tokenId) {
        byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
        Key key = Key.create(tokenIdWithoutLeadingZero);
        if (assetIssueCache.containsKey(key)) {
            return assetIssueCache.get(key).getAssetIssue();
        }

        AssetIssueCapsule assetIssueCapsule;
        if (this.parent != null) {
            assetIssueCapsule = parent.getAssetIssue(tokenIdWithoutLeadingZero);
        } else {
            assetIssueCapsule = Commons.getAssetIssueStoreFinal(dynamicPropertiesStore,assetIssueStore,assetIssueV2Store).get(tokenIdWithoutLeadingZero);
        }
        if (assetIssueCapsule != null) {
            assetIssueCache.put(key, Value.create(assetIssueCapsule.getData()));
        }
        return assetIssueCapsule;
    }

    @Override
    public AccountCapsule createAccount(byte[] address, Protocol.AccountType type) {
        Key key = new Key(address);
        AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), type);
        accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
        return account;    }

    @Override
    public AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type) {
        Key key = new Key(address);
        AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address),
                ByteString.copyFromUtf8(accountName),
                type);

        accountCache.put(key, new Value(account.getData(), Type.VALUE_TYPE_CREATE));
        return account;
    }

    @Override
    public AccountCapsule getAccount(byte[] address) {
        Key key = new Key(address);
        if (accountCache.containsKey(key)) {
            return accountCache.get(key).getAccount();
        }

        AccountCapsule accountCapsule;
        if (parent != null) {
            accountCapsule = parent.getAccount(address);
        } else {
            accountCapsule = getAccountStore().get(address);
        }

        if (accountCapsule != null) {
            accountCache.put(key, Value.create(accountCapsule.getData()));
        }
        return accountCapsule;
    }

    @Override
    public BytesCapsule getDynamic(byte[] word) {
        Key key = Key.create(word);
        if (dynamicPropertiesCache.containsKey(key)) {
            return dynamicPropertiesCache.get(key).getDynamicProperties();
        }

        BytesCapsule bytesCapsule;
        if (parent != null) {
            bytesCapsule = parent.getDynamic(word);
        } else {
            try {
                bytesCapsule = getDynamicPropertiesStore().get(word);
            } catch (BadItemException | ItemNotFoundException e) {
                logger.warn("Not found dynamic property:" + Strings.fromUTF8ByteArray(word));
                bytesCapsule = null;
            }
        }

        if (bytesCapsule != null) {
            dynamicPropertiesCache.put(key, Value.create(bytesCapsule.getData()));
        }
        return bytesCapsule;
    }

    @Override
    public void deleteContract(byte[] address) {
        //TODO：need codestore
        //getCodeStore().delete(address);
        getAccountStore().delete(address);
        getContractStore().delete(address);
    }

    @Override
    public void createContract(byte[] address, ContractCapsule contractCapsule) {
        Key key = Key.create(address);
        Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_CREATE);
        contractCache.put(key, value);
    }

    @Override
    public ContractCapsule getContract(byte[] address) {
        Key key = Key.create(address);
        if (contractCache.containsKey(key)) {
            return contractCache.get(key).getContract();
        }

        ContractCapsule contractCapsule;
        if (parent != null) {
            contractCapsule = parent.getContract(address);
        } else {
            contractCapsule = getContractStore().get(address);
        }

        if (contractCapsule != null) {
            contractCache.put(key, Value.create(contractCapsule.getData()));
        }
        return contractCapsule;
    }

    @Override
    public void updateContract(byte[] address, ContractCapsule contractCapsule) {
        Key key = Key.create(address);
        Value value = Value.create(contractCapsule.getData(), Type.VALUE_TYPE_DIRTY);
        contractCache.put(key, value);
    }

    @Override
    public void updateAccount(byte[] address, AccountCapsule accountCapsule) {
        Key key = Key.create(address);
        Value value = Value.create(accountCapsule.getData(), Type.VALUE_TYPE_DIRTY);
        accountCache.put(key, value);
    }

    @Override
    public void saveCode(byte[] address, byte[] code) {
        Key key = Key.create(address);
        Value value = Value.create(code, Type.VALUE_TYPE_CREATE);
        codeCache.put(key, value);

        if (VMConfig.allowTvmConstantinople()) {
            ContractCapsule contract = getContract(address);
            byte[] codeHash = Hash.sha3(code);
            contract.setCodeHash(codeHash);
            updateContract(address, contract);
        }
    }

    @Override
    public byte[] getCode(byte[] address) {
        //TODO：code store
/*        Key key = Key.create(address);
        if (codeCache.containsKey(key)) {
            return codeCache.get(key).getCode().getData();
        }

        byte[] code;
        if (parent != null) {
            code = parent.getCode(address);
        } else {
            if (null == getCodeStore().get(address)) {
                code = null;
            } else {
                code = getCodeStore().get(address).getData();
            }
        }
        if (code != null) {
            codeCache.put(key, Value.create(code));
        }
        return code;*/
        return null;
    }

    @Override
    public void putStorageValue(byte[] address, DataWord key, DataWord value) {

    }

    @Override
    public DataWord getStorageValue(byte[] address, DataWord key) {
        return null;
    }

    @Override
    public Storage getStorage(byte[] address) {
        //TODO:ADD getStorageRowStore
/*        Key key = Key.create(address);
        if (storageCache.containsKey(key)) {
            return storageCache.get(key);
        }
        Storage storage;
        if (this.parent != null) {
            Storage parentStorage = parent.getStorage(address);
            if (VMConfig.getEnergyLimitHardFork()) {
                // deep copy
                storage = new Storage(parentStorage);
            } else {
                storage = parentStorage;
            }
        } else {
            storage = new Storage(address, dbManager.getStorageRowStore());
        }
        ContractCapsule contract = getContract(address);
        if (contract != null && !ByteUtil.isNullOrZeroArray(contract.getTrxHash())) {
            storage.generateAddrHash(contract.getTrxHash());
        }
        return storage;*/
        return null;
    }

    @Override
    public long getBalance(byte[] address) {
        AccountCapsule accountCapsule = getAccount(address);
        return accountCapsule == null ? 0L : accountCapsule.getBalance();    }

    @Override
    public long addBalance(byte[] address, long value) {
        AccountCapsule accountCapsule = getAccount(address);
        if (accountCapsule == null) {
            accountCapsule = createAccount(address, Protocol.AccountType.Normal);
        }

        long balance = accountCapsule.getBalance();
        if (value == 0) {
            return balance;
        }

        if (value < 0 && balance < -value) {
            throw new RuntimeException(
                    StringUtil.createReadableString(accountCapsule.createDbKey())
                            + " insufficient balance");
        }
        accountCapsule.setBalance(Math.addExact(balance, value));
        Key key = Key.create(address);
        Value val = Value.create(accountCapsule.getData(),
                Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
        accountCache.put(key, val);
        return accountCapsule.getBalance();
    }


    @Override
    public void setParent(Repository repository) {
        parent = repository;
    }

    @Override
    public void commit() {
        Repository repository = null;
        if (parent != null) {
            repository = parent;
        }
        commitAccountCache(repository);
        commitCodeCache(repository);
        commitContractCache(repository);
        commitStorageCache(repository);
    }

    @Override
    public void putAccount(Key key, Value value) {
        accountCache.put(key, value);
    }


    @Override
    public void putCode(Key key, Value value) {
        codeCache.put(key, value);
    }

    @Override
    public void putContract(Key key, Value value) {
        contractCache.put(key, value);
    }

    @Override
    public void putStorage(Key key, Storage cache) {
        storageCache.put(key, cache);
    }

    @Override
    public void putAccountValue(byte[] address, AccountCapsule accountCapsule) {
        Key key = new Key(address);
        accountCache.put(key, new Value(accountCapsule.getData(), Type.VALUE_TYPE_CREATE));
    }

    @Override
    public long addTokenBalance(byte[] address, byte[] tokenId, long value) {
        byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
        AccountCapsule accountCapsule = getAccount(address);
        if (accountCapsule == null) {
            accountCapsule = createAccount(address, Protocol.AccountType.Normal);
        }
        long balance = accountCapsule.getAssetMapV2()
                .getOrDefault(new String(tokenIdWithoutLeadingZero), new Long(0));
        if (value == 0) {
            return balance;
        }

        if (value < 0 && balance < -value) {
            throw new RuntimeException(
                    StringUtil.createReadableString(accountCapsule.createDbKey())
                            + " insufficient balance");
        }
        if (value >= 0) {
            accountCapsule.addAssetAmountV2(tokenIdWithoutLeadingZero, value, getDynamicPropertiesStore(),getAssetIssueStore());
        } else {
            accountCapsule.reduceAssetAmountV2(tokenIdWithoutLeadingZero, -value, getDynamicPropertiesStore(),getAssetIssueStore());
        }
        Key key = Key.create(address);
        Value V = Value.create(accountCapsule.getData(),
                Type.VALUE_TYPE_DIRTY | accountCache.get(key).getType().getType());
        accountCache.put(key, V);
        return accountCapsule.getAssetMapV2().get(new String(tokenIdWithoutLeadingZero));
    }

    @Override
    public long getTokenBalance(byte[] address, byte[] tokenId) {
        AccountCapsule accountCapsule = getAccount(address);
        if (accountCapsule == null) {
            return 0;
        }
        String tokenStr = new String(ByteUtil.stripLeadingZeroes(tokenId));
        return accountCapsule.getAssetMapV2().getOrDefault(tokenStr, 0L);
    }

    @Override
    public byte[] getBlackHoleAddress() {
        return new byte[0];
    }



    private long increase(long lastUsage, long usage, long lastTime, long now) {
        return increase(lastUsage, usage, lastTime, now, windowSize);
    }

    private long increase(long lastUsage, long usage, long lastTime, long now, long windowSize) {
        long averageLastUsage = divideCeil(lastUsage * precision, windowSize);
        long averageUsage = divideCeil(usage * precision, windowSize);

        if (lastTime != now) {
            assert now > lastTime;
            if (lastTime + windowSize > now) {
                long delta = now - lastTime;
                double decay = (windowSize - delta) / (double) windowSize;
                averageLastUsage = Math.round(averageLastUsage * decay);
            } else {
                averageLastUsage = 0;
            }
        }
        averageLastUsage += averageUsage;
        return getUsage(averageLastUsage, windowSize);
    }

    private long divideCeil(long numerator, long denominator) {
        return (numerator / denominator) + ((numerator % denominator) > 0 ? 1 : 0);
    }

    private long getUsage(long usage, long windowSize) {
        return usage * windowSize / precision;
    }

    private long calculateGlobalEnergyLimit(AccountCapsule accountCapsule) {
        long frozeBalance = accountCapsule.getAllFrozenBalanceForEnergy();
        if (frozeBalance < 1_000_000L) {
            return 0;
        }

        long energyWeight = frozeBalance / 1_000_000L;
        long totalEnergyLimit = getDynamicPropertiesStore().getTotalEnergyCurrentLimit();
        long totalEnergyWeight = getDynamicPropertiesStore().getTotalEnergyWeight();

        assert totalEnergyWeight > 0;

        return (long) (energyWeight * ((double) totalEnergyLimit / totalEnergyWeight));
    }

    public long getHeadSlot() {
        return (getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() -
                Long.parseLong(DBConfig.getGenesisBlock().getTimestamp()))
                / BLOCK_PRODUCED_INTERVAL;
    }




    private void commitAccountCache(Repository deposit) {
        accountCache.forEach((key, value) -> {
            if (value.getType().isCreate() || value.getType().isDirty()) {
                if (deposit != null) {
                    deposit.putAccount(key, value);
                } else {
                    getAccountStore().put(key.getData(), value.getAccount());
                }
            }
        });
    }

    private void commitCodeCache(Repository deposit) {
        codeCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putCode(key, value);
                } else {
                    //TODO
                   // getCodeStore().put(key.getData(), value.getCode());
                }
            }
        }));
    }

    private void commitContractCache(Repository deposit) {
        contractCache.forEach(((key, value) -> {
            if (value.getType().isDirty() || value.getType().isCreate()) {
                if (deposit != null) {
                    deposit.putContract(key, value);
                } else {
                    getContractStore().put(key.getData(), value.getContract());
                }
            }
        }));
    }

    private void commitStorageCache(Repository deposit) {
        storageCache.forEach((Key address, Storage storage) -> {
            if (deposit != null) {
                // write to parent cache
                deposit.putStorage(address, storage);
            } else {
                // persistence
                storage.commit();
            }
        });

    }


    public static RepositoryImpl createRoot(StoreFactory storeFactory) {
        return new RepositoryImpl(storeFactory, null);
    }





}
