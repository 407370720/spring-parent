package com.emily.infrastructure.rpc.core.client.pool;

import com.emily.infrastructure.common.enums.AppHttpStatus;
import com.emily.infrastructure.common.exception.BusinessException;
import com.emily.infrastructure.common.exception.PrintExceptionInfo;
import com.emily.infrastructure.common.utils.json.JSONUtils;
import com.emily.infrastructure.rpc.core.client.ClientResource;
import com.emily.infrastructure.rpc.core.client.RpcClient;
import com.emily.infrastructure.rpc.core.client.channel.RpcClientChannelInitializer;
import com.emily.infrastructure.rpc.core.client.handler.BaseClientHandler;
import com.emily.infrastructure.rpc.core.client.handler.RpcClientChannelHandler;
import com.emily.infrastructure.rpc.core.protocol.RpcRequest;
import com.emily.infrastructure.rpc.core.protocol.RpcResponse;
import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @program: spring-parent
 * @description: 创建Netty客户端及自定义处理器
 * @author: Emily
 * @create: 2021/09/17
 */
public class SocketConn extends Conn<Channel> {

    private static final Logger logger = LoggerFactory.getLogger(SocketConn.class);
    /**
     * 线程工作组
     */
    private static final NioEventLoopGroup GROUP = new NioEventLoopGroup();
    /**
     * 创建客户端的启动对象 bootstrap ，不是 serverBootStrap
     */
    private static final Bootstrap BOOTSTRAP = new Bootstrap();

    private String host;
    private int port;

    public SocketConn(String host, int port) {
        this.host = host;
        this.port = port;
    }

    static {
        //设置线程组
        BOOTSTRAP.group(GROUP);
        //初始化通道
        BOOTSTRAP.channel(NioSocketChannel.class);
    }

    public boolean createConn() {
        try {
            BaseClientHandler handler = new RpcClientChannelHandler();
            //加入自己的处理器
            BOOTSTRAP.handler(new RpcClientChannelInitializer(handler));
            logger.info("客户端连接成功...");
            //连接服务器
            Channel channel = BOOTSTRAP.connect(host, port).sync().channel();
            ClientResource.handlerMap.put(channel.id().asLongText(), handler);
            this.conn = channel;
            if (canUse()) {
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            logger.error(PrintExceptionInfo.printErrorInfo(e));
            throw new BusinessException(AppHttpStatus.EXCEPTION.getStatus(), "创建连接失败");
        }
    }

    /**
     * Socket连接是否可用
     *
     * @return
     */
    public boolean canUse() {
        return null != this.conn && this.conn.isActive() && this.conn.isWritable();
    }

    public void close() {
        this.conn.close();
    }
}
