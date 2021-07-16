contract A {
    uint b;
    uint a;
    uint c;
    uint d;
    uint e;

    function getA() external returns(uint,uint) {
        uint slot;
        uint offset;
        assembly {
//            old grammer
//           slot := a_slot
//           offset := a_offset
            slot := a.slot
            offset := a.offset
        }
        return (slot, offset);
    }

    function getE() external returns(uint,uint) {
        uint slot;
        uint offset;
        assembly {
//            slot := e_slot
//            offset := e_offset
            slot := e.slot
            offset := e.offset
        }
        return (slot, offset);
    }
}
