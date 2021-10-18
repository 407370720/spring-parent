package com.emily.infrastructure.rpc.client.pool;

import com.emily.infrastructure.common.enums.AppHttpStatus;
import com.emily.infrastructure.common.exception.BaseException;
import com.emily.infrastructure.common.exception.PrintExceptionInfo;
import com.emily.infrastructure.common.utils.json.JSONUtils;
import com.emily.infrastructure.rpc.client.IRpcClientProperties;
import com.emily.infrastructure.rpc.client.channel.IRpcClientChannelInitializer;
import com.emily.infrastructure.rpc.client.handler.BaseClientHandler;
import com.emily.infrastructure.rpc.client.handler.IRpcClientChannelHandler;
import com.emily.infrastructure.rpc.core.entity.message.IRMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: spring-parent
 * @description: 创建Netty客户端及自定义处理器
 * @author: Emily
 * @create: 2021/09/17
 */
public class IRpcConnection extends AbstractConnection<Channel> {

    private static final Logger logger = LoggerFactory.getLogger(IRpcConnection.class);
    /**
     * 线程工作组
     */
    private static final EventLoopGroup GROUP = new NioEventLoopGroup();
    /**
     * 创建客户端的启动对象 bootstrap ，不是 serverBootStrap
     */
    private static final Bootstrap BOOTSTRAP = new Bootstrap();
    /**
     * 处理器
     */
    private BaseClientHandler handler;

    private IRpcClientProperties properties;

    public IRpcConnection(IRpcClientProperties properties) {
        this.properties = properties;
    }

    static {
        //设置线程组
        BOOTSTRAP.group(GROUP);
        //初始化通道
        BOOTSTRAP.channel(NioSocketChannel.class)
                /**
                 * 是否启用心跳保活机制。在双方TCP套接字建立连接后（即都进入ESTABLISHED状态）并且在两个小时左右上层没有任何数据传输的情况下，
                 * 这套机制才会被激活
                 */
                .option(ChannelOption.SO_KEEPALIVE, true)
                /**
                 * 1.在TCP/IP协议中，无论发送多少数据，总是要在数据前面加上协议头，同时，对方接收到数据，也需要发送ACK表示确认。
                 * 为了尽可能的利用网络带宽，TCP总是希望尽可能的发送足够大的数据。这里就涉及到一个名为Nagle的算法，该算法的目的就是为了尽可能发送大块数据，
                 * 避免网络中充斥着许多小数据块。
                 * 2.TCP_NODELAY就是用于启用或关于Nagle算法。如果要求高实时性，有数据发送时就马上发送，就将该选项设置为true关闭Nagle算法；
                 * 如果要减少发送次数减少网络交互，就设置为false等累积一定大小后再发送。默认为false。
                 */
                .option(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public boolean connect() {
        try {
            handler = new IRpcClientChannelHandler();
            //加入自己的处理器
            BOOTSTRAP.handler(new IRpcClientChannelInitializer(handler));
            //连接服务器
            ChannelFuture channelFuture = BOOTSTRAP.connect(properties.getHost(), properties.getPort()).sync();
            channelFuture.addListener(listener -> {
                if (listener.isSuccess()) {
                    logger.info("connect success...");
                } else {
                    logger.info("RPC客户端重连接...");
                }
            });
            //获取channel
            Channel channel = channelFuture.channel();
            //将通道赋值给连接对象
            this.setConnection(channel);
            //判定通道是否可用
            if (this.isAvailable()) {
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            logger.error(PrintExceptionInfo.printErrorInfo(e));
            throw new BaseException(AppHttpStatus.EXCEPTION.getStatus(), "创建连接失败");
        }
    }

    /**
     * 发送请求
     *
     * @param message
     */
    @Override
    public Object sendRequest(IRMessage message) {
        logger.info("RPC请求数据：{}  ", JSONUtils.toJSONString(message));
        try {
            synchronized (handler.object) {
                //发送Rpc请求
                this.getConnection().writeAndFlush(message);
                //释放当前线程资源，并等待指定超时时间，默认：60S
                handler.object.wait(message.getHead().getKeepAlive() * 1000);
            }
            return handler.response;
        } catch (Exception exception) {
            throw new BaseException(AppHttpStatus.EXCEPTION.getStatus(), PrintExceptionInfo.printErrorInfo(exception));
        }
    }

    /**
     * Socket连接是否可用
     *
     * @return
     */
    @Override
    public boolean isAvailable() {
        return null != this.getConnection() && this.getConnection().isActive() && this.getConnection().isWritable();
    }

    /**
     * 关闭连接通道
     */
    @Override
    public void close() {
        this.getConnection().close();
    }
}
