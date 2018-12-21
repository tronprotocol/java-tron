package org.tron.common.logsfilter;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.*;
import org.tron.common.logsfilter.trigger.BlockLogTrigger;
import org.tron.common.logsfilter.trigger.ContractEventTrigger;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.common.logsfilter.trigger.TransactionLogTrigger;

import java.io.File;
import java.util.List;

@Slf4j
public class EventPluginLoader {

    private static EventPluginLoader instance;

    private PluginManager pluginManager = null;

    private String pluginPath = "";

    List<IPluginEventListener> eventListeners;

    public static EventPluginLoader getInstance(){
        if (Objects.isNull(instance)){
            synchronized(EventPluginLoader.class) {
                if (Objects.isNull(instance)) {
                    instance = new EventPluginLoader();
                }
            }
        }
        return instance;
    }

    public boolean startPlugin(String path){
        logger.info("Load plugin'{}'", path);

        this.pluginPath = path;

        File pluginPath = new File(path);

        if (false == pluginPath.exists()){
            logger.error("'{}' doesn't exist", path);
            return false;
        }

        if (Objects.isNull(pluginManager)){

            pluginManager = new DefaultPluginManager(pluginPath.toPath()) {
                @Override
                protected CompoundPluginDescriptorFinder createPluginDescriptorFinder() {
                    return new CompoundPluginDescriptorFinder()
                            .add(new ManifestPluginDescriptorFinder());
                }
            };
        }


        pluginManager.loadPlugin(pluginPath.toPath());

        pluginManager.startPlugins();

        eventListeners = pluginManager.getExtensions(IPluginEventListener.class);

        logger.info("'{}' loaded", path);

        return true;
    }

    public void stopPlugin(){
        if (pluginManager == null){
            logger.info("pluginManager is null");
            return;
        }

        pluginManager.stopPlugins();
        logger.info("'{}' stopped", pluginPath);
    }

    private void printPluginInfo(){
        if (Objects.isNull(pluginManager)) return;
        List<PluginWrapper> startedPlugins = pluginManager.getStartedPlugins();
        startedPlugins.forEach(plugin -> {
            String pluginId = plugin.getDescriptor().getPluginId();
            logger.info(String.format("Extensions added by plugin '%s':", pluginId));
        });
    }

    public void postBlockTrigger(BlockLogTrigger trigger){
        if (Objects.isNull(eventListeners)) return;
        eventListeners.forEach(listener -> {
            listener.handleBlockEvent(trigger);
        });
    }

    public void postTransactionTrigger(TransactionLogTrigger trigger){
        if (Objects.isNull(eventListeners)) return;
        eventListeners.forEach(listener -> {
            listener.handleTransactionTrigger(trigger);
        });
    }

    public void postContractLogTrigger(ContractLogTrigger trigger){
        if (Objects.isNull(eventListeners)) return;
        eventListeners.forEach(listener -> {
            listener.handleContractLogTrigger(trigger);
        });
    }

    public void postContractEventTrigger(ContractEventTrigger trigger){
        if (Objects.isNull(eventListeners)) return;
        for (IPluginEventListener listener : eventListeners) {
            listener.handleContractEventTrigger(trigger);
        }
    }

    public static void main(String[] args) {

        String path = "/Users/tron/workplace/java-tronSubmit/java-tronUseSubmit/develop_event_subscribe/eventplugin/plugins/kafkaplugin/build/libs/kafkaplugin-1.0.0.jar";

        EventPluginLoader.getInstance().startPlugin(path);

        EventPluginLoader.getInstance().printPluginInfo();


        BlockLogTrigger trigger = new BlockLogTrigger();
        trigger.setBlockHash("block hash");
        trigger.setBlockNumber(1000000);
        trigger.setTimeStamp(System.currentTimeMillis());

        EventPluginLoader.getInstance().postBlockTrigger(trigger);

        EventPluginLoader.getInstance().stopPlugin();
    }

}


