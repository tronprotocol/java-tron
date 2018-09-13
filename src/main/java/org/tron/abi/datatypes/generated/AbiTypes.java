package org.tron.abi.datatypes.generated;

import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.abi.TypeReference;
import org.tron.abi.datatypes.*;
import org.tron.common.runtime.utils.MUtil;
import org.tron.core.Wallet;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesMapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public final class AbiTypes {
    private AbiTypes() {
    }

    public static byte[] convertFromTronAddress(byte[] address) {
        if (address.length == 21) {
            byte[] newAddress = new byte[20];
            System.arraycopy(address, 1, newAddress, 0, 20);
            address = newAddress;
        }
        return address;
    }

    public static Type getTypeWithValue(String type, String value) {
        switch (type.toLowerCase()) {
            case "address":
                if (StringUtils.isEmpty(value) || StringUtils.equals(value, "0")) {
                    return new Address(new Uint160(0));
                }
                return new Address(Hex.toHexString(convertFromTronAddress(Wallet.decodeFromBase58Check(value))));
            case "bool":
                return new Bool(Boolean.parseBoolean(value));
            case "string":
                return new Utf8String(value);
            case "bytes":
                return new Bytes32(Hex.decode(value));
            case "uint8":
                return new Uint8(Long.parseLong(value));
            case "int8":
                return new Int8(Long.parseLong(value));
            case "uint16":
                return new Uint16(Long.parseLong(value));
            case "int16":
                return new Int16(Long.parseLong(value));
            case "uint24":
                return new Uint24(Long.parseLong(value));
            case "int24":
                return new Int24(Long.parseLong(value));
            case "uint32":
                return new Uint32(Long.parseLong(value));
            case "int32":
                return new Int32(Long.parseLong(value));
            case "uint40":
                return new Uint40(Long.parseLong(value));
            case "int40":
                return new Int40(Long.parseLong(value));
            case "uint48":
                return new Uint48(Long.parseLong(value));
            case "int48":
                return new Int48(Long.parseLong(value));
            case "uint56":
                return new Uint56(Long.parseLong(value));
            case "int56":
                return new Int56(Long.parseLong(value));
            case "uint64":
                return new Uint64(Long.parseLong(value));
            case "int64":
                return new Int64(Long.parseLong(value));
            case "uint72":
                return new Uint72(Long.parseLong(value));
            case "int72":
                return new Int72(Long.parseLong(value));
            case "uint80":
                return new Uint80(Long.parseLong(value));
            case "int80":
                return new Int80(Long.parseLong(value));
            case "uint88":
                return new Uint88(Long.parseLong(value));
            case "int88":
                return new Int88(Long.parseLong(value));
            case "uint96":
                return new Uint96(Long.parseLong(value));
            case "int96":
                return new Int96(Long.parseLong(value));
            case "uint104":
                return new Uint104(Long.parseLong(value));
            case "int104":
                return new Int104(Long.parseLong(value));
            case "uint112":
                return new Uint112(Long.parseLong(value));
            case "int112":
                return new Int112(Long.parseLong(value));
            case "uint120":
                return new Uint120(Long.parseLong(value));
            case "int120":
                return new Int120(Long.parseLong(value));
            case "uint128":
                return new Uint128(Long.parseLong(value));
            case "int128":
                return new Int128(Long.parseLong(value));
            case "uint136":
                return new Uint136(Long.parseLong(value));
            case "int136":
                return new Int136(Long.parseLong(value));
            case "uint144":
                return new Uint144(Long.parseLong(value));
            case "int144":
                return new Int144(Long.parseLong(value));
            case "uint152":
                return new Uint152(Long.parseLong(value));
            case "int152":
                return new Int152(Long.parseLong(value));
            case "uint160":
                return new Uint160(Long.parseLong(value));
            case "int160":
                return new Int160(Long.parseLong(value));
            case "uint168":
                return new Uint168(Long.parseLong(value));
            case "int168":
                return new Int168(Long.parseLong(value));
            case "uint176":
                return new Uint176(Long.parseLong(value));
            case "int176":
                return new Int176(Long.parseLong(value));
            case "uint184":
                return new Uint184(Long.parseLong(value));
            case "int184":
                return new Int184(Long.parseLong(value));
            case "uint192":
                return new Uint192(Long.parseLong(value));
            case "int192":
                return new Int192(Long.parseLong(value));
            case "uint200":
                return new Uint200(Long.parseLong(value));
            case "int200":
                return new Int200(Long.parseLong(value));
            case "uint208":
                return new Uint208(Long.parseLong(value));
            case "int208":
                return new Int208(Long.parseLong(value));
            case "uint216":
                return new Uint216(Long.parseLong(value));
            case "int216":
                return new Int216(Long.parseLong(value));
            case "uint224":
                return new Uint224(Long.parseLong(value));
            case "int224":
                return new Int224(Long.parseLong(value));
            case "uint232":
                return new Uint232(Long.parseLong(value));
            case "int232":
                return new Int232(Long.parseLong(value));
            case "uint240":
                return new Uint240(Long.parseLong(value));
            case "int240":
                return new Int240(Long.parseLong(value));
            case "uint248":
                return new Uint248(Long.parseLong(value));
            case "int248":
                return new Int248(Long.parseLong(value));
            case "uint256":
                return new Uint256(Long.parseLong(value));
            case "int256":
                return new Int256(Long.parseLong(value));
            case "bytes1":
                return new Bytes1(Hex.decode(value));
            case "bytes2":
                return new Bytes2(Hex.decode(value));
            case "bytes3":
                return new Bytes3(Hex.decode(value));
            case "bytes4":
                return new Bytes4(Hex.decode(value));
            case "bytes5":
                return new Bytes5(Hex.decode(value));
            case "bytes6":
                return new Bytes6(Hex.decode(value));
            case "bytes7":
                return new Bytes7(Hex.decode(value));
            case "bytes8":
                return new Bytes8(Hex.decode(value));
            case "bytes9":
                return new Bytes9(Hex.decode(value));
            case "bytes10":
                return new Bytes10(Hex.decode(value));
            case "bytes11":
                return new Bytes11(Hex.decode(value));
            case "bytes12":
                return new Bytes12(Hex.decode(value));
            case "bytes13":
                return new Bytes13(Hex.decode(value));
            case "bytes14":
                return new Bytes14(Hex.decode(value));
            case "bytes15":
                return new Bytes15(Hex.decode(value));
            case "bytes16":
                return new Bytes16(Hex.decode(value));
            case "bytes17":
                return new Bytes17(Hex.decode(value));
            case "bytes18":
                return new Bytes18(Hex.decode(value));
            case "bytes19":
                return new Bytes19(Hex.decode(value));
            case "bytes20":
                return new Bytes20(Hex.decode(value));
            case "bytes21":
                return new Bytes21(Hex.decode(value));
            case "bytes22":
                return new Bytes22(Hex.decode(value));
            case "bytes23":
                return new Bytes23(Hex.decode(value));
            case "bytes24":
                return new Bytes24(Hex.decode(value));
            case "bytes25":
                return new Bytes25(Hex.decode(value));
            case "bytes26":
                return new Bytes26(Hex.decode(value));
            case "bytes27":
                return new Bytes27(Hex.decode(value));
            case "bytes28":
                return new Bytes28(Hex.decode(value));
            case "bytes29":
                return new Bytes29(Hex.decode(value));
            case "bytes30":
                return new Bytes30(Hex.decode(value));
            case "bytes31":
                return new Bytes31(Hex.decode(value));
            case "bytes32":
                return new Bytes32(Hex.decode(value));
            default:
                throw new UnsupportedOperationException("Unsupported type encountered: "
                        + type);
        }
    }

    public static Class<?> getType(String type) {
        switch (type.toLowerCase()) {
            case "address":
                return Address.class;
            case "bool":
                return Bool.class;
            case "string":
                return Utf8String.class;
            case "bytes":
                return DynamicBytes.class;
            case "uint8":
                return Uint8.class;
            case "int8":
                return Int8.class;
            case "uint16":
                return Uint16.class;
            case "int16":
                return Int16.class;
            case "uint24":
                return Uint24.class;
            case "int24":
                return Int24.class;
            case "uint32":
                return Uint32.class;
            case "int32":
                return Int32.class;
            case "uint40":
                return Uint40.class;
            case "int40":
                return Int40.class;
            case "uint48":
                return Uint48.class;
            case "int48":
                return Int48.class;
            case "uint56":
                return Uint56.class;
            case "int56":
                return Int56.class;
            case "uint64":
                return Uint64.class;
            case "int64":
                return Int64.class;
            case "uint72":
                return Uint72.class;
            case "int72":
                return Int72.class;
            case "uint80":
                return Uint80.class;
            case "int80":
                return Int80.class;
            case "uint88":
                return Uint88.class;
            case "int88":
                return Int88.class;
            case "uint96":
                return Uint96.class;
            case "int96":
                return Int96.class;
            case "uint104":
                return Uint104.class;
            case "int104":
                return Int104.class;
            case "uint112":
                return Uint112.class;
            case "int112":
                return Int112.class;
            case "uint120":
                return Uint120.class;
            case "int120":
                return Int120.class;
            case "uint128":
                return Uint128.class;
            case "int128":
                return Int128.class;
            case "uint136":
                return Uint136.class;
            case "int136":
                return Int136.class;
            case "uint144":
                return Uint144.class;
            case "int144":
                return Int144.class;
            case "uint152":
                return Uint152.class;
            case "int152":
                return Int152.class;
            case "uint160":
                return Uint160.class;
            case "int160":
                return Int160.class;
            case "uint168":
                return Uint168.class;
            case "int168":
                return Int168.class;
            case "uint176":
                return Uint176.class;
            case "int176":
                return Int176.class;
            case "uint184":
                return Uint184.class;
            case "int184":
                return Int184.class;
            case "uint192":
                return Uint192.class;
            case "int192":
                return Int192.class;
            case "uint200":
                return Uint200.class;
            case "int200":
                return Int200.class;
            case "uint208":
                return Uint208.class;
            case "int208":
                return Int208.class;
            case "uint216":
                return Uint216.class;
            case "int216":
                return Int216.class;
            case "uint224":
                return Uint224.class;
            case "int224":
                return Int224.class;
            case "uint232":
                return Uint232.class;
            case "int232":
                return Int232.class;
            case "uint240":
                return Uint240.class;
            case "int240":
                return Int240.class;
            case "uint248":
                return Uint248.class;
            case "int248":
                return Int248.class;
            case "uint256":
                return Uint256.class;
            case "int256":
                return Int256.class;
            case "bytes1":
                return Bytes1.class;
            case "bytes2":
                return Bytes2.class;
            case "bytes3":
                return Bytes3.class;
            case "bytes4":
                return Bytes4.class;
            case "bytes5":
                return Bytes5.class;
            case "bytes6":
                return Bytes6.class;
            case "bytes7":
                return Bytes7.class;
            case "bytes8":
                return Bytes8.class;
            case "bytes9":
                return Bytes9.class;
            case "bytes10":
                return Bytes10.class;
            case "bytes11":
                return Bytes11.class;
            case "bytes12":
                return Bytes12.class;
            case "bytes13":
                return Bytes13.class;
            case "bytes14":
                return Bytes14.class;
            case "bytes15":
                return Bytes15.class;
            case "bytes16":
                return Bytes16.class;
            case "bytes17":
                return Bytes17.class;
            case "bytes18":
                return Bytes18.class;
            case "bytes19":
                return Bytes19.class;
            case "bytes20":
                return Bytes20.class;
            case "bytes21":
                return Bytes21.class;
            case "bytes22":
                return Bytes22.class;
            case "bytes23":
                return Bytes23.class;
            case "bytes24":
                return Bytes24.class;
            case "bytes25":
                return Bytes25.class;
            case "bytes26":
                return Bytes26.class;
            case "bytes27":
                return Bytes27.class;
            case "bytes28":
                return Bytes28.class;
            case "bytes29":
                return Bytes29.class;
            case "bytes30":
                return Bytes30.class;
            case "bytes31":
                return Bytes31.class;
            case "bytes32":
                return Bytes32.class;
            default:
                throw new UnsupportedOperationException("Unsupported type encountered: "
                        + type);
        }
    }

    public static TypeReference<?> getTypeReference(String type, boolean indexed) {
        switch (type.toLowerCase()) {
            case "address":
                return new TypeReference<Address>(indexed) {
                };
            case "bool":
                return new TypeReference<Bool>(indexed) {
                };
            case "string":
                return new TypeReference<Utf8String>(indexed) {
                };
            case "bytes":
                return new TypeReference<DynamicBytes>(indexed) {
                };
            case "uint8":
                return new TypeReference<Uint8>(indexed) {
                };
            case "int8":
                return new TypeReference<Int8>(indexed) {
                };
            case "uint16":
                return new TypeReference<Uint16>(indexed) {
                };
            case "int16":
                return new TypeReference<Int16>(indexed) {
                };
            case "uint24":
                return new TypeReference<Uint24>(indexed) {
                };
            case "int24":
                return new TypeReference<Int24>(indexed) {
                };
            case "uint32":
                return new TypeReference<Uint32>(indexed) {
                };
            case "int32":
                return new TypeReference<Int32>(indexed) {
                };
            case "uint40":
                return new TypeReference<Uint40>(indexed) {
                };
            case "int40":
                return new TypeReference<Int40>(indexed) {
                };
            case "uint48":
                return new TypeReference<Uint48>(indexed) {
                };
            case "int48":
                return new TypeReference<Int48>(indexed) {
                };
            case "uint56":
                return new TypeReference<Uint56>(indexed) {
                };
            case "int56":
                return new TypeReference<Int56>(indexed) {
                };
            case "uint64":
                return new TypeReference<Uint64>(indexed) {
                };
            case "int64":
                return new TypeReference<Int64>(indexed) {
                };
            case "uint72":
                return new TypeReference<Uint72>(indexed) {
                };
            case "int72":
                return new TypeReference<Int72>(indexed) {
                };
            case "uint80":
                return new TypeReference<Uint80>(indexed) {
                };
            case "int80":
                return new TypeReference<Int80>(indexed) {
                };
            case "uint88":
                return new TypeReference<Uint88>(indexed) {
                };
            case "int88":
                return new TypeReference<Int88>(indexed) {
                };
            case "uint96":
                return new TypeReference<Uint96>(indexed) {
                };
            case "int96":
                return new TypeReference<Int96>(indexed) {
                };
            case "uint104":
                return new TypeReference<Uint104>(indexed) {
                };
            case "int104":
                return new TypeReference<Int104>(indexed) {
                };
            case "uint112":
                return new TypeReference<Uint112>(indexed) {
                };
            case "int112":
                return new TypeReference<Int112>(indexed) {
                };
            case "uint120":
                return new TypeReference<Uint120>(indexed) {
                };
            case "int120":
                return new TypeReference<Int120>(indexed) {
                };
            case "uint128":
                return new TypeReference<Uint128>(indexed) {
                };
            case "int128":
                return new TypeReference<Int128>(indexed) {
                };
            case "uint136":
                return new TypeReference<Uint136>(indexed) {
                };
            case "int136":
                return new TypeReference<Int136>(indexed) {
                };
            case "uint144":
                return new TypeReference<Uint144>(indexed) {
                };
            case "int144":
                return new TypeReference<Int144>(indexed) {
                };
            case "uint152":
                return new TypeReference<Uint152>(indexed) {
                };
            case "int152":
                return new TypeReference<Int152>(indexed) {
                };
            case "uint160":
                return new TypeReference<Uint160>(indexed) {
                };
            case "int160":
                return new TypeReference<Int160>(indexed) {
                };
            case "uint168":
                return new TypeReference<Uint168>(indexed) {
                };
            case "int168":
                return new TypeReference<Int168>(indexed) {
                };
            case "uint176":
                return new TypeReference<Uint176>(indexed) {
                };
            case "int176":
                return new TypeReference<Int176>(indexed) {
                };
            case "uint184":
                return new TypeReference<Uint184>(indexed) {
                };
            case "int184":
                return new TypeReference<Int184>(indexed) {
                };
            case "uint192":
                return new TypeReference<Uint192>(indexed) {
                };
            case "int192":
                return new TypeReference<Int192>(indexed) {
                };
            case "uint200":
                return new TypeReference<Uint200>(indexed) {
                };
            case "int200":
                return new TypeReference<Int200>(indexed) {
                };
            case "uint208":
                return new TypeReference<Uint208>(indexed) {
                };
            case "int208":
                return new TypeReference<Int208>(indexed) {
                };
            case "uint216":
                return new TypeReference<Uint216>(indexed) {
                };
            case "int216":
                return new TypeReference<Int216>(indexed) {
                };
            case "uint224":
                return new TypeReference<Uint224>(indexed) {
                };
            case "int224":
                return new TypeReference<Int224>(indexed) {
                };
            case "uint232":
                return new TypeReference<Uint232>(indexed) {
                };
            case "int232":
                return new TypeReference<Int232>(indexed) {
                };
            case "uint240":
                return new TypeReference<Uint240>(indexed) {
                };
            case "int240":
                return new TypeReference<Int240>(indexed) {
                };
            case "uint248":
                return new TypeReference<Uint248>(indexed) {
                };
            case "int248":
                return new TypeReference<Int248>(indexed) {
                };
            case "uint256":
                return new TypeReference<Uint256>(indexed) {
                };
            case "int256":
                return new TypeReference<Int256>(indexed) {
                };
            case "bytes1":
                return new TypeReference<Bytes1>(indexed) {
                };
            case "bytes2":
                return new TypeReference<Bytes2>(indexed) {
                };
            case "bytes3":
                return new TypeReference<Bytes3>(indexed) {
                };
            case "bytes4":
                return new TypeReference<Bytes4>(indexed) {
                };
            case "bytes5":
                return new TypeReference<Bytes5>(indexed) {
                };
            case "bytes6":
                return new TypeReference<Bytes6>(indexed) {
                };
            case "bytes7":
                return new TypeReference<Bytes7>(indexed) {
                };
            case "bytes8":
                return new TypeReference<Bytes8>(indexed) {
                };
            case "bytes9":
                return new TypeReference<Bytes9>(indexed) {
                };
            case "bytes10":
                return new TypeReference<Bytes10>(indexed) {
                };
            case "bytes11":
                return new TypeReference<Bytes11>(indexed) {
                };
            case "bytes12":
                return new TypeReference<Bytes12>(indexed) {
                };
            case "bytes13":
                return new TypeReference<Bytes13>(indexed) {
                };
            case "bytes14":
                return new TypeReference<Bytes14>(indexed) {
                };
            case "bytes15":
                return new TypeReference<Bytes15>(indexed) {
                };
            case "bytes16":
                return new TypeReference<Bytes16>(indexed) {
                };
            case "bytes17":
                return new TypeReference<Bytes17>(indexed) {
                };
            case "bytes18":
                return new TypeReference<Bytes18>(indexed) {
                };
            case "bytes19":
                return new TypeReference<Bytes19>(indexed) {
                };
            case "bytes20":
                return new TypeReference<Bytes20>(indexed) {
                };
            case "bytes21":
                return new TypeReference<Bytes21>(indexed) {
                };
            case "bytes22":
                return new TypeReference<Bytes22>(indexed) {
                };
            case "bytes23":
                return new TypeReference<Bytes23>(indexed) {
                };
            case "bytes24":
                return new TypeReference<Bytes24>(indexed) {
                };
            case "bytes25":
                return new TypeReference<Bytes25>(indexed) {
                };
            case "bytes26":
                return new TypeReference<Bytes26>(indexed) {
                };
            case "bytes27":
                return new TypeReference<Bytes27>(indexed) {
                };
            case "bytes28":
                return new TypeReference<Bytes28>(indexed) {
                };
            case "bytes29":
                return new TypeReference<Bytes29>(indexed) {
                };
            case "bytes30":
                return new TypeReference<Bytes30>(indexed) {
                };
            case "bytes31":
                return new TypeReference<Bytes31>(indexed) {
                };
            case "bytes32":
                return new TypeReference<Bytes32>(indexed) {
                };
            default:
                throw new UnsupportedOperationException("Unsupported type encountered: "
                        + type);
        }
    }
}
