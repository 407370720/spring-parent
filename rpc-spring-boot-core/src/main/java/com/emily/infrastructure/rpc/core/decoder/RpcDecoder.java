package com.emily.infrastructure.rpc.core.decoder;

import com.emily.infrastructure.common.utils.json.JSONUtils;
import com.emily.infrastructure.rpc.core.protocol.RpcRequest;
import com.emily.infrastructure.rpc.core.protocol.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @program: spring-parent
 * @description:
 * @author: Emily
 * @create: 2021/09/23
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private Class<?> clazz;

    public RpcDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        // 消息的长度
        int length = byteBuf.readInt();
        if(length == 0){
            return;
        }
        //读取数据
        byte[] data = new byte[length];
        byteBuf.readBytes(data);

        if (clazz == RpcRequest.class) {
            RpcRequest request = (RpcRequest) JSONUtils.toObject(data, clazz);
            list.add(request);
        } else if (clazz == RpcResponse.class) {
            RpcResponse response = (RpcResponse) JSONUtils.toObject(data, clazz);
            list.add(response);
        }
        //重置readerIndex和writerIndex为0
        //byteBuf.discardReadBytes();
        byteBuf.clear();
    }
}
