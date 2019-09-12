## Netty的异步结果

Netty中的所有IO操作都是异步执行，也就是访问IO操作都会立即返回，执行的结果需要Future对象去获取。

例如:

<li> 调用Future的await()方法来阻塞等待结果。
````
ChannelFuture future = ctx.channel().close();
future.await();
// close完成后的回调
````
这里有一个“死锁”问题： 
如果当前代码操作是在一个I/O线程中进行的，则会发生，I/O线程会一直等待close()结果完成，但是close()的真实操作需要I/O线程去完成。
另外，我们的Handler处理代码通常是由一个I/O线程执行的，所以发生死锁的情况很普遍。

另外比较幸运的是：
Netty会检测这种死锁的情况，一旦发现在I/O线程中执行这样的阻塞代码（await()），就抛出：BlockingOperationException异常。

例如DefaultPromise的await()代码如下:
(意思就是当前处理线程为EventLoop中的线程，则抛异常)
````
EventExecutor e = executor();
if (e != null && e.inEventLoop()) {
    throw new BlockingOperationException(toString());
}
````

<li> 优选方案：通过为Future新增一个Listener来异步获取结果：
````
ChannelFuture future = ctx.channel().close();
future.addListener(new ChannelFutureListener() {
    public void operationComplete(ChannelFuture future) {
        // close完成后的回调
    }
});
````


### ChannelFuture

ChannelFuture 代表一个异步IO的操作结果

异步结果分为2大类：
<li> Uncompleted 未完成，代表刚刚创建ChannelFuture
<li> Completed 已完成，代表：成功、失败、已取消 3 种情况

异步结果的状态图
````
                                        +---------------------------+
                                        | Completed successfully    |
                                        +---------------------------+
                                   +---->      isDone() = true      |
   +--------------------------+    |    |   isSuccess() = true      |
   |        Uncompleted       |    |    +===========================+
   +--------------------------+    |    | Completed with failure    |
   |      isDone() = false    |    |    +---------------------------+
   |   isSuccess() = false    |----+---->      isDone() = true      |
   | isCancelled() = false    |    |    |       cause() = non-null  |
   |       cause() = null     |    |    +===========================+
   +--------------------------+    |    | Completed by cancellation |
                                   |    +---------------------------+
                                   +---->      isDone() = true      |
                                        | isCancelled() = true      |
                                        +---------------------------+
````