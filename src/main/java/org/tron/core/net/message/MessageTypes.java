package org.tron.core.net.message;

import java.util.HashMap;
import java.util.Map;

public enum MessageTypes {

    FIRST(0x00),

    TRX(0x01),
    
    BLOCK(0x02),

    TRXS(0x03),

    BLOCKS(0x04),

    BLOCKHEADERS(0x05),

    GETITEMS(0x06),

    LAST(0xFF);

//    trx_message_type                             = 1000,
//    block_message_type                           = 1001,
//    core_message_type_first                      = 5000,
//    item_ids_inventory_message_type              = 5001,
//    blockchain_item_ids_inventory_message_type   = 5002,
//    fetch_blockchain_item_ids_message_type       = 5003,
//    fetch_items_message_type                     = 5004,
//    item_not_available_message_type              = 5005,
//    hello_message_type                           = 5006,
//    connection_accepted_message_type             = 5007,
//    connection_rejected_message_type             = 5008,
//    address_request_message_type                 = 5009,
//    address_message_type                         = 5010,
//    closing_connection_message_type              = 5011,
//    current_time_request_message_type            = 5012,
//    current_time_reply_message_type              = 5013,
//    check_firewall_message_type                  = 5014,
//    check_firewall_reply_message_type            = 5015,
//    get_current_connections_request_message_type = 5016,
//    get_current_connections_reply_message_type   = 5017,
//    core_message_type_last                       = 5099

    private final int type;

    private static final Map<Integer, MessageTypes> intToTypeMap = new HashMap<>();

    static {
        for (MessageTypes type :  MessageTypes.values()) {
            intToTypeMap.put(type.type, type);
        }
    }

    private MessageTypes(int type) { this.type = type;}


    public static MessageTypes fromByte(byte i) {
        return intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
        return  code < LAST.asByte();
    }

    public byte asByte() {
        return (byte) (type);
    }
}
