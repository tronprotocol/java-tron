package org.tron.core.net;

import com.google.protobuf.Message;
import org.tron.protos.Protocol.DisconnectMessageOrBuilder;

public class DisconnectMessageTest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:protocol.DisconnectMessage)
    DisconnectMessageOrBuilder {

  public static final int REASON_FIELD_NUMBER = 1;
  public static final int NAME_FIELD_NUMBER = 2;
  private static final long serialVersionUID = 0L;
  // @@protoc_insertion_point(class_scope:protocol.DisconnectMessage)
  private static final DisconnectMessageTest DEFAULT_INSTANCE;
  private static final com.google.protobuf.Parser<DisconnectMessageTest> PARSER =
      new com.google.protobuf.AbstractParser<DisconnectMessageTest>() {
        public DisconnectMessageTest parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new DisconnectMessageTest(input, extensionRegistry);
        }
      };

  static {
    DEFAULT_INSTANCE = new DisconnectMessageTest();
  }

  private int reason_;
  private int name_;
  private byte memoizedIsInitialized = -1;
  // Use DisconnectMessage.newBuilder() to construct.

  private DisconnectMessageTest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  public DisconnectMessageTest() {
    reason_ = 4;
    name_ = 2;
  }


  private DisconnectMessageTest(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet
        .newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            done = true;
            break;
          }
          case 8: {
            int rawValue = input.readEnum();

            reason_ = rawValue;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return null;
  }

  public static DisconnectMessageTest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  public static com.google.protobuf.Parser<DisconnectMessageTest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return null;
  }

  /**
   * <code>.protocol.ReasonCode reason = 1;</code>
   */
  public int getReasonValue() {
    return reason_;
  }

  public int getNameValue() {
    return name_;
  }

  /**
   * <code>.protocol.ReasonCode reason = 1;</code>
   */
  public org.tron.protos.Protocol.ReasonCode getReason() {
    org.tron.protos.Protocol.ReasonCode result = org.tron.protos.Protocol.ReasonCode
        .valueOf(reason_);
    return result == null ? org.tron.protos.Protocol.ReasonCode.UNRECOGNIZED : result;
  }

  public org.tron.protos.Protocol.ReasonCode getName() {
    org.tron.protos.Protocol.ReasonCode result = org.tron.protos.Protocol.ReasonCode.valueOf(name_);
    return result == null ? org.tron.protos.Protocol.ReasonCode.UNRECOGNIZED : result;
  }

  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) {
      return true;
    }
    if (isInitialized == 0) {
      return false;
    }

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (reason_ != org.tron.protos.Protocol.ReasonCode.REQUESTED.getNumber()) {
      output.writeEnum(1, reason_);
    }
    if (name_ != org.tron.protos.Protocol.ReasonCode.REQUESTED.getNumber()) {
      output.writeEnum(2, name_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) {
      return size;
    }

    size = 0;
    if (reason_ != org.tron.protos.Protocol.ReasonCode.REQUESTED.getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, reason_);
    }
    if (name_ != org.tron.protos.Protocol.ReasonCode.REQUESTED.getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, name_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @Override
  protected Message.Builder newBuilderForType(BuilderParent builderParent) {
    return null;
  }

  @Override
  public Message.Builder newBuilderForType() {
    return null;
  }

  @Override
  public Message.Builder toBuilder() {
    return null;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<DisconnectMessageTest> getParserForType() {
    return PARSER;
  }

  public DisconnectMessageTest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
