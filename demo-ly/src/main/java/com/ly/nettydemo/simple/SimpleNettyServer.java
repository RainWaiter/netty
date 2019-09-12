package com.ly.nettydemo.simple;

import com.ly.nettydemo.util.LogUtilDemo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 每个 SocketChannel 产生一个 DefaultChannelPipeline
 *    一个 DefaultChannelPipeline 内部维护 ChannelHandlerContext 的链表
 *      DefaultChannelPipeline 内部的head节点连接来连接这些 ChannelHandlerContext (保持next节点)
 *        一个 ChannelHandlerContext 内部保持一个 ChannelHandler (通过handler()方法得到)
 *
 */
public class SimpleNettyServer {

    public static void main(String args[]) throws Exception {

        EventLoopGroup boos = new NioEventLoopGroup(1);
        LogUtilDemo.log("boos=>" + boos);
        EventLoopGroup work = new NioEventLoopGroup(2);
        LogUtilDemo.log("work=>" + work);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boos, work);
        bootstrap.channel(NioServerSocketChannel.class)
                 .childHandler(new DemoServerChannelInitializer())
                 .option(ChannelOption.SO_BACKLOG, 1024)
                 .childOption(ChannelOption.SO_KEEPALIVE, false)
                 ;

        ChannelFuture f = bootstrap.bind(8888).sync();
        System.out.println("sever on :" + 8888);
        f.channel().closeFuture().sync();

        boos.shutdownGracefully();
        work.shutdownGracefully();
    }

    /**
     * 用于初始化
     * 通常为 NioSocketChannel 的管道链Pipeline中加入若干ChannelHandler
     *
     * 如果对于连接建立、读、写等处理加入了多个Handler，只有第1个Handler能够生效。
     *
     */
    static class DemoServerChannelInitializer extends ChannelInitializer<NioSocketChannel> {
        @Override
        protected void initChannel(NioSocketChannel ch) throws Exception {

            // 鉴权
            ch.pipeline().addLast(new DemoAuthHandler());

            // decoder
            ch.pipeline().addLast(new DemoAsciiToUpperDecoder());

            // inbound
            ch.pipeline().addLast(new DemoServerChannelHandlerB());
            ch.pipeline().addLast(new DemoServerChannelHandlerA()); // 会被忽略

            // outbound
            ch.pipeline().addLast(new DemoServerChannelHandlerC());
        }
    }


    static class DemoAuthHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LogUtilDemo.log("channelRead start", this);

            ByteBuf buf = (ByteBuf) msg;
            buf.markReaderIndex();
            byte[] b = new byte[buf.readableBytes()];
            buf.readBytes(b);

            String str = new String(b);
            boolean authSuccess = "Y".equalsIgnoreCase(str);

            LogUtilDemo.log("auth result=> " + authSuccess, this);

            if( authSuccess ) {
                // 验证成功
                // 该连接无需再认证，将此handler从流水线中删除
                ctx.pipeline().remove(this);

                // 复原buffer，让后面的Handler去处理
                buf.resetReaderIndex();
                ctx.fireChannelRead(buf);
            } else {
                // 验证失败
                ctx.close();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("channelReadComplete success", this);
        }
    }

    /**
     * 简单的Ascii字符转大写（就是那些字母转大写）
     */
    static class DemoAsciiToUpperDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            LogUtilDemo.log("decode start", this);
            int readBytes = in.readableBytes();
            if( readBytes <= 0 ) {
                return;
            }
            byte[] temp = new byte[readBytes];
            in.readBytes(temp);
            for( int i = 0; i < temp.length; i++ ) {
                // 65=>A  ...  90=>Z
                // 97=>a  ... 122=>b
                byte b = temp[i];
                if( b >= 97 && b <= 122) {
                    temp[i] = (byte) ( b - 32 );
                }
            }
            // 将大写字母的buffer放入out
            out.add(Unpooled.copiedBuffer(temp));
        }
    }

    static class DemoServerChannelHandlerA extends SimpleChannelInboundHandler<Object> {
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("channelReadComplete success", this);
            ctx.flush();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("conn success", this);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("conn close", this);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LogUtilDemo.log("channelRead", this);
            super.channelRead(ctx, msg);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf readBuf = (ByteBuf) msg;
            byte[] readBytes = new byte[readBuf.readableBytes()];
            readBuf.getBytes(0, readBytes);

            LogUtilDemo.log("accept msg : " + new String(readBytes), this);

            ctx.write(Unpooled.copiedBuffer(readBytes));
        }
    }

    static class DemoServerChannelHandlerB extends SimpleChannelInboundHandler<Object> {
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("channelReadComplete success", this);
            ctx.flush();
            //ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("conn success", this);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("conn close", this);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LogUtilDemo.log("channelRead", this);

            /*
            * 1.
            * 由于继承自带泛型的 SimpleChannelInboundHandler
            * super.channelRead(ctx, msg)方法内部可能会调用 channelRead0()方法
            * 如果调用了channelRead0()，则不需要进行Buffer的释放(buffer.realase())。
            * 适合同步处理buffer
            * 具体参考代码：public void channelRead(ChannelHandlerContext ctx, Object msg)
            *
            * 2.
            * 如果继承自：ChannelInboundHandlerAdapter
            * 则read操作只能调用：channelRead()
            * 此时就必须要进行Buffer的释放了(buffer.realase()).
            * 适合异步处理buffer
            *
            * */

            // 方式1
            // 调用super的方法，然后进入 channelRead0()
            super.channelRead(ctx, msg);

            // 方式2
            // 不调用super的方法，直接处理
            // 然后手工将buffer进行释放
//            ByteBuf readBuf = (ByteBuf) msg;
//            dealData(ctx, readBuf);
//            readBuf.release();
        }
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            LogUtilDemo.log("channelRead0", this);
            dealData(ctx, msg);

            // 无需手工释放buffr
            //readBuf.release();
        }
        private void dealData(ChannelHandlerContext ctx, Object msg) {
            ByteBuf readBuf = (ByteBuf) msg;
            byte[] readBytes = new byte[readBuf.readableBytes()];
            readBuf.getBytes(0, readBytes);

            LogUtilDemo.log("readAndWrite : " + new String(readBytes), this);

            ctx.write(Unpooled.copiedBuffer(readBytes));
        }
    }

    static class DemoServerChannelHandlerC extends ChannelOutboundHandlerAdapter {

        @Override
        public void read(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("read", this);
            super.read(ctx);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            LogUtilDemo.log("write", this);
            ctx.writeAndFlush(msg);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            LogUtilDemo.log("flush", this);
            ctx.flush();
        }
    }

}
