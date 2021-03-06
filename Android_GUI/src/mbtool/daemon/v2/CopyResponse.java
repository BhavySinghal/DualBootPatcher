// automatically generated, do not modify

package mbtool.daemon.v2;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

public class CopyResponse extends Table {
  public static CopyResponse getRootAsCopyResponse(ByteBuffer _bb) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (new CopyResponse()).__init(_bb.getInt(_bb.position()) + _bb.position(), _bb); }
  public CopyResponse __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; return this; }

  public byte success() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public String errorMsg() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer errorMsgAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }

  public static int createCopyResponse(FlatBufferBuilder builder,
      byte success,
      int error_msg) {
    builder.startObject(2);
    CopyResponse.addErrorMsg(builder, error_msg);
    CopyResponse.addSuccess(builder, success);
    return CopyResponse.endCopyResponse(builder);
  }

  public static void startCopyResponse(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addSuccess(FlatBufferBuilder builder, byte success) { builder.addByte(0, success, 0); }
  public static void addErrorMsg(FlatBufferBuilder builder, int errorMsgOffset) { builder.addOffset(1, errorMsgOffset, 0); }
  public static int endCopyResponse(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
};

