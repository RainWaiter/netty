## Netty的消息大致处理流程
````
+------------------------------------------------------------------------------------------+
| NioEventLoop.processSelectedKey() -> unsafe.read()
| --- AbstractNioByteChannel$NioByteUnsafe.read() -> pipeline.fireChannelRead(byteBuf)
| ------ DefaultChannelPipeline.fireChannelRead(Object msg)
| --------- AbstractChannelHandlerContext.invokeChannelRead(head, msg) -> head.invokeChannelRead(Object msg)  ->  (handler()).channelRead(this, msg)
| ------------ head.channelRead(ChannelHandlerContext ctx, Object msg) -> ctx.fireChannelRead(msg)
| --------------- head.fireChannelRead(final Object msg)
| --------------- AbstractChannelHandlerContext.fireChannelRead(final Object msg)
| ------------------ AbstractChannelHandlerContext next = findContextInbound(int mask)    # 寻找当前Context下一个Context节点
| ------------------ invokeChannelRead(final AbstractChannelHandlerContext next, Object msg)
| --------------------- next.invokeChannelRead(Object msg)
| ------------------------ (handler()).channelRead(this, msg)   # 具体的Handler的channelRead方法
+------------------------------------------------------------------------------k------------+
````

## ChannelHandlerContext 处理链条

### 入站消息(Inbound)

````
+----------------------------------------+
| DefaultChannelPipeline$HeadContext
|   ↓
| ChannelHandler-1
|   ↓
| ChannelHandler-2
|   ↓
| ......
|   ↓
| DefaultChannelPipeline$TailContext
+----------------------------------------------+
````
涉及方法：
<li> fireChannelRegistered()
<li> fireChannelActive()
<li> fireChannelRead()
<li> fireChannelReadComplete()
<li> fireExceptionCaught()
<li> fireChannelWritabilityChanneld()
<li> fireChannelInactive()
DefaultChannelPipeline中的fireXX方法都是Inbound类型的事件，通常由HeadContext首先处理

### 出站消息(Outbound)
````
+----------------------------------------+
| DefaultChannelPipeline$HeadContext
|   ↑
| ChannelHandler-1
|   ↑
| ChannelHandler-2
|   ↑
| ......
|   ↑
| DefaultChannelPipeline$TailContext
+----------------------------------------------+
````
涉及方法：
bind()
connect()
write()
flush()
read()  --- 这个方法待查
disconnect()
close()

