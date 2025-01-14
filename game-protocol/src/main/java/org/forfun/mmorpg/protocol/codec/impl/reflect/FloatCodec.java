package org.forfun.mmorpg.protocol.codec.impl.reflect;

import java.nio.ByteBuffer;

public class FloatCodec extends Codec {


	@Override
	public Float decode(ByteBuffer in, Class<?> type, Class<?> wrapper) {
		return ByteBuffUtil.readFloat(in);
	}

	@Override
	public void encode(ByteBuffer out, Object value, Class<?> wrapper) {
		ByteBuffUtil.writeFloat(out, (float)value);
	}
}
