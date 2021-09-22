package com.emily.infrastructure.rpc.core.server;

import com.emily.infrastructure.common.exception.PrintExceptionInfo;
import com.emily.infrastructure.rpc.core.decoder.MyMessageDecoder;
import com.emily.infrastructure.rpc.core.encoder.MyMessageEncoder;
import com.emily.infrastructure.rpc.core.server.annotation.RpcService;
import com.emily.infrastructure.rpc.core.server.handler.RpcServerHandler;
import com.emily.infrastructure.rpc.core.server.registry.RpcProviderRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @program: spring-parent
 * @description:
 * @author: Emily
 * @create: 2021/09/17
 */
public class RpcServer implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
    /**
     * RPC服务注册中心
     */
    private RpcProviderRegistry registry = new RpcProviderRegistry();
    /**
     * 端口号
     */
    private int port;

    public RpcServer(int port) {
        this.port = port;
    }

    /**
     * 启动netty服务端
     */
    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建服务端的启动对象，并使用链式编程来设置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //设置两个线程组
            serverBootstrap.group(bossGroup, workerGroup)
                    //使用NioServerSocketChannel 作为服务器的通道实现
                    .channel(NioServerSocketChannel.class)
                    //设置线程队列的连接个数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //设置一直保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    //设置一个通道测试对象
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //给pipeline设置通道处理器
                            ch.pipeline()
                                    .addLast(new MyMessageDecoder())
                                    .addLast(new MyMessageEncoder())
                                    .addLast(new RpcServerHandler(registry));
                        }
                    });
            //启动服务器，并绑定端口并且同步
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            //给 channelFuture 注册监听器，监听关心的事件,异步的时候使用
            channelFuture.addListener((future) -> {
                if (future.isSuccess()) {
                    System.out.println("监听端口成功。。。");
                } else {
                    System.out.println("监听端口失败。。。");
                }
            });
            logger.info("RPC服务器启动成功，端口号【{}】，开始提供服务...", port);
            //对关闭通道进行监听,监听到通道关闭后，往下执行
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error(PrintExceptionInfo.printErrorInfo(e));
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Map<String, Object> beanMap = context.getBeansWithAnnotation(RpcService.class);
        beanMap.forEach((beanName, bean) -> {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            String interfaceName = interfaces[0].getSimpleName();
            logger.info("find rpc service {}", interfaceName);

            //将@RpcService标注的bean注入到注册表当中
            registry.registerServiceBean(interfaceName, bean);
        });
    }
}
