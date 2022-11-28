This is the todo list:

- filter the keys that are not the state data in `DynamicPropertiesStore`, method: `public void put(byte[] key, BytesCapsule item)`
- design the logic that decouple the asset10 from account struct, in `WorldStateCallBackUtils` 
- design contract state query
  - create another implementation of `Repositoty` 
  - create another implementation of `StorageFactory` 
- add the state query interface
- make the world state tree switch as a config, e.g: `worldState = true` in `config.conf`
- make the trie prune as a config, e.g: `stateTriePrune = true` in `config.conf`
or 
``` 
worldstate { 
  open = true;
  prune = false;
}
```